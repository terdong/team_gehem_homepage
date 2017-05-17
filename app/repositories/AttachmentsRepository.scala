package repositories

import javax.inject.{Inject, Singleton}

import models.{Attachment, AttachmentsTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

/**
  * Created by terdo on 2017-05-17 017.
  */
@Singleton
class AttachmentsRepository @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with AttachmentsTable {

  def getAttachments(post_seq: Long): Future[Seq[Attachment]] = {
    db run (attachments.filter(_.container_seq === post_seq).result)
  }

  def removeAttachements(post_seq: Long) = {
    db run attachments.filter(_.container_seq === post_seq).delete
  }

  def create: Future[Unit] = {
    db run (attachments.schema create)
  }

  def dropTable = {
    db run (attachments.schema drop)
  }
}
