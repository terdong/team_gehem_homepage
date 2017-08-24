class A {
  def hi = "hi"
}
trait B {
  self: A =>
  override def toString = "B: " + hi
}
class C extends A with B


class TestSuite(suiteName: String) {def start() {}}

trait RandomSeeded { self: TestSuite =>
  def randomStart(): Unit = {
    util.Random.setSeed(System.currentTimeMillis)
    self.start()
  }
}

class IdSpec extends TestSuite("ID Tests") with RandomSeeded {
  def testId() { println(util.Random.nextInt != 1)}
  override def start() {testId()}

  println("Starting...")
  randomStart()
}

object SelfTypeTest extends App {
  val c = new C
  println(c)

  val id_spec = new IdSpec

}
