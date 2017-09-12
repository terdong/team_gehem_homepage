$(document).ready(function () {
    var clipboard = new Clipboard('.qq-uuid');

});

function hash_button_click(file_name) {
    var message = file_name + "파일의 url을 클립보드(ctrl+c)에 저장했습니다.";
    $.notify({
        message: message
    },{
        // settings
        type: 'info',
        placement: {
            from: "bottom",
            align: "center"
        },
        delay: 1500
    });
}
