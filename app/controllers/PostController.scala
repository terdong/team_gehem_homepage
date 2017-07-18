package controllers

import java.io.File
import java.nio.file.{Files, Paths}
import javax.inject.{Inject, Singleton}

import akka.stream.IOResult
import akka.stream.scaladsl._
import akka.util.ByteString
import com.teamgehem.authentication.{Authenticated, CustomAuthenticatedRequest}
import controllers.traits.ProvidesHeader
import models.{Member, Post}
import org.apache.commons.codec.digest.DigestUtils
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms.{tuple, _}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json._
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
  lazy val attachment_path = config.getString("uploads.path").get

  lazy val page_length = config.getInt("post.pageLength").getOrElse(15)

  def searchAll(page: Int, type_number: Int, word: String) = Action.async {
    implicit request =>
      val permission = member_permission
      for {
        posts <- posts_repo.listAll(page,
                                    page_length,
                                    permission,
                                    Option(type_number),
                                    Option(word))
        count <- posts_repo.getListAllCount(permission,
                                            Option(type_number),
                                            Option(word))
      } yield
        Ok(
          views.html.post
            .list(None,
                  posts,
                  page,
                  page_length,
                  count,
                  search_type = Option(type_number),
                  search_word = Option(word)))
  }

  /*  def listAll(page: Int, type_number: Int, word: String) = Action.async {
    implicit request =>
      val permission = member_permission
      for {
        posts <- posts_repo.all(page, page_length, permission)
        count <- posts_repo.getListAllCount(permission)
      } yield Ok(views.html.post.list(None, posts, page, page_length, count))
  }*/

  def listAll(page: Int) = Action.async { implicit request =>
    val permission = member_permission
    for {
      posts <- posts_repo.all(page, page_length, permission)
      count <- posts_repo.getListAllCount(permission)
    } yield Ok(views.html.post.list(None, posts, page, page_length, count))
  }

  def search(board_seq: Long, page: Int, type_number: Int, word: String) =
    Action.async { implicit request =>
      val permission = member_permission
      for {
        b <- boards_repo.isListValidBoard(board_seq, permission)
        if b == true
        name_op <- boards_repo.getNameBySeq(board_seq)
        posts <- posts_repo.search(board_seq,
                                   page,
                                   page_length,
                                   type_number,
                                   word)
        count <- posts_repo.getPostSearchCount(board_seq, type_number, word)
      } yield
        Ok(
          views.html.post
            .list(name_op,
                  posts,
                  page,
                  page_length,
                  count,
                  board_seq,
                  search_type = Option(type_number),
                  search_word = Option(word)))
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
        attachments <- attachments_repo.getAttachments(post_seq)
      } yield {
        posts_repo.updateHitCount(tuple._1)
        val is_own = member_seq.map(_.toLong == tuple._2.seq).getOrElse(false)
        Ok(
          views.html.post
            .read(tuple, comment_form, comments, attachments, is_own)(
              name_op,
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
          (board_seq, post.subject, post.content.getOrElse(""), Nil, Nil)
        Ok(
          views.html.post
            .edit(post_form.fill(form_data), board_seq, post_seq))
      }
    }

  def editPost(board_seq: Long, post_seq: Long) =
    Authenticated.async { implicit request =>
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

              form._5.foreach {
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
              attachments_repo.updateAttachment2(form._4, post_seq)
              Redirect(
                routes.PostController.showPost(board_seq, post_seq, page))
          }
        }
      )
    }

  def writePostForm(board_seq: Long) = Authenticated.async {
    implicit request =>
      if (board_seq > 0) {
        boards_repo.getBoard(board_seq).map { board =>
          Ok(views.html.post.write(post_form, board, file_form))
        }
      } else {
        boards_repo
          .getBoardInfoForWrite(request.auth.permission)
          .map(seq => Ok(views.html.post.write_all(post_form, board_seq, seq)))
      }
  }

  lazy val temp_dir_path: String = System.getProperty("java.io.tmpdir")

  def writePost(board_seq: Long) = Authenticated.async { implicit request =>
    val form = isWriteValidBoardForm.bindFromRequest
    form.fold(
      hasErrors =>
        boards_repo.getBoard(board_seq).map { board =>
          BadRequest(views.html.post.write(hasErrors, board, file_form))
      },
      form => {
        posts_repo
          .insert(form, request.auth.seq, request.remoteAddress) flatMap {
          post =>
            form._5.foreach {
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
                          post.seq)
                      }
                  }
            }

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
          attachments <- attachments_repo.getAttachments(post_seq)
        } yield {
          posts_repo.updateHitCount(tuple._1)
          val is_own =
            member_seq.map(_.toLong == tuple._2.seq).getOrElse(false)
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
          import java.io.File

          val sub_path = java.time.LocalDate.now.toString
          val file_name = image.filename
          val new_file_name =
            s"${System.currentTimeMillis()}_$file_name"
          val hash = DigestUtils.md5Hex(new_file_name)
          val full_path = s"${attachment_path}/${sub_path}/${hash}"

          val content_type = image.contentType.get

          val sub_full_path = Path(s"${attachment_path}/${sub_path}")
          if (!sub_full_path.exists) { sub_full_path.createDirectory() }

          val file = image.ref.moveTo(new File(full_path))
          attachments_repo
            .insertAttachment(
              (hash, file_name, sub_path, content_type, file.length)
            )
            .map { seq =>
              Ok(Json.obj("location" -> hash, "attachment_seq" -> seq))
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
            Path(
              s"${attachment_path}/${attachment.sub_path}/${attachment.hash}")
          Try(path.delete)
        }
        attachments_repo.deleteAttachements(post_seq)
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

  private val comment_form = Form(
    tuple(
      "post_seq" -> longNumber,
      "reply_comment_seq" -> optional(longNumber),
      "content" -> nonEmptyText(maxLength = 4000)
    )
  )
  case class FormData(name: String)
  private val file_form = Form(
    mapping(
      "name" -> text
    )(FormData.apply)(FormData.unapply)
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
  )(UploadedFileInfo.apply _)

  implicit val uploadedFileInfoWrites: Writes[UploadedFileInfo] = (
    (JsPath \ "hash").write[String] and
      (JsPath \ "file_name").write[String] and
      (JsPath \ "content_type").write[String] and
      (JsPath \ "size").write[Long]
  )(unlift(UploadedFileInfo.unapply))

  def uploadFile =
    Authenticated(parse.multipartFormData(handleFilePartAsFile)) { request =>
      request.body
        .file("qqfile")
        .map {
          /*          file =>
          val uuid = request.body.dataParts("qquuid").mkString
          val filename = file.filename
          val contentType = file.contentType
          file.ref.moveTo(new File(temp_dir_path, uuid))
          Ok(Json.obj("success" -> true, "newUuid" -> "suck"))
        }*/
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

  def deleteFile = Authenticated { request =>
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
}
