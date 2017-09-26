package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.controller.TGBasicController
import com.teamgehem.security.AuthenticatedActionBuilder
import play.api.cache.SyncCacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by terdo on 2017-05-08 008.
  */
@Singleton
class TestController @Inject()(config: Configuration,
                               mcc: MessagesControllerComponents,
                               sync_cache:SyncCacheApi,
                               auth: AuthenticatedActionBuilder)
    extends TGBasicController(mcc, sync_cache) {

  def test_js_routes_view = Action{ implicit request =>
    Ok(views.html.test.test_js_route())
  }

  def test_js_routes = Action{
    Ok("test_js_routes!")
  }

  def test_auth_admin = auth.authrized_semi_admin{ implicit request =>
    Ok("admin")
  }
  def test_auth_member = auth.authrized_member{ implicit request =>
    Ok("member")
  }

  def test_form = Action{ implicit request:MessagesRequest[AnyContent] =>
    Ok(views.html.test.test_form(userForm))
  }

  def test_form_post = TODO

  val userForm = Form(
    mapping(
      "name" -> text,
      "age" -> number
    )(UserData.apply)(UserData.unapply)
  )

  def test_option_param(param: Option[String]) = Action {
    Ok(s"param = $param")
  }

  def test_default_param(param: Int) = Action {
    Ok(s"param = ${param}")
  }

  def test_sendFile2(name: String) = Action {
    val image_path = config.get[String]("uploads.path.images")
    val file = new java.io.File(s"${image_path}/$name")

    Ok.sendFile(file).withHeaders(CONTENT_TYPE -> "image/jpeg")
  }

  def test_sendFile = Action {
    val image_path = config.get[String]("uploads.path.images")
    Ok.sendFile(new java.io.File(s"${image_path}/욕 신고.png"))
  }

  def test_configuration = Action {
    Ok(config.get[String]("uploads.path.files"))
  }

  def test_assets = Action {

    /*    val asset =
      routes.Assets
        .versioned("public", "upload/images/2017-05-17/img_14949593930301.jpg")*/

    /*   val asset2: Call = routes.Assets
      .versioned("upload/images/2017-05-17/img_14949593930301.jpg")*/

    //val url = asset2.url
    //val a = asset2.path()
    /*
    routes.Assets.versioned("public",
                            "upload/images/2017-05-17/img_14949593930301.jpg")*/

    Ok("ok")
  }

  def test_flash = Action { implicit request =>
    Ok(views.html.test.test_flash()).flashing("hi" -> "안녕ㅎ?")
  }

  def test_json = Action {
    val map_status = Map("status" -> "success")
    val json = Json.toJson(map_status)
    Ok(json)
  }

  /*  def test_error = Action.async { request =>
    errorHandler.onClientError(request, FORBIDDEN)
  }*/

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

  def test_messages = Action { implicit request: MessagesRequest[AnyContent] =>
    val messages: Messages = request.messages
    Ok(messages("test.messages"))
  }

  def test_messages2 = Action { implicit request =>
    Ok(Messages("result.button"))
  }
}

case class UserData(name: String, age: Int)

