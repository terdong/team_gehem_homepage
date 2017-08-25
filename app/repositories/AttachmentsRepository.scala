package repositories

import javax.inject.{Inject, Singleton}

import models.{Attachment, AttachmentsTable, Post}
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
    val combined: DBIOAction[Seq[Int], NoStream, Effect.Write] = DBIO.sequence(actions)
    db run combined
  }

  def insertAttachment(
                        form_data: (String, String, String, String, Long)): Future[Long] = {
    db run insertAttachment_(form_data)
  }

  def insertAttachment(form_data: (String, String, String, String, Long, Long))
  : Future[Long] = {
    db run insertAttachment_(form_data)
  }

  def updateDownloadCount(post: Post) = {
    db run (posts filter (_.seq === post.seq) map (_.hit_count) update (post.hit_count + 1))
  }

  def getAttachmentWithoutCount(hash: String) = {
    db run attachments.filter(_.hash === hash).result.headOption
  }

  def getAttachment(hash: String) = {
    val q1 =
      sqlu"update attachments set download_count = download_count + 1 where hash = ${hash}"
    val q2 = attachments.filter(_.hash === hash).result.head

    val a = q1 andThen q2
    db run a
  }

  def getAttachments(post_seq: Long): Future[Seq[Attachment]] = {
    db run (attachments
      .filter(_.container_seq === post_seq)
      .sortBy(_.seq.asc.nullsFirst)
      .result)
  }

  def deleteAttachements(post_seq: Long) = {
    db run attachments.filter(_.container_seq === post_seq).delete
  }

  def deleteAttachment(hash: String) = {
    db run attachments.filter(_.hash === hash).delete
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

  private def insertAttachment_(
                                 form_data: (String, String, String, String, Long, Long)) = {
    (attachments.map(a =>
      (a.hash, a.name, a.sub_path, a.mime_type, a.size, a.container_seq)) returning attachments
      .map(_.seq)) += (form_data)
  }
}
