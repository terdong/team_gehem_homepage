package models

import java.sql.Timestamp
import java.util.Calendar
import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
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
case class Member(userSeq: Int = 0, email: String, name: String, nickName: String, role: String = "guest", level: Int = 0, exp: Int = 0, regDate: Timestamp = null, updateDate: Timestamp = null, lastLogged: Timestamp = null)

@Singleton
class MemberDataAccess @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  //  private val members = TableQuery[MembersTable]
  private val members = TableQuery[MembersTable]

  def all: Future[Seq[Member]] = db run members.result

  def create: Future[Unit] = {
    db run (members.schema create)
  }

  def existsEmail(email: String): Future[Boolean] = db run (members.filter(i => i.email === email).exists.result)

  /*  def create(table_name: String): Future[Unit] = {
      if (!table_map.isDefinedAt(table_name)) {
        table_map += (table_name -> TableQuery[MembersTable]((tag: Tag) => new MembersTable(tag, table_name)))
      }

      db run (table_map(table_name).schema create)
    }*/

  def dropTable = {
    //table_map.values foreach (t => db run (t.schema drop))
    db run (members.schema drop)
  }

  //  def insert(member: Member) = db run (members += member).map { _ => () }
  def insert(member: (String, String, String)): Future[Try[Member]] = {
    val action: FixedSqlAction[Member, NoStream, Write] = (members.map(p => (p.email, p.name, p.nickName))
      returning members.map(_.userSeq)
      into ((member, userSeq) => Member(userSeq, member._1, member._2, member._3))) += (member)
    db run (action.asTry)
  }

  def insertSample: Future[Try[Member]] = {

    val r_name = Random.alphanumeric take (10) mkString
    val action: FixedSqlAction[Member, NoStream, Write] = (members.map(p => (p.email, p.name, p.nickName))
      returning members.map(_.userSeq)
      into ((member, userSeq) => Member(userSeq, member._1, member._2, member._3))) +=
      ("aa" + "@gmail.com", "aa", "ThePresident")

    Logger.debug("this insertSample")

    db run (action.asTry)
  }

  def getTimeInMillis = new Timestamp(Calendar.getInstance().getTimeInMillis())

  class MembersTable(tag: Tag) extends Table[Member](tag, "Member") {
    def userSeq: Rep[Int] = column[Int]("userSeq", O.PrimaryKey, O.AutoInc)

    def email = column[String]("email", NotNull)

    def name = column[String]("name")

    def nickName = column[String]("nickName")

    def role = column[String]("role", O.Default("guest"))

    def level = column[Int]("level", O.Default(0))

    def exp = column[Int]("exp", O.Default(0))

    def regdate = column[Timestamp]("regdate", O.SqlType("timestamp default now()"))

    def updateDate = column[Timestamp]("updateDate", O.SqlType("timestamp default now()"))

    def lastLogged = column[Timestamp]("lastLogged", O.SqlType("timestamp default now()"))

    def * = (userSeq, email, name, nickName, role, level, exp, regdate, updateDate, lastLogged) <> (Member.tupled, Member
      .unapply _)

    def unique = index("unique", (email), unique = true)
  }

}

