package repositories

import javax.inject.{Inject, Singleton}

import models.{CommentsTable, Post}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{GetResult, JdbcProfile}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.Ordering.String

/**
  * Created by terdo on 2017-04-19 019.
  */
@Singleton
class PostsRepository @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with CommentsTable {

  implicit val get_post_result = GetResult(p =>
    Post(p.<<, p.<<, p.<<, p.<<, p.<<, p.<<, p.<<, p.<<, p.<<, p.<<, p.<<))

  def getPostCount(board_seq: Long = 0) = {
    val query =
      if (board_seq == 0) posts.length
      else posts.filter(_.board_seq === board_seq).length
    db run query.result
  }

  def getPostSearchCountAll(type_number: Int, word: String) = {
    val search_query = convertSearchType(type_number, word)
    val q = sql"""SELECT count(p.seq) FROM posts AS p WHERE #${search_query}"""
      .as[Int]
    db run q.head
  }

  val search_0 = "p.subject ILIKE '%%%s%%'" //제목
  val search_1 = "p.content ILIKE '%%%s%%'" //내용
  val search_2 = "(p.subject ILIKE '%%%s%%' OR p.content ILIKE '%%%s%%')" //제목+내용
  val search_3 =
    "p.author_seq = (SELECT m.seq FROM members AS m WHERE m.name ILIKE '%%%s%%')" // 작성자
  val search_4 =
    "p.seq = (SELECT c.post_seq FROM comments AS c WHERE c.content ILIKE '%%%s%%')" //댓글
  val search_format_list =
    Seq(search_0, search_1, search_2, search_3, search_4)

  def convertSearchType(type_number: Int, word: String) = {
    val s = search_format_list(type_number)
    type_number match {
      case 2 => s.format(word, word); case _ => s.format(word);
    }
  }

  def searchAll(type_number: Int,
                word: String,
                page: Int,
                page_length: Int,
                permission: Byte) = {
    val search_query = convertSearchType(type_number, word)
    val q =
      sql"""SELECT p_result.*, m.name, c_result.comment_count FROM (SELECT p.* FROM posts AS p WHERE EXISTS (SELECT * FROM boards AS b WHERE b.list_permission <= 9 AND p.board_seq = b.seq) AND #${search_query}) AS p_result INNER JOIN members AS m ON p_result.author_seq = m.seq LEFT OUTER JOIN (SELECT c.post_seq, COUNT(c.post_seq) AS comment_count FROM comments AS c GROUP BY c.post_seq) AS c_result ON p_result.seq = c_result.post_seq ORDER BY p_result.seq DESC NULLS FIRST LIMIT ${page_length} OFFSET ${(page - 1) * page_length}"""
        .as[(models.Post, String, Int)]

    db run q
  }

  /*val search_0 = "p.subject LIKE '%%s%'"
  val search_1 = s"p.subject LIKE '%%s%'"
  val search_2 = s"p.subject LIKE '%%s%'"
  val search_3 = s"p.subject LIKE '%%s%'"
  val search_4 = s"p.subject LIKE '%%s%'"
  val search_format_list =
    Seq(search_0, search_1, search_2, search_3, search_4)

  def searchAll(type_number: Int,
                word: String,
                page: Int,
                page_length: Int,
                permission: Byte) = {

    val search = search_format_list(type_number).format(word)
    val search2 = s"p.subject LIKE '%${word}%'"

    val q =
      sql"""SELECT p_result.*, m.name, c_result.comment_count FROM (SELECT p.* FROM posts AS p WHERE EXISTS (SELECT * FROM boards AS b WHERE b.list_permission <= 9 AND p.board_seq = b.seq) AND #${search2}) AS p_result INNER JOIN members AS m ON p_result.author_seq = m.seq LEFT OUTER JOIN (SELECT c.post_seq, COUNT(c.post_seq) AS comment_count FROM comments AS c GROUP BY c.post_seq) AS c_result ON p_result.seq = c_result.post_seq ORDER BY p_result.seq DESC NULLS FIRST LIMIT ${page_length} OFFSET ${(page - 1) * page_length}"""
        .as[(models.Post, String, Int)]

    db run q
  }*/

  private def all_(page: Int, page_length: Int, permission: Byte) = {

    sql"""SELECT p_result.*, m.name, c_result.comment_count FROM (SELECT p.* FROM posts AS p WHERE EXISTS (SELECT * FROM boards AS b WHERE b.list_permission <= 9 AND p.board_seq = b.seq)) AS p_result INNER JOIN members AS m ON p_result.author_seq = m.seq LEFT OUTER JOIN (SELECT c.post_seq, COUNT(c.post_seq) AS comment_count FROM comments AS c GROUP BY c.post_seq) AS c_result ON p_result.seq = c_result.post_seq ORDER BY p_result.seq DESC NULLS FIRST LIMIT ${page_length} OFFSET ${(page - 1) * page_length}"""
      .as[(models.Post, String, Int)]

    /*    sql"select x2.seq, x2.board_seq, x2.thread, x2.depth, x2.author_seq, x2.subject, x2.hit_count, x2.content, x2.author_ip, x2.write_date, x2.update_date, x3.name, count(x5.post_seq) from boards x4 inner join posts x2 inner join members x3 on x2.author_seq = x3.seq on (x4.list_permission <= ${permission}) and (x2.board_seq = x4.seq) left outer join comments x5 on x2.seq = x5.post_seq group by x2.seq, x2.content, x2.author_ip, x2.depth, x2.update_date, x2.write_date, x2.author_seq, x2.subject, x2.hit_count, x2.board_seq, x2.thread, x3.name order by x2.seq desc nulls first limit ${page_length} offset ${(page - 1) * page_length}"
      .as[(models.Post, String, Int)]*/

    /* ((for {
        b <- boards.filter(_.list_permission <= permission)
        p <- posts
          .filter(_.board_seq === b.seq)
        m <- members if p.author_seq === m.seq
      } yield (p, m.name)) joinLeft comments on (_._1.seq === _.post_seq))
        .groupBy {
          case (pm, c: Rep[Option[Comments]]) => (pm._1, pm._2)
        }
        .map {
          case ((p, m), list) => {
            (p, m, list.map(_._2).length)
          }
        }
        .sortBy(_._1.seq.desc.nullsFirst)
        .drop((page - 1) * page_length)
        .take(page_length)*/
  }

  def listByBoard_(board_seq: Long, page: Int, page_length: Int) = {
    sql"SELECT p_result.*, m.name, c_result.comment_count FROM (SELECT p.* FROM posts AS p WHERE p.board_seq = ${board_seq}) AS p_result INNER JOIN members AS m ON p_result.author_seq = m.seq LEFT OUTER JOIN (SELECT c.post_seq, COUNT(c.post_seq) AS comment_count FROM comments AS c GROUP BY c.post_seq) AS c_result ON p_result.seq = c_result.post_seq ORDER BY p_result.seq DESC NULLS FIRST LIMIT ${page_length} OFFSET ${(page - 1) * page_length}"
      .as[(models.Post, String, Int)]

    /*    ((for {
      p <- posts
        .filter(_.board_seq === board_seq)
        .drop((page - 1) * page_length)
        .take(page_length)
      m <- members if p.author_seq === m.seq
    } yield (p, m.name)) joinLeft comments on (_._1.seq === _.post_seq))
      .groupBy({
        case (p, comment) => (p._1, p._2)
      })
      .map {
        case ((p, member_name), list) => {
          (p, member_name, list.map(_._2).length)
        }
      }
      .sortBy(_._1.seq.desc.nullsFirst)*/
  }

  def all(page: Int, page_length: Int, permission: Byte) = {
    db run all_(page, page_length, permission)
  }

  def listByBoard(board_seq: Long, page: Int, page_length: Int) = {
    db run listByBoard_(board_seq, page, page_length)
  }

  def getPost(board_seq: Long, post_seq: Long) = {
    val query = posts.filter(p =>
      p.board_seq === board_seq && p.seq === post_seq) join members on (_.author_seq === _.seq)

    db run query.result.head
  }
  def getPost(post_seq: Long) = {
    val query = posts.filter(_.seq === post_seq) join members on (_.author_seq === _.seq)

    db run query.result.head
  }

  def isOwnPost(post_seq: Long, author_seq: Long) = {
    db run posts
      .filter(p => p.seq === post_seq && p.author_seq === author_seq)
      .exists
      .result
  }

  def updateHitCount(post: Post) = {
    db run (posts filter (_.seq === post.seq) map (_.hit_count) update (post.hit_count + 1))
  }

  def create: Future[Unit] = {
    db run (posts.schema create)
  }

  def dropTable = {
    db run (posts.schema drop)
  }

  def insertQueryBase =
    posts map (p =>
      (p.board_seq,
       p.thread,
       p.depth,
       p.author_seq,
       p.subject,
       p.content,
       p.author_ip))

  def insert(post: Post): Future[Unit] =
    db run (posts += post) map (_ => ())

  def insert(form: (Long, String, String, Seq[Long], Seq[String]),
             author_seq: Long,
             ip: String) = {
    val action = posts.map(_.thread).max.result.flatMap {
      (thread: Option[Long]) =>
        insertQueryBase.returning(posts) += (form._1,
        thread.getOrElse[Long](0) + 100,
        0,
        author_seq,
        form._2,
        Some(form._3),
        ip)
    }

    db run action
  }

  def update(form: (Long, String, String, Seq[Long], Seq[String]),
             post_seq: Long) = {
    val action = posts
      .filter(_.seq === post_seq)
      .map(p => (p.subject, p.content, p.update_date))
      .update(
        (form._2,
         Some(form._3),
         new java.sql.Timestamp(System.currentTimeMillis())))

    db run action
  }

  def delete(post_seq: Long) = {
    val action = posts.filter(_.seq === post_seq).delete
    db run action
  }

  def insertSample = {
    val action = posts.map(_.thread).max.result.flatMap {
      (thread: Option[Long]) =>
        insertQueryBase += (6,
        thread.getOrElse[Long](0) + 100,
        0,
        3,
        "subject",
        Some("content"),
        "127.0.0.1")
    }
    db run (action)
  }
}
