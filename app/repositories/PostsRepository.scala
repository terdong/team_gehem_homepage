package repositories

import javax.inject.{Inject, Singleton}

import com.teamgehem.model.BoardSearchInfo
import models.{CommentsTable, Post}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{GetResult, JdbcProfile}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by terdo on 2017-04-19 019.
  */
@Singleton
class PostsRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile]
    with CommentsTable {

  val Max_Sub_Post_Count = 100

  implicit val get_post_result = GetResult(p =>
    Post(p.<<, p.<<, p.<<, p.<<, p.<<, p.<<, p.<<, p.<<, p.<<, p.<<, p.<<))

  val search_0 = "p.subject ILIKE '%%%s%%'" //제목
  val search_1 = "p.content ILIKE '%%%s%%'" //내용
  val search_2 = "(p.subject ILIKE '%%%s%%' OR p.content ILIKE '%%%s%%')" //제목+내용
  val search_3 =
    "p.author_seq = (SELECT m.seq FROM members AS m WHERE m.name ILIKE '%%%s%%')" // 작성자
  val search_4 =
    "p.seq = (SELECT c.post_seq FROM comments AS c WHERE c.content ILIKE '%%%s%%')" //댓글
  val search_format_list =
    Seq(search_0, search_1, search_2, search_3, search_4)

  private def convertSearchType(implicit search_info : BoardSearchInfo) = {
    val s = search_format_list(search_info.search_type)
    search_info.search_type match {
      case 2 => s.format(search_info.search_word, search_info.search_word);
      case _ => s.format(search_info.search_word);
    }
  }

  /*    def isDateToday(time:Long) = {
      val secondsInADay = 60*60*24;
      val now = Calendar.getInstance().getTimeInMillis

      val days_now = now / secondsInADay
      val days_target = time / secondsInADay;

      days_now - days_target >= 1
    }
  val get_post_by_boards_query = Compiled((seq:ConstColumn[Long]) => {
    for {
      post  <- posts.filter(_.board_seq === seq).sortBy(p => (p.thread.desc, p.seq.desc)).take(5)
    }yield{
      (post.subject, comments.filter(_.post_seq === post.seq).length, post.update_date)
    }
  })
*/

  def getPostInfoByBoards(seq_board:Seq[Long], limit:Int = 5) = {

    def setParam(board_seq:Long) = {
      sql"""SELECT seq, subject, c_result.comment_count, update_date >= NOW()::DATE AND update_date < NOW()::DATE + INTERVAL '1 DAY' as is_today FROM posts LEFT OUTER JOIN (SELECT c.post_seq, COUNT(c.post_seq) AS comment_count FROM comments AS c GROUP BY c.post_seq) AS c_result ON seq = c_result.post_seq WHERE board_seq = ${board_seq} ORDER BY thread DESC, seq DESC NULLS FIRST LIMIT ${limit}""".as[(Long, String, Int, Boolean)]
    }

    db run DBIO.sequence(seq_board.map(setParam(_)))
  }

  def getPostCount(board_seq: Long = 0) = {
    val query =
      if (board_seq == 0) posts.length
      else posts.filter(_.board_seq === board_seq).length
    db run query.result
  }

  def getAllPostCount(permission: Byte) = {
    val action = for {
      b <- boards.filter(b => b.status === true && b.list_permission <= permission)
    } yield posts.filter(_.board_seq === b.seq).length
    db run action.result.map(_.sum)
  }

  def getSearchPostCount(board_seq: Long, search_info : BoardSearchInfo) = {
    val search_query = convertSearchType(search_info)
    val q =
      sql"""SELECT count(p.seq) FROM posts AS p WHERE EXISTS (SELECT * FROM boards AS b WHERE  b.seq = p.board_seq) AND #${search_query}"""
        .as[Int]
    db run q.head
  }

  def getSearchAllPostCount(permission: Byte,
                            search_info : BoardSearchInfo) = {
    val search_query = convertSearchType(search_info)
    val action = sql"""SELECT count(p.seq) FROM posts AS p WHERE EXISTS (SELECT * FROM boards AS b WHERE b.status = TRUE AND b.list_permission <= $permission AND b.seq = p.board_seq) AND #${search_query}""".as[Int]
    db run action.head
  }

  def searchAll(page: Int,
                page_length: Int,
                permission: Byte,
                search_info: BoardSearchInfo) = {
    val search_query = convertSearchType(search_info)
    val q =
      sql"""SELECT p_result.*, m.name, c_result.comment_count FROM (SELECT p.* FROM posts AS p WHERE EXISTS (SELECT * FROM boards AS b WHERE b.list_permission <= ${permission} AND p.board_seq = b.seq) AND #${search_query}) AS p_result INNER JOIN members AS m ON p_result.author_seq = m.seq LEFT OUTER JOIN (SELECT c.post_seq, COUNT(c.post_seq) AS comment_count FROM comments AS c GROUP BY c.post_seq) AS c_result ON p_result.seq = c_result.post_seq ORDER BY p_result.thread DESC, p_result.seq DESC NULLS FIRST LIMIT ${page_length} OFFSET ${(page - 1) * page_length}"""
        .as[(models.Post, String, Int)]

    db run q
  }

  def search(board_seq: Long,
             page: Int,
             page_length: Int,
             search_info: BoardSearchInfo) = {
    val search_query = convertSearchType(search_info)
    val q =
      sql"""
            SELECT p_result.*, m.name, c_result.comment_count FROM (SELECT p.* FROM posts AS p WHERE p.board_seq = ${board_seq} AND #${search_query}) AS p_result INNER JOIN members AS m ON p_result.author_seq = m.seq LEFT OUTER JOIN (SELECT c.post_seq, COUNT(c.post_seq) AS comment_count FROM comments AS c GROUP BY c.post_seq) AS c_result ON p_result.seq = c_result.post_seq ORDER BY p_result.thread DESC, p_result.seq DESC NULLS FIRST LIMIT ${page_length} OFFSET ${(page - 1) * page_length}
         """.as[(models.Post, String, Int)]
    db run q
  }

  private def all_(page: Int, page_length: Int, permission: Byte) = {

    sql"""SELECT p_result.*, m.name, c_result.comment_count FROM (SELECT p.* FROM posts AS p WHERE EXISTS (SELECT * FROM boards AS b WHERE b.list_permission <= ${permission} AND b.read_permission <= ${permission} AND p.board_seq = b.seq)) AS p_result INNER JOIN members AS m ON p_result.author_seq = m.seq LEFT OUTER JOIN (SELECT c.post_seq, COUNT(c.post_seq) AS comment_count FROM comments AS c GROUP BY c.post_seq) AS c_result ON p_result.seq = c_result.post_seq ORDER BY p_result.thread DESC, p_result.seq DESC NULLS FIRST LIMIT ${page_length} OFFSET ${(page - 1) * page_length}"""
      .as[(models.Post, String, Int)]
  }

  def listByBoard_(board_seq: Long, page: Int, page_length: Int) = {
    sql"""SELECT p_result.*, m.name, c_result.comment_count FROM (SELECT p.* FROM posts AS p WHERE p.board_seq = ${board_seq}) AS p_result INNER JOIN members AS m ON p_result.author_seq = m.seq LEFT OUTER JOIN (SELECT c.post_seq, COUNT(c.post_seq) AS comment_count FROM comments AS c GROUP BY c.post_seq) AS c_result ON p_result.seq = c_result.post_seq ORDER BY p_result.thread DESC, p_result.seq DESC NULLS FIRST LIMIT ${page_length} OFFSET ${(page - 1) * page_length}"""
      .as[(models.Post, String, Int)]

  }

  def all(page: Int, page_length: Int, permission: Byte) = {
    db run all_(page, page_length, permission)
  }

  def getListByBoard(board_seq: Long, page: Int, page_length: Int) = {
    db run listByBoard_(board_seq, page, page_length)
  }

/*  def getPost(board_seq: Long, post_seq: Long) = {
    val query = posts.filter(p =>
      p.board_seq === board_seq && p.seq === post_seq) join members on (_.author_seq === _.seq)

    db run query.result.head
  }*/

  def getPostWithMemberWithBoard(post_seq: Long) = {

    val query = for{
      post <- posts.filter(_.seq === post_seq)
      member <- members.filter(_.seq === post.author_seq)
      board <- boards.filter(_.seq === post.board_seq)
    }yield(post,member,board)
    //val query = posts.filter(_.seq === post_seq) join (members on (_.author_seq === _.seq) join boards on (_._1.board_seq === _.seq))

    db run query.result.head
  }

  def getPostWithMember(post_seq:Long) = {
    val query = posts.filter(_.seq === post_seq) join members on (_.author_seq === _.seq)

    db run query.result.head
  }

  def getPostWithBoard(post_seq:Long) = {
    val query = posts.filter(_.seq === post_seq) join boards on (_.board_seq === _.seq)

    db run query.result.head
  }

  def getPost(post_seq:Long) = {
    db run posts.filter(_.seq === post_seq).result.head
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

  def insert(form: (Long, String, String, Seq[String], Option[Long]),
             author_seq: Long,
             ip: String) = {
    form._5 match {
      case Some(reply_post_seq) =>{
        val query_for_thread = sql"""SELECT thread, depth FROM posts WHERE seq = ${reply_post_seq}""".as[(Long, Int)].head
        val final_query = query_for_thread.flatMap { tuple =>
          val parent_thread = tuple._1
          val depth = tuple._2
          val prev_thread = (parent_thread  - 1) / Max_Sub_Post_Count * Max_Sub_Post_Count
          val sub_q1 = sqlu"UPDATE posts SET thread = thread - 1 WHERE thread > ${prev_thread} AND thread < ${parent_thread}"
/*          val sub_q2 = sqlu"INSERT INTO post (board_seq,thread,depth, author_seq,subject,content,author_ip) VALUES (${form._1}, ${parent_thread} - 1, ${depth} - 1, ${author_seq},${form._2},${form._3},${ip})"*/
          val sub_q2 = insertQueryBase.returning(posts) += (form._1,
            parent_thread - 1,
            depth + 1,
            author_seq,
            form._2,
            Some(form._3),
            ip)
          val verification_query = sql"""SELECT count(*) FROM posts WHERE thread >= ${prev_thread} AND thread < ${parent_thread}""".as[Int].head
          val verification_action = verification_query.flatMap { count =>
            if (count > Max_Sub_Post_Count) {
              DBIO.failed(new Exception(s"The number of child threads for that thread has been exceeded ${Max_Sub_Post_Count}."))
            }
            else {
              DBIO.successful(s"It has been inserted.")
            }
          }
          sub_q1 andThen sub_q2 zip verification_action
        }
        db.run(final_query.transactionally).map {
          case (query_result, rollback_result: String) => {
            query_result
          }
        }
      }
      case _ => {
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
    }
  }

  def update(form: (Long, String, String, Seq[String], Option[Long]),
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

  def delete(post_seq: Long): Future[Int] = {
    val action = posts.filter(_.seq === post_seq).delete
    db run action
  }

  def insertSample = {
    insert((2,"subject", "content", Nil, None), 2, "127.0.0.1")
  }
}
