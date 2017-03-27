import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

val futures = (0 to 9) map {
  i =>
    Future {
      val s = i.toString
      println(s)
      s
    }
}

val f = Future.reduce(futures)((a, b) => a + b)
val n = Await.result(f, Duration.Inf)