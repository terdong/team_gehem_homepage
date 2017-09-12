package repositories

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.{Inject, Singleton}

import models.{Comment, CommentsTable}
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

  val Max_Sub_Comment_Count = 1000

  def commentCount(post_seq: Long): Future[Int] = {
    val q = comments.filter(_.post_seq === post_seq).length
    db run q.result
  }

  def all(post_seq: Long) = {
    db run comments.filter(_.post_seq === post_seq).result
  }

  def allWithMemberNameForPagination(post_seq: Long, page: Int, page_length: Int) = {
    val query1 = for {
      comment <- comments.filter(_.post_seq === post_seq)
      member1 <- members.filter(_.seq === comment.author_seq)
    } yield (comment, member1)

    val query2 = for {
      comment <- comments.filter(c => c.post_seq === post_seq && c.reply_comment_seq.isDefined)
      comment2 <- comments.filter(_.seq === comment.reply_comment_seq)
      member2 <- members.filter(_.seq === comment2.author_seq)
    } yield (comment, member2)

    val joined_query = for {
      (q1, q2) <- (query1 joinLeft query2 on ((q1, q2) => q1._1.seq === q2._1.seq)).sortBy(_._1._1.thread.asc.nullsFirst).drop((page - 1) * page_length).take(page_length)
    } yield (q1._1, q1._2.name, q2.map(_._2.name))

    db run joined_query.result
  }

  def allWithMemberName(post_seq: Long): Future[Seq[(Comment, String, Option[String])]] = {
    val query1 = for {
      comment <- comments.filter(_.post_seq === post_seq)
      member1 <- members.filter(_.seq === comment.author_seq)
    } yield (comment, member1)

    val query2 = for {
      comment <- comments.filter(c => c.post_seq === post_seq && c.reply_comment_seq.isDefined)
      comment2 <- comments.filter(_.seq === comment.reply_comment_seq)
      member2 <- members.filter(_.seq === comment2.author_seq)
    } yield (comment, member2)

    val joined_query = for {
      (q1, q2) <- (query1 joinLeft query2 on ((q1, q2) => q1._1.seq === q2._1.seq)).sortBy(_._1._1.thread.asc.nullsFirst)
    } yield (q1._1, q1._2.name, q2.map(_._2.name))

    db run joined_query.result
  }

  private def insertQueryBase =
    comments map (c =>
      (c.post_seq,
        c.thread,
        c.author_seq,
        c.reply_comment_seq,
        c.content,
        c.author_ip))

  def insert(form: (Long, Option[Long], String), author_seq: Long, ip: String) = {
    form._2 match{
      case Some(reply_comment_seq) => {
        val query_for_thread = sql"""SELECT thread FROM comments WHERE seq = ${reply_comment_seq}""".as[Int].head
        val final_query = query_for_thread.flatMap{ thread =>
          val next_thread = (thread / Max_Sub_Comment_Count + 1) * Max_Sub_Comment_Count
          val prev_thread = next_thread - Max_Sub_Comment_Count
          val sub_q1 = sqlu"UPDATE comments SET thread = thread - 1 WHERE thread > ${prev_thread} AND thread < ${next_thread}"
          val sub_q2 = sqlu"INSERT INTO comments (post_seq,thread,author_seq,reply_comment_seq,content,author_ip) VALUES (${form._1}, ${next_thread} - 1, ${author_seq},${form._2},${form._3},${ip})"
          val verification_query = sql"""SELECT count(*) FROM comments WHERE thread >= ${prev_thread} AND thread < ${next_thread}""".as[Int].head
          val verification_action = verification_query.flatMap { count =>
            //Logger.debug(s"parent_thread(${prev_thread})'s sub comment count = $count")
            if (count > Max_Sub_Comment_Count) {
              DBIO.failed(new Exception("The number of child threads for that thread has been exceeded 1000."))
            }
            else {
              DBIO.successful(s"It has been inserted.")
            }
          }
          sub_q1 andThen sub_q2 zip verification_action
        }

        db.run(final_query.transactionally).map {
          case (query_result, rollback_result: String) => {
            //Logger.debug(s"query_result: ${query_result}, rollback_result: ${rollback_result}")
            query_result
          }
        }
      }
      case _ => {
        val q = sqlu"INSERT INTO comments (post_seq,thread,author_seq,reply_comment_seq,content,author_ip) VALUES (${form._1},(SELECT COALESCE(MAX(thread) + 1000,0) FROM comments WHERE post_seq = ${form._1}), ${author_seq},${form._2},${form._3},${ip})"
        db.run(q.transactionally)
      }
    }
  }

  val atomicCounter = new AtomicInteger()

  def insertSample = {
    for(i <- 1 to 10) {
      insert((836, None, s"content_${atomicCounter.getAndIncrement()}"), 2, "127.0.0.1")
    }
  }

  def delete(comment_seq: Long, member_seq: Long) = {
    db run comments
      .filter(c => c.seq === comment_seq && c.author_seq === member_seq)
      .delete
  }
}
