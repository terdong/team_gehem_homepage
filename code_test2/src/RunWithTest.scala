/**
  * Created by terdo on 2017-05-15 015.
  */
object RunWithTest extends App {

  val base_map = Map[String, String]("Hollo" -> "World", "One" -> "1")

  val action: String => Unit = (T: String) => println(T + "1")

  def aaction: String => Unit = (T: String) => println(T + "2")

  val runWithFunction = base_map.runWith(action)
  val runWithFunction2 = base_map.runWith(aaction)

  val result1 = runWithFunction("Hollo")
  println(result1)

  val result2 = runWithFunction2("no")
  println(result2)

  val lst = List(1, 2, 3, 4, 5)

  val action2: Int => Unit = T => println(T)

  val runWithListFunction = lst.runWith(action2)

  println(runWithListFunction(1)) // "2"
  println(runWithListFunction(3)) // "2"
  // true

  println(runWithListFunction(100)) // false

  val action3: String => String = T => T + "!"
  val runWithFunction3 = base_map.runWith(action3)

  println(runWithFunction3("Hollo")) // true
  println(runWithFunction3("Six")) // false

  val andThenBasemap = base_map.andThen { // andThen은 부분 함수를 파라미터로 입력받는다.
    x: String =>
      x + "!!!"
  }

  val andThenBasemap2 = base_map.andThen(action3)

  println(andThenBasemap("Hollo")) // "World!!!!"
  println(andThenBasemap2("Hollo")) //
  println(andThenBasemap("Six")) // NoSuchElementException

}
