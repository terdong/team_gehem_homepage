package controllers

import javax.inject.Inject

import models.MemberDataAccess
import play.api.Logger
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by terdong on 2017-03-19 019.
  */
class DBController @Inject()(member_dao: MemberDataAccess) extends Controller {


  def memberCreate = Action {
    member_dao.create
    Ok("member table created")
  }

  def memberInsert = Action {
    member_dao.insertSample
    Ok("insert sample")
  }

  def memberList = Action.async {
    member_dao.all map {
      m =>
        val r = m.mkString("\n")
        Logger.debug(r)
        Ok(r)
    }
  }

}
