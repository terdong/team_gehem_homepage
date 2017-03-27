import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration

/**
  * Created by terdong on 2017-03-24 024.
  */
case class ThatsOdd(i: Int) extends RuntimeException(s"odd $i received!")

object FutureCallbacks extends App {


  import scala.util.{Failure, Success, Try}

  println("start")

  val doComplete: PartialFunction[Try[String], Unit] = {
    case s@Success(_) => println(s)
    case f@Failure(_) => println(f)
  }

  val futures: Seq[Future[String]] = (0 to 9) map {
    case i if i % 2 == 0 => Future.successful(i.toString)
    case i => Future.failed(ThatsOdd(i))
  }


  val f: Future[Seq[String]] = Future.sequence(futures)

  f onComplete {
    case s@Success(_) => println(s)
    case f@Failure(_) => println(f)
  }

  //Await.result(f, Duration.Inf)

  futures map (_ onComplete doComplete)
  //Thread.sleep(100)

  val ff: Future[Int] = Future.successful(5)
  //ff.foreach(println)

  ff onSuccess {
    case int => println(int)
  }

  Await.result(ff, Duration.Inf)

}
