/**
  * Package:
  * Created by DongHee Kim on 2017-09-18 018.
  */
object ReduceTest extends App{

  val numbers = 1 to 10

  println(numbers.reduceLeft((a,b) => s"${a.toString} + ${b.toString}"))

  println("reduceTest12345")


}
