/**
  * Created by terdo on 2017-05-17 017.
  */
object EnumTest extends App {

  object Fruits extends Enumeration {
    val Apple = Value
    val Banana = Value(5)
    val Orange = Value("Or")
    val berry = Value(11, "hello")
  }

  val fruits = Fruits.withName("Apple")
  println(fruits) // Apple
  println(fruits.id) // a

  val fruits2 = Fruits.Banana
  println(fruits2) // Banana
  println(fruits2.id) // 5

  val fruits3 = Fruits.Orange
  println(fruits3) // Or
  println(fruits3.id) // 6

  val fruit4: Fruits.Value = Fruits.berry

  println(fruit4)
  println(fruit4.id)
}
