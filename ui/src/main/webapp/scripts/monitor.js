function requestTemplate(url) {
    var request = new XMLHttpRequest();
    request.open("GET", url);
    request.send();
    request.addEventListener('readystatechange', function(evt) {
        switch (request.readyState) {
            case 0: case 1: case 2: case 3: break;
            case 4: {
                document.getElementById('debug').innerHTML = request.responseText;
                break;
            }
            default: {
                log.warn("Unhandled ready state", evt);
            }
        }
    });
}

Client.prototype.start = function() {
    requestTemplate("api/start");
};

Client.prototype.stop = function() {
    requestTemplate("api/stop");
};

Client.prototype.ping = function() {
    requestTemplate("api/ping");
};

Client.prototype.test = function() {
    requestTemplate("api/test");
};

function Client() {
    this.connection = new XMLHttpRequest("/api/client");
}

var client = new Client();