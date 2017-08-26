/**
 * Created by terdo on 2017-06-11 011.
 */

$(document).ready(function () {

    var post_seq = $('input[id="post_seq"]').val();

    $('#fine-uploader').fineUploader({
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
        session:{
            endpoint: "/files/" + post_seq
        },
        callbacks: {
            onComplete: function(id, name, response, maybeXhr) {
                //console.log(JSON.stringify(response));
                var form = $('form[id=postForm]');
                var file_info = JSON.stringify(response['file_info']);
                $("<input></input>").attr({
                    type: "hidden",
                    id: id,
                    name: "upload_files[]",
                    value: file_info
                }).appendTo(form);

                file_info = JSON.parse(file_info);

                var button = $('.qq-file-id-' + id).children('.qq-uuid');
                button.attr("data-clipboard-text", "/images/" + file_info.hash);
                button.removeClass("qq-hide");
                button.click(function(){
                    hash_button_click(file_info.file_name);
                });

            },
            onDelete: function(id) {
                $('input[name="upload_files[]"]').remove("#" + id);
            },
            onSessionRequestComplete: function (response, success, xhrOrXdr) {
                //console.log("onSessionRequestComplete");
                var uuid_spans = $('.qq-uuid');
                $.each(response,function(i, data) {
                    var button = uuid_spans.eq(i);
                    button.attr("data-clipboard-text", "/images/" + data['uuid']);
                    button.removeClass("qq-hide");
                    button.click(function(){
                        hash_button_click(data['name']);
                    });
                });
            }
        }
    });
});