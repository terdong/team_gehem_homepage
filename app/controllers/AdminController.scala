package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.security.{AuthMessagesRequest, AuthenticatedActionBuilder}
import models.Permission
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{Messages, MessagesImpl, MessagesProvider}
import play.api.mvc.{
  AnyContent,
  MessagesAbstractController,
  MessagesControllerComponents,
  Result
}
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
  * Created by DongHee Kim on 2017-08-11 011.
  */
@Singleton
class AdminController @Inject()(cc: MessagesControllerComponents,
                                auth: AuthenticatedActionBuilder,
                                members_repo: MembersRepository,
                                boards_repo: BoardsRepository,
                                posts_repo: PostsRepository,
                                permission_repo: PermissionsRepository)
    extends MessagesAbstractController(cc) {

  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(cc.langs.availables.head, messagesApi)
  }

  def members = auth.authrized_admin.async { implicit request =>
    for {
      members <- members_repo.allWithPermission
    } yield Ok(views.html.admin.members("members", members))
  }

  def editMember(email: String) = auth.authrized_admin.async {
    implicit request =>
      val form = member_form.bindFromRequest
      form.fold(
        has_errors =>
          for {
            member <- members_repo.findByEmail(email)
            permissions <- permission_repo.allwithActive
          } yield
            BadRequest(
              views.html.admin.members_edit(has_errors, member, permissions)),
        form => {
          members_repo.update(email, form) map (_ =>
            Redirect(routes.AdminController.members()))
        }
      )
  }

  def editMemberForm(email: String) = auth.authrized_admin.async {
    implicit request =>
      for {
        member <- members_repo.findByEmail(email)
        permissions <- permission_repo.allwithActive
      } yield {
        val form = (member.name,
                    member.nick,
                    member.permission,
                    member.level,
                    member.exp)
        Ok(
          views.html.admin
            .members_edit(member_form.fill(form), member, permissions))
      }
  }

  def permissions = auth.authrized_admin.async { implicit request =>
    permissions_(permissionForm)
  }

  def createPermission = auth.authrized_admin.async { implicit request =>
    permissionForm.bindFromRequest.fold(
      hasErrors => permissions_(hasErrors),
      form => {
        permission_repo
          .insert(form)
          .map(_ => Redirect(routes.AdminController.permissions))
      }
    )
  }

  def deletePermission(permission_code: Int) = auth.authrized_admin.async {
    implicit request =>
      permission_repo
        .delete(permission_code.toByte)
        .map(_ => Redirect(routes.AdminController.permissions))
  }

  private val member_form = Form(
    tuple(
      "name" -> nonEmptyText(2, 30),
      "nick" -> nonEmptyText(2, 12),
      "permission" -> byteNumber,
      "level" -> number,
      "exp" -> number
    )
  )

  private val permissionForm: Form[Permission] = Form(
    mapping(
      "permission_code" -> byteNumber(min = 0, max = 99)
        .verifying(Messages("admin.permission.exists"), !permissionExists(_)),
      "active" -> boolean,
      "content" -> text(maxLength = 80)
    )(Permission.apply)(Permission.unapply)
  )

  private def permissionExists(code: Byte): Boolean = {
    Await.result(permission_repo.existsCode(code), Duration.Inf)
  }

  private def permissions_(form: Form[Permission])(
      implicit request: AuthMessagesRequest[AnyContent]): Future[Result] = {
    for {
      permissions <- permission_repo.all
    } yield Ok(views.html.admin.permissions(permissions, form))
  }

  /*  private def members_(): Future[Result] = {
    for {
      members <- members_repo.allWithPermission
    } yield Ok(views.html.admin.members("members", members))
  }*/
}
