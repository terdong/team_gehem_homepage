/**
 * Created by terdo on 2017-05-26 026.
 */
$(document).ready(function () {
    $('form').submit(function(){
        var input = $(this).find('[name=content]');
        var text_area = $(this).find('textarea');
        var replaceed_content = text_area.val().replace(/\n/g, '<br/>');
        input.val(replaceed_content);
    });
});