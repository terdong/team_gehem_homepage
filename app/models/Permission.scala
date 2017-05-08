package models

import slick.jdbc.PostgresProfile.api._

import scala.language.postfixOps

/**
  * Created by terdo on 2017-04-19 019.
  *
  * M: Member
  * P: Permission
  * 00: guest
  * 01: pre member
  * 02: memeber
  * 09: administrator
  * 99: block
  *
  * example: (permission_code -> MP09)
  *
  */
case class Permission(permission_code: Byte, active: Boolean, content: String)

trait PermissionsTable {
  protected val permissions = TableQuery[Permissions]

  protected class Permissions(tag: Tag)
      extends Table[Permission](tag, "Permissions") {

    def permission_code =
      column[Byte]("permission_code", O.PrimaryKey)

    def active = column[Boolean]("active", O.Default(true))

    def content = column[String]("content", O.Length(80))

    def * =
      (permission_code, active, content) <> (Permission.tupled, Permission.unapply)
  }
}
