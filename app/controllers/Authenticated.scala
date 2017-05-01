package controllers

import play.api.mvc
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc._

import scala.concurrent.Future

/**
  * Created by terdo on 2017-04-21 021.
  */
case class Authentication(email: String, permission: String)
class CustomAuthenticatedRequest[A](val auth: Authentication,
                                    request: Request[A])
    extends WrappedRequest[A](request)

object Authenticated extends mvc.ActionBuilder[CustomAuthenticatedRequest] {
  lazy val email: String = "email"
  lazy val permission: String = "permission"

  def getAuthentication(request: RequestHeader) = {
    for {
      email <- request.session.get(email)
      permission <- request.session.get(permission)
    } yield Authentication(email, permission)
  }

  def onUnauthorized(request: RequestHeader) =
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

/*object Authenticated {

  trait Authenticated {

    def getAuthentication(request: RequestHeader) = {
      for {
        email <- request.session.get(email)
        permission <- request.session.get(permission)
      } yield Authentication(email, permission)
    }

    def onUnauthorized(request: RequestHeader) =
      Results.Redirect(routes.AccountController.createSignInForm)

    def isAuthenticated(
        f: => Authentication => Request[AnyContent] => Result) = {
      play.api.mvc.Security.Authenticated(getAuthentication, onUnauthorized) {
        auth =>
          Action(request => f(auth)(request))
      }
    }

    def isAuthenticatedAsync(
        f: => Authentication => Request[AnyContent] => Future[Result]) = {
      Action.async { request =>
        getAuthentication(request)
          .map { auth =>
            f(auth)(request)
          }
          .getOrElse(Future.successful(onUnauthorized(request)))
      }
    }
  }*/
/*   def isAuthenticated(f: => String => Request[AnyContent] => Result) = {
      Security.Authenticated { user =>
        Action(request => f(user)(request))
      }
    }*/
/*  def memberInfo(request: RequestHeader): Option[(String, String)] = {
    for {
      email <- request.session.get(AccountController.EMAIL_KEY)
      permission <- request.session.get(AccountController.PERMISSION_KEY)
    } yield (email, permission)
  }

  def username(request: RequestHeader): Option[String] =
    request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = {
    Results.Redirect(routes.AccountController.signin()).withNewSession
  }

  def withAuth(f: => (String) => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }
}*/

/*class AuthenticatedDbRequest[A](val user: User,
                                val conn: Connection,
                                request: Request[A])
    extends WrappedRequest[A](request)

object Authenticated extends ActionBuilder[AuthenticatedDbRequest] {
  def invokeBlock[A](request: Request[A],
                     block: (AuthenticatedDbRequest[A]) => Future[Result]) = {
    AuthenticatedBuilder(req => getUserFromRequest(req))
      .authenticate(request, { authRequest: AuthenticatedRequest[A, User] =>
        DB.withConnection { conn =>
          block(new AuthenticatedDbRequest[A](authRequest.user, conn, request))
        }
      })
  }
}*/

/*
case class AuthorizedRequest[A](request: Request[A], member: Member)
    extends WrappedRequest(request)

object AuthorizedAction extends ActionBuilder[AuthorizedRequest] {
  override def invokeBlock[A](
      request: Request[A],
      block: (AuthorizedRequest[A]) ⇒ Future[Result]): Future[Result] = {
    request.headers
      .get(AuthTokenHeader)
      .orElse(request.getQueryString(AuthTokenUrlKey)) match {
      case Some(token) =>
        userService.findByToken(token).map {
          case Some(user) =>
            val req = AuthorizedRequest(request, user)
            block(req)
          case None => Future.successful(Results.Unauthorized)
        }
      case None => Future.successful(Results.Unauthorized)
    }
  }
}
 */
