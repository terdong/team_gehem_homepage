package services

import javax.inject.Singleton

import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent._;

/**
  * Created by terdong on 2017-03-30 030.
  */
@Singleton
class ErrorHandler extends HttpErrorHandler {
  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val r = statusCode match {
      case BAD_REQUEST =>
        views.html.error_pages.HTTP400()
      case UNAUTHORIZED =>
        views.html.error_pages.HTTP401()
      case FORBIDDEN =>
        views.html.error_pages.HTTP403()
      case NOT_FOUND =>
        views.html.error_pages.HTTP404()
      case _ =>
        null
    }
    Future.successful(
      Status(statusCode)(r)
    )
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Logger.error("A server error occurred: " + exception.getMessage)
    Future.successful(
      Status(INTERNAL_SERVER_ERROR)(views.html.error_pages.HTTP500())
      //InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }
}
