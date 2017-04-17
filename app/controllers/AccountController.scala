package controllers

import javax.inject.{Inject, Singleton}

import models.MemberDataAccess
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, _}

import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Created by terdo on 2017-04-12 012.
  */

case class SignIn(email: String, checkbox: Boolean)

@Singleton
class AccountController @Inject()(member_dao: MemberDataAccess, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  def emailExists(email: String): Boolean = {
    Await.result(member_dao.existsEmail(email), Duration.Inf)
  }

  val signin_form: Form[SignIn] = Form {
    mapping(
      "email" -> email,
      "remember" -> boolean
    )(SignIn.apply)(SignIn.unapply)
  }

  val signup_form = Form {
    tuple(
      "email" -> email.verifying (Messages("account.exists.email"), !emailExists(_)),
      "name" -> nonEmptyText(2),
      "nickName" -> nonEmptyText(2)
    )
  }

  def createSignUpForm = Action {
    Ok(views.html.Account.signup(signup_form))
  }

  def signup = Action.async { implicit request =>
    val form = signup_form.bindFromRequest
    form.fold(
      hasErrors =>
        Future.successful(BadRequest(views.html.Account.signup(hasErrors))),
      (form: (String, String, String)) => {
        member_dao.insert(form) map (_ match {
          case Success(result) => Ok(views.html.Account.signup_complete(result.email, signin_form))
          case Failure(e) => throw e
        })
      })
  }

  def createSignInForm = Action {
    Ok(views.html.Account.signin(signin_form))
  }

  def signin = Action { implicit request =>
    signin_form.bindFromRequest.fold(
      hasErrors =>
        Ok(views.html.Account.signin(hasErrors)),
      form => {
        Logger.debug(form.toString)

        Redirect(routes.HomeController.index()).withSession("connected" -> form.email)
      }
    )
  }

  def memberInsert = Action {
    implicit request =>
      Ok("Hello")
  }

}