import scala.reflect.io.Path

/**
  * Created by terdo on 2017-05-15 015.
  */
object FileTest extends App {

  val path = Path("tmp/file")
  println(path.createDirectory())
  if (path.exists) { println("exists") }

  println(path.separatorStr)
  println(path.path)
}
