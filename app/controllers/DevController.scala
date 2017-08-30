package controllers

import javax.inject.Inject

import com.teamgehem.security.AuthenticatedActionBuilder
import play.api.mvc.{AbstractController, ControllerComponents}
import repositories.PostsRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Package: controllers
  * Created by DongHee Kim on 2017-08-31 031.
  */
class DevController @Inject() (cc: ControllerComponents,
                               auth: AuthenticatedActionBuilder,
                               posts: PostsRepository
                              ) extends AbstractController(cc) {

  def insertPost100 = auth.authrized_dev.async {
    for(i <- 1 to 100){
      posts.insertSample.map(println)
    }
    Future.successful(Redirect(routes.HomeController.index()))
  }
}
