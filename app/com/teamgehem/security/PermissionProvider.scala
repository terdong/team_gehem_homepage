package com.teamgehem.security

/**
  * Created by terdo on 2017-05-08 008.
  */
object PermissionProvider extends Enumeration {
  type PermissionProvider = Value
  val Guest: Byte = 0
  val Standby: Byte = 1
  val Member: Byte = 2
  val Developer: Byte = 8
  val Admin: Byte = 9
  val Spare: Byte = 99
}
