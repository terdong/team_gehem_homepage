package com.teamgehem.security

import javax.inject.Inject

import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class MemberAuthorizedFilter (requiredPermissions: Byte = 0)(
    implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthMessagesRequest] {

  @Inject
  def this(parser: BodyParsers.Default)(implicit ec: ExecutionContext) = {
    this
  }

  //def authorized(a_requiredPermissions: Byte) = {
  //  requiredPermissions = a_requiredPermissions
  //  this
  //}*/

  def filter[A](request: AuthMessagesRequest[A]) = {
    Logger.debug(s"member_authorized_filter's hashcoded = ${hashCode.toString}")
    //Logger.debug(s"requiredPermissions = $requiredPermissions, request.user.permission = ${request.member.permission}")
    Future.successful {
      if (request.member.permission >= requiredPermissions) { None } else {
        Some(Results.Unauthorized(views.html.defaultpages.unauthorized()))
      }
    }
  }
}
