package com.teamgehem.authentication

/**
  * Created by terdo on 2017-05-08 008.
  */
object PermissionProvider extends Enumeration {
  type PermissionProvider = Value
  val Guest: Byte = 0
  val PreMember: Byte = 1
  val Member: Byte = 2
  val Admin: Byte = 9
  val Block: Byte = 99
}
