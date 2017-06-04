package repositories

import javax.inject.{Inject, Singleton}

import models.{Comment, CommentsTable}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by terdo on 2017-05-21 021.
  */
@Singleton
class CommentsRepository @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with CommentsTable {

  def commentCount(post_seq: Long): Future[Int] = {
    val q = comments.filter(_.post_seq === post_seq).length
    db run q.result
  }

  def all(post_seq: Long) = {
    db run comments.filter(_.post_seq === post_seq).result
  }

  def allWithMemberName(
      post_seq: Long): Future[Seq[(Comment, String, Option[String])]] = {
    val query1 = for {
      comment <- comments.filter(_.post_seq === post_seq)
      member1 <- members.filter(_.seq === comment.author_seq)
    } yield (comment, member1)

    val query2 = for {
      comment <- comments.filter(c =>
        c.post_seq === post_seq && c.reply_comment_seq.isDefined)
      comment2 <- comments.filter(_.seq === comment.reply_comment_seq)
      member2 <- members.filter(_.seq === comment2.author_seq)
    } yield (comment, member2)
    val final_query = for {
      (q1, q2) <- (query1 joinLeft query2 on ((q1,
                                               q2) => q1._1.seq === q2._1.seq))
        .sortBy(_._1._1.thread.asc.nullsFirst)
    } yield (q1._1, q1._2.name, q2.map(_._2.name))
    db run final_query.result
  }

  private def insertQueryBase =
    comments map (c =>
      (c.post_seq,
       c.thread,
       c.author_seq,
       c.reply_comment_seq,
       c.content,
       c.author_ip))

  def insert(form: (Long, Option[Long], String),
             author_seq: Long,
             ip: String) = {

    val action = form._2 match {
      case Some(reply_comment_seq) => {
        comments
          .filter(c =>
            c.reply_comment_seq === reply_comment_seq || c.seq === reply_comment_seq)
          .map(_.thread)
          .max
          .result
          .flatMap { t =>
            val thread = t.getOrElse(0)

            val prev_thread = (thread / 1000 + 1) * 1000

            val q =
              sqlu"update Comments set thread = thread + 1 where seq = ${reply_comment_seq} AND post_seq === ${form._1} AND (thread > ${thread} AND thread < ${prev_thread}) "
            db.run(q)
            /*    val query = comments
              .filter(c =>
                c.seq === reply_comment_seq && c.post_seq === form._1 && (c.thread < thread && c.thread > prev_thread))
              .map(_.thread)
             */
            Logger.debug(s"thread = $thread")
            (insertQueryBase returning comments.map(_.post_seq)) += (form._1,
            thread + 1,
            author_seq,
            form._2,
            form._3,
            ip)
          }
      }
      case _ => {
        comments
          .filter(_.post_seq === form._1)
          .map(_.thread)
          .max
          .result
          .flatMap { (thread: Option[Int]) =>
            (insertQueryBase returning comments.map(_.post_seq)) += (form._1,
            thread.map(_ + 1000) getOrElse (0),
            author_seq,
            form._2,
            form._3,
            ip)
          }
      }
    }
    db run action
  }

  def delete(comment_seq: Long, member_seq: Long) = {
    db run comments
      .filter(c => c.seq === comment_seq && c.author_seq === member_seq)
      .delete
  }
}
