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

  def all: Future[Seq[Post]] = db run posts.result

  def create: Future[Unit] = {
    db run (posts.schema create)
  }

  def dropTable = {
    db run (posts.schema drop)
  }

  def insert(post: Post): Future[Unit] =
    db run (posts += post) map (_ => ())

  def insertSample: Future[Int] = {

    val action = posts map (p =>
      (p.board_seq,
       p.thread,
       p.depth,
       p.author,
       p.subject,
       p.hit_count,
       p.content,
       p.author_ip)) += (1,
    1,
    0,
    "terdong@gmail.com",
    "subject",
    0,
    Some("content"),
    "127.0.0.1")
    db run action
  }
}
