/**
 * Created by terdo on 2017-05-16 016.
 */
$(document).ready(function () {
    var checkUnload = true;
/*    $(window).on("beforeunload", function(){
        if(checkUnload) return "이 페이지를 벗어나면 작성된 내용은 저장되지 않습니다.";
    });*/

    $('button[type="button"]').click(function () {
        var form = $('form[id=postForm]');
        if (!form[0].checkValidity()) {
            form.find(':submit').click();
            return ;
        }

        tinymce.activeEditor.uploadImages(function(success) {
            console.log("tinymce.activeEditor.uploadImages")
            $('button[type="button"]').unbind('click');
            checkUnload = false;
            form.submit();
        });
    });
});