package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.controller.TGBasicController
import com.teamgehem.security.{AuthMessagesRequest, AuthenticatedActionBuilder}
import play.api.cache.{AsyncCacheApi, SyncCacheApi}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._
import repositories.{BoardsRepository, MembersRepository, PermissionsRepository, PostsRepository}
import services.CacheManager

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by terdo on 2017-04-20 020.
  */
@Singleton
class BoardController @Inject()(cache_manager:CacheManager,
                                 async_cache: AsyncCacheApi,
                                sync_cache:SyncCacheApi,
                                auth: AuthenticatedActionBuilder,
                                mcc: MessagesControllerComponents,
                                members_repo: MembersRepository,
                                boards_repo: BoardsRepository,
                                posts_repo: PostsRepository,
                                permissions_repo: PermissionsRepository)
  extends TGBasicController(mcc, sync_cache) {

  def boards = auth.authrized_semi_admin.async { implicit request: AuthMessagesRequest[AnyContent] =>
    okWithFormBoards_(board_form, routes.BoardController.createBoard)
  }

  def editBoardForm(board_seq: Long) = auth.authrized_semi_admin.async { implicit request =>
      boards_repo.getBoard(board_seq).map { b =>
        val form_data = (b.seq,
          b.name,
          b.description.getOrElse(""),
          b.status,
          b.is_reply,
          b.is_comment,
          b.is_attachment,
          b.is_notice,
          b.list_permission,
          b.read_permission,
          b.write_permission,
          b.priority)
        //okWithFormBoards_(board_edit_form.fill(form_data), routes.BoardController.editBoard)

        Ok(Json.toJson(board_edit_form.fill(form_data).data))
        //Ok(Json.obj("client_id" -> "hello"))
      }
  }

  def editBoard = auth.authrized_admin.async { implicit request =>
    board_edit_form.bindFromRequest.fold(
      hasErrors => {
        okWithFormBoards_(hasErrors, routes.BoardController.editBoard)
      },
      form => {
        val email = request.member.email
        for {
          member <- members_repo.findByEmail(email)
          result <- boards_repo.update(form, member.name)
        } yield{
          // TODO: The result that is about "editBoard" will have to be implemented with "success", "failure" pattern matching.
          if(result == 1){
            cacheUpdatedBoard
          }
          Redirect(routes.BoardController.boards())
        }
      }
    )
  }

  def createBoard = auth.authrized_semi_admin.async { implicit request =>
    board_form.bindFromRequest.fold(
      hasErrors => okWithFormBoards_(hasErrors, routes.BoardController.createBoard),
      form => {
        val email = request.member.email
        for {
          member <- members_repo.findByEmail(email)
          result <- boards_repo.insert(form, member.name)
        } yield {
          cacheUpdatedBoard
          Redirect(routes.BoardController.boards())
        }
      }
    )
  }

  def setActiveBoard(board_seq: Long, is_active: Boolean) =
    auth.authrized_semi_admin.async { implicit request =>
      boards_repo.setActiveBoard(board_seq, is_active).map{ _ =>
        cacheUpdatedBoard
        Redirect(routes.BoardController.boards())
      }
    }

  def deleteBoard(board_seq: Long) = auth.authrized_admin.async {
    implicit request =>
      boards_repo.delete(board_seq).map{ _ =>
        cacheUpdatedBoard
        Redirect(routes.BoardController.boards())
      }
  }

  private def boardExists(name: String): Boolean = {
    Await.result(boards_repo.existsName(name), Duration.Inf)
  }

  private def boardExistsExceptMe(name: String): Boolean = {
    Await.result(boards_repo.existsName(name), Duration.Inf)
  }

  private val board_edit_form = Form(
    tuple(
      "seq" -> longNumber,
      "name" -> nonEmptyText(maxLength = 30),
      "description" -> text(maxLength = 2000),
      "status" -> boolean,
      "is_reply" -> boolean,
      "is_comment" -> boolean,
      "is_attachment" -> boolean,
      "is_notice" -> boolean,
      "list_perm" -> byteNumber(min = 0, max = 99),
      "read_perm" -> byteNumber(min = 0, max = 99),
      "write_perm" -> byteNumber(min = 0, max = 99),
      "priority" -> number(min = 0)
    )
  )

  private val board_form = Form(
    tuple(
      "name" -> nonEmptyText(maxLength = 30)
        .verifying(Messages("board.create.exists.name"), !boardExists(_)),
      "description" -> text(maxLength = 2000),
      "status" -> boolean,
      "is_reply" -> boolean,
      "is_comment" -> boolean,
      "is_attachment" -> boolean,
      "is_notice" -> boolean,
      "list_perm" -> byteNumber(min = 0, max = 99),
      "read_perm" -> byteNumber(min = 0, max = 99),
      "write_perm" -> byteNumber(min = 0, max = 99),
      "priority" -> number(min = 0)
    )
  )

  private def okWithFormBoards_(form: Form[_], url: Call)(
    implicit request: AuthMessagesRequest[AnyContent]): Future[Result] = {
    for {
      boards <- boards_repo.all
      permissions <- permissions_repo.all
    } yield Ok(views.html.admin.boards(boards, permissions, form, url))
  }

  private def cacheUpdatedBoard = {
    cache_manager.updateBoardCache
  }

}
