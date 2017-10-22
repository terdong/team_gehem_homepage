package com.teamgehem.controller

import javax.inject.Inject

import com.teamgehem.helper.TGContextProvider
import play.api.cache.SyncCacheApi
import play.api.i18n.{MessagesImpl, MessagesProvider}
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}

/**
  * Created by DongHee Kim on 2017-09-25 025.
  */
abstract class TGBasicController @Inject()(protected val mcc: MessagesControllerComponents, val sync_cache: SyncCacheApi) extends MessagesAbstractController(mcc) with TGContextProvider{
  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(mcc.langs.availables.head, messagesApi)
  }
}