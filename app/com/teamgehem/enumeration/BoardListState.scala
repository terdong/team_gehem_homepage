package com.teamgehem.enumeration

/**
  * Package: com.teamgehem.enumeration
  * Created by DongHee Kim on 2017-09-03 003.
  */
object BoardListState extends Enumeration {
  type BoardListState = Value
  val List_Mode:String = "list_mode"
  val Default_List = "default_list"
  val All_List = "all_list"
  val Search_List = "search_list"
  val All_Search_List = "all_search_list"
}
