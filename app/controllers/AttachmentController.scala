package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.authentication.Authenticated
import org.apache.commons.codec.digest.DigestUtils
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, Controller}
import repositories.AttachmentsRepository

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by terdo on 2017-05-19 019.
  */
@Singleton
class AttachmentController @Inject()(implicit val messagesApi: MessagesApi,
                                     attachments_repo: AttachmentsRepository,
                                     config: play.api.Configuration)
    extends Controller
    with I18nSupport {

  def file(hash: String) = Authenticated.async { request =>
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
          Ok.sendFile(file, false, _ => attachment.name)
        }
      }
  }
  def files(post_seq: Long) = Authenticated.async { request =>
    attachments_repo.getAttachments(post_seq).map { attachments =>
      val jsons: Seq[JsObject] = for {
        attachment <- attachments
      } yield
        Json.obj(
          "name" -> attachment.name,
          "uuid" -> attachment.hash,
          "size" -> attachment.size,
          "is_image" -> attachment.mime_type.split("/")(0).equals("image"),
          "deleteFileParams" -> Json.obj("sub_path" -> attachment.sub_path)
        )
      Ok(Json.toJson(jsons))
    }
  }

  def images(hash: String) = Action.async { implicit request =>
    attachments_repo
      .getAttachmentWithoutCount(hash)
      .map { attachment =>
        if (!attachment.mime_type.split("/")(0).equals("image")) {
          BadRequest("this file is not image file.");
        }

        val e_tag =
          s""""${DigestUtils
            .md5Hex(s"${attachment.hash}${attachment.seq}")}""""

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
              Logger.error(
                s"Attachment Seq: ${attachment.seq} (${full_path}) does not exist on storage")
              InternalServerError("The file does not exist")
            } else {
              Ok.sendFile(file, true, _ => attachment.name)
                .as(attachment.mime_type)
                .withHeaders(header_cache_control, header_etag)
            }
          }
        }
      }
  }

  private lazy val cache_value = if (play.Environment.simple().isProd) {
    default_cache_control
  } else {
    "no-cache"
  }

  private lazy val default_cache_control = config
    .getString("attachments.aggressiveCache")
    .getOrElse("private, max-age=3600")

  private lazy val files_path = config.getString("uploads.path").get
}
