$(document).ready(function () {
    $('span[class=file_size]').each(function () {
        var value = $(this).attr("value");
        $(this).text(filesize(value));
    })
});
