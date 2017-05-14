package repositories

import javax.inject.{Inject, Singleton}

import models.{Member, MembersTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.dbio.Effect.Write
import slick.dbio.NoStream
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.sql.FixedSqlAction

import scala.concurrent.Future
import scala.util.Try

/**
  * Created by terdo on 2017-04-17 017.
  */
@Singleton
class MembersRepository @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with MembersTable {

  def all: Future[Seq[Member]] = db run members.result

  def allWithPermission: Future[Seq[(Member, String)]] = {

    val query = for {
      m <- members
      p <- permissions.filter(_.permission_code === m.permission)
    } yield (m, p.content)

    db run query.result
  }

  def findByEmail(email: String) = {
    db run members.filter(_.email === email).result.head
  }

  def create: Future[Unit] = {
    db run (members.schema create)
  }

  def dropTable = {
    db run (members.schema drop)
  }

  def existsEmail(email: String): Future[Boolean] =
    db run (members.filter(i => i.email === email).exists.result)

  def insert(member: Member) = {
    db run (members += member)
  }

  def insert(member: (String, String, String)): Future[Try[Member]] = {
    val action: FixedSqlAction[Member, NoStream, Write] = (members map (m =>
      (m.email, m.name, m.nick)) returning members
      += (member._1, member._2, member._3))
    db run (action.asTry)
  }

  def update(email: String, form: (String, String, Byte, Int, Int)) = {
    val action = members
      .filter(_.email === email)
      .map(m => (m.name, m.nick, m.permission, m.level, m.exp, m.update_date))
      .update(
        (form._1,
         form._2,
         form._3,
         form._4,
         form._5,
         new java.sql.Timestamp(System.currentTimeMillis())))

    db run action
  }

  def update(email: String, form: (String, String)) = {
    val action = members
      .filter(_.email === email)
      .map(m => (m.name, m.nick, m.update_date))
      .update(
        (form._1, form._2, new java.sql.Timestamp(System.currentTimeMillis())))

    db run action
  }

  def insertSample = {
    insert(("terdong@gmail.com", "김동희", "ThePresident"))
  }

}
