class User3(val n: String) {
  val name: String = n
  def greet = s"Hello formm $n"
  override def toString = s"Userr($n)"
}

object ClassTest extends App {

  val u = new User3("동희")

  println(u.greet)
  println(u.n)

}
