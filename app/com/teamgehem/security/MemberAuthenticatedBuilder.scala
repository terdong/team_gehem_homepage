package com.teamgehem.security

import javax.inject.Inject

import play.api.mvc.{AnyContent, BodyParser, BodyParsers, RequestHeader}
import play.api.mvc.Security.AuthenticatedBuilder

import scala.concurrent.ExecutionContext

class MemberAuthenticatedBuilder(parser: BodyParser[AnyContent])(
    implicit ec: ExecutionContext)
    extends AuthenticatedBuilder[Member](
      { req: RequestHeader =>
        for {
          email: String <- req.session.get("email")
          permission: String <- req.session.get("permission")
          seq: String <- req.session.get("seq")
        } yield (Member(email, permission.toByte, seq.toLong))
      },
      parser
    ) {
  @Inject
  def this(parser: BodyParsers.Default)(implicit ec: ExecutionContext) = {
    this(parser: BodyParser[AnyContent])
  }
}
