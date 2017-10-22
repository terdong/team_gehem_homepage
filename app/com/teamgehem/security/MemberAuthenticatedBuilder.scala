package com.teamgehem.security

import javax.inject.Inject

import com.teamgehem.model.MemberInfo
import play.api.mvc.{AnyContent, BodyParser, BodyParsers, RequestHeader}
import play.api.mvc.Security.AuthenticatedBuilder

import scala.concurrent.ExecutionContext

class MemberAuthenticatedBuilder(parser: BodyParser[AnyContent])(
    implicit ec: ExecutionContext)
    extends AuthenticatedBuilder[MemberInfo](
      { req: RequestHeader =>
        for {
          email: String <- req.session.get("email")
          permission: String <- req.session.get("permission")
          seq: String <- req.session.get("seq")
          nick:String <- req.session.get("nick")
        } yield (MemberInfo(email, permission.toByte, seq.toLong, nick))
      },
      parser
    ) {
  @Inject
  def this(parser: BodyParsers.Default)(implicit ec: ExecutionContext) = {
    this(parser: BodyParser[AnyContent])
  }
}
