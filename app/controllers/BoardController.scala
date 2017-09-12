package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.security.{AuthMessagesRequest, AuthenticatedActionBuilder}
import play.api.cache.AsyncCacheApi
import play.api.data.Forms._
import play.api.data._
import play.api.i18n._
import play.api.mvc._
import repositories.{BoardsRepository, MembersRepository, PermissionsRepository, PostsRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by terdo on 2017-04-20 020.
  */
@Singleton
class BoardController @Inject()(cache: AsyncCacheApi,
                                auth: AuthenticatedActionBuilder,
                                cc: MessagesControllerComponents,
                                members_repo: MembersRepository,
                                boards_repo: BoardsRepository,
                                posts_repo: PostsRepository,
                                permissions_repo: PermissionsRepository)
  extends MessagesAbstractController(cc) {

  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(cc.langs.availables.head, messagesApi)
  }

  def boards = auth.authrized_admin.async { implicit request: AuthMessagesRequest[AnyContent] =>
    boards_(board_form, routes.BoardController.createBoard)
  }

  def editBoardForm(board_seq: Long) = auth.authrized_admin.async {
    implicit request =>
      boards_repo.getBoard(board_seq).flatMap { b =>
        val form_data = (b.seq,
          b.name,
          b.description.getOrElse(""),
          b.status,
          b.is_reply,
          b.is_comment,
          b.is_attachment,
          b.list_permission,
          b.read_permission,
          b.write_permission,
          b.priority)
        boards_(board_edit_form.fill(form_data), routes.BoardController.editBoard)
      }
  }

  def editBoard = auth.authrized_admin.async { implicit request =>
    board_edit_form.bindFromRequest.fold(
      hasErrors => {
        boards_(hasErrors, routes.BoardController.editBoard)
      },
      form => {
        val email = request.member.email
        val result = for {
          member <- members_repo.findByEmail(email)
          r <- boards_repo.update(form, member.name)
        } yield{
          // TODO: The result that is about "editBoard" will have to be implemented with "success", "failure" pattern matching.
          if(r == 1){
            boards_repo.getAvailableBoards.foreach(cache.set("board.list.available",_))
          }
          Redirect(routes.BoardController.boards())
        }
        result
      }
    )
  }

  def createBoard = auth.authrized_admin.async { implicit request =>
    board_form.bindFromRequest.fold(
      hasErrors => boards_(hasErrors, routes.BoardController.createBoard),
      form => {
        val email = request.member.email
        val result = for {
          member <- members_repo.findByEmail(email)
          r <- boards_repo.insert(form, member.name)
        //_ <- setCacheBoardList
        } yield Redirect(routes.BoardController.boards())
        result
      }
    )
  }

  def setActiveBoard(board_seq: Long, is_active: Boolean) =
    auth.authrized_admin.async { implicit request =>
      for {
        _ <- boards_repo.setActiveBoard(board_seq, is_active)
      //_ <- setCacheBoardList
      } yield Redirect(routes.BoardController.boards())
    }

  def deleteBoard(board_seq: Long) = auth.authrized_admin.async {
    implicit request =>
      for {
        _ <- boards_repo.delete(board_seq)
      // _ <- setCacheBoardList
      } yield Redirect(routes.BoardController.boards())
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
      "list_perm" -> byteNumber(min = 0, max = 99),
      "read_perm" -> byteNumber(min = 0, max = 99),
      "write_perm" -> byteNumber(min = 0, max = 99),
      "priority" -> number(min = 0)
    )
  )

  private def boards_(form: Form[_], url: Call)(
    implicit request: AuthMessagesRequest[AnyContent]): Future[Result] = {

    for {
      boards <- boards_repo.all
      permissions <- permissions_repo.all
    } yield Ok(views.html.admin.boards(boards, permissions, form, url))
  }

  /*  private def setCacheBoardList = {
    val r: Future[Seq[BoardInfo]] = for {
      r: Seq[(Long, String, Byte)] <- boards_repo.allSeqAndNameAndListPermission
    } yield r.map(BoardInfo tupled)

    r.map(cache.set("board_list", _))
  }*/
}
