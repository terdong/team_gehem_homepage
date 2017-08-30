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

  def all: Future[Seq[Permission]] = db run permissions.sortBy(_.permission_code.asc).result

  def allwithActive: Future[Seq[Permission]] =
    db run (permissions.filter(_.active === true).sortBy(_.permission_code.asc) result)

  def getContentByCode(code: Byte) = {
    val query = permissions.filter(_.permission_code === code).map(_.content)
    db run (query result).head
  }

  def create: Future[Unit] = {
    db run (permissions.schema create)
  }

  def dropTable = {
    db run (permissions.schema drop)
  }

  def insert(permission: Permission) = {
    db run (permissions += permission)
  }

  def delete(permission_code: Byte): Future[Int] = {
    db run (permissions filter (_.permission_code === permission_code) delete)
  }

  def existsCode(code: Byte): Future[Boolean] =
    db run (permissions.filter(_.permission_code === code).exists.result)

}
