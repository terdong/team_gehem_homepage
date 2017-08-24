import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

val f = concurrent.Future { Thread.sleep(3000); println("hi") }

println("waiting")


def nextFtr(i: Int = 0) = Future {
  def rand(x:Int) = util.Random.nextInt(x)

  Thread.sleep(rand(5000))
  if(rand(3) > 0) (i + i) else throw new Exception
}


nextFtr(3)
