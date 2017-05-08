package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.authentication.Authorized
import com.teamgehem.authentication.PermissionProvider._
import controllers.traits.{Header, ProvidesHeader}
import models.Permission
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Controller, Result}
import repositories.{
  BoardsRepository,
  MembersRepository,
  PermissionsRepository,
  PostsRepository
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by terdo on 2017-05-02 002.
  */
@Singleton
class AdminController @Inject()(implicit cache: CacheApi,
                                members_repo: MembersRepository,
                                boards_repo: BoardsRepository,
                                posts_repo: PostsRepository,
                                permission_repo: PermissionsRepository,
                                val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport
    with ProvidesHeader {

  def permissions = Authorized(Admin).async { implicit request =>
    permissions_(permissionForm)
  }

  def createPermission = Authorized(Admin).async { implicit request =>
    permissionForm.bindFromRequest.fold(
      hasErrors => permissions_(hasErrors),
      form => {
        permission_repo
          .insert(form)
          .map(_ => Redirect(routes.AdminController.permissions()))
      }
    )
  }

  def deletePermission(permission_code: Int) = Authorized(Admin).async {
    implicit request =>
      permission_repo
        .delete(permission_code.toByte)
        .map(_ => Redirect(routes.AdminController.permissions))
  }

  private def permissionExists(code: Byte): Boolean = {
    Await.result(permission_repo.existsCode(code), Duration.Inf)
  }

  private val permissionForm: Form[Permission] = Form(
    mapping(
      "permission_code" -> byteNumber(min = 0, max = 99)
        .verifying(messagesApi("admin.permission.exists"),
                   !permissionExists(_)),
      "active" -> boolean,
      "content" -> text(maxLength = 80)
    )(Permission.apply)(Permission.unapply)
  )

  private def permissions_(form: Form[Permission])(
      implicit header: Header): Future[Result] = {
    for {
      permissions <- permission_repo.all
    } yield Ok(views.html.admin.permission(permissions, form))
  }
}
