package models

import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp

/**
  * Created by terdo on 2017-05-17 017.
  */
case class Attachment(seq: Long,
                      hash: String,
                      name: String,
                      sub_path: String,
                      mime_type: String,
                      size: Long,
                      container_seq: Long,
                      download_count: Int,
                      uploaded_date: Timestamp)

trait AttachmentsTable extends PostsTable {
  protected val attachments = TableQuery[Attachments]

  protected class Attachments(tag: Tag)
      extends Table[Attachment](tag, "Attachements") {
    def seq = column[Long]("seq", O.PrimaryKey, O.AutoInc)

    def hash = column[String]("hash", O.Length(32))

    def name = column[String]("name", O.Length(255))

    def sub_path = column[String]("sub_path", O.Length(64))

    def mime_type = column[String]("mime_type", O.Length(255))

    def size = column[Long]("size")

    def container_seq = column[Long]("container_seq", O.Default(0))

    def download_count = column[Int]("download_count", O.Default(0))

    def uploaded_date =
      column[Timestamp]("uploaded_date",
                        O.SqlType("timestamp default current_timestamp"))

    def * =
      (seq,
       hash,
       name,
       sub_path,
       mime_type,
       size,
       container_seq,
       download_count,
       uploaded_date) <> (Attachment.tupled, Attachment.unapply)

    /*    def attachments_container_seq_fk =
      foreignKey("attachments_container_seq_fk", container_seq, posts)(
        _.seq,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)*/
  }
}
