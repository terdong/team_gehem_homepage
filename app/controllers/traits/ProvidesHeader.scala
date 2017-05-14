package controllers.traits

import com.teamgehem.authentication.Authenticated
import play.api.cache.CacheApi
import play.api.mvc.{Request, RequestHeader}

/**
  * Created by terdo on 2017-04-22 022.
  */
case class Header(member_email: Option[(String)],
                  permission: Option[(String)],
                  board_list: Option[Seq[BoardInfo]],
                  request_header: RequestHeader)
case class BoardInfo(seq: Long, name: String, list_perm: Byte)

trait ProvidesHeader {

  def member_email[A](implicit request: Request[A]): Option[String] =
    request.session.get(Authenticated.email)

  def permission[A](implicit request: Request[A]) =
    request.session.get(Authenticated.permission).getOrElse("0").toByte

  implicit def header[A](implicit request: Request[A], cache: CacheApi) = {
    val email = member_email
    val permission = request.session.get(Authenticated.permission)
    val board_list: Option[Seq[BoardInfo]] =
      cache.get[Seq[BoardInfo]]("board_list")

    val p = permission.getOrElse("0").toByte
    val new_list = for {
      seq <- board_list
    } yield seq.filter(info => p >= info.list_perm)

    Header(email, permission, new_list, request)
  }
}
