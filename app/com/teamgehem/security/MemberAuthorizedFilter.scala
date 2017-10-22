package com.teamgehem.security

import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class MemberAuthorizedFilter (requiredPermissions: Byte = 0)(implicit val ec: ExecutionContext)
  extends ActionFilter[AuthMessagesRequest] {
  override def executionContext: ExecutionContext = ec
  override protected def filter[A](request: AuthMessagesRequest[A]) = {
    Future.successful {
      if (request.member.permission >= requiredPermissions) { None } else {
        Some(Results.Unauthorized(views.html.defaultpages.unauthorized()))
      }
    }
  }
}
