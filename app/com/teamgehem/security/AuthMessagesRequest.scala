package com.teamgehem.security

import play.api.i18n.MessagesApi
import play.api.mvc.{MessagesRequest, Request}

class AuthMessagesRequest[A](val member: Member,
                             messagesApi: MessagesApi,
                             request: Request[A])
    extends MessagesRequest[A](request, messagesApi)
