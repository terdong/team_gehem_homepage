package models

import java.sql.Timestamp
import slick.jdbc.PostgresProfile.api._
import scala.language.postfixOps

/**
  * Created by terdong on 2017-03-19 019.
  */
case class Member(email: String,
                  name: String,
                  nick: String,
                  permission: String = null,
                  level: Int = 0,
                  exp: Int = 0,
                  register_date: Timestamp = null,
                  update_date: Timestamp = null,
                  last_logged: Timestamp = null)

trait MembersTable {

  protected val members = TableQuery[Members]

  protected class Members(tag: Tag) extends Table[Member](tag, "Members") {
    def email = column[String]("email", O.Length(80), O.PrimaryKey)

    def name = column[String]("name", O.Length(30))

    def nick = column[String]("nick", O.Length(12))

    def permission =
      column[String]("permission",
                     O.Length(4, varying = false),
                     O.Default("MP02"))

    def level = column[Int]("level", O.Default(0))

    def exp = column[Int]("exp", O.Default(0))

    def register_date =
      column[Timestamp]("register_date", O.SqlType("timestamp default now()"))

    def update_date =
      column[Timestamp]("update_date", O.SqlType("timestamp default now()"))

    def last_logged =
      column[Timestamp]("last_logged", O.SqlType("timestamp default now()"))

    def * =
      (email,
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
