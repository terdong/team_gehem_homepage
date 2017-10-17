package com.teamgehem.errors

import javax.inject._

import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent._

/**
  * Created by DongHee Kim on 2017-10-05 005.
  */
@Singleton
class ErrorHandler @Inject()(env: Environment,
                             config:Configuration,
                             sourceMapper:OptionalSourceMapper,
                             router:Provider[Router]) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }

/*  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)("A client error occurred: " + message)
    )
  }*/

  override def onForbidden(request: RequestHeader, message: String) = {
    Future.successful(
      Forbidden("You're not allowed to access this resource.")
    )
  }

/*  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(
      BadRequest(message)
    )
  }*/
}