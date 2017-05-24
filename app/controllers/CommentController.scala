package controllers

import javax.inject.{Inject, Singleton}

import play.api.Configuration
import play.api.cache.CacheApi
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import repositories.{
  AttachmentsRepository,
  BoardsRepository,
  MembersRepository,
  PostsRepository
}

/**
  * Created by terdo on 2017-05-22 022.
  */
@Singleton
class CommentController @Inject()(implicit cache: CacheApi,
                                  config: Configuration,
                                  members_repo: MembersRepository,
                                  boards_repo: BoardsRepository,
                                  posts_repo: PostsRepository,
                                  attachments_repo: AttachmentsRepository,
                                  val messagesApi: MessagesApi)
    extends Controller
    with I18nSupport {}
