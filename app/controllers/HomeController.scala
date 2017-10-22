package controllers

import javax.inject._

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Concat, Keep, MergeHub, Sink, Source}
import com.teamgehem.controller.TGBasicController
import com.teamgehem.enumeration.CacheString
import com.teamgehem.helper.DbResultChecker
import com.teamgehem.model.BoardInfo
import play.api.{Environment, Logger, Mode}
import play.api.cache.{AsyncCacheApi, SyncCacheApi}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.iteratee.Enumeratee
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import repositories.{NavigationsRepository, PostsRepository}
import services.Counter

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HomeController @Inject()(cc: MessagesControllerComponents,
                               posts_repo: PostsRepository,
                               navis_repo: NavigationsRepository,
                               cache: AsyncCacheApi,
                               sync_cache: SyncCacheApi,
                               counter: Counter,
                               //result_cache: Cached
                               environment: Environment
                              )(implicit mat: Materializer)
  extends TGBasicController(cc, sync_cache) with DbResultChecker {

  val isProd = environment.mode.equals(Mode.Prod)

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

  def connDeathWatch(addr: String): Enumeratee[JsValue, JsValue] =
    Enumeratee.onIterateeDone{ () => Logger.info(s"$addr - SSE disconnected") }

  def welcome: Source[JsValue, NotUsed] = Source.single[JsValue](Json.obj(
    "message" -> "Welcome! Write a message and hit ENTER."
  ))

  private[this] val (chatSink: Sink[JsValue, NotUsed], chatSource: Source[JsValue, NotUsed]) =
    MergeHub.source[JsValue]
      .toMat(BroadcastHub.sink[JsValue])(Keep.both)
      .run()

  def chatFeed = Action { req =>
    val userAddress = req.remoteAddress
    Logger.info(s"${userAddress} - connected")
    val watchFlow = EventSource.flow[JsValue].watchTermination()((_, termination) => termination.onComplete(_ => {
      Logger.info(s"$userAddress - SSE disconnected")
    }))

    if(isProd && req.cookies.get("chat").isDefined){
      Ok.chunked(chatSource via watchFlow).as(ContentTypes.EVENT_STREAM)
    }else{
      val combined_source = Source.combine(welcome, chatSource)(Concat(_))
      val cookie_chat = Cookie("chat","1", Some(3600), httpOnly = false)

      val result = Ok.chunked(combined_source via watchFlow).as(ContentTypes.EVENT_STREAM)

      if(req.session.get("nick").isDefined){
        result.withCookies(cookie_chat).bakeCookies()
      }else{
        val cookie_nick = Cookie("temp_nick",s"guest_${ counter.nextCount()}", Some(3600), httpOnly = false)
        result.withCookies(cookie_chat, cookie_nick).bakeCookies()
      }

    }
  }

  def postMessage = Action(parse.json) { req =>
    val optionNick = req.session.get("nick")
    val message = (Json.toJson(req.body) \ "message").as[String]
    val mixed_message = s"${optionNick.map{nick => nick}.getOrElse{ req.cookies.get("temp_nick").map(cookie=>cookie.value).getOrElse("guest_??") }}: ${message}"
    Source.single(Json.obj(
      "message" ->mixed_message
    )).runWith(chatSink)
    Ok
  }

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
        routes.javascript.AccountController.getClientId,
        routes.javascript.HomeController.chatFeed,
        routes.javascript.HomeController.postMessage
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
