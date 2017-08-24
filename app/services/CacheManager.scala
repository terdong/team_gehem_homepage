package services

import javax.inject.{Inject, Singleton}

import com.teamgehem.model.BoardInfo
import play.api.cache.AsyncCacheApi
import play.api.inject.ApplicationLifecycle
import repositories._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by DongHee Kim on 2017-08-22 022.
  */
@Singleton
class CacheManager @Inject()(appLifecycle: ApplicationLifecycle,
                             boards_repo: BoardsRepository,
                             cache: AsyncCacheApi) {

  boards_repo.allSeqAndNameAndListPermission.map(_.map(BoardInfo tupled)).map(cache.set("board.list", _))

  appLifecycle.addStopHook { () =>
    Future.successful(())
  }
}
