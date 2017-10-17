package controllers

import javax.inject._

import com.teamgehem.controller.TGBasicController
import com.teamgehem.enumeration.CacheString
import com.teamgehem.helper.DbResultChecker
import com.teamgehem.model.BoardInfo
import play.api.cache.{AsyncCacheApi, SyncCacheApi}
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import repositories.{NavigationsRepository, PostsRepository}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HomeController @Inject()(cc: MessagesControllerComponents,
                               posts_repo: PostsRepository,
                               navis_repo: NavigationsRepository,
                               cache: AsyncCacheApi,
                               sync_cache: SyncCacheApi
                               //result_cache: Cached
                              )
  extends TGBasicController(cc, sync_cache) with DbResultChecker {

  def index = Action.async { implicit request =>
    //Logger.debug(request.headers.headers.mkString("\n"))
    for {
      notice_board_seq_list_option <- cache.get[Seq[Long]](CacheString.Notice_Board_Seq_List)
      if notice_board_seq_list_option.isDefined
      post_result <- posts_repo.getPostInfoByBoards(notice_board_seq_list_option.get)
      board_info_option <- cache.get[Seq[BoardInfo]](CacheString.List_Permission)
    } yield {
      board_info_option.flatMap { board_info =>
        notice_board_seq_list_option.map { notice_board_seq_list =>
          notice_board_seq_list.flatMap(seq => board_info.find(_.seq == seq))
        }
      }.map(board_info_seq =>
        Ok(views.html.index(board_info_seq, post_result))).getOrElse(InternalServerError(views.html.index(Nil, post_result)))
    }
  }

  def result() = Action { implicit request =>
    Ok(views.html.result())
  }

  def navigation(name: String) = //result_cache(s"navigation_$name") {
    Action.async { implicit request =>
      implicit val result: ((String, Option[String])) => Result = (t) => Ok(views.html.navigation(t._2, t._1))
      navis_repo.getPostContentByName(name)
    }
  //}

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.AccountController.signinOpenId,
        routes.javascript.PostController.commentList
      )
    ).as("text/javascript")
  }

  def javascriptRoutesMain = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.AccountController.getClientId
      )
    ).as("text/javascript")
  }

  def javascriptRoutesUpload = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.PostController.uploadFile,
        routes.javascript.PostController.deleteFile
      )
    ).as("text/javascript")
  }
}
