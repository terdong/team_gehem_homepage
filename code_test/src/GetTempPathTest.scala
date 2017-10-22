/**
  * Created by terdo on 2017-05-28 028.
  */
object GetTempPathTest extends App {

  val property = "java.io.tmpdir"
  val temp_dir = System.getProperty(property)
  println(s"temp_dir = $temp_dir")

}
