package com.teamgehem.helper

import play.api.Logger
import play.api.mvc.{ControllerHelpers, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
/**
  * Created by DongHee Kim on 2017-09-25 025.
  */
trait DbResultChecker {
  self: ControllerHelpers =>

  protected implicit def checkFutureTry[A](f: Future[Try[A]])(implicit result:Result): Future[Result] = {
    f.map(_.fold(e => {Logger.debug(e.getMessage);InternalServerError(views.html.error_pages.HTTP500())}, r => result))
  }
  protected implicit def checkFutureTry2[A](f: Future[Try[A]])(implicit result: A => Result): Future[Result] = {
    f.map(_.fold(e => {Logger.debug(e.getMessage);InternalServerError(views.html.error_pages.HTTP500())}, r => result(r)))
  }
  protected implicit def checkFutureTry3[A](f: Future[Try[A]])(implicit result: A => Future[Result]): Future[Result] = {
    f.flatMap(_.fold(e => {Logger.debug(e.getMessage);Future.successful(InternalServerError(views.html.error_pages.HTTP500()))}, r => result(r)))
  }
}
