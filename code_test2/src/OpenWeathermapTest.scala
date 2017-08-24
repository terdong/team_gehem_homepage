import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object OpenWeathermapTest extends App {

  def cityTemp(name: String): Double = {

    val url = "http://api.openweathermap.org/data/2.5/weather"
    val cityUrl = s"$url?APPID=911a8391a48c402c4b981601e47d307c&q=$name"
    val json = scala.io.Source.fromURL(cityUrl).mkString.trim
    println(json)
    val pattern = """.*"temp":([\d.]+).*""".r
    val pattern(temp) = json
    println(temp.toDouble - 273.15)
    temp.toDouble - 273.15
  }

  val cityTemps = Future sequence Seq(
    Future(cityTemp("Naju, KR")),
    Future(cityTemp("Seoul, KR"))
  )

  cityTemps onSuccess {
    case Seq(x, y) if x > y => println(s"Naju is warmer: ${x} C")
    case Seq(x, y) if x < y => println(s"Seoul is warmer: ${y} C")
  }

  Thread.sleep(5000)
  println("end")
}
