package services

import javax.inject.{Inject, Singleton}

import models.MemberDataAccess
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

/**
  * Created by terdong on 2017-03-20 020.
  */
@Singleton
class TableManager @Inject()(member_dao: MemberDataAccess, appLifecycle: ApplicationLifecycle) {

  // This code is called when the application starts.
  member_dao.create
  Logger.debug("member_dao.create")

  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    member_dao.dropTable
    Logger.debug("member_dao.dropTable")
    Future.successful(())
  }
}