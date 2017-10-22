object ObjectTest extends App {
  ObjectOne("Helo?")
}

object ObjectOne {
  def apply(str: String) = {
    Print(str)
  }
  def Print(str: String) = {
    printf(str)
  }
}
