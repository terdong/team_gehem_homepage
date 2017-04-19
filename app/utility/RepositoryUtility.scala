package utility

import java.sql.Timestamp
import java.util.Calendar

/**
  * Created by terdo on 2017-04-19 019.
  */
trait CurrentTimestamp {
  def getCurretnTimeInMillis =
    new Timestamp(Calendar.getInstance().getTimeInMillis())
}
