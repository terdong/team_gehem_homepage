package com.teamgehem.security

import javax.inject.Inject

import models.Board
import play.api.cache.AsyncCacheApi
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
  * Package: com.teamgehem.security
  * Created by DongHee Kim on 2017-09-11 011.
  */
class BoardStateFilter @Inject()(cache: AsyncCacheApi, action_builder:DefaultActionBuilder)(implicit ec: ExecutionContext) {
  import com.teamgehem.enumeration.BoardCacheString._
  def checkCommentWriting(board_seq: Long) = new ActionFilter[AuthMessagesRequest] {
    override def executionContext: ExecutionContext = ec
    override protected def filter[A](request: AuthMessagesRequest[A]) = {
      cache.get[Board](combineBoardSeq(board_seq)).map{ board_option =>
        board_option.map(_.is_comment).getOrElse(false) match{
          case true => None
          case _ => Some(Results.Forbidden(views.html.error_pages.HTTP403()))
        }
      }
    }
  }
  def checkAttachmentUploading(board_seq: Long) = new ActionFilter[AuthMessagesRequest] {
    override def executionContext: ExecutionContext = ec
    override protected def filter[A](request: AuthMessagesRequest[A]) = {
      cache.get[Board](combineBoardSeq(board_seq)).map{ board_option =>
        board_option.map(_.is_attachment).getOrElse(false) match{
          case true => None
          case _ => Some(Results.Forbidden(views.html.error_pages.HTTP403()))
        }
      }
    }
  }

  def checkReplyWriting(board_seq: Long) = new ActionFilter[AuthMessagesRequest] {
    override def executionContext: ExecutionContext = ec
    override protected def filter[A](request: AuthMessagesRequest[A]) = {
      cache.get[Board](combineBoardSeq(board_seq)).map{ board_option =>
        board_option.map(_.is_reply).getOrElse(false) match{
          case true => None
          case _ => Some(Results.Forbidden(views.html.error_pages.HTTP403()))
        }
      }
    }
  }

}
