package controllers

import java.io.File
import java.net.{URLDecoder, URLEncoder}
import java.nio.file.{Files, Paths}
import javax.inject.{Inject, Singleton}

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.ByteString
import com.teamgehem.enumeration.BoardListState._
import com.teamgehem.model.{BoardInfo, BoardPaginationInfo, BoardSearchInfo, MemberInfo}
import com.teamgehem.security.AuthenticatedActionBuilder
import models.{Member, Post}
import org.apache.commons.codec.digest.DigestUtils
import play.api.cache.SyncCacheApi
import play.api.data.Form
import play.api.data.Forms.{seq, tuple, _}
import play.api.i18n.Messages
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.api.{Configuration, Logger}
import play.core.parsers.Multipart.{FileInfo, FilePartHandler}
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

  implicit def getMemberInfo(implicit request: MessagesRequest[AnyContent]) = {
    for {
      email <- getEmail
      permission <- request.session.get("permission")
      seq <- getSeq
    } yield {
      MemberInfo(email, permission.toByte, seq.toLong)
    }
  }

  def getPermission(implicit request: MessagesRequest[AnyContent]) = request.session.get("permission").getOrElse("0").toByte

  def getSeq(implicit request: MessagesRequest[AnyContent]) = request.session.get("seq")

  def getEmail(implicit request: MessagesRequest[AnyContent]) = request.session.get("email")

  def getReadPage(implicit request: MessagesRequest[AnyContent]) = {request.cookies.get("read_page").map(_.value.toInt).getOrElse(1)}

  def list(board_seq: Long, page: Int) = Action.async { implicit request =>
    val permission = getPermission
    val route: (Int) => Call = routes.PostController.list(board_seq, _)
    board_seq match {
      case 0 => {
        for {
          posts_in_page <- posts_repo.all(page, page_length, permission)
          all_posts_count <- posts_repo.getAllPostCount(permission)
        } yield {
          Ok(views.html.post.list("all", posts_in_page, BoardPaginationInfo(page, page_length, all_posts_count), route)).withCookies(Cookie(List_Mode, All_List)).bakeCookies()
        }
      }
      case _ => {
        for {
          b <- boards_repo.isListValidBoard(board_seq, permission)
          if b == true
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts_in_page <- posts_repo.getListByBoard(board_seq, page, page_length)
          posts_count_on_board <- posts_repo.getPostCount(board_seq)
        } yield
          Ok(views.html.post.list(board_name, posts_in_page, BoardPaginationInfo(page, page_length, posts_count_on_board), route, Some(board_seq))).withCookies(Cookie(List_Mode, Default_List)).bakeCookies()
      }
    }
  }

  def searchAll(page: Int, type_number: Int, word: String) = Action.async { implicit request =>
    val permission = getPermission
    for {
      posts_in_page <- posts_repo.searchAll(page, page_length, permission, BoardSearchInfo(type_number, word))
      posts_count_on_board <- posts_repo.getSearchAllPostCount(permission, BoardSearchInfo(type_number, word))
    } yield
      Ok(
        views.html.post.list(
          "all",
          posts_in_page,
          BoardPaginationInfo(page, page_length, posts_count_on_board),
          routes.PostController.searchAll(_, type_number, word),
          search_info = Some(BoardSearchInfo(type_number, word))
        )
      ).withCookies(Cookie(List_Mode, All_Search_List), Cookie("type_number", type_number.toString, Some(3600)), Cookie("word", URLEncoder.encode(word,"utf-8"), Some(3600))).bakeCookies()
  }

  def search(board_seq: Long, page: Int, type_number: Int, word: String) = Action.async { implicit request =>
    val permission = getPermission
    for {
      b <- boards_repo.isListValidBoard(board_seq, permission)
      if b == true
      board_name <- boards_repo.getNameBySeq(board_seq)
      posts_in_page <- posts_repo.search(board_seq, page, page_length, BoardSearchInfo(type_number, word))
      posts_count_on_board <- posts_repo.getSearchPostCount(board_seq, BoardSearchInfo(type_number, word))
    } yield
      Ok(
        views.html.post
          .list(
            board_name,
            posts_in_page,
            BoardPaginationInfo(page, page_length, posts_count_on_board),
            routes.PostController.search(board_seq, _, type_number, word),
            Some(board_seq),
            Some(BoardSearchInfo(type_number, word))
          )
      ).withCookies(Cookie(List_Mode,Search_List), Cookie("type_number", type_number.toString, Some(3600)), Cookie("word", URLEncoder.encode(word,"utf-8"), Some(3600))).bakeCookies()
  }

  /** This method prints the contents of the post and prints a list of the posts at the bottom of the screen.
    *
    * But, the list output process is a bit complicated.
    *
    * @param board_seq
    * @param post_seq
    * @param page
    * @return
    */
  def read(board_seq: Long, post_seq: Long, page: Int) = Action.async { implicit request =>
    // TODO: Maybe have to refactor later...
    // get list of the posts
    val permission = getPermission
    val list_mode = request.cookies.get(List_Mode).map(_.value).getOrElse(Default_List)
    val route: Future[(String, Vector[(Post, String, Int)], BoardPaginationInfo, (Int) => Call, Option[Long], Option[BoardSearchInfo])] = list_mode match {
      case Default_List => {
        for{
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts <- posts_repo.getListByBoard(board_seq, page, page_length)
          posts_count_on_board <- posts_repo.getPostCount(board_seq)
        }yield{
          (board_name, posts, BoardPaginationInfo(page, page_length, posts_count_on_board), routes.PostController.list(board_seq, _), Some(board_seq), None)
        }
      }
      case All_List => {
        for{
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts_in_page <- posts_repo.all(page, page_length, permission)
          all_posts_count <- posts_repo.getAllPostCount(permission)
        }yield{
          (board_name, posts_in_page, BoardPaginationInfo(page, page_length, all_posts_count), routes.PostController.list(0, _), None, None)
        }
      }
      case Search_List => {
        val type_number = request.cookies.get("type_nubmer").map(_.value.toInt).getOrElse(0)
        val word =  URLDecoder.decode(request.cookies.get("word").map(_.value).getOrElse(""),"utf-8")
        for {
          b <- boards_repo.isListValidBoard(board_seq, permission)
          if b == true
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts_in_page <- posts_repo.search(board_seq, page, page_length, BoardSearchInfo(type_number, word))
          posts_count_on_board <- posts_repo.getSearchPostCount(board_seq, BoardSearchInfo(type_number, word))
        } yield{
          (board_name, posts_in_page, BoardPaginationInfo(page, page_length, posts_count_on_board), routes.PostController.search(board_seq, _, type_number, word), Some(board_seq),
            Some(BoardSearchInfo(type_number, word)))
        }
      }
      case All_Search_List => {
        val type_number = request.cookies.get("type_nubmer").map(_.value.toInt).getOrElse(0)
        val word = URLDecoder.decode(request.cookies.get("word").map(_.value).getOrElse(""),"utf-8")
        for {
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts_in_page <- posts_repo.searchAll(page, page_length, permission, BoardSearchInfo(type_number, word))
          posts_count_on_board <- posts_repo.getSearchAllPostCount(permission, BoardSearchInfo(type_number, word))
        } yield {
          (board_name, posts_in_page, BoardPaginationInfo(page, page_length, posts_count_on_board), routes.PostController.searchAll(_, type_number, word), None, Some(BoardSearchInfo(type_number, word)))
        }
      }
    }

    // get contents of the post and combine
    for {
      r <- boards_repo.isReadValidBoard(board_seq, getPermission)
      if r == true
      tuple <- posts_repo.getPost(board_seq, post_seq)
      comments <- comments_repo.allWithMemberName(post_seq)
      attachments <- attachments_repo.getAttachments(post_seq)
      result <- route
    }yield {
      posts_repo.updateHitCount(tuple._1)
      val is_own = getSeq.map(_.toLong == tuple._2.seq).getOrElse(false)

      Ok((views.html.post.read(tuple, comment_form, comments, attachments, is_own) _).tupled(result)).withCookies(Cookie("read_page", page.toString, Some(3600))).bakeCookies()
    }
  }

  def writePostForm(board_seq: Long) = auth.async { implicit request =>
    if (board_seq > 0) {
      boards_repo.getBoard(board_seq).map { board =>
        Ok(views.html.post.write(post_form, board))
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
          BadRequest(views.html.post.write(hasErrors, board))
        }
      },
      (form: (Long, String, String, Seq[String])) => {
        getSeq.fold(Future.successful(InternalServerError("There is no seq"))) { seq =>
          posts_repo.insert(form, seq.toLong, request.remoteAddress).map { post =>
            insertAttachment(post.seq, form._4)
            Redirect(routes.PostController.read(post.board_seq, post.seq, 1))
          }
        }
      }
    )
  }

  def editPostForm(board_seq: Long, post_seq: Long) = auth.async { implicit request =>
    for {
      r <- posts_repo.isOwnPost(post_seq, getSeq.getOrElse("0").toLong) if r == true
      tuple: (Post, Member) <- posts_repo.getPost(board_seq, post_seq)
    } yield {
      val post = tuple._1
      val form_data = (board_seq, post.subject, post.content.getOrElse(""), Nil)
      Ok(views.html.post.edit(post_form.fill(form_data), board_seq, post_seq))
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
            insertAttachment(post_seq, form._4)
            Redirect(routes.PostController.read(board_seq, post_seq, getReadPage))
        }
      }
    )
  }

  def deletePost(board_seq: Long, post_seq: Long) = auth.async { implicit request =>
    posts_repo.isOwnPost(post_seq, getSeq.getOrElse("0").toLong).flatMap {
      _ match {
        case true => {
          posts_repo.delete(post_seq).map { _ =>
            attachments_repo.getAttachments(post_seq).map { attachments =>
              for (attachment <- attachments) {
                val path = Path(s"${attachment_path}/${attachment.sub_path}/${attachment.hash}")
                Try(path.delete)
              }
              attachments_repo.deleteAttachements(post_seq)
            }
            Redirect(routes.PostController.list(board_seq, 1))
          }
        }
        case false => Future.successful(Unauthorized(views.html.defaultpages.unauthorized()))
      }
    }
  }

  def uploadFile = auth(parse.multipartFormData(handleFilePartAsFile)) { request =>
    request.body
      .file("qqfile")
      .map {
        case FilePart(key, filename, contentType, file: File) =>
          val hash = file.getName
          Logger.info(
            s"key = ${key}, filename = ${filename}, contentType = ${contentType}, file = $file, filename = ${hash}")

          Json.obj(
            "success" -> true,
            "newUuid" -> hash,
            "file_info" -> Json.toJson(
              UploadedFileInfo(
                hash,
                filename,
                contentType.getOrElse("application/octet-stream"),
                file.length))
          )
      }
      .map {
        Ok(_)
      }
      .getOrElse(BadRequest("Unable to upload."))
  }

  def deleteFile = auth { request =>
    val body = request.body
    val data: Option[Map[String, Seq[String]]] = body.asFormUrlEncoded

    // Expecting json body
    data
      .map { map =>
        val hash = map("qquuid").head

        if (map.contains("sub_path")) {
          val sub_path = map("sub_path").head
          attachments_repo.deleteAttachment(hash)
          val path =
            Path(s"${attachment_path}/${sub_path}/${hash}")
          Try(path.delete)
        } else {
          val path =
            Paths.get(s"${temp_dir_path}/${hash}")
          Files.deleteIfExists(path)
        }
        Ok("file delete")
      }
      .getOrElse {
        BadRequest("Expecting application/FormUrlEncoded request body")
      }
  }

  def writeComment(board_seq: Long) = auth.async { implicit request =>
    val form = comment_form.bindFromRequest
    form.fold(
      hasErrors => {
        val post_seq = hasErrors.get._1
        val page = getReadPage
        for {
          r <- boards_repo.isReadValidBoard(board_seq, getPermission)
          if r == true
          tuple <- posts_repo.getPost(board_seq, post_seq)
          comments <- comments_repo.allWithMemberName(post_seq)
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts <- posts_repo.getListByBoard(board_seq, page, page_length)
          all_posts_count <- posts_repo.getPostCount(board_seq)
          attachments <- attachments_repo.getAttachments(post_seq)
        } yield {
          posts_repo.updateHitCount(tuple._1)
          val is_own = getSeq.map {
            _ == tuple._2.seq
          }.getOrElse(false)

          BadRequest(
            views.html.post.read(tuple, hasErrors, comments, attachments, is_own)
            (
              board_name,
              posts,
              BoardPaginationInfo(page, page_length, all_posts_count),
              routes.PostController.list(board_seq, _),
              Some(board_seq)
            )
          )
        }
      },
      form => {
        getSeq.map { seq =>
          comments_repo
            .insert(form, seq.toLong, request.remoteAddress)
            .map { post_seq =>
              Redirect(routes.PostController.read(board_seq, post_seq, getReadPage))
            }
        }.getOrElse(Future.successful(InternalServerError))
      }
    )
  }

  def deleteComment(board_seq: Long, post_seq: Long, comment_seq: Long) = auth.async { implicit request =>

    getSeq.map { seq =>
      comments_repo.delete(comment_seq, seq.toLong).map { _ =>
        Redirect(routes.PostController.read(board_seq, post_seq, getReadPage))
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
      "upload_files" -> seq(text)
    )
  )

  /**
    * Uses a custom FilePartHandler to return a type of "File" rather than
    * using Play's TemporaryFile class.  Deletion must happen explicitly on
    * completion, rather than TemporaryFile (which uses finalization to
    * delete temporary files).
    *
    * @return
    */
  private def handleFilePartAsFile: FilePartHandler[File] = {
    case FileInfo(partName, filename, contentType) =>
      val new_file_hash =
        DigestUtils.md5Hex(s"${System.currentTimeMillis()}_$filename")
      val file = new File(temp_dir_path, new_file_hash)

      val fileSink: Sink[ByteString, Future[IOResult]] =
        FileIO.toPath(file.toPath)
      /*
      val file = path.toFile
      val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(path)
       */
      val accumulator: Accumulator[ByteString, IOResult] = Accumulator(
        fileSink)
      accumulator.map {
        case IOResult(count, status) =>
          Logger.debug(s"count = $count, status = $status")
          FilePart(partName, filename, contentType, file)
      }
  }

  private def insertAttachment(post_seq: Long, seq_file_info: Seq[String]) = {
    seq_file_info.foreach {
      json_str =>
        Json
          .fromJson[UploadedFileInfo](Json.parse(json_str))
          .map {
            (file_info: UploadedFileInfo) =>
              val path =
                Paths.get(s"${temp_dir_path}/${file_info.hash}")
              if (Files.exists(path)) {
                val sub_path = java.time.LocalDate.now.toString

                val new_directory_path =
                  Path(s"$attachment_path/${sub_path}")
                if (!new_directory_path.exists) {
                  new_directory_path.createDirectory()
                }

                val new_file_path = Paths.get(
                  s"${new_directory_path.path}/${file_info.hash}")
                Files.move(path, new_file_path)

                attachments_repo.insertAttachment(
                  file_info.hash,
                  file_info.file_name,
                  sub_path,
                  file_info.content_type,
                  file_info.size,
                  post_seq)
              }
          }
    }
  }

  private val post_form = Form(
    tuple(
      "board_seq" -> longNumber,
      "subject" -> nonEmptyText(maxLength = 80),
      "content" -> text,
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

  import play.api.libs.functional.syntax._

  case class UploadedFileInfo(hash: String,
                              file_name: String,
                              content_type: String,
                              size: Long)

  implicit val uploadedFileInfoReads: Reads[UploadedFileInfo] = (
    (JsPath \ "hash").read[String] and
      (JsPath \ "file_name").read[String] and
      (JsPath \ "content_type").read[String] and
      (JsPath \ "size").read[Long]
    ) (UploadedFileInfo.apply _)

  implicit val uploadedFileInfoWrites: Writes[UploadedFileInfo] = (
    (JsPath \ "hash").write[String] and
      (JsPath \ "file_name").write[String] and
      (JsPath \ "content_type").write[String] and
      (JsPath \ "size").write[Long]
    ) (unlift(UploadedFileInfo.unapply))
}
