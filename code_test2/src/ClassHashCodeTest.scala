object ClassHashCodeTest extends App {

  val class_list = for (i <- 1 to 10) yield ClassHashCodeTest1(i)

  println("ClassHashCodeTest")
  class_list.map(_.hashCode).map(println)

  println(ClassHashCodeTest1(1).hashCode)
  println(ClassHashCodeTest1(2).hashCode)
  println(ClassHashCodeTest1(3).hashCode)
  println(ClassHashCodeTest1(4).hashCode)
  println(ClassHashCodeTest1(4).hashCode)
  println(ClassHashCodeTest1(4).hashCode)
  println(ClassHashCodeTest1(4).hashCode)

  val case_class = ClassHashCodeTest1(99)
  //case_class(10)

}

case class ClassHashCodeTest1(var num: Int) {
  println(s"num = $num")
}
class ClassHashCodeTest2
