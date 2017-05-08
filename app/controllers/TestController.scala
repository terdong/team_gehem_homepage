package controllers

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._

/**
  * Created by terdo on 2017-05-08 008.
  */
@Singleton
class TestController @Inject()(implicit errorHandler: HttpErrorHandler,
                               val messagesApi: MessagesApi,
                               config: play.api.Configuration)
    extends Controller
    with I18nSupport {

  def test_flash = Action { implicit request =>
    Ok(views.html.test.test_flash()).flashing("hi" -> "안녕ㅎ?")
  }

  def test_json = Action {
    val map_status = Map("status" -> "success")
    val json = Json.toJson(map_status)
    Ok(json)
  }

  def test_error = Action.async { request =>
    errorHandler.onClientError(request, FORBIDDEN)
  }

  def test_param(param: Int) = Action {
    Ok(param.toString)
  }

  def test_set_authorized = Action {
    Ok("set session complete!").withSession(("connected" -> "DongHee"))
  }

  def test_get_authorized = Action { implicit request =>
    request.session
      .get("connected")
      .map { user =>
        Ok(s"Hello $user")
      }
      .getOrElse(Unauthorized("Ooops, you are not connected"))
  }

  def test_set_cookie = Action {
    val c = Cookie("cookie_test", "this_is_test_for_cookie")
    Ok("set cookie complete").withCookies(c)
  }

  def test_remove_cookie = Action {
    Ok("cookie clean complete").discardingCookies(
      DiscardingCookie("cookie_test"))
  }

  def test_get_request = Action { request =>
    val str = request.queryString.mkString("\n")
    Logger.debug(str)
    Ok(str)
  }

  def test_get_headers = Action { request =>
    val headers = request.headers.headers.mkString("\n")
    Logger.debug(headers)
    Ok(headers)
  }

  def test_messages = Action {
    Ok(Messages("test.messages"))
  }
}
