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
  * Created by terdong on 2017-03-19 019.
  */
case class Member(serSeq: Int, userId: String, password: String, nickName: String, score: Int, level: Int, regdate: Timestamp)

@Singleton
class MemberDataAccess @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  private val members = TableQuery[MembersTable]

  def all: Future[Seq[Member]] = db run members.result

  def create: Future[Unit] = {
    db run (members.schema create)
  }

  def dropTable = {
    db run (members.schema drop)
  }

  def insert(member: Member): Future[Unit] = db run (members += member) map (_ => ())

  def insertSample: Future[Int] = db run (members += Member(1, "admin", "12345", "ThePresident", 100, 20, new Timestamp(Calendar.getInstance().getTimeInMillis())))

  class MembersTable(tag: Tag) extends Table[Member](tag, "MEMBER") {
    def userSeq = column[Int]("userSeq", O.PrimaryKey, O.AutoInc)

    def userId = column[String]("userId")

    def password = column[String]("password")

    def nickName = column[String]("nickName")

    def score = column[Int]("score", O.Default(0))

    def level = column[Int]("level", O.Default(0))

    def regdate = column[Timestamp]("regdate")

    def * = (userSeq, userId, password, nickName, score, level, regdate) <> (Member.tupled, Member.unapply _)
  }

}

