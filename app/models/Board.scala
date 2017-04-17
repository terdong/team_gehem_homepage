package models

import java.sql.Timestamp
import java.util.Calendar
import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.postfixOps

/**
  * Created by terdong on 2017-03-25 019.
  */
case class Board(seq: Int, name: String, password: String, email: String, nickName: String, level: Int, exp: Int,
                 regdate: Timestamp)

@Singleton
class BoardDataAccess @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  private val boards = TableQuery[BoardsTable]

  //def idx = ("unique_username", (username), unique = true)

  def all: Future[Seq[Board]] = db run boards.result

  def create: Future[Unit] = {
    db run (boards.schema create)
  }

  def dropTable = {
    db run (boards.schema drop)

  }

  def insert(board: Board): Future[Unit] = db run (boards += board) map (_ => ())

  def insertSample: Future[Int] = db run (boards += Board(1, "admin", "12345", "terdong@gmail.com", "ThePresident", 100, 20, new Timestamp(Calendar.getInstance().getTimeInMillis())))

  class BoardsTable(tag: Tag) extends Table[Board](tag, "BOARD") {
    def seq = column[Int]("seq", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def password = column[String]("password")

    def email = column[String]("email")

    def nickName = column[String]("nickName")

    def level = column[Int]("level", O.Default(0))

    def exp = column[Int]("exp", O.Default(0))

    def regdate = column[Timestamp]("regdate")

    def * = (seq, name, password, email, nickName, level, exp, regdate) <> (Board.tupled, Board.unapply _)

    def board_name_idx = index("board_name_idx", name, unique = true)
  }

}

