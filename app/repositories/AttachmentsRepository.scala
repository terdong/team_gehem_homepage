package repositories

import javax.inject.{Inject, Singleton}

import models.{Attachment, AttachmentsTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.dbio.Effect
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.sql.FixedSqlAction

import scala.concurrent.Future

/**
  * Created by terdo on 2017-05-17 017.
  */
@Singleton
class AttachmentsRepository @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with AttachmentsTable {

  def updateAttachment(attachments_seq: Long, container_seq: Long) = {
    val action = attachments
      .filter(_.seq === attachments_seq)
      .map(_.container_seq)
      .update(container_seq)
    db run action
  }

  def updateAttachment2(attachments_seq: Seq[Long], container_seq: Long) = {

    val actions: Seq[FixedSqlAction[Int, NoStream, Effect.Write]] = for {
      seq <- attachments_seq
    } yield {
      attachments
        .filter(_.seq === seq)
        .map(_.container_seq)
        .update(container_seq)
    }

    /*val actions = for {
      seq <- attachments_seq
      _ <- DBIO.seq(
        attachments
          .filter(_.seq === seq)
          .map(_.container_seq)
          .update(container_seq))
    } yield ()*/

    /*val action = attachments
      .filter(_.seq === attachments_seq)
      .map(_.container_seq)
      .update(container_seq)*/
    val combined: DBIOAction[Seq[Int], NoStream, Effect.Write] =
      DBIO.sequence(actions)
    db run combined
  }

  def insertAttachment(
      form_data: (String, String, String, String, Long)): Future[Long] = {
    db run insertAttachment_(form_data)
  }

  def getAttachment(hash: String): Future[Attachment] = {
    db run attachments.filter(_.hash === hash).result.head
  }

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

  private def insertAttachment_(
      form_data: (String, String, String, String, Long)) = {
    (attachments.map(a => (a.hash, a.name, a.sub_path, a.mime_type, a.size)) returning attachments
      .map(_.seq)) += (form_data)
  }
}