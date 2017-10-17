package controllers

import java.io.{File, FileInputStream}
import java.util.Collections
import javax.inject.Inject

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.inject.Provider
import com.teamgehem.controller.TGBasicController
import com.teamgehem.security.AuthenticatedActionBuilder
import fly.play.s3.{BucketFile, S3, S3Exception}
import org.apache.commons.io.IOUtils
import play.api.cache.SyncCacheApi
import play.api.mvc.{MessagesControllerComponents, MessagesRequest}
import play.api.{Application, Configuration, Logger}
import repositories.{CommentsRepository, MembersRepository, PostsRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

/**
  * Package: controllers
  * Created by DongHee Kim on 2017-08-31 031.
  */
class DevController @Inject() (config: Configuration,
                               mcc: MessagesControllerComponents,
                               sync_cache:SyncCacheApi,
                               auth: AuthenticatedActionBuilder,
                               posts: PostsRepository,
                               comments: CommentsRepository,
                               members:MembersRepository,
                               //client: WSClient, configuration: Configuration,
                               appProvider: Provider[Application]
                              ) extends TGBasicController(mcc, sync_cache) {


  def error = Action {

    if (true) throw new RuntimeException("foo")
    Forbidden("for")
  }

  def addFile = auth.authrized_dev.async{ implicit request =>

    implicit val app = appProvider.get()

    val bucket = S3("com.teamgehem.files")

    val file_name = "C:/Users/terdo/Pictures/IMG_2349_.jpg"
    val file = new File(file_name)
    val size = IOUtils.toByteArray(new FileInputStream(file))

    Logger.debug(s"size = ${size.length}, file_length = ${file.length()}")

    val result = bucket + BucketFile(file.getName, "image/jpeg", size)
    result
      .map { unit =>
        Logger.info("Saved the file")
        Ok.sendFile(file)
      }
      .recover {
        case S3Exception(status, code, message, originalXml) => { Logger.info("Error: " + message);  Ok.sendFile(file)}
      }


/*    val result_list = bucket.list

    result_list.map { items =>
      items.map {
        case BucketItem(name, isVirtual) => {
          Logger.debug(name)
        }
      }
    }
    val url = bucket.url("fixed_planet_nirn___geographical__v2__by_hori873-d6h7sh0.jpg")
    Logger.debug(s"url = $url")

    val result = bucket get "fixed_planet_nirn___geographical__v2__by_hori873-d6h7sh0.jpg"
    val r = result.map {
      case b:BucketFile => {
        Logger.debug(b.toString)
        Ok(views.html.dev.dev_ok(b.toString))
      }
      case BucketFile(name, contentType, content, acl, headers) => {
        val result = s"$name, $contentType, $content, $acl, $headers"
        Logger.debug(result)
        Ok(views.html.dev.dev_ok(result))
      }
      case _ =>  Ok(views.html.dev.dev_ok("nothing happend"))
    }
    r*/
  }

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

  def insertMemeber100 = auth.authrized_dev.async {
    val x = Random.alphanumeric

    //for(i <- 1 to 10){
      members.insertSample(x.take(10).mkString, s"${x.take(8).mkString}@teamgehem.com")
    //}
    Future.successful(Redirect(routes.HomeController.index()))
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
