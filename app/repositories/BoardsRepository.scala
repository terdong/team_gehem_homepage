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

  def all: Future[Seq[Board]] =
    db run boards.sortBy(_.seq.nullsFirst).result

  def allSeqAndNameAndListPermission = {
    val query = for {
      board <- boards.filter(_.status === true).sortBy(_.seq.nullsFirst)
    } yield (board.seq, board.name, board.list_permission)
    db run (query.result)
  }

  def getNameBySeq(board_seq: Long) = {
    db run boards.filter(_.seq === board_seq).map(_.name).result.headOption
  }

  def getBoard(board_seq: Long) = {
    db run boards.filter(_.seq === board_seq).result.head
  }

  def existsName(name: String): Future[Boolean] =
    db run (boards.filter(i => i.name === name).exists.result)

  def existsName(board_seq: Long, name: String): Future[Boolean] = {
    val query =
      boards.filter(b => b.name === name && b.seq != board_seq).exists.result
    val sql = query.statements.head
    db run query
  }

  def isListValidBoard(board_seq: Long, permission: Byte) = {
    val query = boards
      .filter(b =>
        b.status === true && b.seq === board_seq && b.list_permission <= permission)
      .exists
      .result
    db run query
  }
  def isReadValidBoard(board_seq: Long, permission: Byte) = {
    val query = boards
      .filter(b =>
        b.status === true && b.seq === board_seq && b.read_permission <= permission)
      .exists
      .result
    db run query
  }
  def isWriteValidBoard(board_seq: Long, permission: Byte) = {
    val query = boards
      .filter(b =>
        b.status === true && b.seq === board_seq && b.write_permission <= permission)
      .exists
      .result
    db run query
  }

  def insert(board: Board): Future[Unit] =
    db run (boards += board) map (_ => ())

  def insert(form_data: (String, String, Boolean, Byte, Byte, Byte),
             name: String): Future[Int] = {
    val action = boards map (b =>
      (b.name,
       b.description,
       b.status,
       b.list_permission,
       b.read_permission,
       b.write_permission,
       b.author)) += (form_data._1,
    Some(form_data._2),
    form_data._3,
    form_data._4,
    form_data._5,
    form_data._6,
    name)
    db run action
  }

  def update(form: (Long, String, String, Boolean, Byte, Byte, Byte),
             name: String) = {
    val action = boards
      .filter(_.seq === form._1)
      .map(
        b =>
          (b.name,
           b.description,
           b.status,
           b.list_permission,
           b.read_permission,
           b.write_permission,
           b.author))
      .update(
        (form._2, Some(form._3), form._4, form._5, form._6, form._7, name))

    db run action
  }

  def setActiveBoard(board_seq: Long, is_active: Boolean) = {
    val q = boards filter (_.seq === board_seq) map (_.status)
    val action = q.update(is_active)
    db run (action)
  }

  def delete(board_seq: Long) = {
    val q = boards filter (_.seq === board_seq)
    val action = q.delete
    db run (action)
  }

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
    99,
    99,
    99,
    "terdong")
    db run action
  }

  def create: Future[Unit] = {
    db run (boards.schema create)
  }

  def dropTable = {
    db run (boards.schema drop)
  }
}
