$(document).ready(function () {

    var result_count = $('#result_counter');
    var counter = 3;
    var str = result_count.data("str");
    var result_counter_format = "<div class='alert alert-success text-center' role='alert'>{0}</div>";
    result_count.html(result_counter_format.format(str.format(counter--)));
    var interval = setInterval(function () {
        result_count.html(result_counter_format.format(str.format(counter)));
        // Display 'counter' wherever you want to display it.
        if (counter <= 0) {
            // Display a login box
            clearInterval(interval);
            //console.log("finished interval");
            window.location.replace("/");
        }
        counter--;
    }, 1000);
});