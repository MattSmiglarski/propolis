# Propolis
Propolis is the development name for a new HTTP 2 Server.

## Building
```Shell
./gradlew build
```

## Running
```Shell
java -cp ./build/libs/propolis-1.0.jar http.Server
# Press Control-c to exit.
```

## Usage
Start the server as above and view http://localhost:8000 in your browser.

Or of course you could use Groovy.

```Groovy
server = new http.Server.Daemon(
    new http.Server.TcpServer(8004, {
    http.Http11.handleHttp11Connection(it) }))
server.start()
http.Client.quickGetResponseBody("http://localhost:8004")
```