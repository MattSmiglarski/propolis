package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.shared.Messages;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static propolis.shared.Messages.Request;
import static propolis.shared.Utils.closeQuietly;

public class Http11 {

    private static Logger log = LoggerFactory.getLogger(Http11.class.getName());

    public static void handleHttp11Connection(Socket client) {

        log.debug("Handling connection from " + client + ": " + client.isClosed());

        try {
            Request request = Messages.readRequest(client.getInputStream());
            Messages.Response response = new Messages.Response();
            Messages.writeResponse(client.getOutputStream(), response);

            Handlers.ResponseBodyCallback responseBodyCallback;
            if (!validateHeader(request)) {
                response.status = 400;
                responseBodyCallback = null;
            } else {
                responseBodyCallback = Handlers.rootHandler(request, response, client.getInputStream());
            }

            log.debug("Response header has been written");

            if (responseBodyCallback != null) {
                log.debug("Writing response body.");
                responseBodyCallback.handleResponseBody(client.getOutputStream());
            }
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(client);
        }
    }

    public static boolean validateHeader(Request header) {
        List<String> validMethods = Arrays.asList("HEAD", "GET", "POST");
        return validMethods.contains(header.method);
    }
}
