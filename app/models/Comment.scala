package models

import java.sql.Timestamp

import slick.jdbc.PostgresProfile.api._

/**
  * Created by terdo on 2017-05-21 021.
  */
case class Comment(seq: Long,
                   post_seq: Long,
                   thread: Int,
                   author_seq: Long,
                   reply_comment_seq: Option[Long],
                   content: String,
                   author_ip: String,
                   write_date: Timestamp)

trait CommentsTable extends PostsTable {

  protected val comments = TableQuery[Comments]

  protected class Comments(tag: Tag) extends Table[Comment](tag, "Comments") {
    def seq = column[Long]("seq", O.PrimaryKey, O.AutoInc)

    def post_seq = column[Long]("post_seq")

    def thread = column[Int]("thread")

    def author_seq = column[Long]("author_seq")

    def reply_comment_seq = column[Option[Long]]("reply_comment_seq")

    def content = column[String]("content", O.Length(4000))

    def author_ip = column[String]("author_ip", O.Length(50))

    def write_date =
      column[Timestamp]("write_date",
                        O.SqlType("timestamp default current_timestamp"))

    def * =
      (seq,
       post_seq,
       thread,
       author_seq,
       reply_comment_seq,
       content,
       author_ip,
       write_date) <> (Comment.tupled, Comment.unapply)

    def comments_posts_seq_fk =
      foreignKey("comments_posts_seq_fk", post_seq, posts)(
        _.seq,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)

    def comments_members_seq_fk =
      foreignKey("comments_members_seq_fk", author_seq, members)(
        _.seq,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)
  }
}
