package repositories

import javax.inject.{Inject, Singleton}

import models.{Member, Post, PostsTable}
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

  /*  val tupledJoin = posts join members on (_.author === _.email) sortBy (_._1.seq.desc.nullsFirst)

  def tupledJoinFilter(board_seq: Long) =
    posts.filter(_.board_seq === board_seq) join members on (_.author === _.email) sortBy (_._1.seq.desc.nullsFirst)*/

  def getPostCount(board_seq: Long = 0) = {
    val query =
      if (board_seq == 0) posts.length
      else posts.filter(_.board_seq === board_seq).length
    db run query.result
  }

  def getAllPostQuery(page: Int, page_length: Int) =
    for {
      p <- posts
        .sortBy(_.seq.desc.nullsFirst)
        .drop((page - 1) * page_length)
        .take(page_length)
      m <- members if p.author === m.email
    } yield (p, m.name)

  def getPostQuery(board_seq: Long, page: Int, page_length: Int) =
    for {
      p <- posts
        .filter(_.board_seq === board_seq)
        .sortBy(_.seq.desc.nullsFirst)
        .drop((page - 1) * page_length)
        .take(page_length)
      m <- members if p.author === m.email
    } yield (p, m.name)

  def all(page: Int, page_length: Int) = {
    db run getAllPostQuery(page, page_length).result
  }

  def allByBoard(board_seq: Long, page: Int, page_length: Int) = {
    db run getPostQuery(board_seq, page, page_length).result
  }

  def showPost(board_seq: Long,
               post_seq: Long): Future[Option[(Post, Member)]] = {
    val query = posts.filter(p =>
      p.board_seq === board_seq && p.seq === post_seq) join members on (_.author === _.email)

    db run query.result.headOption
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
        insertQueryBase += (form._1,
        thread.getOrElse[Long](0) + 100,
        0,
        email,
        form._2,
        Some(form._3),
        ip)
    }

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
/*
object Paging {
  case class PageReq(
                      page: Int = 1,
                      size: Int = DefaultPageSize,
                      sortFields: Option[List[String]] = None,
                      sortDirections: Option[List[String]] = None) {
    def offset = (page - 1) * size
  }

  case class PageRes[T](items: Seq[T], total: Long)

}

UsersDao {
  val users: TableQuery[UsersTable] = UsersTable.query

  def findBySomeParam(params: A, pageReq: PageReq): Future[PageRes[User]] = {
  db.run(users.result)
  .filter(params)
  .map { r =>
  PageRes(items = r.slice(pageReq.offset, pageReq.offset + pageReq.size), total = r.size)
}
}
}*/
