package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.authentication.Authenticated
import controllers.traits.ProvidesHeader
import models.{Member, Post}
import org.apache.commons.codec.digest.DigestUtils
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms.{tuple, _}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import repositories.{
  AttachmentsRepository,
  BoardsRepository,
  MembersRepository,
  PostsRepository
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
                               val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport
    with ProvidesHeader {
  lazy val images_path = config.getString("uploads.path.images").get

  //case class PostForm("au")

  val postForm = Form(
    tuple(
      "board_seq" -> longNumber,
      "subject" -> nonEmptyText(maxLength = 80),
      "content" -> text,
      "attachments" -> seq(longNumber)
    )
  )

  val page_length = 15

  def listAll(page: Int) = Action.async { implicit request =>
    for {
      posts <- posts_repo.all(page, page_length, permission)
      count <- posts_repo.getPostCount()
    } yield Ok(views.html.post.list(None, posts, page, page_length, count))
  }

  def list(board_seq: Long, page: Int) = Action.async { implicit request =>
    for {
      b <- boards_repo.isListValidBoard(board_seq, permission) if b == true
      name_op <- boards_repo.getNameBySeq(board_seq)
      posts <- posts_repo.listByBoard(board_seq, page, page_length)
      count <- posts_repo.getPostCount(board_seq)
    } yield
      Ok(
        views.html.post
          .list(name_op, posts, page, page_length, count, board_seq))
  //Future.successful(Ok(""))
  }

  def showPost(board_seq: Long, post_seq: Long) = Action.async {
    implicit request =>
      for {
        r <- boards_repo.isListValidBoard(board_seq, permission) if r == true
        post <- posts_repo.getPost(board_seq, post_seq)
      } yield {
        posts_repo.updateHitCount(post._1)
        Ok(
          views.html.post.read(post,
                               member_email
                                 .map(post._2.email.equals(_))
                                 .getOrElse(false)))
      }
  }

  def editPostForm(board_seq: Long, post_seq: Long) =
    Authenticated.async { implicit request =>
      for {
        r <- posts_repo.isOwnPost(post_seq, member_email.get) if r == true
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
          posts_repo.update(form, post_seq) map (_ =>
            Redirect(routes.PostController.showPost(board_seq, post_seq)))
        }
      )
    }

  def writePostForm(board_seq: Long) = Authenticated { implicit request =>
    Ok(views.html.post.write(postForm, board_seq))
  }

  def writePost(board_seq: Long) = Authenticated.async { implicit request =>
    val form = postForm.bindFromRequest
    form.fold(
      hasErrors =>
        Future.successful(
          BadRequest(views.html.post.write(hasErrors, board_seq))),
      form => {
        posts_repo
          .insert(form, request.auth.email, request.remoteAddress) flatMap {
          post =>
            attachments_repo.updateAttachment2(form._4, post.seq).map { _ =>
              Redirect(
                routes.PostController.showPost(post.board_seq, post.seq))
            }

          /* for {
              seq <- form._4
              r <- attachments_repo.updateAttachment(seq, post.seq)
            } yield {
              Redirect(
                routes.PostController.showPost(post.board_seq, post.seq))
            }*/
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
    /*val date: LocalDate = java.time.LocalDate.now
          val path = Path(s"$path2${date}/")
          if (!path.exists) { path.createDirectory() }

          import java.io.File
          val file_name = image.filename
          val contentType = image.contentType
          val hash_input = s"${System.currentTimeMillis()}_$file_name"
          val hash_name = DigestUtils.md5Hex(hash_input)
          val location = s"$date/$hash_input"
          val file_path = s"$path2$location"
          val file: File = image.ref.moveTo(new File(file_path))*/
    //val hash: HashCode = Files.hash(file, Hashing.md5())

    /*       attachments_repo
            .insertAttachment(
              (file_name, hash_name, location, contentType.get, file.length()))
            .map(seq =>
              Ok(Json.obj("location" -> location, "attachment_seq" -> seq)))
        }
        .getOrElse {
          Future.successful(
            Redirect(routes.HomeController.index)
              .flashing("error" -> "Missing file"))
        }*/
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
}
