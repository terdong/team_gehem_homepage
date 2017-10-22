$(document).ready(function () {
    var r = jsRoutes.controllers.AccountController.signinOpenId();
    var google_client_id = $("input[name='google_client_id']").val();
    //console.log("google_client_id = " + google_client_id);
    if (google_client_id) {
        gapi.load('auth2', function () {
            // Retrieve the singleton for the GoogleAuth library and set up the client.
            auth2 = gapi.auth2.init({
                client_id: google_client_id,
                // cookiepolicy: 'single_host_origin',
                // Request scopes in addition to 'profile' and 'email'
                //scope: 'additional_scope'
            });
            attachSignin(document.getElementById('google_sign_in_button'));
        });
    }

    function attachSignin(element) {
        auth2.attachClickHandler(element, {},
            function (googleUser) {
/*                var profile = googleUser.getBasicProfile();
                console.log("ID: " + profile.getId()); // Don't send this directly to your server!
                console.log('Full Name: ' + profile.getName());
                console.log('Given Name: ' + profile.getGivenName());
                console.log('Family Name: ' + profile.getFamilyName());
                console.log("Image URL: " + profile.getImageUrl());
                console.log("Email: " + profile.getEmail());*/

                // The ID token you need to pass to your backend:
                var id_token = googleUser.getAuthResponse().id_token;
                //console.log("ID Token: " + id_token);
                var csrf = $("input[name='csrfToken']").val();
                //console.log("CSRF Token: " + csrf);

                $.ajax({
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
                });
            }, function (err) {
                var error_message = "sign in failed. " + JSON.stringify(err);
                console.log(error_message);
                show_error_noti(error_message);
            });
    }
});
