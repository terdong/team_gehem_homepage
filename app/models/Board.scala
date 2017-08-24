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
                 is_reply: Boolean,
                 is_comment: Boolean,
                 is_attachment: Boolean,
                 list_permission: Byte,
                 read_permission: Byte,
                 write_permission: Byte,
                 author: String,
                 priority: Int,
                 register_date: Timestamp = null)

trait BoardsTable {
  protected val boards = TableQuery[Boards]

  protected class Boards(tag: Tag) extends Table[Board](tag, "boards") {
    def seq = column[Long]("seq", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name", O.Unique, O.Length(30))

    def description = column[Option[String]]("description", O.Length(2000))

    def status = column[Boolean]("status")

    /**
      * 답글 가능 여부
      *
      * @return
      */
    def is_reply = column[Boolean]("is_reply")

    /**
      * 댓글 가능 여부
      *
      * @return
      */
    def is_comment = column[Boolean]("is_comment")

    /**
      * 파일 첨부 가능 여부
      *
      * @return
      */
    def is_attachment = column[Boolean]("is_attachment")

    def list_permission =
      column[Byte]("list_permission", O.Length(2))

    def read_permission =
      column[Byte]("read_permission", O.Length(2))

    def write_permission =
      column[Byte]("write_permission", O.Length(2))

    def author = column[String]("author", O.Length(80))

    def priority = column[Int]("priority", O.Default(0))

    def register_date =
      column[Timestamp]("register_date", O.SqlType("timestamp default now()"))

    def * =
      (seq,
        name,
        description,
        status,
        is_reply,
        is_comment,
        is_attachment,
        list_permission,
        read_permission,
        write_permission,
        author,
        priority,
        register_date) <> (Board.tupled, Board.unapply _)
  }

}
