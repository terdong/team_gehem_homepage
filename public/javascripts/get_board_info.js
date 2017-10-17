/**
 * Created by DongHee Kim on 2017-09-15 015.
 */

$(document).ready(function () {
    $(".button_edit").click(function () {
        var board_seq = $(this).data("boardSeq");
        var route = jsRoutes.controllers.BoardController.editBoardForm(board_seq);
        $.ajax({
            url: route.url,
            method: route.method
        }).done(function (result) {
            var edit_route = jsRoutes.controllers.BoardController.editBoard();
            var form = $("#board_form");
            var prev_url = form.attr('action');
            form.prop('action', edit_route.url);
            var json_data = jQuery.parseJSON(JSON.stringify(result));
            form .autofill( json_data );

            form.find(':submit').html("<span class='fa fa-check'></span> Edit");

            var button_create = "<button type='button' class='btn btn-default'><span class='fa fa-check'></span> Create</button>";
            form.append(button_create);
            form.find(':button[type=button]').click(function () {
                form.find(':submit').html("<span class='fa fa-check'></span> Create");
                form.each(function() {
                    this.reset();
                });
                form.prop('action', prev_url);
                $(this).remove();
            });

        }).fail(function () {
            console.log("fail");
        });
    });
});