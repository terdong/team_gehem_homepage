package com.teamgehem.enumeration

/**
  * Package: com.teamgehem.enumeration
  * Created by DongHee Kim on 2017-09-11 011.
  */
object CacheString extends Enumeration {
  type CacheString  = Value
  val List_Permission = "board.list.list_permission"
  val Available_Board_List_Format = "board.list.available.seq"
  val Notice_Board_Seq_List = "board.list.notice.seq"

  val Navigation_List = "navigation.list"

  def combineBoardSeq(board_seq:Long) = s"${Available_Board_List_Format}${board_seq}"
}