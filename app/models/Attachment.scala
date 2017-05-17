package models

import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp

/**
  * Created by terdo on 2017-05-17 017.
  */
case class Attachment(seq: Long,
                      file_name: String,
                      file_path: String,
                      mime_type: String,
                      size: Long,
                      container_seq: Long,
                      uploaded_date: Timestamp)

trait AttachmentsTable extends PostsTable {
  protected val attachments = TableQuery[Attachments]

  protected class Attachments(tag: Tag)
      extends Table[Attachment](tag, "Attachements") {
    def seq = column[Long]("seq", O.PrimaryKey, O.AutoInc)

    def file_name = column[String]("file_name", O.Length(127))

    def file_path = column[String]("file_path", O.Length(255))

    def mime_type = column[String]("mime_type", O.Length(255))

    def size = column[Long]("size")

    def container_seq = column[Long]("container_seq")

    def uploaded_date =
      column[Timestamp]("uploaded_date",
                        O.SqlType("timestamp default current_timestamp"))

    def * =
      (seq,
       file_name,
       file_path,
       mime_type,
       size,
       container_seq,
       uploaded_date) <> (Attachment.tupled, Attachment.unapply)

    def attachments_container_seq_fk =
      foreignKey("attachments_container_seq_fk", container_seq, posts)(
        _.seq,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)
  }
}
