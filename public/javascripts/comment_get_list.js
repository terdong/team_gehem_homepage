/**
 * Created by DongHee Kim on 2017-09-15 015.
 */

function convertTimestampToDate(timestamp) {

    var date = new Date(timestamp);
    var year = date.getFullYear();
    var month = date.getMonth() + 1;
    var day = date.getDate();
    var hour = date.getHours();
    var min = date.getMinutes();
    var sec = date.getSeconds();

    var retVal = year + "-" + (month < 10 ? "0" + month : month) + "-"
        + (day < 10 ? "0" + day : day) + " "
        + (hour < 10 ? "0" + hour : hour) + ":"
        + (min < 10 ? "0" + min : min) + ":"
        + (sec < 10 ? "0" + sec : sec);
    return retVal
}

function getLowBound(current_page, bound) {
    return Math.floor(current_page / bound) * bound;
}

function getHighBound(bound, low_bound, page_length, all_item_count) {
    //var op = (low_bound + bound) * page_length;
    //console.log("low_bound = " + low_bound +", bound = " + bound + ", page_length = " + page_length + ", op = " + op + ", all_item_count =" + all_item_count);

    if ((low_bound + bound) * page_length >= all_item_count) {
        return parseInt(all_item_count / page_length);
    } else {
        return parseInt(low_bound + bound);
    }
}

$(document).ready(function () {

    $(document).on("click", ".comment_page", function () {
        if($(this).parent().hasClass("disabled")){ return;}

        var post_seq = $("input[name*='post_seq_for_js']").val();
        var page_length = parseInt($("input[name*='page_info_length']").val());
        var bound = parseInt($("input[name*='page_info_bound']").val());
        var all_comment_count = parseInt($("input[name*='page_info_all_comment_count']").val());
        var selected_page = parseInt($(this).text());
        if (!selected_page) {
            selected_page = parseInt($(this).attr('value'));
        }
        var parent = $(this).parent().parent();

        var r = jsRoutes.controllers.PostController.commentList(post_seq, parseInt(selected_page));
        $.ajax({
            url: r.url,
            method: r.method
            //beforeSend:(function () {})
        }).done(function (callback) {

            var low_bound = getLowBound(selected_page, bound);
            var high_bound = getHighBound(bound, low_bound, page_length, all_comment_count);
            //console.log("low_bound = " + low_bound + ", higi_bound = " + high_bound);

            // change state of the pagination
            if (selected_page == 1) {
                parent.find("li#page_front").addClass("disabled");
                parent.find("li#page_prev").addClass("disabled");
            } else {
                parent.find("li#page_front").removeClass();
                parent.find("li#page_prev").removeClass();
                parent.find("li#page_prev > a").attr('value', selected_page - 1);
            }

            if (selected_page < high_bound) {
                parent.find("li#page_end").removeClass();
                parent.find("li#page_next").removeClass();
                parent.find("li#page_next > a").attr('value', selected_page + 1);
            } else {
                parent.find("li#page_end").addClass("disabled");
                parent.find("li#page_next").addClass("disabled");
            }

            parent.find("li").each(function (i, e) {
                if (!$(e).attr('id')) {
                    $(e).remove();
                }
            });

            var selected_page_tag = $("<li class='active'><a href='javascript:;' class='comment_page'>" + selected_page + "</a></li>").insertAfter("#page_prev");

            for (var i = Math.max(low_bound, 1); i < selected_page; ++i) {
                var li_tag = "<li><a href='javascript:;' class='comment_page'>" + i + "</a></li>";
                $(li_tag).insertBefore(selected_page_tag);
            }

            for (var i = high_bound; i > selected_page; --i) {
                var li_tag = "<li><a href='javascript:;' class='comment_page'>" + i + "</a></li>";
                $(li_tag).insertAfter(selected_page_tag);
            }

            var comment_form_index = 0;
            var comment_form_list = $("#comment_form_list > .row");

            for (var i = 0; i < callback.length; i++) {
                var comment = callback[i].comment;
                var author_name = callback[i].author_name;
                var reply_author_name = callback[i].reply_author_name;

                var comment_form = $(comment_form_list[i]);
                comment_form.find("strong.comment_name").html(author_name);
                comment_form.find("span.comment_date").html("commented " + convertTimestampToDate(comment.write_date));
                comment_form.find("span.comment_content").html(comment.content);

                var col_thumbnail = comment_form.find("div#comment_col_thumbnail_" + i);
                var col_contents = comment_form.find("div#comment_col_contents_" + i);
                if (comment.reply_comment_seq) {
                    col_thumbnail.removeClass().addClass("col-sm-offset-1 col-sm-1");
                    col_contents.removeClass().addClass("col-sm-10");
                    var reply_author_name = reply_author_name ? reply_author_name : "삭제된 댓글";
                    var tag = "<small><em>" + reply_author_name + "&nbsp</em></small>";
                    comment_form.find("span.comment_reply_author_name").html(tag);


                } else {
                    col_thumbnail.removeClass().addClass("col-sm-1");
                    col_contents.removeClass().addClass("col-sm-11");
                    comment_form.find("span.comment_reply_author_name").empty();
                }

                var input_member_seq = comment_form.find("input[name*='member_seq']");
                if (input_member_seq) {
                    comment_form.find("input[name*='reply_comment_seq']").val(comment.seq);

                    comment_form.find("div.panel-heading > button").remove();
                    comment_form.find("div.panel-heading > div.modal").remove();

                    var member_seq = input_member_seq.val();
                    if (member_seq == comment.author_seq) {
                        var button_delete = "<button type='button' title='delete' data-toggle='modal' data-target='#remove_reply_modal_" + i + "' class='btn btn-warning btn-xs'><span class='fa fa-times'></span></button>"

                        comment_form.find("div.panel-heading").append(button_delete);

                        var modal = "<div class='modal fade' id='remove_reply_modal_" + i + "' tabindex='-1' role='dialog' aria-labelledby='myModalLabel'><div class='modal-dialog modal-sm' role='document'><div class='modal-content'><div class='modal-header'><button type='button' class='close' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>×</span></button><h4 class='modal-title text-center' id='myModalLabel'>댓글 삭제</h4></div><div class='modal-body text-center'>정말 이 게시물을 삭제 하시겠습니까?</div><div class='modal-footer'><button type='button' class='btn btn-default' data-dismiss='modal'>No</button><a href='/board/2/post/108/comment/" + comment.seq + "/delete' class='btn btn-danger btn-primary' data-method='delete'>Yes</a></div></div></div></div>"

                        comment_form.find("div.panel-heading").append(modal);
                    }

                    //console.log("member_seq = " + member_seq);
                }

                comment_form.show();
                ++comment_form_index;
            }

            //console.log("comment_form_index = " + comment_form_index);

            for (var i = comment_form_index; i < comment_form_list.length; ++i) {
                $(comment_form_list[i]).hide();
            }
        }).fail(function () {
            console.log("failed to get comment list");
        });
    });

});