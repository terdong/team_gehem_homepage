$(document).ready(function () {
    function convertType(value) {
        switch (value.replace(/ /g, '')) {
            case '제목':
            case 'subject':
                return 0;
            case '내용':
            case 'content':
                return 1;
            case '제목+내용':
            case 'subject + content':
                return 2;
            case '작성자':
            case 'author':
                return 3;
            case '댓글':
            case 'comment':
                return 4;
            default:
                return 0;
        }
    }

    $("#dropdown a").click(function () {
        $("#AdvancedSearch").html($(this).text() + ' <span class="caret"/>');
    });

    $("#search_button").click(function () {
        var converted_type = convertType($("#AdvancedSearch").text());
        var word = $("#search").val();
        if (word) {
            var request = $(this).attr("href") + "/page/" + $(this).attr("val") +"/type/" + converted_type + "/word/" + word;
            $(this).attr("href", request);
        }
        //return false;
    });

    $("input[id=search]").keydown(function (key) {
        if (key.keyCode == 13) {
            $("#search_button")[0].click();
        }
    });
});