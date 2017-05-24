package com.teamgehem.authentication

import controllers.routes
import play.api.mvc
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc.{Result, _}

import scala.concurrent.Future

/**
  * Created by terdo on 2017-04-21 021.
  */
case class Authentication(seq: Long, email: String, permission: Byte)
class CustomAuthenticatedRequest[A](val auth: Authentication,
                                    request: Request[A])
    extends WrappedRequest[A](request)

object Authenticated extends mvc.ActionBuilder[CustomAuthenticatedRequest] {
  lazy val seq: String = "seq"
  lazy val email: String = "email"
  lazy val permission: String = "permissions"

  private def getAuthentication(request: RequestHeader) = {
    val result = for {
      seq <- request.session.get(seq)
      email <- request.session.get(email)
      permission <- request.session.get(permission)
    } yield Authentication(seq.toLong, email, permission.toByte)
    result
  }

  private def onUnauthorized(request: RequestHeader) =
    Results.Redirect(routes.AccountController.createSignInForm)

  def invokeBlock[A](
      request: Request[A],
      block: (CustomAuthenticatedRequest[A]) => Future[Result]) = {
    AuthenticatedBuilder(req => getAuthentication(req), onUnauthorized)
      .authenticate(request, {
        authRequest: AuthenticatedRequest[A, Authentication] =>
          block(new CustomAuthenticatedRequest[A](authRequest.user, request))
      })
  }
}
