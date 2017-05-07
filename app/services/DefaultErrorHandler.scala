package services

import javax.inject._

import play.api.http.DefaultHttpErrorHandler
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router
import scala.concurrent._

/**
  * Created by terdo on 2017-05-06 006.
  */
@Singleton
class DefaultErrorHandler @Inject()(
    env: Environment,
    config: Configuration,
    sourceMapper: OptionalSourceMapper,
    router: Provider[Router]
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onProdServerError(request: RequestHeader,
                                 exception: UsefulException) = {
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }

  override def onForbidden(request: RequestHeader, message: String) = {
    Logger.debug(message)
    Future.successful(
      Forbidden("You're not allowed to access this resource.")
    )
  }

  override def onNotFound(request: RequestHeader,
                          message: String): Future[Result] = {
    Future.successful(NotFound("Hello?"))
  }
}
