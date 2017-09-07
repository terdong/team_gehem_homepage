package com.teamgehem.enumeration

/**
  * Package: com.teamgehem.enumeration
  * Created by DongHee Kim on 2017-09-03 003.
  */
object BoardListState extends Enumeration {
  type BoardListState = Value
  val List_Mode:String = "list_mode"
  val Default_List = "0"
  val All_List = "1"
  val Search_List = "2"
  val All_Search_List = "3"
}
