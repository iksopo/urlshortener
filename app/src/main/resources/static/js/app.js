// https://coderedirect.com/questions/52077/how-to-iso-8601-format-a-date-with-timezone-offset-in-javascript
function toIsoString(date) {
  var tzo = -date.getTimezoneOffset(),
      dif = tzo >= 0 ? '+' : '-',
      pad = function(num) {
          var norm = Math.floor(Math.abs(num));
          return (norm < 10 ? '0' : '') + norm;
      };

  return date.getFullYear() +
      '-' + pad(date.getMonth() + 1) +
      '-' + pad(date.getDate()) +
      'T' + pad(date.getHours()) +
      ':' + pad(date.getMinutes()) +
      ':' + pad(date.getSeconds()) +
      dif + pad(tzo / 60) +
      ':' + pad(tzo % 60);
}

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
                   formData.expiration = toIsoString(expirationDate)
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
                        console.log("msg: ", msg)
                        console.log("status: ", status)
                        console.log("error: ", error)
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR " + msg.status + " (" + msg.state() + "): " + msg.responseJSON.message + "</div>");
                    }
                });
            });
    });