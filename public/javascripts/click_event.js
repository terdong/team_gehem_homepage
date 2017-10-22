/**
 * Created by terdong on 2017-03-24 024.
 */
$(function () {
    $("#clickEvent tr").on("click", function () {
        var p = $(this).attr("url");
        if(p != undefined){
            location.href = p;
        }else{
            console.error("url= " + p);
        }
    });
});