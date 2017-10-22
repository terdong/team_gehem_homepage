$(document).ready(function () {
    var connecting_route = jsRoutes.controllers.HomeController.chatFeed();

    var sending_route = jsRoutes.controllers.HomeController.postMessage();

    var log = $(".log");

   $("input[name='message']" ).on("keyup", function(e) {
        if( e.keyCode != 13)
            return;
        var value = this.value.replace(/\s/g,"");
        if(value.length <= 0){return;}
        var m = { message: this.value};
        this.value = "";
        $.ajax({
            url: sending_route.url,
            data: JSON.stringify(m),
            method: "post",
            contentType: "application/json"
        }).done(function (result) {
            Cookies.set('chat', '1', { expires: 3600});
        });
    });

    var feed = new EventSource(connecting_route.url);
    feed.onopen = function(event){
        //alert("open event!");
    }
    feed.onmessage = function(e) {
        //var newElement = document.createElement("li");

        //newElement.textContent = "message: " + e.data;

        var m = JSON.parse(e.data);
        //console.log(m);
        if(log.children().length > 9999){
            log.empty();
        }
        log.append("<div class='message'>" + m.message + "</div>");
        log.scrollTop(log.prop('scrollHeight'));
    }
    feed.onerror = function(event){
        console.log("onerror = " + event);
    }
/*    feed.addEventListener("message", function (msg) {
        var m = JSON.parse(msg.data);
        $(".log" ).append("<div class='message'>" + m.user + ": " + m.message + "</div>");
    }, false);*/

});