package repositories

import javax.inject.{Inject, Singleton}

import models.{Permission, PermissionsTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

/**
  * Created by terdo on 2017-04-19 019.
  */
@Singleton
class PermissionsRepository @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with PermissionsTable {

  def all: Future[Seq[Permission]] = db run permissions.result

  def create: Future[Unit] = {
    db run (permissions.schema create)
  }

  def dropTable = {
    db run (permissions.schema drop)
  }

  /*def insertSample = {
    db run
  }*/
}
