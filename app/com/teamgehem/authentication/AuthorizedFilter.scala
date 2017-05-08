package com.teamgehem.authentication

import play.api.mvc.{ActionFilter, Results}

import scala.concurrent.Future

/**
  * Created by terdo on 2017-05-06 006.
  */
case class AuthorizedFilter(requiredPermissions: Byte = 0)
    extends ActionFilter[CustomAuthenticatedRequest] {
  override def filter[A](request: CustomAuthenticatedRequest[A]) = {
    val result =
      if (request.auth.permission >= requiredPermissions)
        None
      else Some(Results.Unauthorized(views.html.error_pages.HTTP401()))

    Future.successful(result)
  }
}
