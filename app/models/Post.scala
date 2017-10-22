package models

import java.sql.Timestamp
import slick.jdbc.PostgresProfile.api._

/**
  * Created by terdong on 2017-03-25 019.
  */
case class Post(seq: Long,
                board_seq: Long,
                thread: Long,
                depth: Int,
                author_seq: Long,
                subject: String,
                hit_count: Int,
                content: Option[String],
                author_ip: String,
                write_date: Timestamp = null,
                update_date: Timestamp = null)

trait PostsTable extends BoardsTable with MembersTable {
  protected val posts = TableQuery[Posts]

  protected class Posts(tag: Tag) extends Table[Post](tag, "posts") {
    def seq = column[Long]("seq", O.PrimaryKey, O.AutoInc)

    def board_seq = column[Long]("board_seq")

    def thread = column[Long]("thread")

    def depth = column[Int]("depth")

    def author_seq = column[Long]("author_seq")

    def subject = column[String]("subject", O.Length(80))

    def hit_count = column[Int]("hit_count", O.Default(0))

    def content = column[Option[String]]("content", O.SqlType("text"))

    def author_ip = column[String]("author_ip", O.Length(50))

    def write_date =
      column[Timestamp]("write_date",
                        O.SqlType("timestamp default current_timestamp"))

    def update_date =
      column[Timestamp]("update_date",
                        O.SqlType("timestamp default current_timestamp"))

    def * =
      (seq,
       board_seq,
       thread,
       depth,
       author_seq,
       subject,
       hit_count,
       content,
       author_ip,
       write_date,
       update_date) <> (Post.tupled, Post.unapply)

    def posts_boards_seq_fk =
      foreignKey("posts_boards_seq_fk", board_seq, boards)(
        _.seq,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)

    def posts_members_email_fk =
      foreignKey("posts_members_seq_fk", author_seq, members)(
        _.seq,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)
  }
}
