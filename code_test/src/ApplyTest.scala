class CApplyTest {
  def apply = { "hello" }
  def apply(str: String) = {
    str
  }
}

object CApplyTest {
  def apply(str: String) = {
    str
  }
}

object ApplyTest extends App {
  val a_test = new CApplyTest
  println(a_test)
  println(a_test("hello???"))
  println(CApplyTest("hhh"))
}
