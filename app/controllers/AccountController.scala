package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.authentication.Authenticated
import controllers.traits.ProvidesHeader
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, _}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, Controller}
import repositories.{MembersRepository, PermissionsRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Created by terdo on 2017-04-12 012.
  */
case class SignIn(email: String, checkbox: Boolean)

/**
  * @param cache
  * @param members_repo
  * @param messagesApi
  */
@Singleton
class AccountController @Inject()(implicit cache: CacheApi,
                                  members_repo: MembersRepository,
                                  permission_repo: PermissionsRepository,
                                  val messagesApi: MessagesApi)
    extends Controller
    with ProvidesHeader
    with I18nSupport {

  def edit = Authenticated.async { implicit request =>
    val email = member_email.get
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
    )
  }
  def editForm = Authenticated.async { implicit request =>
    for {
      member <- members_repo.findByEmail(member_email.get)
      permission_content <- permission_repo.getContentByCode(member_permission)
    } yield {
      val form =
        (member.name, member.nick)
      Ok(
        views.html.account
          .edit(member_form.fill(form), member, permission_content))
    }
  }

  def createSignUpForm = Action { implicit request =>
    Ok(views.html.account.signup(signup_form))
  }

  /**
    * 회원가입
    * @return
    */
  def signup = Action.async { implicit request =>
    val form = signup_form.bindFromRequest
    form.fold(
      hasErrors =>
        Future.successful(BadRequest(views.html.account.signup(hasErrors))),
      (form: (String, String, String)) => {
        //Future.successful(Ok("test"))
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
            .withSession(
              Authenticated.seq -> member.seq.toString,
              Authenticated.email -> member.email,
              Authenticated.permission -> member.permission.toString)
        }
      }
    )
  }

  def signout = Authenticated { implicit request =>
    Redirect(routes.HomeController.index()).withNewSession
  }

  private def emailExists(email: String): Boolean = {
    Await.result(members_repo.existsEmail(email), Duration.Inf)
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

  private val member_form = Form(
    tuple(
      "name" -> nonEmptyText(2, 30),
      "nick" -> nonEmptyText(2, 12)
    )
  )
}
