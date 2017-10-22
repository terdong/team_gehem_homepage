package repositories

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

/**
  * Created by terdo on 2017-05-21 021.
  */
trait RepositoryOrigin[T <: Table[_]]
    extends HasDatabaseConfigProvider[JdbcProfile] {
  //self: ITableQueryProvider[C] =>

  val table_query: TableQuery[T]

  def create: Future[Unit] = {
    db run (table_query.schema create)
  }

  def dropTable = {
    db run (table_query.schema drop)
  }
}
