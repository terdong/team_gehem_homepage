$(document).ready(function () {

    $(".navigation_click").click(function () {

        //alert("click");
        //console.log("click");
        //var url

        event.preventDefault();


        var route = jsRoutes.controllers.HomeController.navigation1;

        var post_seq = $(this).siblings("input[name='post_seq']").val();
        var tab = $(this).siblings("input[name='tab']").val();
        //console.log(post_seq.val());
        //return;
        var href = $(this).attr('href');
        var json_data = {post_seq: post_seq, tab: tab};
        //var route = jsRoutes.controllers.AccountController.getClientId();
        console.log(JSON.stringify(json_data));
        //return;
        $.ajax({
            url: route.url,
            method: route.method,
            contentType: 'application/x-www-form-urlencoded',
            data : {"post_seq": post_seq,"tab": tab},
        }).done(function (result) {
            console.log("done");
            /*var json_data = jQuery.parseJSON(JSON.stringify(result));
            if(json_data.client_id){
                gapi.load('auth2', function(){
                    auth2 = gapi.auth2.init({
                        client_id: json_data.client_id
                    }).then(function(){
                        gapi.auth2.getAuthInstance().signOut().then(function(){
                            window.location = href;
                        });
                    });
                });
            }*/
        }).fail(function (xhr, status, error) {
            console.log("fail: " + error.toString());
            console.log("fail: " + status.toString());

            /* var error_message = jQuery.parseJSON(JSON.stringify(e)).error;
             if(error_message ){
                 show_error_noti(error_message);
             }else {
                 console.log("sign out failed." + JSON.stringify(e));
                 show_error_noti("sign out failed");
             }*/
        });

        /*$.ajax({
            url: r.url,
            method: r.method,
            contentType: 'application/x-www-form-urlencoded',
            data: {idtoken: id_token, csrfToken: csrf},
        }).done(function (result) {

            var json_data = jQuery.parseJSON(JSON.stringify(result));

            if (json_data.redirect) {
                window.location.replace(json_data.redirect);
                return;
            }

            $("h3.panel-title").html(json_data.title);
            $("#google_sign_in_button").remove();

            var counter = 3;
            var str = json_data.counter;
            var result_counter_format = "<div class='alert alert-success' role='alert'>{0}</div>";
            $('#result_counter').html(result_counter_format.format(str.format(counter--)));
            var interval = setInterval(function () {
                $('#result_counter').html(result_counter_format.format(str.format(counter)));
                // Display 'counter' wherever you want to display it.
                if (counter <= 0) {
                    // Display a login box
                    clearInterval(interval);
                    //console.log("finished interval");
                    window.location.replace("/");
                }
                counter--;
            }, 1000);

        }).fail(function (err) {
            var json_data = jQuery.parseJSON(JSON.stringify(err));
            var error_message = json_data.responseJSON.error;
            console.log(JSON.stringify(err));
            if (error_message) {
                show_error_noti(error_message);
            } else {
                show_error_noti("sign in failed");
            }
        });*/

    });

});