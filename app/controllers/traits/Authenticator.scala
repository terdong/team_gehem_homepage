package controllers.traits
import controllers.{AccountController, routes}
import play.api.mvc._;

/**
  * Created by terdo on 2017-04-21 021.
  */
trait Secured {

  def memberInfo(request: RequestHeader): Option[(String, String)] = {
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
}
