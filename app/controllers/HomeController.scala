package controllers

import javax.inject._

import com.typesafe.config.ConfigFactory
import play.api._
import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, _}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext;

case class Login(email: String, password: String, checkbox: Boolean)

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(implicit exec: ExecutionContext, val messagesApi: MessagesApi, config: play.api.Configuration) extends Controller with I18nSupport {

  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */

  val login_form: Form[Login] = Form {
    mapping(
      "email" -> email,
      "password" -> nonEmptyText(4, 12),
      "remember" -> boolean
    )(Login.apply)(Login.unapply)
  }

  val writeForm = Form(tuple(
    "title" -> nonEmptyText(maxLength = 20),
    "contents" -> nonEmptyText
  ))

  def index = Action { implicit request =>
    Logger.debug(request.headers.headers.mkString("\n"))
    Ok(views.html.index("Your new application is ready."))
    //Ok("Index")
  }

  def session = Action { implicit request =>
    Logger.debug(ConfigFactory.load().getString("version"))
    Ok(views.html.session("Your new application is ready.")).withSession("connected" -> "동희")
  }

  def authorized = Action { implicit request =>
    request.session.get("connected").map { user =>
      Ok("Hello " + user)
    }.getOrElse {
      Unauthorized("Oops, you are not connected")
    }
  }

  def login = Action {
    implicit request =>

      /*request.method match {
        case
      }*/

      val form = login_form.bindFromRequest

      form.fold(
        hasErrors = {
          _ =>
            Ok(views.html.Login.login(login_form))
        },
        success = {
          form =>
            Logger.debug(form.toString)
            Ok(form.toString)
        }
      )

    /*form.value match {
      case Some(_) => {
        form.fold(
          hasErrors = { _ =>
            Redirect(routes.HomeController.formtest)
          },
          success = { form =>

            Logger.debug(form.toString)

            Ok(form.toString)
          }
        )
      }
      case None => Ok(views.html.Login.login(login_form))
    }*/
  }

  def list = Action {
    Ok(views.html.Board.list())
  }

  def write = Action {
    implicit request =>
      val new_form = writeForm.bindFromRequest

      Ok(views.html.Board.write(writeForm))
  }

  def writeResult = Action {
    implicit request =>
      val new_form = writeForm.bindFromRequest

      Ok(views.html.Board.write(new_form))
  }

  def formtest = Action {
    request =>
      Logger.debug(messagesApi("greeting"))
      Logger.debug(Messages("greeting"))

      Logger.debug("한글")

      Logger.debug(request.acceptLanguages.map(_.code).mkString(", "))
      Ok(views.html.form_test(login_form))
  }

  def formTestResult = Action {
    implicit request =>
      val new_form = login_form.bindFromRequest

      new_form.fold(
        hasErrors = {
          form =>
            Redirect(routes.HomeController.formtest)
        },
        success = {
          new_form =>
            Ok(new_form.toString)
        }
      )
  }

  def str2 = Action {
    request =>
      Ok(request.queryString.mkString("\n"))
  }

  def str(str: String) = Action {
    request =>

      Logger.debug(request.queryString.mkString("\n"))

      Ok(request.queryString.mkString("\n"))
  }
}
