package controllers

import java.io.{File, FileInputStream}
import java.net.{URLDecoder, URLEncoder}
import java.nio.file.{Paths, Files => JFiles}
import javax.inject.{Inject, Singleton}

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.ByteString
import com.google.inject.Provider
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.JpegWriter
import com.teamgehem.controller.TGBasicController
import com.teamgehem.enumeration.BoardListState._
import com.teamgehem.enumeration.CacheString
import com.teamgehem.model.{BoardInfo, BoardSearchInfo, MemberInfo, PaginationInfo}
import com.teamgehem.security.{AuthenticatedActionBuilder, BoardStateFilter}
import fly.play.s3.{BucketFile, S3, S3Exception}
import models.{Board, Comment, Post}
import org.apache.commons.codec.digest.DigestUtils
import play.api.cache.SyncCacheApi
import play.api.data.Form
import play.api.data.Forms.{seq, tuple, _}
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.api.{Application, Configuration, Logger}
import play.core.parsers.Multipart.{FileInfo, FilePartHandler}
import repositories._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.io.Streamable


/**
  * Created by DongHee Kim on 2017-08-12 012.
  */

@Singleton
class PostController @Inject()(sync_cache: SyncCacheApi,
                               config: Configuration,
                               auth: AuthenticatedActionBuilder,
                               mcc: MessagesControllerComponents,
                               members_repo: MembersRepository,
                               boards_repo: BoardsRepository,
                               posts_repo: PostsRepository,
                               attachments_repo: AttachmentsRepository,
                               comments_repo: CommentsRepository,
                               board_state_filter: BoardStateFilter,
                               appProvider: Provider[Application])
  extends TGBasicController(mcc, sync_cache) {

  lazy val attachment_path = config.get[String]("uploads.path")
  lazy val post_page_length = config.get[Int]("post.pageLength")
  lazy val comment_page_length = config.get[Int]("comment.pageLength")
  lazy val temp_dir_path: String = System.getProperty("java.io.tmpdir")
  lazy val cloud_front_url = config.get[String]("cloud_frount.url")
  lazy val images_url = routes.AttachmentController.images("").url
  implicit def getBoardInfo: Option[Seq[BoardInfo]] = sync_cache.get[Seq[BoardInfo]](CacheString.List_Permission)

  implicit def getMemberInfo(implicit request: MessagesRequest[AnyContent]) = {
    for {
      email <- getEmail
      permission <- request.session.get("permission")
      seq <- getSeq
      nick <- getNick
    } yield {
      MemberInfo(email, permission.toByte, seq.toLong, nick)
    }
  }

  def getPermission(implicit request: MessagesRequest[AnyContent]) = request.session.get("permission").getOrElse("0").toByte

  def getSeq(implicit request: MessagesRequest[AnyContent]) = request.session.get("seq")

  def getEmail(implicit request: MessagesRequest[AnyContent]) = request.session.get("email")

  def getNick(implicit request: MessagesRequest[AnyContent]) = request.session.get("nick")

  def getReadPage(implicit request: MessagesRequest[AnyContent]) = {
    request.cookies.get("read_page").map(_.value.toInt).getOrElse(1)
  }

  def list(board_seq: Long, page: Int) = Action.async { implicit request =>
    val permission = getPermission
    val route: (Int) => Call = routes.PostController.list(board_seq, _)
    board_seq match {
      case 0 => {
        for {
          posts_in_page <- posts_repo.all(page, post_page_length, permission)
          all_posts_count <- posts_repo.getAllPostCount(permission)
        } yield {
          Ok(views.html.post.list("all", posts_in_page, PaginationInfo(page, post_page_length, all_posts_count), route)).withCookies(Cookie(List_Mode, All_List)).bakeCookies()
        }
      }
      case _ => {
        for {
          b <- boards_repo.isListValidBoard(board_seq, permission)
          if b == true
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts_in_page <- posts_repo.getListByBoard(board_seq, page, post_page_length)
          posts_count_on_board <- posts_repo.getPostCount(board_seq)
        } yield
          Ok(views.html.post.list(board_name, posts_in_page, PaginationInfo(page, post_page_length, posts_count_on_board), route, Some(board_seq))).withCookies(Cookie(List_Mode, Default_List)).bakeCookies()
      }
    }
  }

  def searchAll(page: Int, type_number: Int, word: String) = Action.async { implicit request =>
    val permission = getPermission
    for {
      posts_in_page <- posts_repo.searchAll(page, post_page_length, permission, BoardSearchInfo(type_number, word))
      posts_count_on_board <- posts_repo.getSearchAllPostCount(permission, BoardSearchInfo(type_number, word))
    } yield
      Ok(
        views.html.post.list(
          "all",
          posts_in_page,
          PaginationInfo(page, post_page_length, posts_count_on_board),
          routes.PostController.searchAll(_, type_number, word),
          search_info = Some(BoardSearchInfo(type_number, word))
        )
      ).withCookies(Cookie(List_Mode, All_Search_List), Cookie("type_number", type_number.toString, Some(3600)), Cookie("word", URLEncoder.encode(word, "utf-8"), Some(3600))).bakeCookies()
  }

  def search(board_seq: Long, page: Int, type_number: Int, word: String) = Action.async { implicit request =>
    val permission = getPermission
    for {
      b <- boards_repo.isListValidBoard(board_seq, permission)
      if b == true
      board_name <- boards_repo.getNameBySeq(board_seq)
      posts_in_page <- posts_repo.search(board_seq, page, post_page_length, BoardSearchInfo(type_number, word))
      posts_count_on_board <- posts_repo.getSearchPostCount(board_seq, BoardSearchInfo(type_number, word))
    } yield
      Ok(
        views.html.post
          .list(
            board_name,
            posts_in_page,
            PaginationInfo(page, post_page_length, posts_count_on_board),
            routes.PostController.search(board_seq, _, type_number, word),
            Some(board_seq),
            Some(BoardSearchInfo(type_number, word))
          )
      ).withCookies(Cookie(List_Mode, Search_List), Cookie("type_number", type_number.toString, Some(3600)), Cookie("word", URLEncoder.encode(word, "utf-8"), Some(3600))).bakeCookies()
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
    val route: Future[(String, Vector[(Post, String, Int)], PaginationInfo, (Int) => Call, Option[Long], Option[BoardSearchInfo])] = list_mode match {
      case Default_List => {
        for {
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts <- posts_repo.getListByBoard(board_seq, page, post_page_length)
          posts_count_on_board <- posts_repo.getPostCount(board_seq)
        } yield {
          (board_name, posts, PaginationInfo(page, post_page_length, posts_count_on_board), routes.PostController.list(board_seq, _), Some(board_seq), None)
        }
      }
      case All_List => {
        for {
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts_in_page <- posts_repo.all(page, post_page_length, permission)
          all_posts_count <- posts_repo.getAllPostCount(permission)
        } yield {
          (board_name, posts_in_page, PaginationInfo(page, post_page_length, all_posts_count), routes.PostController.list(0, _), None, None)
        }
      }
      case Search_List => {
        val type_number = request.cookies.get("type_nubmer").map(_.value.toInt).getOrElse(0)
        val word = URLDecoder.decode(request.cookies.get("word").map(_.value).getOrElse(""), "utf-8")
        for {
          b <- boards_repo.isListValidBoard(board_seq, permission)
          if b == true
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts_in_page <- posts_repo.search(board_seq, page, post_page_length, BoardSearchInfo(type_number, word))
          posts_count_on_board <- posts_repo.getSearchPostCount(board_seq, BoardSearchInfo(type_number, word))
        } yield {
          (board_name, posts_in_page, PaginationInfo(page, post_page_length, posts_count_on_board), routes.PostController.search(board_seq, _, type_number, word), Some(board_seq),
            Some(BoardSearchInfo(type_number, word)))
        }
      }
      case All_Search_List => {
        val type_number = request.cookies.get("type_nubmer").map(_.value.toInt).getOrElse(0)
        val word = URLDecoder.decode(request.cookies.get("word").map(_.value).getOrElse(""), "utf-8")
        for {
          board_name <- boards_repo.getNameBySeq(board_seq)
          posts_in_page <- posts_repo.searchAll(page, post_page_length, permission, BoardSearchInfo(type_number, word))
          posts_count_on_board <- posts_repo.getSearchAllPostCount(permission, BoardSearchInfo(type_number, word))
        } yield {
          (board_name, posts_in_page, PaginationInfo(page, post_page_length, posts_count_on_board), routes.PostController.searchAll(_, type_number, word), None, Some(BoardSearchInfo(type_number, word)))
        }
      }
    }

    // get contents of the post and combine
    for {
      r <- boards_repo.isReadValidBoard(board_seq, getPermission)
      if r == true
      tuple <- posts_repo.getPostWithMemberWithBoard(post_seq)
      comments <- comments_repo.allWithMemberNameForPagination(post_seq, 1, comment_page_length)
      comments_count <- comments_repo.commentCount(post_seq)
      attachments <- attachments_repo.getAttachments(post_seq)
      result <- route
    } yield {
      val post_and_member = (tuple._1, tuple._2)
      val board = tuple._3
      val is_own = getSeq.map(_.toLong == post_and_member._2.seq).getOrElse(false)

      val is_signed_in = getEmail.isDefined
      val is_reply = is_signed_in && board.is_reply
      val is_comment = is_signed_in && board.is_comment

      posts_repo.updateHitCount(post_and_member._1)
      Ok((views.html.post.read(post_and_member, if (board.is_comment) {
        Some(comment_form, comments, PaginationInfo(1, comment_page_length, comments_count))
      } else {
        None
      }, attachments, is_own, is_reply, is_comment) _).tupled(result)).withCookies(Cookie("read_page", page.toString, Some(3600))).bakeCookies()
    }
  }

  implicit val commentWrites = Json.writes[Comment]

  implicit def tuple3[A: Writes, B: Writes, C: Writes] = Writes[(A, B, C)](t => Json.obj("comment" -> t._1, "author_name" -> t._2, "reply_author_name" -> t._3))


  def commentList(post_seq: Long, page: Int) = Action.async { implicit request =>
    for {
      comments: Seq[(Comment, String, Option[String])] <- comments_repo.allWithMemberNameForPagination(post_seq, page, comment_page_length)
    } yield {
      Ok(Json.toJson(comments))
    }
  }

  def writePostForm(board_seq: Long) = auth.authrized_member.async { implicit request =>
    if (board_seq > 0) {
      boards_repo.getBoard(board_seq).map { board =>
        val form_data = (board.seq, "", "", Nil, None)
        Ok(views.html.post.write(post_form.fill(form_data), board.name, board.seq, board.is_attachment))
      }
    } else {
      boards_repo
        .getBoardInfoForWrite(request.member.permission)
        .map(seq => Ok(views.html.post.write_all(post_form, seq)))
    }
  }

  def writePost = auth.authrized_member.async { implicit request =>
    val form = isWriteValidBoardForm.bindFromRequest
    form.fold(
      hasErrors => {
        boards_repo.getBoard(form.get._1).map { board =>
          BadRequest(views.html.post.write(hasErrors, board.name, board.seq, board.is_attachment))
        }
      },
      (form: (Long, String, String, Seq[String], Option[Long])) => {
        getSeq.fold(Future.successful(InternalServerError("There is no seq"))) { seq =>
          posts_repo.insert(changeDomainOnForm(form), seq.toLong, request.remoteAddress).map { post =>
            insertAttachment(post.seq, form._4)
            Redirect(routes.PostController.read(post.board_seq, post.seq, 1))
          }
        }
      }
    )
  }

  def writeReplyPostForm(board_seq: Long, post_seq: Long) = auth.authrized_member andThen board_state_filter.checkReplyWriting(board_seq) async { implicit request =>
    for {
      tuple: (Post, Board) <- posts_repo.getPostWithBoard(post_seq)
    } yield {
      val post = tuple._1
      val board = tuple._2
      val form_data = (board.seq, post.subject, s">>${post.content.getOrElse("")}", Nil, Some(post_seq))
      Ok(views.html.post.write(post_form.fill(form_data), board.name, board.seq, board.is_attachment))
    }
  }

  def editPostForm(board_seq: Long, post_seq: Long) = auth.async { implicit request =>
    for {
      r <- posts_repo.isOwnPost(post_seq, getSeq.getOrElse("0").toLong) if r == true
      tuple: (Post, Board) <- posts_repo.getPostWithBoard(post_seq)
    } yield {
      val post = tuple._1
      val board = tuple._2
      val form_data = (board_seq, post.subject, post.content.getOrElse(""), Nil, None)
      Ok(views.html.post.edit(post_form.fill(form_data), board.name, board.seq, board.is_attachment, post_seq))
    }
  }

  def editPost(board_seq: Long, post_seq: Long) = auth.async { implicit request =>
    val form = post_form.bindFromRequest
    form.fold(
      hasErrors =>
        boards_repo.getBoard(board_seq).map(board => BadRequest(views.html.post.edit(hasErrors, board.name, board.seq, board.is_attachment, post_seq))),
      form => {
        posts_repo.update(changeDomainOnForm(form), post_seq) map {
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
                implicit val app = appProvider.get()
                val bucket = S3("com.teamgehem.files")
                (bucket - attachment.hash).map { _ =>
                  Logger.debug("Deleted the file")
                }.recover {
                  case S3Exception(status, code, message, originalXml) => Logger.debug(s"Bucket Delete File Error: $message")
                }
        /*        val path = Path(s"${attachment_path}/${attachment.sub_path}/${attachment.hash}")
                Try(path.delete)*/
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

  def uploadFile = auth.authrized_member(parse.multipartFormData(handleFilePartAsFile)) { request =>
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

          implicit val app = appProvider.get()
          val bucket = S3("com.teamgehem.files")
          (bucket - hash).map { _ =>
            Logger.debug("Deleted the file")
          }.recover {
            case S3Exception(status, code, message, originalXml) => Logger.debug(s"Bucket Delete File Error: $message")
          }

          attachments_repo.deleteAttachment(hash)
          /*val sub_path = map("sub_path").head
          val path =
            Path(s"${attachment_path}/${sub_path}/${hash}")
          Try(path.delete)*/
        } else {
          val path =
            Paths.get(s"${temp_dir_path}/${hash}")
          JFiles.deleteIfExists(path)
        }
        Ok("file delete")
      }
      .getOrElse {
        BadRequest("Expecting application/FormUrlEncoded request body")
      }
  }

  // TODO: If the form fails, change the request method to ajax to prevent unnecessary operations.
  def writeComment(board_seq: Long) = (auth.authrized_member andThen board_state_filter.checkCommentWriting(board_seq)).async { implicit request =>
    val form = comment_form.bindFromRequest
    form.fold(
      hasErrors => {
        val post_seq: Long = hasErrors.data.get("post_seq").map(_.toLong).getOrElse(0)
        val page = getReadPage
        for {
          r <- boards_repo.isReadValidBoard(board_seq, getPermission)
          if r == true
          tuple <- posts_repo.getPostWithMemberWithBoard(post_seq)
          comments <- comments_repo.allWithMemberName(post_seq)
          comments_count <- comments_repo.commentCount(post_seq)
          posts <- posts_repo.getListByBoard(board_seq, page, post_page_length)
          all_posts_count <- posts_repo.getPostCount(board_seq)
          attachments <- attachments_repo.getAttachments(post_seq)
        } yield {
          val post_and_member = (tuple._1, tuple._2)
          val board = tuple._3
          val is_own = getSeq.map {
            _ == post_and_member._2.seq
          }.getOrElse(false)

          val is_signed_in = getEmail.isDefined
          val is_reply = is_signed_in && board.is_reply
          val is_comment = is_signed_in && board.is_comment

          BadRequest(
            views.html.post.read(post_and_member, Some(hasErrors, comments, PaginationInfo(1, 5, comments_count)), attachments, is_own, is_reply, is_comment)
            (
              board.name,
              posts,
              PaginationInfo(page, post_page_length, all_posts_count),
              routes.PostController.list(board_seq, _),
              Some(board_seq)
            )
          )
        }
      },
      form => {
        val author_seq = request.member.seq
        comments_repo
          .insert(form, author_seq, request.remoteAddress)
          .map { result =>
            Redirect(routes.PostController.read(board_seq, form._1, getReadPage))
          }
      }
    )
  }

  def deleteComment(board_seq: Long, post_seq: Long, comment_seq: Long) = auth.async { implicit request =>
    val author_seq = request.member.seq
    comments_repo.delete(comment_seq, author_seq).map { _ =>
      Redirect(routes.PostController.read(board_seq, post_seq, getReadPage))
    }
  }


  private def changeDomainOnForm(form: (Long, String, String, Seq[String], Option[Long])) = {
    val content = form._3.replace(images_url, cloud_front_url )
    form.copy(_3 = content)
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
      "upload_files" -> seq(text),
      "reply_post_seq" -> optional(longNumber)
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

/*      val perms = java.util.EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
      val attr = PosixFilePermissions.asFileAttribute(perms)
      val path = JFiles.createTempFile("multipartBody", "tempFile", attr)
      val file = path.toFile

      val fileSink = FileIO.toPath(path)
      val accumulator = Accumulator(fileSink)
      accumulator.map { case IOResult(count, status) =>
        contentType match {
          case Some("image/jpeg") | Some("image/png") | Some("image/gif") | Some("image/tiff") =>{
            val image: Image = Image.fromFile(file)
            image.output(file)(JpegWriter())
            Logger.debug(s"this file is image.")
          }
          case _ => Logger.debug(s"this file is not image.")
        }
        FilePart(partName, filename, contentType, file)
      }*/

    val fixed_file_name = if (filename.contains("\\")) {
        filename.substring(filename.lastIndexOf("\\") + 1)
      } else {
        filename
      }
      val new_file_hash = DigestUtils.md5Hex(s"${System.currentTimeMillis()}_$fixed_file_name")
      val file = new File(temp_dir_path, new_file_hash)

      val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(file.toPath)

      val accumulator: Accumulator[ByteString, IOResult] = Accumulator(
        fileSink)
      accumulator.map {
        case IOResult(count, status) =>
          Logger.debug(s"count = $count, status = $status")

          contentType match {
            case Some("image/jpeg") | Some("image/png") | Some("image/gif") | Some("image/tiff") =>{
              val image: Image = Image.fromFile(file)
              image.output(file)(JpegWriter())
              Logger.debug(s"this file is image.")
            }
            case _ => Logger.debug(s"this file is not image.")
          }

          FilePart(partName, fixed_file_name, contentType, file)
      }
  }

  private def insertAttachment(post_seq: Long, seq_file_info: Seq[String]) = {
      seq_file_info.foreach {
        json_str =>
          Json
            .fromJson[UploadedFileInfo](Json.parse(json_str))
            .map {
              (file_info: UploadedFileInfo) =>
                val full_path = s"${temp_dir_path}/${file_info.hash}"
                val path = Paths.get(full_path)
                if (JFiles.exists(path)) {
                  val sub_path = java.time.LocalDate.now.toString

                  implicit val app = appProvider.get()
                  val bucket = S3("com.teamgehem.files")

                  try {
                    val fis = new FileInputStream(full_path)
                    val size = Streamable.bytes(fis)
                    fis.close()
                    JFiles.delete(path)

                   (bucket + BucketFile(file_info.hash, file_info.content_type, size))
                      .map { _ =>
                        Logger.debug("Saved the file")
                        attachments_repo.insertAttachment(
                          file_info.hash,
                          file_info.file_name,
                          sub_path,
                          file_info.content_type,
                          file_info.size,
                          post_seq)
                      }
                      .recover {
                        case S3Exception(status, code, message, originalXml) => Logger.debug(s"Bucket Send File Error: $message")
                      }
                  } catch {
                    case e: Exception => Logger.debug(s"File Error: ${e.getMessage}")
                  }

                  /*                val new_directory_path =
                  Path(s"$attachment_path/${sub_path}")
                if (!new_directory_path.exists) {
                  new_directory_path.createDirectory()
                }

                val new_file_path = Paths.get(
                  s"${new_directory_path.path}/${file_info.hash}")
                Files.move(path, new_file_path)*/


                }
            }
      }
  }

  private val post_form = Form(
    tuple(
      "board_seq" -> longNumber,
      "subject" -> nonEmptyText(maxLength = 80),
      "content" -> text,
      "upload_files" -> seq(text),
      "reply_post_seq" -> optional(longNumber)
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

  //implicit val commentWrites = Json.writes[Comment]
  //implicit val commentTupleWrites = Json.writes[Seq[(Comment, String, Option[String])]]

  /*  implicit def tuple2Writes[A, B, C](implicit a: Writes[A], b: Writes[B], c: Writes[C]): Writes[Tuple3[A, B,C]] = new Writes[Tuple3[A, B, C]] {
      def writes(tuple: Tuple3[A, B, C]) = JsArray(Seq(a.writes(tuple._1), b.writes(tuple._2), c.writes(tuple._3)))
    }*/
}
