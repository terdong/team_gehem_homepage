package repositories

import javax.inject.{Inject, Singleton}

import models.{Post, PostsTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by terdo on 2017-04-19 019.
  */
@Singleton
class PostsRepository @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with PostsTable {

  def getPostCount(board_seq: Long = 0) = {
    val query =
      if (board_seq == 0) posts.length
      else posts.filter(_.board_seq === board_seq).length
    db run query.result
  }

  private def all_(page: Int, page_length: Int, permission: Byte) = {
    val query = for {
      b <- boards.filter(_.list_permission <= permission)
      p <- posts.filter(_.board_seq === b.seq)
      m <- members if p.author === m.email
    } yield (p, m.name)
    query
      .sortBy(_._1.seq.desc.nullsFirst)
      .drop((page - 1) * page_length)
      .take(page_length)
  }

  def listByBoard_(board_seq: Long, page: Int, page_length: Int) =
    for {
      p <- posts
        .filter(_.board_seq === board_seq)
        .sortBy(_.seq.desc.nullsFirst)
        .drop((page - 1) * page_length)
        .take(page_length)
      m <- members if p.author === m.email
    } yield (p, m.name)

  def all(page: Int, page_length: Int, permission: Byte) = {
    db run all_(page, page_length, permission).result
  }

  def listByBoard(board_seq: Long, page: Int, page_length: Int) = {
    db run listByBoard_(board_seq, page, page_length).result
  }

  def getPost(board_seq: Long, post_seq: Long) = {
    val query = posts.filter(p =>
      p.board_seq === board_seq && p.seq === post_seq) join members on (_.author === _.email)

    db run query.result.head
  }
  def getPost(post_seq: Long) = {
    val query = posts.filter(_.seq === post_seq) join members on (_.author === _.email)

    db run query.result.head
  }

  def isOwnPost(post_seq: Long, email: String) = {
    db run posts
      .filter(p => p.seq === post_seq && p.author === email)
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
       p.author,
       p.subject,
       p.content,
       p.author_ip))

  def insert(post: Post): Future[Unit] =
    db run (posts += post) map (_ => ())

  def insert(form: (Long, String, String), email: String, ip: String) = {
    val action = posts.map(_.thread).max.result.flatMap {
      (thread: Option[Long]) =>
        insertQueryBase.returning(posts) += (form._1,
        thread.getOrElse[Long](0) + 100,
        0,
        email,
        form._2,
        Some(form._3),
        ip)
    }

    db run action
  }

  def update(form: (Long, String, String), post_seq: Long) = {
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
        insertQueryBase += (17,
        thread.getOrElse[Long](0) + 100,
        0,
        "terdong@gmail.com",
        "subject",
        Some("content"),
        "127.0.0.1")
    }

    db run (action)

  }
}
