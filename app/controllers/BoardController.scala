package controllers

import javax.inject.{Inject, Singleton}

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import repositories.{BoardsRepository, MembersRepository, PostsRepository}

/**
  * Created by terdo on 2017-04-20 020.
  */
@Singleton
class BoardController @Inject()(members_repo: MembersRepository,
                                boards_repo: BoardsRepository,
                                posts_repo: PostsRepository,
                                val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport {

  def createBoard = Action { implicit request =>
    Ok(views.html.Board.create_board())
  }
}
