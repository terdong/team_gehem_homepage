package controllers.traits

import controllers.AccountController
import play.api.cache.CacheApi
import play.api.mvc.Request

/**
  * Created by terdo on 2017-04-22 022.
  */
case class Header(member_info: Option[(String, String)],
                  board_list: Option[Seq[(Long, String)]])
case class MenuItem(url: String, name: String)

trait ProvidesHeader {
  implicit def header[A](implicit request: Request[A], cache: CacheApi) = {
    val email = request.session.get(AccountController.EMAIL_KEY)
    val permission = request.session.get(AccountController.PERMISSION_KEY)
    val board_list = cache.get[Seq[(Long, String)]]("board_list")
    Header((email zip permission) headOption, board_list)
  }
}
