
class RGBColor(color:Int) { def hex = f"$color%06x"}

trait Opaque extends RGBColor { override def hex = s"${super.hex}FF"}
trait Sheer extends RGBColor { override def hex = s"${super.hex}33"}

class Paint(color: Int) extends RGBColor(color) with Opaque
class Overlay(color:Int) extends RGBColor(color) with Sheer


object TraitTest extends App{

  val green = new RGBColor(255 << 8).hex
  println(s"green = $green")

  val red = new Paint(128 << 16).hex
  println(s"red = $red")

  val blue = new Overlay(192).hex
  println(s"blue = $blue")
}
