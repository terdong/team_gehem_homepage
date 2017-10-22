package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.controller.TGBasicController
import com.teamgehem.helper.DbResultChecker
import com.teamgehem.security.{AuthMessagesRequest, AuthenticatedActionBuilder}
import models.{Navigation, Permission}
import play.api.cache.SyncCacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import repositories._
import services.CacheManager

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by DongHee Kim on 2017-08-11 011.
  */
@Singleton
class AdminController @Inject()(mcc: MessagesControllerComponents,
                                sync_cache:SyncCacheApi,
                                auth: AuthenticatedActionBuilder,
                                members_repo: MembersRepository,
                                boards_repo: BoardsRepository,
                                posts_repo: PostsRepository,
                                navigations_repo:NavigationsRepository,
                                permission_repo: PermissionsRepository,
                                cache_manager:CacheManager
                               )
  extends TGBasicController(mcc, sync_cache) with DbResultChecker {

  def members = auth.authrized_semi_admin.async { implicit request =>
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

  def editMemberForm(email: String) = auth.authrized_semi_admin.async {
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

  def permissions = auth.authrized_semi_admin.async { implicit request =>
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

  def navigations = auth.authrized_semi_admin.async { implicit request =>
    getNavigationResult_(navigation_form, routes.AdminController.createNavigation)
  }

  def createNavigation = auth.authrized_admin.async { implicit request =>
    navigation_form.bindFromRequest.fold(
      hasErrors => getNavigationResult_(hasErrors, routes.AdminController.createNavigation),
      (form: (Option[Long], String, String, Option[String], Boolean, Long, Int)) => {

        implicit val func_r: (Navigation) => Result = (nav) => {
          cache_manager.updateNavigationCache
          Redirect(routes.AdminController.navigations)
        }
        val result: Future[Result] = navigations_repo.insert(form)
        result
      })
  }

  def editNavigationForm(seq: Long) = auth.authrized_semi_admin.async { implicit request =>

    implicit val func_r: Navigation => Future[Result] = (nav) => {
      val form_data = (Some(nav.seq),
        nav.name,
        nav.shortcut,
        nav.description,
        nav.status,
        nav.post_seq,
        nav.priority)
      getNavigationResult_(navigation_form.fill(form_data), routes.AdminController.editNavigation)
    }
    val result: Future[Result] = navigations_repo.getNavigation(seq)
    result
  }

  def editNavigation = auth.authrized_admin.async { implicit request =>
    val form = navigation_form.bindFromRequest
    form.fold(
      has_errors => getNavigationResult_(has_errors, routes.AdminController.editNavigation, BadRequest),
      form => {
        implicit val func_r: (Int) => Result = (result) => {
          cache_manager.updateNavigationCache
          Redirect(routes.AdminController.navigations)
        }
        val future_result: Future[Result] = navigations_repo.update(form)
        future_result
      }
    )
  }

  def setActiveNavigation(seq: Long, is_active: Boolean) = auth.authrized_admin.async { implicit request =>
    implicit val func_r: (Int) => Result = (result) => {
      cache_manager.updateNavigationCache
      Redirect(routes.AdminController.navigations)
    }
    navigations_repo.setActiveNavigation(seq, is_active)
  }

  def deleteNavigation(seq: Long) = auth.authrized_admin.async { implicit request =>
    implicit val func_r: (Int) => Result = (result) => {
      cache_manager.updateNavigationCache
      Redirect(routes.AdminController.navigations)
    }
    navigations_repo.delete(seq)
  }

  def javascriptRoutesAdmin = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.BoardController.editBoardForm,
        routes.javascript.BoardController.editBoard
      )
    ).as("text/javascript")
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


  private val navigation_form = Form(
    tuple(
      "seq" -> optional(longNumber),
      "name" -> nonEmptyText(2, 30),
      "shortcut" -> nonEmptyText(2, 30),
      "description" -> optional(text(maxLength = 2000)),
      "status" -> boolean,
      "post_seq" -> longNumber,
      "priority" -> default(number(min = 0), 0)
    )
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

  private def getNavigationResult_(form: Form[_], url: Call, default_status: Status = Ok)(implicit request: AuthMessagesRequest[AnyContent]): Future[Result] = {
    implicit val func_r: Seq[Navigation] => Result = (navs) => default_status(views.html.admin.navigations(navs, form, url))
    navigations_repo.getList
  }
}
