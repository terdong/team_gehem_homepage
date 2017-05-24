package services

import javax.inject.{Inject, Singleton}

import controllers.traits.BoardInfo
import play.api.Logger
import play.api.cache.CacheApi
import play.api.inject.ApplicationLifecycle
import repositories._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by terdong on 2017-03-20 020.
  */
@Singleton
class TableManager @Inject()(appLifecycle: ApplicationLifecycle,
                             members_repo: MembersRepository,
                             posts_repo: PostsRepository,
                             boards_repo: BoardsRepository,
                             permissions_repo: PermissionsRepository,
                             attachments_repo: AttachmentsRepository,
                             comments_repo: CommentsRepository,
                             cache: CacheApi) {

  Logger.info(s"TableManager start")
  // This code is called when the application starts.

  //members_repo.insertSample

  val cache_board_list: Future[Seq[BoardInfo]] =
    boards_repo.allSeqAndNameAndListPermission.map(_.map(BoardInfo tupled))
  cache_board_list.map(cache.set("board_list", _))

  /*val result = boards_repo.insertSample
  result onComplete {
    case Success(_) => cache_board_list
    case Failure(e: PSQLException) if (e.getSQLState == "23505") =>
      cache_board_list
  }*/

  //for (i <- 1 to 100) { posts_repo.insertSample }

  //  members_repo.create map (_ => members_repo.insertSample)
//  posts_repo.create
//  permissions_repo.create*/

  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    /*    permissions_repo.dropTable
    posts_repo.dropTable
    boards_repo.dropTable
    members_repo.dropTable*/
    Future.successful(())
  }
}
