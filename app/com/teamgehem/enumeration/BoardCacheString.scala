package com.teamgehem.enumeration

/**
  * Package: com.teamgehem.enumeration
  * Created by DongHee Kim on 2017-09-11 011.
  */
object BoardCacheString extends Enumeration {
  type BoardCacheString = Value
  val List_Permission = "board.list.list_permission"
  val Available_Board_List_Format = "board.list.available.seq."

  def combineBoardSeq(board_seq:Long) = s"${Available_Board_List_Format}${board_seq}"
}