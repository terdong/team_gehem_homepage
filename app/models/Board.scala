package models

import java.sql.Timestamp

import slick.jdbc.PostgresProfile.api._

import scala.language.postfixOps

/**
  * Created by terdong on 2017-03-25 019.
  */
case class Board(seq: Long,
                 name: String,
                 description: Option[String],
                 status: Boolean,
                 list_permission: Byte,
                 read_permission: Byte,
                 write_permission: Byte,
                 author: String,
                 register_date: Timestamp = null)

trait BoardsTable {
  protected val boards = TableQuery[Boards]

  protected class Boards(tag: Tag) extends Table[Board](tag, "Boards") {
    def seq = column[Long]("seq", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name", O.Unique, O.Length(30))

    def description = column[Option[String]]("description", O.Length(2000))

    def status = column[Boolean]("status")

    def list_permission =
      column[Byte]("list_permission", O.Length(2))

    def read_permission =
      column[Byte]("read_permission", O.Length(2))

    def write_permission =
      column[Byte]("write_permission", O.Length(2))

    def author = column[String]("author", O.Length(80))

    def register_date =
      column[Timestamp]("register_date", O.SqlType("timestamp default now()"))

    def * =
      (seq,
       name,
       description,
       status,
       list_permission,
       read_permission,
       write_permission,
       author,
       register_date) <> (Board.tupled, Board.unapply _)
  }

}
