package models

import slick.jdbc.PostgresProfile.api._

import scala.language.postfixOps

/**
  * Created by terdo on 2017-04-19 019.
  *
  * M: Member
  * P: Permission
  * 00: default number
  * 01: useless
  * 02: guest
  * 03: member
  * 04 ~ 08: spare
  * 09: administrator
  *
  * example: (permission_code -> MP09)
  *
  */
case class Permission(permission_code: String,
                      active: Boolean,
                      content: String)

trait PermissionsTable {
  protected val permissions = TableQuery[Permissions]

  protected class Permissions(tag: Tag)
      extends Table[Permission](tag, "Permissions") {

    def permission_code =
      column[String]("permission_code",
                     O.PrimaryKey,
                     O.Length(4, varying = false))

    def active = column[Boolean]("active", O.Default(true))

    def content = column[String]("content", O.Length(80))

    def * =
      (permission_code, active, content) <> (Permission.tupled, Permission.unapply)
  }
}
