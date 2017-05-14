package controllers

import javax.inject._

import controllers.traits._
import play.api._
import play.api.cache.CacheApi
import play.api.http.HttpErrorHandler
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(implicit exec: ExecutionContext,
                               cache: CacheApi,
                               errorHandler: HttpErrorHandler,
                               val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport
    with ProvidesHeader {

  def index = Action { implicit request =>
    Logger.debug(request.headers.headers.mkString("\n"))
    Ok(views.html.index("Your new application is ready."))
  }

  def result() = Action { implicit request =>
    Ok(views.html.result())
  }
}
