$(document).ready(function () {
    function convertType(value) {
        switch (value.replace(/ /g, '')) {
            case '제목':
                return 0;
            case '내용':
                return 1;
            case '제목+내용':
                return 2;
            case '작성자':
                return 3;
            case '댓글':
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