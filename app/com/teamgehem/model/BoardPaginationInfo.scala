package com.teamgehem.model

/**
  * Created by DongHee Kim on 2017-08-21 021.
  */
//case class BoardListInfo (board_name:Option[String], posts:Seq[(Post,String,Int)], page:Int, page_length:Int)
case class BoardPaginationInfo(current_page: Int, page_length: Int, all_post_count:Int) {
  def getLowBound(bound: Int) = {
    ((current_page.toDouble / bound).floor * bound).toInt
  }

  def getHighBound(bound: Int, low_bound: Int) = {
    if ((low_bound + bound) * page_length >= all_post_count) {
      all_post_count / page_length + 1
    } else {
      low_bound + bound
    }
  }
}