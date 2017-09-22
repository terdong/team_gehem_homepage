$(document).ready(function () {
    var msg = $('#noti_message');
    if (msg.length > 0) {
        $.notify({
            // options
            message: msg.data("message")
        }, {
            type: 'info',
            placement: {
                from: "top",
                align: "center"
            }
        });
        msg.remove();
    }
});