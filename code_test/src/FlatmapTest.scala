import scala.collection.immutable

/**
  * Created by terdo on 2017-05-06 006.
  */
object FlatmapTest extends App {

  val range: immutable.Seq[Int] = 1 to 9

  val result: immutable.Seq[Int] = range.flatMap { i =>
    range.flatMap { j =>
      Seq(i * j)
    }
  }

  result foreach println
}
