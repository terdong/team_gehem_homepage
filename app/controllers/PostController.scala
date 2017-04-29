package controllers

import javax.inject.{Inject, Singleton}

import controllers.traits.ProvidesHeader
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import repositories.{BoardsRepository, MembersRepository, PostsRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by terdo on 2017-04-22 022.
  */
@Singleton
class PostController @Inject()(implicit cache: CacheApi,
                               members_repo: MembersRepository,
                               boards_repo: BoardsRepository,
                               posts_repo: PostsRepository,
                               val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport
    with ProvidesHeader {

  //case class PostForm("au")

  val postForm = Form(
    tuple(
      "board_seq" -> longNumber,
      "subject" -> nonEmptyText(maxLength = 80),
      "content" -> text
    )
  )

  val page_length = 15

  def listAll(page: Int) = Action.async { implicit request =>
    for {
      posts <- posts_repo.all(page, page_length)
      count <- posts_repo.getPostCount()
    } yield Ok(views.html.Post.list(None, posts, page, page_length, count))
  }

  def list(board_seq: Long, page: Int) = Action.async { implicit request =>
    for {
      name_op <- boards_repo.getNameBySeq(board_seq)
      posts <- posts_repo.allByBoard(board_seq, page, page_length)
      count <- posts_repo.getPostCount(board_seq)
    } yield
      Ok(
        views.html.Post
          .list(name_op, posts, page, page_length, count, board_seq))
  //Future.successful(Ok(""))
  }

  /*  def showPostList(board_seq: Long) = Action.async { implicit request =>
    for {
      name_op <- boards_repo.getNameBySeq(board_seq)
      posts <- posts_repo.allByBoard(board_seq)
    } yield Ok(views.html.Post.list(name_op, posts))
  }

  def showPostListAll = Action.async { implicit request =>
    posts_repo.all.map(posts => Ok(views.html.Post.list(Some("All"), posts)))
  }*/

  def showPost(board_seq: Long, post_seq: Long) = Action.async {
    implicit request =>
      posts_repo
        .showPost(board_seq, post_seq)
        .map(
          op =>
            op.map(tuple => {
                posts_repo.updateHitCount(tuple._1)
                Ok(views.html.Post.read(tuple))
              })
              .getOrElse(NotFound))
  }
  def writePostForm(implicit board_seq: Long) = Action { implicit request =>
    Ok(views.html.Post.write(postForm))
  }

  def writePost(implicit board_seq: Long) = Action.async { implicit request =>
    val form = postForm.bindFromRequest
    form.fold(
      hasErrors =>
        Future.successful(BadRequest(views.html.Post.write(hasErrors))),
      form => {
        posts_repo.insert(form,
                          request.session.get(AccountController.EMAIL_KEY).get,
                          request.remoteAddress) map (_ =>
          Redirect(routes.PostController.list(form._1, 1)))
      }
    )
  }

}
