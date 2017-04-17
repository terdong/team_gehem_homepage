package models

import java.sql.Timestamp
import java.util.Calendar
import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.postfixOps

/**
  * Created by terdong on 2017-03-25 019.
  */
case class Post(userSeq: Int, userId: String, password: String, name: String, email: String, nickName: String, level: Int, exp: Int,
                regdate: Timestamp)

@Singleton
class PostDataAccess @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  private val posts = TableQuery[PostsTable]((tag: Tag) => new PostsTable(tag, "test_post"))

  //def idx = ("unique_username", (username), unique = true)

  def all: Future[Seq[Post]] = db run posts.result

  def create: Future[Unit] = {
    db run (posts.schema create)
  }

  def dropTable = {
    db run (posts.schema drop)

  }

  def insert(board: Post): Future[Unit] = db run (posts += board) map (_ => ())

  def insertSample: Future[Int] = db run (posts += Post(1, "admin", "12345", "김동희", "terdong@gmail.com", "ThePresident", 100, 20, new Timestamp(Calendar.getInstance().getTimeInMillis())))

  class PostsTable(tag: Tag, post_name: String) extends Table[Post](tag, post_name) {
    def userSeq = column[Int]("userSeq", O.PrimaryKey, O.AutoInc)

    def userId = column[String]("userId")

    def password = column[String]("password")

    def name = column[String]("name")

    def email = column[String]("email")

    def nickName = column[String]("nickName")

    def level = column[Int]("level", O.Default(0))

    def exp = column[Int]("exp", O.Default(0))

    def regdate = column[Timestamp]("regdate")

    def * = (userSeq, userId, password, name, email, nickName, level, exp, regdate) <> (Post.tupled, Post.unapply _)

    def userId_idx = index("userId_idx", userId, unique = true)
  }

}

