package com.teamgehem.actions

import javax.inject.Inject

import com.teamgehem.model.BoardInfo
import play.api.i18n.MessagesApi
import play.api.mvc._
import repositories.BoardsRepository

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by DongHee Kim on 2017-08-20 020.
  */
@deprecated("This is no longer used.","0.3.7")
class BoardTransformer(val parser: BodyParser[AnyContent],
                        messagesApi: MessagesApi,
                       board_repo: BoardsRepository)(implicit val executionContext: ExecutionContext)
  extends ActionTransformer[MessagesRequest, BoardTransformerRequest] {

  @Inject
  def this(parser: BodyParsers.Default,
           messagesApi: MessagesApi,
           board_repo: BoardsRepository)(implicit ec: ExecutionContext) = {
    this(parser: BodyParser[AnyContent], messagesApi, board_repo)
  }

  def transform[A](request: MessagesRequest[A]): Future[BoardTransformerRequest[A]] = {
    board_repo.getAllSeqAndNameAndListPermission.map(_.map(BoardInfo tupled)).map(new BoardTransformerRequest[A](_, messagesApi, request))
  }
}

class BoardTransformerRequest[A](val board_list: Seq[BoardInfo],
                                 messagesApi: MessagesApi,
                                 request: Request[A])
  extends MessagesRequest[A](request, messagesApi)
