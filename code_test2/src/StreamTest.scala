/**
  * Created by terdo on 2017-05-06 006.
  */
object StreamTest extends App {

  val fibs: Stream[Int] =
    0 #:: 1 #:: fibs
      .zip(fibs.tail)
      .map(n => n._1 + n._2)

  fibs.take(3).foreach(println)

  val fibs2: Stream[Int] =
    0 #:: 1 #:: fibs2
      .zip(fibs2.tail)
      .map(n => {
        println("Adding %d and %d".format(n._1, n._2))
        n._1 + n._2
      })

  fibs2 take 5 foreach println
  println("And then prints")
  fibs2 take 6 foreach println

  def fibs3: Stream[BigInt] = {
    def fibRecu(n2: BigInt = 0, n1: BigInt = 1): Stream[BigInt] =
      n2 #:: fibRecu(n1, n2 + n1)

    fibRecu()
  }

  fibs3 take 3 foreach println

  println("Stream grammer")

  Stream.from(1, 2) take 10 foreach println

  Stream.iterate(1)(s => s + 2) take 10 foreach println

  Stream.fill(5)(100) take 10 foreach println

}
