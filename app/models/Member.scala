package models

import java.sql.Timestamp
import slick.jdbc.PostgresProfile.api._
import scala.language.postfixOps

/**
  * Created by terdong on 2017-03-19 019.
  */
case class Member(seq: Long,
                  email: String,
                  name: String,
                  nick: String,
                  permission: Byte,
                  level: Int = 0,
                  exp: Int = 0,
                  register_date: Timestamp = null,
                  update_date: Timestamp = null,
                  last_logged: Timestamp = null)

trait MembersTable extends PermissionsTable {

  protected val members = TableQuery[Members]

  protected class Members(tag: Tag) extends Table[Member](tag, "Members") {
    def seq = column[Long]("seq", O.PrimaryKey, O.AutoInc)

    def email = column[String]("email", O.Length(80), O.Unique)

    def name = column[String]("name", O.Length(30))

    def nick = column[String]("nick", O.Length(12))

    def permission =
      column[Byte]("permission", O.Length(2), O.Default(2))

    def level = column[Int]("level", O.Default(0))

    def exp = column[Int]("exp", O.Default(0))

    def register_date =
      column[Timestamp]("register_date", O.SqlType("timestamp default now()"))

    def update_date =
      column[Timestamp]("update_date", O.SqlType("timestamp default now()"))

    def last_logged =
      column[Timestamp]("last_logged", O.SqlType("timestamp default now()"))

    def * =
      (seq,
       email,
       name,
       nick,
       permission,
       level,
       exp,
       register_date,
       update_date,
       last_logged) <> (Member.tupled, Member.unapply)
  }
}
