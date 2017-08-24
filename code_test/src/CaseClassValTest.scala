object CaseClassValTest extends App {
  val c = CaseClassValTest(1, 2)
  val c2 = new CaseClassValTest2(1, 2)
  println(c.a)
  println(c.b)
  c.c = 19
  println(c.c)
  //println(c2.a)
  println(c2.b)
}

case class CaseClassValTest(a: Int, val b: Int, var c: Int = 3)
class CaseClassValTest2(a: Int, val b: Int)
