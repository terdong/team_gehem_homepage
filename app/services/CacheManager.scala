package services

import javax.inject.{Inject, Singleton}

import com.teamgehem.enumeration.CacheString
import com.teamgehem.model.{BoardInfo, NavigationInfo}
import models.Board
import play.api.{Configuration, Logger}
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
                             config: Configuration,
                             boards_repo: BoardsRepository,
                             navs_repo:NavigationsRepository,
                             cache: AsyncCacheApi) {
  import CacheString._

  Logger.info("CacheManager Start")

  updateBoardCache
  updateNavigationCache

  def updateBoardCache = {
    boards_repo.getAllSeqAndNameAndListPermission.map(_.map(BoardInfo tupled)).foreach(cache.set(List_Permission, _))
    boards_repo.getAvailableBoards.foreach{
      _.foreach{ (board: Board) =>
        cache.set(combineBoardSeq(board.seq), board)
      }
    }
    val notice_board_count = config.getOptional[Int]("board.notice.count").getOrElse(3)
    boards_repo.getNoticeBoardSeqList(notice_board_count).map(cache.set(Notice_Board_Seq_List, _))
  }

  def updateNavigationCache ={
    navs_repo.getValidListForNavigationInfo.map(_.fold(
      e => Logger.error(e.getMessage) ,
      list => {
        val r = list.map(NavigationInfo tupled)
        cache.set(Navigation_List, r)
      })
    )
  }
  appLifecycle.addStopHook { () =>
    Future.successful(())
  }
}
