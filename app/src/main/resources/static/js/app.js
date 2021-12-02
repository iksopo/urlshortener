$(document).ready(
    function() {
        $("#shortener").submit(
            function(event) {
                event.preventDefault();
                let formData = {
                    url: document.getElementById("url").value
                }
                console.log("enumber", document.getElementById("numberCheckBox").checked)
                if ( document.getElementById("numberCheckBox").checked ) {
                    formData.leftUses = parseInt(document.getElementById("leftUses").value)
                }

                if ( document.getElementById("timeCheckBox").checked ) {
                    let currentTime = new Date()
                    let minutes = parseInt(document.getElementById("expiration").value)
                    formData.expiration = new Date(currentTime.getTime() + minutes * 60000)
                }
                console.log("fData", formData)
                $.ajax({
                    type : "POST",
                    url : "/api/link",
                    //data: $(this).serialize(),
                    data : formData.stringify(),
                    success : function(msg, status, request) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>");
                    },
                    error : function() {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
    });