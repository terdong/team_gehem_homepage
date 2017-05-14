package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.authentication.Authorized
import com.teamgehem.authentication.PermissionProvider._
import controllers.traits.{BoardInfo, Header, ProvidesHeader}
import play.api.cache.CacheApi
import play.api.data.Forms._
import play.api.data._
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
  * Created by terdo on 2017-04-20 020.
  */
@Singleton
class BoardController @Inject()(implicit cache: CacheApi,
                                members_repo: MembersRepository,
                                boards_repo: BoardsRepository,
                                posts_repo: PostsRepository,
                                permissions_repo: PermissionsRepository,
                                val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport
    with ProvidesHeader {

  def boards = Authorized(Admin).async { implicit request =>
    boards_(boardForm)
  }

  def createBoard = Authorized(Admin).async { implicit request =>
    boardForm.bindFromRequest.fold(
      hasErrors => boards_(hasErrors),
      form => {
        val email = request.auth.email
        val result = for {
          member <- members_repo.findByEmail(email)
          r <- boards_repo.insert(form, member.name)
          _ <- setCacheBoardList
        } yield Redirect(routes.BoardController.boards())
        result
      }
    )
  }

  def deleteBoard(board_seq: Long) = Authorized(Admin).async {
    implicit request =>
      for {
        _ <- boards_repo.delete(board_seq)
        _ <- setCacheBoardList
      } yield Redirect(routes.BoardController.boards())
  }

  private def boardExists(name: String): Boolean = {
    Await.result(boards_repo.existsName(name), Duration.Inf)
  }

  private val boardForm = Form(
    tuple(
      "name" -> nonEmptyText(maxLength = 30)
        .verifying(messagesApi("board.create.exists.name"), !boardExists(_)),
      "description" -> text(maxLength = 2000),
      "status" -> boolean,
      "list_perm" -> byteNumber(min = 0, max = 99),
      "read_perm" -> byteNumber(min = 0, max = 99),
      "write_perm" -> byteNumber(min = 0, max = 99)
    )
  )

  private def boards_(form: Form[_])(implicit header: Header): Future[Result] = {
    for {
      boards <- boards_repo.all
      permissions <- permissions_repo.all
    } yield Ok(views.html.board.boards(boards, permissions, form))
  }

  private def setCacheBoardList = {
    val r: Future[Seq[BoardInfo]] = for {
      r: Seq[(Long, String, Byte)] <- boards_repo.allSeqAndNameAndListPermission
    } yield r.map(BoardInfo tupled)

    r.map(cache.set("board_list", _))
  }
}
