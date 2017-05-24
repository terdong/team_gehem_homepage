package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.authentication.{Authenticated, CustomAuthenticatedRequest}
import controllers.traits.ProvidesHeader
import models.{Member, Post}
import org.apache.commons.codec.digest.DigestUtils
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms.{tuple, _}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import repositories._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.io.Path
import scala.util.Try

/**
  * Created by terdo on 2017-04-22 022.
  */
@Singleton
class PostController @Inject()(implicit cache: CacheApi,
                               config: Configuration,
                               members_repo: MembersRepository,
                               boards_repo: BoardsRepository,
                               posts_repo: PostsRepository,
                               attachments_repo: AttachmentsRepository,
                               comments_repo: CommentsRepository,
                               val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport
    with ProvidesHeader {
  lazy val images_path = config.getString("uploads.path.images").get

  lazy val page_length = config.getInt("post.pageLength").getOrElse(15)

  def listAll(page: Int) = Action.async { implicit request =>
    for {
      posts <- posts_repo.all(page, page_length, member_permission)
      count <- posts_repo.getPostCount()
    } yield Ok(views.html.post.list(None, posts, page, page_length, count))
  }

  def list(board_seq: Long, page: Int) = Action.async { implicit request =>
    for {
      b <- boards_repo.isListValidBoard(board_seq, member_permission)
      if b == true
      name_op <- boards_repo.getNameBySeq(board_seq)
      posts <- posts_repo.listByBoard(board_seq, page, page_length)
      count <- posts_repo.getPostCount(board_seq)
    } yield
      Ok(
        views.html.post
          .list(name_op, posts, page, page_length, count, board_seq))
  }

  def showPost(board_seq: Long, post_seq: Long, page: Int) = Action.async {
    implicit request =>
      for {
        r <- boards_repo.isReadValidBoard(board_seq, member_permission)
        if r == true
        tuple <- posts_repo.getPost(board_seq, post_seq)
        comments <- comments_repo.allWithMemberName(post_seq)
        name_op <- boards_repo.getNameBySeq(board_seq)
        posts <- posts_repo.listByBoard(board_seq, page, page_length)
        count <- posts_repo.getPostCount(board_seq)
      } yield {
        posts_repo.updateHitCount(tuple._1)
        val is_own = member_seq.map(_.toLong == tuple._2.seq).getOrElse(false)
        Ok(
          views.html.post
            .read(tuple, comment_form, comments, is_own)(name_op,
                                                         posts,
                                                         page,
                                                         page_length,
                                                         count,
                                                         board_seq))
          .withCookies(Cookie("page", page.toString))
      }
  }

  def editPostForm(board_seq: Long, post_seq: Long) =
    Authenticated.async { implicit request =>
      for {
        r <- posts_repo.isOwnPost(post_seq, request.auth.seq) if r == true
        tuple: (Post, Member) <- posts_repo.getPost(board_seq, post_seq)
      } yield {
        val post = tuple._1
        val form_data =
          (board_seq, post.subject, post.content.getOrElse(""), Nil)
        Ok(
          views.html.post
            .edit(postForm.fill(form_data), board_seq, post_seq))
      }
    }

  def editPost(board_seq: Long, post_seq: Long) =
    Authenticated.async { implicit request =>
      val form = postForm.bindFromRequest
      form.fold(
        hasErrors =>
          Future.successful(
            BadRequest(views.html.post.edit(hasErrors, board_seq, post_seq))),
        form => {
          posts_repo.update(form, post_seq) map { _ =>
            val page =
              request.cookies
                .get("page")
                .getOrElse(Cookie("page", "1"))
                .value
                .toInt

            Redirect(routes.PostController.showPost(board_seq, post_seq, page))
          }
        }
      )
    }

  def writePostForm(board_seq: Long) = Authenticated.async {
    implicit request =>
      if (board_seq > 0) {
        Future.successful(Ok(views.html.post.write(postForm, board_seq)))
      } else {
        boards_repo
          .getBoardInfoForWrite(request.auth.permission)
          .map(seq => Ok(views.html.post.write_all(postForm, board_seq, seq)))
      }
  }

  def writePost(board_seq: Long) = Authenticated.async { implicit request =>
    val form = isWriteValidBoardForm.bindFromRequest
    form.fold(
      hasErrors =>
        Future.successful(
          BadRequest(views.html.post.write(hasErrors, board_seq))),
      form => {
        posts_repo
          .insert(form, request.auth.seq, request.remoteAddress) flatMap {
          post =>
            attachments_repo.updateAttachment2(form._4, post.seq).map { _ =>
              Redirect(routes.PostController
                .showPost(post.board_seq, post.seq, 1))
            }
        }
      }
    )
  }

  def writeComment(board_seq: Long) = Authenticated.async { implicit request =>
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
          r <- boards_repo.isReadValidBoard(board_seq, member_permission)
          if r == true
          tuple <- posts_repo.getPost(board_seq, post_seq)
          comments <- comments_repo.allWithMemberName(post_seq)
          name_op <- boards_repo.getNameBySeq(board_seq)
          posts <- posts_repo.listByBoard(board_seq, page, page_length)
          count <- posts_repo.getPostCount(board_seq)
        } yield {
          posts_repo.updateHitCount(tuple._1)
          val is_own =
            member_seq.map(_.toLong == tuple._2.seq).getOrElse(false)
          BadRequest(
            views.html.post
              .read(tuple, hasErrors, comments, is_own)(name_op,
                                                        posts,
                                                        page,
                                                        page_length,
                                                        count,
                                                        board_seq))
        }
      },
      form => {
        comments_repo
          .insert(form, request.auth.seq, request.remoteAddress)
          .map { post_seq =>
            val page =
              request.cookies
                .get("page")
                .getOrElse(Cookie("page", "1"))
                .value
                .toInt
            Redirect(routes.PostController.showPost(board_seq, post_seq, page))
          }
      }
    )
  }

  def uploadImage = Authenticated.async(parse.multipartFormData) {
    implicit request =>
      request.body
        .file("file")
        .map { image =>
          config
            .getString("uploads.path.images")
            .fold {
              Future.successful(InternalServerError("Directory not found."))
            } { path =>
              import java.io.File

              val sub_path = java.time.LocalDate.now.toString
              val file_name = image.filename
              val new_file_name =
                s"${System.currentTimeMillis()}_$file_name"
              val hash = DigestUtils.md5Hex(new_file_name)
              val full_path = s"${path}/${sub_path}/${hash}"

              val content_type = image.contentType.get

              val sub_full_path = Path(s"${path}/${sub_path}")
              if (!sub_full_path.exists) { sub_full_path.createDirectory() }

              val file = image.ref.moveTo(new File(full_path))
              attachments_repo
                .insertAttachment(
                  (hash, file_name, sub_path, content_type, file.length)
                )
                .map { seq =>
                  Ok(Json.obj("location" -> hash, "attachment_seq" -> seq))
                }
            }
        } getOrElse {
        Future.successful(InternalServerError("Can not found File."))
      }
  }

  def deletePost(board_seq: Long, post_seq: Long) = Authenticated.async {
    implicit request =>
      attachments_repo.getAttachments(post_seq).map { attachments =>
        for (attachment <- attachments) {
          val path =
            Path(s"${images_path}/${attachment.sub_path}/${attachment.hash}")
          Try(path.delete)
        }
        attachments_repo.removeAttachements(post_seq)
      }
      posts_repo.delete(post_seq).map { _ =>
        Redirect(routes.PostController.list(board_seq, 1))
      }
  }

  def deleteComment(board_seq: Long, post_seq: Long, comment_seq: Long) =
    Authenticated.async { implicit request =>
      val page = request.cookies
        .get("page")
        .getOrElse(Cookie("page", "1"))
        .value
        .toInt

      comments_repo.delete(comment_seq, request.auth.seq).map { _ =>
        Redirect(routes.PostController.showPost(board_seq, post_seq, page))
      }
    }

  private def isWriteValidBoard(board_seq: Long, permission: Byte)(
      implicit request: Request[AnyContent]): Boolean = {
    Await.result(boards_repo.isWriteValidBoard(board_seq, permission),
                 Duration.Inf)
  }

  private def isWriteValidBoardForm(
      implicit request: CustomAuthenticatedRequest[AnyContent]) = Form(
    tuple(
      "board_seq" -> longNumber.verifying(
        Messages("post.write.permission.error"),
        isWriteValidBoard(_, request.auth.permission)),
      "subject" -> nonEmptyText(maxLength = 80),
      "content" -> text,
      "attachments" -> seq(longNumber)
    )
  )

  private val postForm = Form(
    tuple(
      "board_seq" -> longNumber,
      "subject" -> nonEmptyText(maxLength = 80),
      "content" -> text,
      "attachments" -> seq(longNumber)
    )
  )

  private val comment_form = Form(
    tuple(
      "post_seq" -> longNumber,
      "reply_comment_seq" -> optional(longNumber),
      "content" -> nonEmptyText(maxLength = 4000)
    )
  )
}
