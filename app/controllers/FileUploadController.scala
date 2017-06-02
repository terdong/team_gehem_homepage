package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.authentication.Authenticated
import controllers.traits.ProvidesHeader
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import repositories._

import scala.concurrent.Future

/**
  * Created by terdo on 2017-05-27 027.
  */
@Singleton
class FileUploadController @Inject()(implicit cache: CacheApi,
                                     config: Configuration,
                                     posts_repo: PostsRepository,
                                     attachments_repo: AttachmentsRepository,
                                     val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport
    with ProvidesHeader {
  case class FormData(name: String)

  private val file_form = Form(
    mapping(
      "name" -> text
    )(FormData.apply)(FormData.unapply)
  )

  def uploadFile = Authenticated.async(parse.multipartFormData) {
    implicit request =>
      Future.successful(Ok("ok"))

  }
}
