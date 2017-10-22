/**
 * Created by DongHee Kim on 2017-09-15 015.
 */

$(document).ready(function () {
    var r = jsRoutes.controllers.TestController.test_js_routes()
    $.ajax({
        url: r.url,
        method: r.method,
        beforeSend:(function () {
            console.log("method: " + r.method);
        })
    }).done(function (callback) {
        console.log(callback + ": done");
        //document.write(callback);
        //$(document.body).load(callback);
    }).fail(function () {
        console.log("fail");
    }).always(function () {
        console.log("성공 실패 상관없이 호출");
    });
});