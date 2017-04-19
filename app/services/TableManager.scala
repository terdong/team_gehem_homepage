package services

import javax.inject.{Inject, Singleton}

import play.api.inject.ApplicationLifecycle
import repositories.{
  BoardsRepository,
  MembersRepository,
  PermissionsRepository,
  PostsRepository
}

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
                             permissions_repo: PermissionsRepository) {

  // This code is called when the application starts.
  members_repo.create map (_ => members_repo.insertSample)
  boards_repo.create map (_ => boards_repo.insertSample)
  posts_repo.create
  permissions_repo.create

  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    permissions_repo.dropTable
    posts_repo.dropTable
    boards_repo.dropTable
    members_repo.dropTable
    Future.successful(())
  }
}
