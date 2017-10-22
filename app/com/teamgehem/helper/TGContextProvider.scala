package com.teamgehem.helper

import com.teamgehem.enumeration.CacheString
import com.teamgehem.model.{NavigationInfo, TGContext}
import play.api.cache.SyncCacheApi

/**
  * Created by DongHee Kim on 2017-09-25 025.
  */
trait TGContextProvider {
  val sync_cache: SyncCacheApi

  implicit def getContext: TGContext = {
    val op_list = sync_cache.get[Seq[NavigationInfo]](CacheString.Navigation_List)
    TGContext(op_list)
  }
}
