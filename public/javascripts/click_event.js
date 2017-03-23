/**
 * Created by terdong on 2017-03-24 024.
 */
$(function () {
    $("#clickEvent tr").on("click", function () {
        location.href = $(this).attr("url");
    });

});