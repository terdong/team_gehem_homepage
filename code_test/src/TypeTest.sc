class Car {
  override def toString = "Car()"
}

class Volvo extends Car {
  override def toString: String = "Volvo()"
}

val c:Car = new Volvo


case class Item[+A](a:A) { def get:A = a}
//case class Item[A](a:A) { def get:A = a} error

val i:Item[Car] = new Item[Volvo](new Volvo)

println(i.get)


class Check[-A] {def check(a:A) = {}}
class Check2[-A] {def check(a:A) = {}}
//class Check3[+A] {def check(a:A) = {}} error