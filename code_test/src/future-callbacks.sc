import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ThatsOdd(i: Int) extends RuntimeException(// <1>
  s"odd $i received!")

import scala.util.{Failure, Success, Try}
// <2>

val doComplete: PartialFunction[Try[String], Unit] = {
  // <3>
  case s@Success(_) => println(s) // <4>
  case f@Failure(_) => println(f)
}

val futures = (0 to 9) map {
  // <5>
  case i if i % 2 == 0 => Future.successful(i.toString)
  case i => Future.failed(ThatsOdd(i))
}
futures map (_ onComplete doComplete) // <6>

val f: Future[Int] = Future.successful(5)
f.foreach(println)
