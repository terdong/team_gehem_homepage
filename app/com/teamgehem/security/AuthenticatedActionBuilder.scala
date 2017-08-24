package com.teamgehem.security

import javax.inject.Inject

import play.api.i18n.MessagesApi
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc._
import PermissionProvider._
import com.teamgehem.model.MemberInfo

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedActionBuilder(val parser: BodyParser[AnyContent],
                                 messagesApi: MessagesApi,
                                 builder: AuthenticatedBuilder[MemberInfo],
                                 filter: MemberAuthorizedFilter)(
    implicit val executionContext: ExecutionContext)
    extends ActionBuilder[AuthMessagesRequest, AnyContent] {
  type ResultBlock[A] = (AuthMessagesRequest[A]) => Future[Result]

  @Inject
  def this(parser: BodyParsers.Default,
           messagesApi: MessagesApi,
           builder: MemberAuthenticatedBuilder,
           filter: MemberAuthorizedFilter)(implicit ec: ExecutionContext) = {

    this(parser: BodyParser[AnyContent], messagesApi, builder, filter)
  }

  def authrized_standby = {
    this andThen MemberAuthorizedFilter(Standby)
  }
  def authrized_member = {
    this andThen MemberAuthorizedFilter(PermissionProvider.Member)
  }
  def authrized_admin = {
    this andThen MemberAuthorizedFilter(Admin)
  }
  def authrized_spare = {
    this andThen MemberAuthorizedFilter(Spare)
  }

  def invokeBlock[A](request: Request[A],
                     block: ResultBlock[A]): Future[Result] = {
    //Logger.debug(s"requiredPermissions = $requiredPermissions")
    builder.authenticate(request, {
      authRequest: AuthenticatedRequest[A, MemberInfo] =>
        block(
          new AuthMessagesRequest[A](authRequest.user, messagesApi, request))
    })
  }
}
