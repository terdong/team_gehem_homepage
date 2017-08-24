package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.model.{BoardInfo, BoardPaginationInfo, BoardSearchInfo, MemberInfo}
import com.teamgehem.security.AuthenticatedActionBuilder
import models.{Member, Post}
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.data.Form
import play.api.data.Forms.{seq, _}
import play.api.i18n.Messages
import play.api.mvc._
import repositories._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.io.Path
import scala.util.Try

/**
  * Created by DongHee Kim on 2017-08-12 012.
  */

@Singleton
class PostController @Inject()(cache: SyncCacheApi,
                               config: Configuration,
                               auth: AuthenticatedActionBuilder,
                               cc: MessagesControllerComponents,
                               members_repo: MembersRepository,
                               boards_repo: BoardsRepository,
                               posts_repo: PostsRepository,
                               attachments_repo: AttachmentsRepository,
                               comments_repo: CommentsRepository)
//cache: AsyncCacheApi)
  extends MessagesAbstractController(cc) {

  lazy val attachment_path = config.get[String]("uploads.path")
  lazy val page_length = config.get[Int]("post.pageLength")
  lazy val temp_dir_path: String = System.getProperty("java.io.tmpdir")

  implicit def getBoardInfo: Option[Seq[BoardInfo]] = cache.get[Seq[BoardInfo]]("board.list")

  def getPermission(implicit request: MessagesRequest[AnyContent]) = request.session.get("permission").getOrElse("0").toByte

  def getSeq(implicit request: MessagesRequest[AnyContent]) = request.session.get("seq")

  def getEmail(implicit request: MessagesRequest[AnyContent]) = request.session.get("email")

  implicit def getMemberInfo(implicit request: MessagesRequest[AnyContent]) = {
    for {
      email <- getEmail
      permission <- request.session.get("permission")
      seq <- getSeq
    } yield {
      MemberInfo(email, permission.toByte, seq.toLong)
    }
  }

  def list(board_seq: Long, page: Int) = Action.async { implicit request =>
    val permission = getPermission
    val a_route: (Int) => Call = routes.PostController.list(board_seq, _)
    board_seq match {
      case 0 => {
        for {
          posts <- posts_repo.all(page, page_length, permission)
          count <- posts_repo.getAllPostCount(permission)
        } yield {
          Ok(views.html.post.list("all", posts, BoardPaginationInfo(page, page_length, count), route = a_route))
        }
      }
      case _ => {
        for {
          b <- boards_repo.isListValidBoard(board_seq, permission)
          if b == true
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts <- posts_repo.listByBoard(board_seq, page, page_length)
          count <- posts_repo.getPostCount(board_seq)
        } yield
          Ok(views.html.post.list(board_name, posts, BoardPaginationInfo(page, page_length, count, board_seq), route = a_route))
      }
    }
  }

  def searchAll(page: Int, type_number: Int, word: String) = Action.async { implicit request =>
    val permission = getPermission
    for {
      posts <- posts_repo.searchAll(page,
        page_length,
        permission,
        BoardSearchInfo(type_number, word))
      count <- posts_repo.getSearchAllPostCount(permission, BoardSearchInfo(type_number, word))
    } yield
      Ok(
        views.html.post
          .list(
            "all",
            posts,
            BoardPaginationInfo(page, page_length, count),
            Option(BoardSearchInfo(type_number, word)),
            routes.PostController.searchAll(_, type_number, word)
          )
      )
  }

  def search(board_seq: Long, page: Int, type_number: Int, word: String) = Action.async { implicit request =>
    val permission = getPermission
    for {
      b <- boards_repo.isListValidBoard(board_seq, permission)
      if b == true
      board_name <- boards_repo.getNameBySeq(board_seq)
      posts <- posts_repo.search(board_seq,
        page,
        page_length,
        BoardSearchInfo(type_number, word))
      count <- posts_repo.getSearchPostCount(board_seq, BoardSearchInfo(type_number, word))
    } yield
      Ok(
        views.html.post
          .list(
            board_name,
            posts,
            BoardPaginationInfo(page, page_length, count, board_seq),
            Option(BoardSearchInfo(type_number, word)),
            routes.PostController.search(board_seq, _, type_number, word)
          )
      )
  }

  def read(board_seq: Long, post_seq: Long, page: Int) = Action.async {
    implicit request =>
      for {
        r <- boards_repo.isReadValidBoard(board_seq, getPermission)
        if r == true
        tuple <- posts_repo.getPost(board_seq, post_seq)
        comments <- comments_repo.allWithMemberName(post_seq)
        board_name <- boards_repo.getNameBySeq(board_seq)
        posts <- posts_repo.listByBoard(board_seq, page, page_length)
        count <- posts_repo.getPostCount(board_seq)
        attachments <- attachments_repo.getAttachments(post_seq)
      } yield {
        posts_repo.updateHitCount(tuple._1)
        val is_own = getSeq.map(_ == tuple._2.seq).getOrElse(false)
        Ok(
          views.html.post.read(tuple, comment_form, comments, attachments, is_own)(
            board_name,
            posts,
            page,
            page_length,
            count,
            board_seq))
          .withCookies(Cookie("page", page.toString))
      }
  }

  def writePostForm(board_seq: Long) = auth.async { implicit request =>
    if (board_seq > 0) {
      boards_repo.getBoard(board_seq).map { board =>
        Ok(views.html.post.write(post_form, board, file_form))
      }
    } else {
      boards_repo
        .getBoardInfoForWrite(request.member.permission)
        .map(seq => Ok(views.html.post.write_all(post_form, seq)))
    }
  }


  def writePost = auth.async { implicit request =>
    val form = isWriteValidBoardForm.bindFromRequest
    form.fold(
      hasErrors => {
        boards_repo.getBoard(form.get._1).map { board =>
          BadRequest(views.html.post.write(hasErrors, board, file_form))
        }
      },
      form => {
        getSeq.map { seq =>
          posts_repo.insert(form, seq.toLong, request.remoteAddress).map { post =>
            Redirect(routes.PostController.read(post.board_seq, post.seq, 1))
          }
        }.getOrElse(Future.successful(InternalServerError))
      }
    )
  }

  def editPostForm(board_seq: Long, post_seq: Long) = auth.async { implicit request =>
    for {
      r <- posts_repo.isOwnPost(post_seq, request.session.get("seq").getOrElse("0").toLong) if r == true
      tuple: (Post, Member) <- posts_repo.getPost(board_seq, post_seq)
    } yield {
      val post = tuple._1
      val form_data =
        (board_seq, post.subject, post.content.getOrElse(""), Nil, Nil)
      Ok(
        views.html.post.edit(post_form.fill(form_data), board_seq, post_seq))
    }
  }

  def editPost(board_seq: Long, post_seq: Long) = auth.async { implicit request =>
    val form = post_form.bindFromRequest
    form.fold(
      hasErrors =>
        Future.successful(
          BadRequest(views.html.post.edit(hasErrors, board_seq, post_seq))),
      form => {
        posts_repo.update(form, post_seq) map {
          _ =>
            val page =
              request.cookies
                .get("page")
                .getOrElse(Cookie("page", "1"))
                .value
                .toInt

            Redirect(routes.PostController.read(board_seq, post_seq, page))
        }
      }
    )
  }

  def deletePost(board_seq: Long, post_seq: Long) = auth.async { implicit request =>
    attachments_repo.getAttachments(post_seq).map { attachments =>
      for (attachment <- attachments) {
        val path =
          Path(s"${attachment_path}/${attachment.sub_path}/${attachment.hash}")
        Try(path.delete)
      }
      attachments_repo.deleteAttachements(post_seq)
    }
    posts_repo.delete(post_seq).map { _ =>
      Redirect(routes.PostController.list(board_seq, 1))
    }
  }

  def writeComment(board_seq: Long) = auth.async { implicit request =>
    val form = comment_form.bindFromRequest
    form.fold(
      hasErrors => {
        val post_seq = hasErrors.get._1
        val page =
          request.cookies
            .get("page")
            .getOrElse(Cookie("page", "1"))
            .value
            .toInt
        for {
          r <- boards_repo.isReadValidBoard(board_seq, getPermission)
          if r == true
          tuple <- posts_repo.getPost(board_seq, post_seq)
          comments <- comments_repo.allWithMemberName(post_seq)
          name_op <- boards_repo.getNameBySeq(board_seq)
          posts <- posts_repo.listByBoard(board_seq, page, page_length)
          count <- posts_repo.getPostCount(board_seq)
          attachments <- attachments_repo.getAttachments(post_seq)
        } yield {
          posts_repo.updateHitCount(tuple._1)
          val is_own = getSeq.map {
            _ == tuple._2.seq
          }.getOrElse(false)

          BadRequest(
            views.html.post
              .read(tuple, hasErrors, comments, attachments, is_own)(
                name_op,
                posts,
                page,
                page_length,
                count,
                board_seq))
        }
      },
      form => {
        getSeq.map { seq =>
          comments_repo
            .insert(form, seq.toLong, request.remoteAddress)
            .map { post_seq =>
              val page =
                request.cookies
                  .get("page")
                  .getOrElse(Cookie("page", "1"))
                  .value
                  .toInt
              Redirect(routes.PostController.read(board_seq, post_seq, page))
            }
        }.getOrElse(Future.successful(InternalServerError))
      }
    )
  }

  def deleteComment(board_seq: Long, post_seq: Long, comment_seq: Long) = auth.async { implicit request =>
    val page = request.cookies
      .get("page")
      .getOrElse(Cookie("page", "1"))
      .value
      .toInt

    getSeq.map { seq =>
      comments_repo.delete(comment_seq, seq.toLong).map { _ =>
        Redirect(routes.PostController.read(board_seq, post_seq, page))
      }
    }.getOrElse(Future.successful(InternalServerError))
  }


  private def isWriteValidBoard(board_seq: Long, permission: Byte)(
    implicit request: Request[AnyContent]): Boolean = {
    Await.result(boards_repo.isWriteValidBoard(board_seq, permission),
      Duration.Inf)
  }

  private def isWriteValidBoardForm(implicit request: MessagesRequest[AnyContent]) = Form(
    tuple(
      "board_seq" -> longNumber.verifying(
        Messages("post.write.permission.error"),
        isWriteValidBoard(_, getPermission)),
      "subject" -> nonEmptyText(maxLength = 80),
      "content" -> text,
      "attachments" -> seq(longNumber),
      "upload_files" -> seq(text)
    )
  )

  private val post_form = Form(
    tuple(
      "board_seq" -> longNumber,
      "subject" -> nonEmptyText(maxLength = 80),
      "content" -> text,
      "attachments" -> seq(longNumber),
      "upload_files" -> seq(text)
    )
  )

  case class FormData(name: String)

  private val file_form = Form(
    mapping(
      "name" -> text
    )(FormData.apply)(FormData.unapply)
  )

  private val comment_form = Form(
    tuple(
      "post_seq" -> longNumber,
      "reply_comment_seq" -> optional(longNumber),
      "content" -> nonEmptyText(maxLength = 4000)
    )
  )
}
