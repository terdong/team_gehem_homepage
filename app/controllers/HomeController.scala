package controllers

import javax.inject._

import play.api.Logger
import play.api.mvc._

@Singleton
class HomeController @Inject()(cc: MessagesControllerComponents)
    extends MessagesAbstractController(cc) {
  def index = {

    val a: Action[AnyContent] = Action { implicit request =>
      Logger.debug(request.headers.headers.mkString("\n"))
      Ok(views.html.index("Your new application is ready."))
    }

    Logger.debug(s"action's hashcode = ${a.hashCode()}")
    a
  }

  def result() = Action { implicit request =>
    Ok(views.html.result())
  }
}
