

object MonixTest extends App{

  println("MonixTest Start!")

/*  import scala.concurrent.Await
  import scala.concurrent.duration._
  import monix.execution.Scheduler.Implicits.global

  val task = Task{1 + 1}

  val future = task.runAsync

  Await.result(future, 5.seconds)*/

  import monix.reactive._

  import scala.concurrent.duration._

  // Nothing happens here, as observable is lazily
  // evaluated only when the subscription happens!
  val tick = {
    Observable.interval(1.second)
      // common filtering and mapping
      .filter(_ % 2 == 0)
      .map(_ * 2)
      // any respectable Scala type has flatMap, w00t!
      .flatMap(x => Observable.fromIterable(Seq(x,x)))
      // only take the first 5 elements, then stop
      .take(5)
      // to print the generated events to console
      .dump("Out")
  }

}
