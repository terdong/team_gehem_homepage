/**
 * Created by terdo on 2017-05-17 017.
 */

tinymce.init({
    selector: 'textarea',
    height: 500,
    forced_root_block : false,
    theme: "modern",
    menubar: false,
    plugins: [
        'advlist autolink lists link image imagetools charmap print preview anchor',
        'searchreplace visualblocks code fullscreen',
        'insertdatetime media table contextmenu paste code'
    ],

    toolbar: 'undo redo | insert | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image | code',
    content_css: ['//www.tinymce.com/css/codepen.min.css', '/assets/stylesheets/tinymce_body.css'],

    image_title: true,
    // enable automatic uploads of images represented by blob or data URIs
    automatic_uploads: false,
    relative_urls : false,
    // URL of our upload handler (for more details check: https://www.tinymce.com/docs/configure/file-image-upload/#images_upload_url)
    //images_upload_url: '/upload/image',
    images_upload_base_path: '/images',
    images_upload_handler: function (blobInfo, success, failure) {
        console.log("images_upload_handler")

        var xhr, formData;
        xhr = new XMLHttpRequest();
        xhr.withCredentials = false;
        xhr.open('POST', '/upload/image');
        xhr.onload = function() {
            var json;

            if (xhr.status != 200) {
                failure('HTTP Error: ' + xhr.status);
                return;
            }
            json = JSON.parse(xhr.responseText);

            if (!json || (typeof json.location != 'string' && typeof json.attachment_seq != 'Number')) {
                failure('Invalid JSON: ' + xhr.responseText);
                return;
            }
            console.log(json.location);
            console.log(json.attachment_seq);

            var form = $('form');

            $("<input></input>").attr({ type: "hidden", name:"attachments[]", value:json.attachment_seq}).appendTo(form);

            success('/images/' + json.location);
        };
        formData = new FormData();
        formData.append('file', blobInfo.blob(), blobInfo.filename());
        xhr.send(formData);
    },

    // here we add custom filepicker only to Image dialog
    file_picker_types: 'image',
    // and here's our custom image picker
    file_picker_callback: function(cb, value, meta) {
        var input = document.createElement('input');
        input.setAttribute('type', 'file');
        input.setAttribute('accept', 'image/*');

        // Note: In modern browsers input[type="file"] is functional without
        // even adding it to the DOM, but that might not be the case in some older
        // or quirky browsers like IE, so you might want to add it to the DOM
        // just in case, and visually hide it. And do not forget do remove it
        // once you do not need it anymore.

        input.onchange = function () {
            var file = this.files[0];

            var reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = function () {
                // Note: Now we need to register the blob in TinyMCEs image blob
                // registry. In the next release this part hopefully won't be
                // necessary, as we are looking to handle it internally.
                var file_name = file.name.split(".")[0];
                //var id = 'img_' + $.now();
                var blobCache = tinymce.activeEditor.editorUpload.blobCache;
                var blobInfo = blobCache.create(file_name, file, reader.result);
                blobCache.add(blobInfo);

                // call the callback and populate the Title field with the file name
                cb(blobInfo.blobUri(), {title: file_name});
            };
        };
        input.click();
    }
});
