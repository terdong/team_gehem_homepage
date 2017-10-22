class Car;

class Volvo extends Car;

class VolvoWagon extends Volvo

class Item[+A](a: A) {
  def get: A = a
}

class Check[-A] {
  def check(a: A) = {}
}

def item(v: Item[Volvo]) {
  val c: Car = v.get
}

def check(v: Check[Volvo]) {
  v.check(new VolvoWagon())
}

//item(new Item[Car](new Car())) error
item (new Item[Volvo](new Volvo()))
item( new Item[VolvoWagon](new VolvoWagon()))

check(new Check[Car]())
check(new Check[Volvo]())
//check(new Check[VolvoWagon]()) error

