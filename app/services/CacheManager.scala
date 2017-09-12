package services

import javax.inject.{Inject, Singleton}

import com.teamgehem.enumeration.BoardCacheString
import com.teamgehem.model.BoardInfo
import models.Board
import play.api.Logger
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
  import BoardCacheString._

  Logger.info("CacheManager Start")

  boards_repo.getAllSeqAndNameAndListPermission.map(_.map(BoardInfo tupled)).foreach(cache.set(List_Permission, _))
  boards_repo.getAvailableBoards.foreach{
    _.foreach{ (board: Board) =>
      cache.set(combineBoardSeq(board.seq), board)
    }
  }

  appLifecycle.addStopHook { () =>
    Future.successful(())
  }
}
