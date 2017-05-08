package com.teamgehem.authentication

/**
  * Created by terdo on 2017-05-06 006.
  */
object Authorized {
  def apply(requiredPermissions: Byte = 0) = {
    Authenticated andThen AuthorizedFilter(requiredPermissions)
  }
}
