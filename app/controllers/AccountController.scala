package controllers

import java.util.Collections
import javax.inject.{Inject, Singleton}

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.teamgehem.controller.TGBasicController
import com.teamgehem.security.AuthenticatedActionBuilder
import play.api.cache.SyncCacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc.{MessagesControllerComponents, MessagesRequest}
import play.api.{Configuration, Logger}
import repositories.{MembersRepository, PermissionsRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Random, Success}

@Singleton
class AccountController @Inject()(config: Configuration,
                                  mcc: MessagesControllerComponents,
                                  sync_cache: SyncCacheApi,
                                  auth: AuthenticatedActionBuilder,
                                  members_repo: MembersRepository,
                                  permission_repo: PermissionsRepository,
                                  rand: Random)
    extends TGBasicController(mcc, sync_cache){

  lazy val google_client_id = config.get[String]("google.client.id")

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
        request.session.get("id").map { id =>
          members_repo.insert(id, form) map (_ match {
            case Success(member) =>
              Ok(views.html.account.signup_complete(member.email, signin_form)).withSession("seq" -> member.seq.toString,
                "email" -> member.email,
                "permission" -> member.permission.toString)
            case Failure(e) => throw e
          })
        }.getOrElse(Future.successful(PreconditionFailed(views.html.error_pages.HTTP412())))
      }
    )
  }

  @deprecated("This is no longer used.","0.5.2")
  def createSignInForm = Action { implicit request =>
    Ok(views.html.account.signin(signin_form))
  }

  @deprecated("This is no longer used.","0.5.2")
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

  def signinOpenIdForm = Action { implicit request =>
    Ok(views.html.account.signin_open_id(google_client_id))
  }


  def signinOpenId = Action(parse.formUrlEncoded).async { implicit request: MessagesRequest[Map[String, Seq[String]]] =>
    val id_token = request.body("idtoken")(0)
    //Logger.debug(s"id_token = $id_token")

    val idToken = verifier.verify(id_token)

    if (idToken != null) {
      val payload = idToken.getPayload()
      // Print user identifier
      val userId = payload.getSubject()
      //Logger.debug("User ID: " + userId)

      // Get profile information from payload
/*      val email = payload.getEmail()
      val emailVerified = payload.getEmailVerified.toString
      val name = payload.get("name").toString
      val pictureUrl = payload.get("picture").toString
      val locale =  payload.get("locale").toString
      val familyName =  payload.get("family_name").toString
      val givenName =  payload.get("given_name").toString*/

      members_repo.findById(userId).map { members_option =>
        members_option.headOption.map{ member =>
          members_repo.updateLastSignin(member.seq)

          Ok(Json.obj(
            "counter" -> s"${Messages("account.signin.succeed.message")}<br/>${Messages("account.signin.succeed.counter")}" ,
            "title" -> Messages("account.signin.succeed.title")
          )).withSession("seq" -> member.seq.toString,
            "email" -> member.email,
            "permission" -> member.permission.toString)
        }.getOrElse(Ok(Json.obj("redirect" -> routes.AccountController.createSignUpForm().url)).withSession("id" -> userId) )
      }
    } else {
      val error_message= Messages("account.signin.empty.id")
      Logger.debug(error_message)
      Future.successful(BadRequest(Json.obj("error"-> error_message)))
    }
  }

  def getClientId = auth {implicit request =>
    Ok(Json.obj("client_id" -> google_client_id))
  }

  def signout = auth { implicit request =>
    Redirect(routes.HomeController.index()).withNewSession.flashing("message" -> Messages("account.signout.message"))
  }

  private  val verifier = new GoogleIdTokenVerifier.Builder( new NetHttpTransport(), new JacksonFactory())
    .setAudience(Collections.singletonList(google_client_id))
    // Or, if multiple clients access the backend:
    //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
    .build()

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
