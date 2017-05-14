package com.teamgehem.form

import play.api.data.Form
import play.api.data.Forms._

/**
  * Created by terdo on 2017-05-15 015.
  */
@deprecated
trait PublicForm {
  val member_form = Form(
    tuple(
      "name" -> nonEmptyText(2, 30),
      "nick" -> nonEmptyText(2, 12),
      "permission" -> byteNumber,
      "level" -> number,
      "exp" -> number
    )
  )
}
