package repositories

import javax.inject.{Inject, Singleton}

import models.{Board, BoardsTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by terdo on 2017-04-19 019.
  */
@Singleton
class BoardsRepository @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with BoardsTable {

  def all: Future[Seq[Board]] = db run boards.result

  def create: Future[Unit] = {
    db run (boards.schema create)
  }

  def dropTable = {
    db run (boards.schema drop)
  }

  def insert(board: Board): Future[Unit] =
    db run (boards += board) map (_ => ())

  def insertSample: Future[Int] = {
    val action = boards map (b =>
      (b.name,
       b.description,
       b.status,
       b.list_permission,
       b.read_permission,
       b.write_permission,
       b.author)) += ("noti",
    Some("notification board"),
    true,
    "admin",
    "admin",
    "admin",
    "terdong")
    db run action
  }
}
