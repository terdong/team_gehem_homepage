package controllers

import javax.inject.Inject

import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.mvc.{Action, Controller}
import repositories.{BoardsRepository, MembersRepository, PostsRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Created by terdong on 2017-03-19 019.
  */
class DBController @Inject()(member_dao: MembersRepository,
                             board_dao: BoardsRepository,
                             post_dao: PostsRepository,
                             errorHandler: HttpErrorHandler)
    extends Controller {

  def memberCreate = Action {
    member_dao.create
    Ok("member table created")
  }

  def samplePostInsert = Action.async { implicit request =>
    post_dao.insertSample map (m => Ok(m.toString))
  }

  def sampleBoardInsert = Action.async { implicit request =>
    board_dao.insertSample map (m => Ok(m.toString))
  }

  def memberInsert = Action.async { implicit request =>
    member_dao.insertSample map (m =>
      m match {
        case Success(user) => Ok(user.toString)
        case Failure(e: PSQLException) if (e.getSQLState == "23505") =>
          //        errorHandler.onClientError(request, FORBIDDEN)
          //        errorHandler.onServerError(request, e).map(s => Ok("hleo"))
          InternalServerError(
            s"Some sort of unique key violation.. ${e.getMessage}")
        case Failure(e: PSQLException) =>
          InternalServerError(e.getMessage)
        case Failure(_) =>
          InternalServerError("Something else happened.. it was bad..")
      })
  /*    member_dao.insertSample match {
          case Success(user) => Ok(user)
          case Failure(e: PSQLException) if (e.getSQLState == "23505") => InternalServerError("Some sort of unique key violation..")
          case Failure(t: PSQLException) => InternalServerError("Some sort of psql error..")
          case Failure(_) => InternalServerError("Something else happened.. it was bad..")
        }*/
  /*   r map { m =>
         m match {
           case m => Ok(s"project ${m.toString} created")
           case e: Exception => Logger.debug(e.getMessage); NotAcceptable
         }*/
  /*      onComplete {
          case Success(m) => Ok(s"project ${m.toString} created")
          case Failure(t) =>
            Logger.debug(t.getMessage)
            NotAcceptable
        }
    }*/
  }

  def memberList = Action.async {
    member_dao.all map { m =>
      val r = m.mkString("\n")
      Logger.debug(r)
      Ok(r)
    }
  }

}
