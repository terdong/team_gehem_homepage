import java.text.SimpleDateFormat
import java.util.Calendar

/**
  * Created by terdo on 2017-05-17 017.
  */
object DateTest extends App {
  //val date1 = DateTimeFormat.forPattern("yyyy-MM-dd").print(DateTime.now())
  val date = new SimpleDateFormat("yyyy_mm_dd")

  val d = date.get2DigitYearStart()
  val dd = date.format(Calendar.getInstance().getTime)
  println(d)
  println(dd)

  val now = Calendar.getInstance()
  val currentHour = now.get(Calendar.DAY_OF_YEAR)
  println(currentHour)

  println(java.time.LocalDate.now)

}
