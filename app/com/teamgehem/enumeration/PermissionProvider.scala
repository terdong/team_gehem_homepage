package com.teamgehem.enumeration

/**
  * Created by terdo on 2017-05-08 008.
  */
object PermissionProvider extends Enumeration {
  type PermissionProvider = Value
  val Guest: Byte = 0
  val Standby: Byte = 1
  val Member: Byte = 2
  val Developer: Byte = 7
  val Monitor: Byte = 8
  val SemiAdmin: Byte = 9
  val Admin: Byte = 20
  val Spare: Byte = 99
}
