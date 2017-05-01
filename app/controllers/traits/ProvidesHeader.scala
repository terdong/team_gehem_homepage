package controllers.traits

import controllers.Authenticated
import play.api.cache.CacheApi
import play.api.mvc.Request

/**
  * Created by terdo on 2017-04-22 022.
  */
case class Header(member_email: Option[(String)],
                  board_list: Option[Seq[(Long, String)]])
case class MenuItem(url: String, name: String)

trait ProvidesHeader {
  implicit def header[A](implicit request: Request[A], cache: CacheApi) = {
    val email = request.session.get(Authenticated.email)
    val board_list = cache.get[Seq[(Long, String)]]("board_list")
    Header(email, board_list)
  }
}
