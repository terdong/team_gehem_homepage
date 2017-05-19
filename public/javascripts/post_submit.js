/**
 * Created by terdo on 2017-05-16 016.
 */
$(document).ready(function () {
    $('button[type="button"]').click(function () {
        var form = $('form');
        if (!form[0].checkValidity()) {
            form.find(':submit').click();
            return ;
        }

        tinymce.activeEditor.uploadImages(function(success) {
            console.log("tinymce.activeEditor.uploadImages")
            $('button[type="button"]').unbind('click');
            form.submit();
        });
    });
});