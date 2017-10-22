package com.teamgehem.security

import com.teamgehem.model.MemberInfo
import play.api.i18n.MessagesApi
import play.api.mvc.{MessagesRequest, Request}

class AuthMessagesRequest[A](val member: MemberInfo,
                             messagesApi: MessagesApi,
                             request: Request[A])
    extends MessagesRequest[A](request, messagesApi)
