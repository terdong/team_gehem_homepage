function show_error_noti(message) {
    $.notify({
        icon: "fa fa-exclamation-triangle",
        message: message
    }, {
        type: 'danger',
        placement: {
            from: "top",
            align: "center"
        },
        delay: 3000,
        mouse_over: "pause",
        template: '<div data-notify="container" class="col-xs-11 col-sm-3 alert alert-{0} text-center" role="alert">' +
        '<button type="button" aria-hidden="true" class="close" data-notify="dismiss">×</button>' +
        '<span data-notify="icon"></span> ' +
        '<span data-notify="message">{2}</span>' +
        '</div>'
    });
}