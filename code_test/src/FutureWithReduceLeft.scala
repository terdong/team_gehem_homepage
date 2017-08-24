import java.util.concurrent.TimeUnit

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by terdo on 2017-05-06 006.
  */
object FutureWithReduceLeft extends App {
  println("go")
  val futures = (0 to 9).map { v =>
    Future { // 2초 후 값을 가지게되는 Future의 리스트
      TimeUnit.SECONDS.sleep(2L)
      v
    }
  }.toList

  //Await.result(futures, Duration.Inf)

  val cur = System.currentTimeMillis()
  val result1: Future[Int] = Future.reduce[Int, Int](futures)(_ + _) // futures의 값들을 합친다.
  result1.foreach { v =>
    println(v)
    println(System.currentTimeMillis() - cur)
  }
}
