package models

import java.sql.Timestamp

import slick.jdbc.PostgresProfile.api._

import scala.language.postfixOps

/**
  * Created by DongHee Kim on 2017-09-25 025.
  */
case class Navigation(seq: Long,
                      name: String,
                      shortcut: String,
                      description: Option[String],
                      status: Boolean,
                      post_seq: Long,
                      priority: Int,
                      register_date: Timestamp)


trait NavigationsTable extends PostsTable {
  protected val navigations = TableQuery[Navigations]

  protected class Navigations(tag: Tag) extends Table[Navigation](tag, "navigations") {
    def seq = column[Long]("seq", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name", O.Unique, O.Length(30))

    def shortcut = column[String]("shortcut", O.Unique, O.Length(30))

    def description = column[Option[String]]("description", O.Length(2000))

    def status = column[Boolean]("status")

    def post_seq = column[Long]("post_seq")

    def priority = column[Int]("priority", O.Default(0))

    def register_date =
      column[Timestamp]("register_date", O.SqlType("timestamp default now()"))

    def * =
      (seq,
        name,
        shortcut,
        description,
        status,
        post_seq,
        priority,
        register_date) <> (Navigation.tupled, Navigation.unapply _)

    def navigations_posts_seq_fk =
      foreignKey("navigations_posts_seq_fk", post_seq, posts)(
        _.seq,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade)
  }
}
