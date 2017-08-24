import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
object FutureTest extends App {
  val f = concurrent.Future { Thread.sleep(5000); println("hi") }
  println("waiting")

//  Thread.sleep(6000)

  def nextFtr(i: Int = 0) = Future {
    def rand(x: Int) = util.Random.nextInt(x)

    Thread.sleep(rand(5000))
    if (rand(3) > 0) (i + 1) else throw new Exception
  }
  nextFtr(3) foreach { x =>
    print("x = "); println(x);
  }

  Thread.sleep(6000)
}
