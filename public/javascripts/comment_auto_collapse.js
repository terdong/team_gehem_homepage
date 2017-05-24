/**
 * Created by terdo on 2017-05-22 022.
 */
$(document).ready(function () {
    $(".collapse").on("show.bs.collapse", function(e) {
        var current_id = e.target.id;
        $('.list-group-item').find('.collapse').each(function () {
            var id = this.id
            if(id != current_id){
                $("#"+id).collapse('hide');
            }
        });
    });
});