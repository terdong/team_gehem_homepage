import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._


/**
  * Created by terdong on 2017-03-24 024.
  */
object FuterAndPromise extends App {

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
