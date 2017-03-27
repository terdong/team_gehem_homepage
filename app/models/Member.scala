package models

import java.sql.Timestamp
import java.util.Calendar
import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.dbio.Effect.Write
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import slick.profile.FixedSqlAction
import slick.profile.SqlProfile.ColumnOption.NotNull

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Random, Try}

/**
  * Created by terdong on 2017-03-19 019.
  */
case class Member(userSeq: Int, userId: String, password: String, name: String, email: String, nickName: String, level: Int = 0, exp: Int = 0, regDate: Timestamp = null, updateDate: Timestamp = null, lastLogged: Timestamp = null)

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

  def insertSample: Future[Try[Member]] = {

    val r_name = Random.alphanumeric take (10) mkString
    val action: FixedSqlAction[Member, NoStream, Write] = (members.map(p => (p.userId, p.password, p.name, p.email, p.nickName))
      returning members.map(_.userSeq)
      into ((member, userSeq) => Member(userSeq, member._1, member._2, member._3, member._4, member._5))) +=
      ("admin", "12345", "aa", "aa" + "@gmail.com", "ThePresident")

    Logger.debug("this insertSample")

    db run (action.asTry)
  }

  def getTimeInMillis = new Timestamp(Calendar.getInstance().getTimeInMillis())

  class MembersTable(tag: Tag)
    extends Table[Member](tag, "MEMBER") {
    def userSeq: Rep[Int] = column[Int]("userSeq", O.PrimaryKey, O.AutoInc)

    def userId = column[String]("userId", NotNull)

    def password = column[String]("password")

    def name = column[String]("name")

    def email = column[String]("email")

    def nickName = column[String]("nickName")

    def level = column[Int]("level", O.Default(0))

    def exp = column[Int]("exp", O.Default(0))

    def regdate = column[Timestamp]("regdate", O.SqlType("timestamp default now()"))

    def updateDate = column[Timestamp]("updateDate", O.SqlType("timestamp default now()"))

    def lastLogged = column[Timestamp]("lastLogged", O.SqlType("timestamp default now()"))

    def * = (userSeq, userId, password, name, email, nickName, level, exp, regdate, updateDate, lastLogged) <> (Member.tupled, Member
      .unapply _)

    def unique = index("unique", (userId, email), unique = true)
  }

}

