package controllers

import javax.inject._

import com.teamgehem.model.BoardInfo
import play.api.cache.AsyncCacheApi
import play.api.mvc._
import repositories.PostsRepository

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HomeController @Inject()(cc: MessagesControllerComponents, posts_repo: PostsRepository, cache: AsyncCacheApi)
  extends MessagesAbstractController(cc) {

  def index = Action.async { implicit request =>
    //Logger.debug(request.headers.headers.mkString("\n"))
    val seq_board_seq: Seq[Long] = Seq(1, 2, 4)
    for {
      post_result <- posts_repo.getPostInfoByBoards(seq_board_seq)
      board_info_option <- cache.get[Seq[BoardInfo]]("board.list.list_permission")
    } yield {
      board_info_option.map(board_info => seq_board_seq.flatMap(seq => board_info.find(_.seq == seq))).map(r => Ok(views.html.index(r, post_result))).getOrElse(InternalServerError)
    }
  }

  def result() = Action { implicit request =>
    Ok(views.html.result())
  }
}
