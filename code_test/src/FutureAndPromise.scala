import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by terdo on 2017-05-06 006.
  */
object FutureAndPromise extends App {

  def simple() = {
    val promise = Promise[String]()
    val future = promise.future

    future.onFailure {
      case e => println("onFailure: e= " + e.getMessage)
    }

    //future.onFailure { case e => println("onFailure: e=" + e.getMessage) }

    println("a")

    future foreach println
    Thread.sleep(1000)
    //promise.success("입금했음")
    promise.failure(new IllegalArgumentException("미안해"))
  }

  def failedExample() = {

    val promise = Promise.failed(new IllegalArgumentException("미안해1"))

    val future = promise.future

    future.onFailure {

      case e => println("onFailure: e=" + e.getMessage)

    }

  }

  simple

  failedExample
}
