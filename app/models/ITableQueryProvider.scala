package models

import slick.jdbc.PostgresProfile.api._

/**
  * Created by terdo on 2017-05-21 021.
  */
trait ITableQueryProvider[T <: Table[Any]] {
  def getTableQuery: TableQuery[T]
}
