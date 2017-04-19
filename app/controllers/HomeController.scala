package controllers

import javax.inject._

import com.typesafe.config.ConfigFactory
import play.api._
import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, _}
import play.api.http.HttpErrorHandler
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.ExecutionContext

import traits.AccountInfo

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(implicit exec: ExecutionContext,
                               errorHandler: HttpErrorHandler,
                               val messagesApi: MessagesApi,
                               config: play.api.Configuration)
    extends Controller
    with I18nSupport
    with AccountInfo {

  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  val writeForm = Form(
    tuple(
      "title" -> nonEmptyText(maxLength = 20),
      "contents" -> nonEmptyText
    ))

  def flash = Action {
    Ok.flashing(("hi" -> "안녕하세요?"))
  }

  def json = Action {
    import play.api.libs.json.Json
    val success = Map("status" -> "success")
    val json: JsValue = Json.toJson(success)
    Ok(json)
  }

  def error = Action.async { req =>
    errorHandler.onClientError(req, FORBIDDEN)
  }

  def testparam(page: Int) = Action {
    Ok(page.toString)
  }

  def index = Action { implicit request =>
    Logger.debug(request.headers.headers.mkString("\n"))
    Ok(views.html.index("Your new application is ready."))
  //Ok("Index")
  }

  def session = Action { implicit request =>
    Logger.debug(ConfigFactory.load().getString("version"))
    Ok(views.html.session("Your new application is ready."))
      .withSession("connected" -> "동희")
  }

  def authorized = Action { implicit request =>
    request.session
      .get("connected")
      .map { user =>
        Ok("Hello " + user)
      }
      .getOrElse {
        Unauthorized("Oops, you are not connected")
      }
  }

  def list = Action {
    Ok(views.html.Board.list())
  }

  def write = Action { implicit request =>
    val new_form = writeForm.bindFromRequest

    Ok(views.html.Board.write(writeForm))
  }

  def writeResult = Action { implicit request =>
    val new_form = writeForm.bindFromRequest

    Ok(views.html.Board.write(new_form))
  }

  /*  def formtest = Action {
      request =>
        Logger.debug(messagesApi("greeting"))
        Logger.debug(Messages("greeting"))

        Logger.debug("한글")

        Logger.debug(request.acceptLanguages.map(_.code).mkString(", "))
        Ok(views.html.form_test(login_form))
    }*/

  def str2 = Action { request =>
    Ok(request.queryString.mkString("\n"))
  }

  def str(str: String) = Action { request =>
    Logger.debug(request.queryString.mkString("\n"))

    Ok(request.queryString.mkString("\n"))
  }
}
