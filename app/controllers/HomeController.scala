package controllers

import javax.inject._

import com.teamgehem.enumeration.BoardCacheString
import com.teamgehem.model.BoardInfo
import play.api.cache.AsyncCacheApi
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import repositories.PostsRepository

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HomeController @Inject()(cc: MessagesControllerComponents, posts_repo: PostsRepository, cache: AsyncCacheApi)
  extends MessagesAbstractController(cc) {

  def index = Action.async { implicit request =>
    //Logger.debug(request.headers.headers.mkString("\n"))
    // TODO : this should be fixed.
    val seq_board_seq: Seq[Long] = Seq(16, 17, 19)

    for {
      post_result <- posts_repo.getPostInfoByBoards(seq_board_seq)
      board_info_option <- cache.get[Seq[BoardInfo]](BoardCacheString.List_Permission)
    } yield {
      board_info_option.map(board_info => seq_board_seq.flatMap(seq => board_info.find(_.seq == seq))).map(r =>
        Ok(views.html.index(r, post_result))).getOrElse(InternalServerError(views.html.index(Nil, post_result)))
    }
  }

  def result() = Action { implicit request =>
    Ok(views.html.result())
  }

  def about() = Action.async { implicit request =>
    posts_repo.getPost(6, "about").map { result =>
      Ok(views.html.contents.about(result.content))
    }
    //Future.successful(Ok(""))
  }

  def contact() = Action.async { implicit request =>
    posts_repo.getPost(6, "contact").map { result =>
      Ok(views.html.contents.about(result.content))
    }
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.AccountController.signinOpenId,
        routes.javascript.PostController.commentList
      )
    ).as("text/javascript")
  }
}
