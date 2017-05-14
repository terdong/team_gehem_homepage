package controllers

import javax.inject.{Inject, Singleton}

import com.teamgehem.authentication.Authenticated
import controllers.traits.ProvidesHeader
import models.{Member, Post}
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms.{tuple, _}
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
      posts <- posts_repo.all(page, page_length, permission)
      count <- posts_repo.getPostCount()
    } yield Ok(views.html.post.list(None, posts, page, page_length, count))
  }

  def list(board_seq: Long, page: Int) = Action.async { implicit request =>
    for {
      b <- boards_repo.isListValidBoard(board_seq, permission) if b == true
      name_op <- boards_repo.getNameBySeq(board_seq)
      posts <- posts_repo.listByBoard(board_seq, page, page_length)
      count <- posts_repo.getPostCount(board_seq)
    } yield
      Ok(
        views.html.post
          .list(name_op, posts, page, page_length, count, board_seq))
  //Future.successful(Ok(""))
  }

  def showPost(board_seq: Long, post_seq: Long) = Action.async {
    implicit request =>
      for {
        r <- boards_repo.isListValidBoard(board_seq, permission) if r == true
        post <- posts_repo.getPost(board_seq, post_seq)
      } yield {
        posts_repo.updateHitCount(post._1)
        Ok(
          views.html.post.read(post,
                               member_email
                                 .map(post._2.email.equals(_))
                                 .getOrElse(false)))
      }
  }

  def editPostForm(board_seq: Long, post_seq: Long) = Authenticated.async {
    implicit request =>
      for {
        r <- posts_repo.isOwnPost(post_seq, member_email.get) if r == true
        tuple: (Post, Member) <- posts_repo.getPost(board_seq, post_seq)
      } yield {
        val post = tuple._1
        val form_data = (board_seq, post.subject, post.content.getOrElse(""))
        Ok(
          views.html.post
            .edit(postForm.fill(form_data), board_seq, post_seq))
      }
  }

  def editPost(board_seq: Long, post_seq: Long) = Authenticated.async {
    implicit request =>
      val form = postForm.bindFromRequest
      form.fold(
        hasErrors =>
          Future.successful(
            BadRequest(views.html.post.edit(hasErrors, board_seq, post_seq))),
        form => {
          posts_repo.update(form, post_seq) map (_ =>
            Redirect(routes.PostController.showPost(board_seq, post_seq)))
        }
      )
  }

  def writePostForm(implicit board_seq: Long) = Authenticated {
    implicit request =>
      Ok(views.html.post.write(postForm))
  }

  def writePost(implicit board_seq: Long) = Authenticated.async {
    implicit request =>
      val form = postForm.bindFromRequest
      form.fold(
        hasErrors =>
          Future.successful(BadRequest(views.html.post.write(hasErrors))),
        form => {
          posts_repo
            .insert(form, request.auth.email, request.remoteAddress) map (
              post =>
                Redirect(
                  routes.PostController.showPost(post.board_seq, post.seq)))
        }
      )
  }

  def deletePost(board_seq: Long, post_seq: Long) = Authenticated.async {
    implicit request =>
      posts_repo
        .delete(post_seq)
        .map(_ => Redirect(routes.PostController.list(board_seq, 1)))
  }

}
