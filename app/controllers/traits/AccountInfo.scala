package controllers.traits

import controllers.AccountController
import play.api.mvc.Session

/**
  * Created by terdo on 2017-04-20 020.
  */
trait AccountInfo {
  implicit def account(implicit session: Session): Option[(String, String)] = {
    for {
      email <- session.get(AccountController.EMAIL_KEY)
      permission <- session.get(AccountController.PERMISSION_KEY)
    } yield (email, permission)
  }
}
