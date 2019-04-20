$(document).ready(function () {

    $("a#signout").click(function () {
        event.preventDefault();
        var href = $(this).attr('href');
        var route = jsRoutes.controllers.AccountController.getClientId();
        $.ajax({
            url: route.url,
            method: route.method,
            data : {csrfToken: $("input[name='csrfToken']").val()}
        }).done(function (result) {
            var json_data = jQuery.parseJSON(JSON.stringify(result));
            if(json_data.client_id){
                gapi.load('auth2', function(){
                    auth2 = gapi.auth2.init({
                        client_id: json_data.client_id
                    }).then(function(e){
                        gapi.auth2.getAuthInstance().signOut().then(function(){
                            window.location = href;
                        });
                    }, function(e){
                        window.location = href;
                    });
                });
            }
        }).fail(function (e) {
            var error_message = jQuery.parseJSON(JSON.stringify(e)).error;
            if(error_message ){
                show_error_noti(error_message);
            }else {
                console.log("sign out failed." + JSON.stringify(e));
                show_error_noti("sign out failed");
            }
        });
    });

});