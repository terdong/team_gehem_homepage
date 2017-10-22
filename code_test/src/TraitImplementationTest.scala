object TraitImplementationTest extends App{
  trait Monoid[A] {
    def empty: A
    def combine(x: A, y: A): A
  }

  // Implementation for Int
  val intAdditionMonoid: Monoid[Int] = new Monoid[Int] {
    def empty: Int = 0
    def combine(x: Int, y: Int): Int = x + y
  }

  println(intAdditionMonoid.empty)

  def combineAll[A](list: List[A], A: Monoid[A]): A = list.foldRight(A.empty)(A.combine)
}
