package http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static http.Messages.RequestToRead;
import static http.Messages.ResponseToWrite;
import static http.Utils.closeQuietly;

public class Http11 {

    private static Logger log = Logger.getLogger(Http11.class.getName());

    public static void handleHttp11Connection(Socket client) {

        log.info("Handling connection from " + client + ": " + client.isClosed());

        try {
            RequestToRead request = new RequestToRead(client.getInputStream());
            ResponseToWrite response = new ResponseToWrite();
            Handlers.ResponseBodyCallback responseBodyCallback;
            if (!validateHeader(request)) {
                response.status = 400;
                responseBodyCallback = null;
            } else {
                responseBodyCallback = Handlers.rootHandler(request, response, client.getInputStream());
            }
            OutputStream os = client.getOutputStream();
            os.write(response.asBytes());

            log.info("Response header has been written");

            if (responseBodyCallback != null) {
                log.info("Writing response body.");
                responseBodyCallback.handleResponseBody(client.getOutputStream());
            }
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(client);
        }
    }

    public static boolean validateHeader(RequestToRead header) {
        List<String> validMethods = Arrays.asList("HEAD", "GET", "POST");
        return validMethods.contains(header.method);
    }
}
