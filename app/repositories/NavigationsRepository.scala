package repositories

import javax.inject.{Inject, Singleton}

import models.{Navigation, NavigationsTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.util.{Failure, Try}

/**
  * Created by DongHee Kim on 2017-09-25 025.
  */
@Singleton
class NavigationsRepository @Inject()(protected val dbConfigProvider:DatabaseConfigProvider)extends HasDatabaseConfigProvider[JdbcProfile] with NavigationsTable{


  def getPostContentByName(name:String) = {
    val query = for{
      (n, p) <- navigations.filter(_.name === name) join posts on (_.post_seq === _.seq)
    }yield(n.shortcut, p.content)
    db run query.result.head.asTry
  }

  def getValidList = {
    db run navigations.filter(_.status === true).result.asTry
  }
  def getValidListForNavigationInfo = {
    db run navigations.filter(_.status === true).sortBy(_.priority.desc).map(n => (n.name, n.shortcut, n.post_seq)).result.asTry
  }

  def getList = {
    db run navigations.result.asTry
  }

  def getNavigation(seq:Long): Future[Try[Navigation]] = {
    db run navigations.filter(_.seq === seq).result.head.asTry
  }

  def setActiveNavigation(seq:Long, is_active:Boolean) = {
    val action = navigations.filter(_.seq === seq).map(_.status).update(is_active)
    db run action.asTry
  }

  def insert(form:(Option[Long], String, String, Option[String], Boolean, Long, Int)) = {
    val action = insertQueryBase.returning(navigations) += (form._2, form._3, form._4, form._5, form._6, form._7)

    db run action.asTry
  }

  def update(form:(Option[Long], String, String, Option[String], Boolean, Long, Int)) : Future[Try[Int]]= {
   form._1.map{ seq =>
      val action = navigations.filter(_.seq === seq).map (n => (n.name, n.shortcut, n.description, n.status, n.post_seq, n.priority)).update((form._2, form._3, form._4, form._5, form._6, form._7))
      db run action.asTry
    }.getOrElse{
      Future.successful(Failure(new Exception("Seq of navigation is missing.")))
    }
  }

  def delete(seq:Long) = {
    db run navigations.filter(_.seq === seq).delete.asTry
  }

  private def insertQueryBase =
    navigations map (n => (n.name, n.shortcut, n.description, n.status, n.post_seq, n.priority))

/*  def create: Future[Unit] = {
    db run (navigations.schema create)
  }*/
}
