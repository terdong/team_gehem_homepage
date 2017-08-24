
//class A
//trait B {self: A =>}

class User(val name:String) {
  def suffix = ""

  override def toString: String = s"$name$suffix"
}

trait Attorney {self: User => override def suffix = ", esq."}
trait Wizard { self: User => override def suffix = ", Wizard"}
trait Reverser { override def toString = super.toString.reverse}

object TraitTest2 extends App{
  //val a = new A with B

  println(new User("Harry P") with Wizard)
  println(new User("Ginny W") with Attorney)
  println(new User("Luna L") with Wizard with Reverser)

}
