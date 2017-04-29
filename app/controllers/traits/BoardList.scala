package controllers.traits

import repositories.BoardsRepository

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by terdo on 2017-04-21 021.
  */
trait BoardList {

  implicit def boardList(
      implicit boards_repo: BoardsRepository): Future[Seq[String]] = {
    for (names <- boards_repo.allName) yield names
  }
}
