package controllers.traits

import controllers.routes
import play.api.mvc._

import scala.concurrent.Future

/**
  * Created by terdo on 2017-04-21 021.
  */
trait Security {

  def memberEmail(request: RequestHeader) =
    request.session.get(Security.username)
  def onUnauthorized(request: RequestHeader) =
    Results.Redirect(routes.AccountController.createSignInForm)
  def isAuthenticated(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(memberEmail, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  def isAuthenticatedAsync(
      f: => String => Request[AnyContent] => Future[Result]) = {
    Action.async { request =>
      memberEmail(request)
        .map { login =>
          f(login)(request)
        }
        .getOrElse(Future.successful(onUnauthorized(request)))
    }
  }

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
  }*/
}

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
