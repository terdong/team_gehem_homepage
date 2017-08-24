class User

val u = new User

class User2(val n:String) {
  val name: String = n
  def greet = s"Hello formm $n"
  override def toString= s"Userr($n)"
}

val u2 = new User2("DongHee")

println(u2.greet)

