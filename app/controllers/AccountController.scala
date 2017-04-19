package controllers

import javax.inject.{Inject, Singleton}

import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, _}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, Controller}
import repositories.MembersRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Created by terdo on 2017-04-12 012.
  */
case class SignIn(email: String, checkbox: Boolean)

object AccountController {
  val EMAIL_KEY = "email"
  val PERMISSION_KEY = "permission"
}

@Singleton
class AccountController @Inject()(member_rep: MembersRepository,
                                  val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport {

  def emailExists(email: String): Boolean = {
    Await.result(member_rep.existsEmail(email), Duration.Inf)
  }

  val signin_form: Form[SignIn] = Form {
    mapping(
      "email" -> email.verifying(Messages("account.signin.exists.email"),
                                 emailExists(_)),
      "remember" -> boolean
    )(SignIn.apply)(SignIn.unapply)
  }

  val signup_form = Form {
    tuple(
      "email" -> email.verifying(Messages("account.signup.exists.email"),
                                 !emailExists(_)),
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
        //Future.successful(Ok("test"))
        member_rep.insert(form) map (_ match {
          case Success(member) =>
            Ok(views.html.Account.signup_complete(member.email, signin_form))
          case Failure(e) => throw e
        })
      }
    )
  }

  def createSignInForm = Action {
    Ok(views.html.Account.signin(signin_form))
  }

  def signin = Action.async { implicit request =>
    signin_form.bindFromRequest.fold(
      hasErrors => Future.successful(Ok(views.html.Account.signin(hasErrors))),
      form => {
        member_rep.finByEmail(form.email) map (_ match {
          case Success(member) =>
            Redirect(routes.HomeController.index()).withSession(
              AccountController.EMAIL_KEY -> form.email,
              AccountController.PERMISSION_KEY -> member.permission)
          case Failure(e) => throw e
        })
      }
    )
  }

  def signout = Action { request =>
    Redirect(routes.HomeController.index()).withNewSession
  }

  def memberInsert = Action { implicit request =>
    Ok("Hello")
  }

}