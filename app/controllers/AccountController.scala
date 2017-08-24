package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.security.AuthenticatedActionBuilder
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import repositories.{MembersRepository, PermissionsRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Random, Success}

@Singleton
class AccountController @Inject()(auth: AuthenticatedActionBuilder,
                                  mcc: MessagesControllerComponents,
                                  members_repo: MembersRepository,
                                  permission_repo: PermissionsRepository,
                                  rand: Random)
    extends MessagesAbstractController(mcc) {
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(mcc.langs.availables.head, messagesApi)
  }

  def edit = auth.authrized_member { implicit request =>
    Ok("Hello edit")
  /*    val email = member_email.get
    val form = member_form.bindFromRequest
    form.fold(
      has_errors =>
        for {
          member <- members_repo.findByEmail(email)
          permission_content <- permission_repo.getContentByCode(
            member_permission)
        } yield
          BadRequest(
            views.html.account.edit(has_errors, member, permission_content)),
      form => {
        members_repo.update(email, form) map (_ =>
          Redirect(routes.HomeController.result())
            .flashing("success" -> Messages("account.edit.success")))
      }
    )*/
  }

  def editForm = auth.authrized_member { implicit request =>
    Ok("Hello editForm")
  /*for {
      member <- members_repo.findByEmail(member_email.get)
      permission_content <- permission_repo.getContentByCode(member_permission)
    } yield {
      val form =
        (member.name, member.nick)
      Ok(
        views.html.account
          .edit(member_form.fill(form), member, permission_content))
    }*/
  }

  def createSignUpForm = Action { implicit request =>
    Ok(views.html.account.signup(signup_form))
  }

  /**
    * 회원가입
    *
    * @return
    */
  def signup = Action.async { implicit request =>
    val form = signup_form.bindFromRequest
    form.fold(
      hasErrors =>
        Future.successful(BadRequest(views.html.account.signup(hasErrors))),
      (form: (String, String, String)) => {
        members_repo.insert(form) map (_ match {
          case Success(member) =>
            Ok(views.html.account.signup_complete(member.email, signin_form))
          case Failure(e) => throw e
        })
      }
    )
  }

  def createSignInForm = Action { implicit request =>
    Ok(views.html.account.signin(signin_form))
  }

  /**
    * 회원인증
    *
    * @return
    */
  def signin = Action.async { implicit request =>
    signin_form.bindFromRequest.fold(
      hasErrors => Future.successful(Ok(views.html.account.signin(hasErrors))),
      form => {
        members_repo.findByEmail(form.email) map { member =>
          members_repo.updateLastSignin(member.seq)
          Redirect(
            routes.HomeController
              .index())
            .withSession("seq" -> member.seq.toString,
                         "email" -> member.email,
                         "permission" -> member.permission.toString)
        }
      }
    )
  }

  def signout = Action { implicit request =>
    Redirect(routes.HomeController.index()).withNewSession
  }

  private val signin_form: Form[SignIn] = Form {
    mapping(
      "email" -> email.verifying(Messages("account.signin.exists.email"),
                                 emailExists(_)),
      "remember" -> boolean
    )(SignIn.apply)(SignIn.unapply)
  }

  private val signup_form = Form {
    tuple(
      "email" -> email.verifying(Messages("account.signup.exists.email"),
                                 !emailExists(_)),
      "name" -> nonEmptyText(2),
      "nickName" -> nonEmptyText(2)
    )
  }

  private def emailExists(email: String): Boolean = {
    Await.result(members_repo.existsEmail(email), Duration.Inf)
  }
}

case class SignIn(email: String, checkbox: Boolean)
