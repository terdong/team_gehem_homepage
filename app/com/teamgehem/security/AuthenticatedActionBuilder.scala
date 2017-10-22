package com.teamgehem.security

import javax.inject.Inject

import com.teamgehem.enumeration.PermissionProvider
import play.api.i18n.MessagesApi
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc._
import com.teamgehem.enumeration.PermissionProvider._
import com.teamgehem.model.MemberInfo

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedActionBuilder(val parser: BodyParser[AnyContent],
                                 messagesApi: MessagesApi,
                                 builder: AuthenticatedBuilder[MemberInfo])(
    implicit val executionContext: ExecutionContext)
    extends ActionBuilder[AuthMessagesRequest, AnyContent] {
  type ResultBlock[A] = (AuthMessagesRequest[A]) => Future[Result]

  @Inject
  def this(parser: BodyParsers.Default,
           messagesApi: MessagesApi,
           builder: MemberAuthenticatedBuilder)(implicit ec: ExecutionContext) = {

    this(parser: BodyParser[AnyContent], messagesApi, builder)
  }

  def authrized_standby = {
    this andThen MemberAuthorizedFilter(Standby)
  }
  def authrized_member = {
    this andThen MemberAuthorizedFilter(PermissionProvider.Member)
  }
  def authrized_dev = {
    this andThen MemberAuthorizedFilter(Developer)
  }
  def authrized_semi_admin = {
    this andThen MemberAuthorizedFilter(SemiAdmin)
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
