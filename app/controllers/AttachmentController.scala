package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.security.AuthenticatedActionBuilder
import org.apache.commons.codec.digest.DigestUtils
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import play.api.{Configuration, Logger}
import repositories._
import views.html.helper.CSRF

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by terdo on 2017-05-19 019.
  */
@Singleton
class AttachmentController @Inject()(cache: AsyncCacheApi,
                                     config: Configuration,
                                     auth: AuthenticatedActionBuilder,
                                     cc: MessagesControllerComponents,
                                     attachments_repo: AttachmentsRepository)
  extends MessagesAbstractController(cc) {

  def file(hash: String) = auth.async { request =>
    attachments_repo
      .getAttachment(hash)
      .map { attachment =>
        val full_path =
          s"${files_path}/${attachment.sub_path}/${attachment.hash}"
        val file = new java.io.File(full_path)

        if (file != null && !file.exists) {
          Logger.error(
            s"Attachment Seq: ${attachment.seq} (${full_path}) does not exist on storage")
          InternalServerError("The file does not exist")
        } else {
          Ok.sendFile(file, false, _ => attachment.name).as(attachment.mime_type)
        }
      }
  }

  def files(post_seq: Long) = auth.async { implicit request =>
    attachments_repo.getAttachments(post_seq).map { attachments =>
      val jsons: Seq[JsObject] = for {
        attachment <- attachments
      } yield
        Json.obj(
          "name" -> attachment.name,
          "uuid" -> attachment.hash,
          "size" -> attachment.size,
          "is_image" -> attachment.mime_type.split("/")(0).equals("image"),
          "deleteFileParams" -> Json.obj("sub_path" -> attachment.sub_path, "csrfToken" -> CSRF.getToken.value)
        )
      Ok(Json.toJson(jsons))
    }
  }

  def images(hash: String) = Action.async { implicit request =>
    attachments_repo.getAttachmentWithoutCount(hash).map { attachment_option =>
      attachment_option.map { attachment =>
        if (!attachment.mime_type.split("/")(0).equals("image")) {
          BadRequest("this file is not image file.");
        }
        val e_tag =
          s""""${
            DigestUtils
              .md5Hex(s"${attachment.hash}${attachment.seq}")
          }""""

        val header_cache_control = (CACHE_CONTROL -> cache_value)
        val header_etag = (ETAG -> e_tag)

        request.headers.get(IF_NONE_MATCH) match {
          case Some(etags) if etags == e_tag =>
            NotModified.withHeaders(header_cache_control, header_etag)
          case None => {
            val full_path =
              s"${files_path}/${attachment.sub_path}/${attachment.hash}"
            val file = new java.io.File(full_path)

            if (file != null && !file.exists) {
              Logger.debug(s"image hash: ${attachment.seq} (${full_path}) does not exist on storage")
              //InternalServerError("The file does not exist")
              Redirect(routes.Assets.versioned("images/image_not_found.jpg"))
            } else {
              Ok.sendFile(file, true, _ => attachment.name)
                .as(attachment.mime_type)
                .withHeaders(header_cache_control, header_etag)
            }
          }
        }
      }.getOrElse {
        val full_path = s"${temp_dir_path}/$hash"
        val file = new java.io.File(full_path)

        if (file != null && !file.exists) {
          Logger.debug(s"temp image hash: $hash does not exist on storage")
          //InternalServerError("The file does not exist")
          Redirect(routes.Assets.versioned("images/image_not_found.jpg"))
        } else {
          Ok.sendFile(file)
        }
      }
    }
  }

  private lazy val cache_value = if (play.Environment.simple().isProd) {
    default_cache_control
  } else {
    "no-cache"
  }

  private lazy val default_cache_control = config.get[String]("attachments.aggressiveCache")

  private lazy val files_path = config.get[String]("uploads.path")

  private lazy val temp_dir_path: String = System.getProperty("java.io.tmpdir")
}
