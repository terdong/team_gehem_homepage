package controllers

import javax.inject.{Inject, Singleton}

import org.apache.commons.codec.digest.DigestUtils
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
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

  lazy val default_cache_control = config
    .getString("attachments.aggressiveCache")
    .getOrElse("private, max-age=3600")

  lazy val images_path = config.getString("uploads.path.images").get

  def images(hash: String) = Action.async { implicit request =>
    attachments_repo
      .getAttachment(hash)
      .map { attachment =>
        val e_tag =
          s""""${DigestUtils
            .md5Hex(s"${attachment.hash}${attachment.seq}")}""""

        val cache_value = if (play.Environment.simple().isProd) {
          default_cache_control
        } else {
          "no-cache"
        }
        val header_cache_control = (CACHE_CONTROL -> cache_value)
        val header_etag = (ETAG -> e_tag)

        request.headers.get(IF_NONE_MATCH) match {
          case Some(etags) if etags == e_tag =>
            NotModified.withHeaders(header_cache_control, header_etag)
          case None => {
            val full_path =
              s"${images_path}/${attachment.sub_path}/${attachment.hash}"
            val file = new java.io.File(full_path)

            if (file != null && !file.exists) {
              Logger.error(
                s"Attachment Seq: ${attachment.seq} ($full_path) does not exist on storage")
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
}
