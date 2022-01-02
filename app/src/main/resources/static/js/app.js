
$(document).ready(
    () => {
        $("#shortener").submit(
            (event) => {
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
                   let expirationDate = new Date(currentTime.getTime() + minutes * 60000)
                   formData.expiration = expirationDate.toGMTString()
                }
                console.log("fData", formData)

                let params = new URLSearchParams(formData);

                $.ajax({
                    type : "POST",
                    url : "/api/link",
                    data : params.toString(),
                    success : (msg, status, request) => {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>");
                    },
                    error : (msg, status, error) => {
                        console.log("msg: " + msg)
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
    });