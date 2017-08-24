class BaseUser(val name: String)

class Admin(name: String, val level: String) extends BaseUser(name)

class Customer(name: String) extends BaseUser(name)

class PreferredCustomer(name: String) extends Customer(name)


// UpperBound Test
def check[A <: BaseUser](u:A) { if (u.name.isEmpty) println("Fail!")}
//def check[A](u:A) { if (u.name.isEmpty) println("Fail!")}  error!
check(new Customer("Fred"))

check(new Admin("", "strict"))

//LowerBound Test
def recruit[A >: Customer](u: Customer): A = u match {
  case p:PreferredCustomer => new PreferredCustomer(u.name)
  case c: Customer => new Customer(u.name)
}

val customer = recruit(new Customer("Fred"))

val preferred = recruit(new PreferredCustomer("George"))

//val preferred1 = recruit(new BaseUser("George")) error

//val error = recruit(new Admin("George"))


abstract class Card {
  type UserType <: BaseUser
  def verify(u:UserType):Boolean
}

class SecurityCard extends Card{
  type UserType = Admin
  override def verify(u: Admin): Boolean = true
}

val v1 = new SecurityCard().verify(new Admin("George", "high"))

class GiftCard extends Card{
  type UserType = Customer
  override def verify(u: Customer): Boolean = true
}

val v2 = new GiftCard().verify(new Customer("Fred"))


