package controllers

import javax.inject.{Inject, Singleton}

import controllers.traits.ProvidesHeader
import play.api.cache.CacheApi
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import repositories.{BoardsRepository, MembersRepository, PostsRepository}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
  * Created by terdo on 2017-04-20 020.
  */
@Singleton
class BoardController @Inject()(implicit cache: CacheApi,
                                members_repo: MembersRepository,
                                boards_repo: BoardsRepository,
                                posts_repo: PostsRepository,
                                val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport
    with ProvidesHeader {

  def boardExists(name: String): Boolean = {
    Await.result(boards_repo.existsName(name), Duration.Inf)
  }

  val boardForm = Form(
    tuple(
      "name" -> nonEmptyText(maxLength = 30)
        .verifying(messagesApi("board.create.exists.name"), !boardExists(_)),
      "description" -> text(maxLength = 2000),
      "status" -> boolean,
      "list_perm" -> nonEmptyText(maxLength = 4),
      "read_perm" -> nonEmptyText(maxLength = 4),
      "write_perm" -> nonEmptyText(maxLength = 4)
    )
  )

  def boards = Action.async { implicit request =>
    boards_repo.all map (boards =>
      Ok(views.html.Board.boards(boards, boardForm)))
  }

  def createBoard = Action.async { implicit request =>
    boardForm.bindFromRequest.fold(
      hasErrors =>
        boards_repo.all map (boards =>
          Ok(views.html.Board.boards(boards, hasErrors))),
      form => {
        val email = request.session.get(AccountController.EMAIL_KEY).get
        val result = for {
          member <- members_repo.findByEmail2(email)
          r <- boards_repo.insert(form, member.name)
          _ <- setCacheBoardList
        } yield Redirect(routes.BoardController.boards())
        result
      }
    )
  }

  def deleteBoard(board_seq: Long) = Action.async { implicit request =>
    for {
      _ <- boards_repo.delete(board_seq)
      _ <- setCacheBoardList
    } yield Redirect(routes.BoardController.boards())
  }

  private def setCacheBoardList = {
    boards_repo.allSeqAndName.map(cache.set("board_list", _))
  }
}
