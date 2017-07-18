/**
 * Created by terdo on 2017-06-11 011.
 */
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

$(document).ready(function () {
    var clipboard = new Clipboard('.qq-uuid');

    $('#fine-uploader-gallery').fineUploader({
        template: 'qq-template-gallery',
        request: {
            endpoint: '/upload/file'
        },
        thumbnails: {
            placeholders: {
                waitingPath: '/assets/fine-uploader/placeholders/waiting-generic.png',
                notAvailablePath: '/assets/fine-uploader/placeholders/not_available-generic.png'
            }
        },
        validation: {
            allowedExtensions: ['jpeg', 'jpg', 'gif', 'png', 'mp3', 'flac', 'ogg', 'avi', 'mp4', 'zip', '7z', 'xls', 'doc', 'sh', 'txt', 'rar']
        },
        deleteFile: {
            enabled: true,
            method: "POST",
            endpoint: "/upload/file/delete"
        },
        autoUpload: true,
        callbacks: {
            onComplete: function(id, name, responseJSON, maybeXhr) {
                //console.log(JSON.stringify(responseJSON));
                var form = $('form[id=postForm]');
                $("<input></input>").attr({
                    type: "hidden",
                    id: id,
                    name: "upload_files[]",
                    value: JSON.stringify(responseJSON['file_info'])
                }).appendTo(form);

                // 추가된 파일은 임시디렉토리에 있기 때문에 url을 지정할 수 없음.
                /*                            var content_type = responseJSON['file_info']['content_type'];
                 if(content_type.split("/")[0] == "image"){
                 var uuid_span = $('.qq-uuid').eq(id);
                 uuid_span.val(name);
                 uuid_span.attr("data-clipboard-text", "/images/" + responseJSON['newUuid']);
                 uuid_span.removeClass("qq-hide");
                 }*/
            },
            onDelete: function(id) {
                $('input[name="upload_files[]"]').remove("#" + id);
            }
        }
    });
});