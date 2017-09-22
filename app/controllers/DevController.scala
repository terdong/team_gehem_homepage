package controllers

import java.util.Collections
import javax.inject.Inject

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.teamgehem.security.AuthenticatedActionBuilder
import play.api.{Configuration, Logger}
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents, MessagesRequest}
import repositories.{CommentsRepository, PostsRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Package: controllers
  * Created by DongHee Kim on 2017-08-31 031.
  */
class DevController @Inject() (config: Configuration,
                               cc: MessagesControllerComponents,
                               auth: AuthenticatedActionBuilder,
                               posts: PostsRepository,
                               comments: CommentsRepository,
                              ) extends MessagesAbstractController(cc) {

  def insertPost100 = auth.authrized_dev.async {

    for(i <- 1 to 100){
      posts.insertSample.map(println)
    }

    Future.successful(Redirect(routes.HomeController.index()))
  }

  def insertComment100 = auth.authrized_dev.async {
    //for(i <- 1 to 100){
      comments.insertSample(105, 100)
    //}
    Future.successful(Redirect(routes.PostController.list(0,1)))
  }

  def google_sign_in = Action { implicit request =>
    Ok(views.html.dev.google_sign_in())
  }


  lazy val google_client_id = config.get[String]("google.client.id")
  val verifier = new GoogleIdTokenVerifier.Builder( new NetHttpTransport(), new JacksonFactory())
    .setAudience(Collections.singletonList(google_client_id))
    // Or, if multiple clients access the backend:
    //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
    .build()

  def authenticate_google_sign_in = Action(parse.formUrlEncoded) { implicit request: MessagesRequest[Map[String, Seq[String]]] =>

    val id_token = request.body("idtoken")(0)
    Logger.debug(s"id_token = $id_token")

    val idToken = verifier.verify(id_token)
    if (idToken != null) {
      val payload = idToken.getPayload()
      // Print user identifier
      val userId = payload.getSubject()
      Logger.debug("User ID: " + userId)

      // Get profile information from payload
      val email = payload.getEmail()
      val emailVerified = payload.getEmailVerified.toString
      val name = payload.get("name").toString
      val pictureUrl = payload.get("picture").toString
      val locale =  payload.get("locale").toString
      val familyName =  payload.get("family_name").toString
      val givenName =  payload.get("given_name").toString
    } else {
      Logger.debug("Invalid ID token.");
    }
    Ok("success!!")
  }
}
