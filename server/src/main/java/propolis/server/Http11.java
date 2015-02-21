package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.shared.Messages;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static propolis.shared.Messages.Request;
import static propolis.shared.Utils.closeQuietly;

public class Http11 {

    private static Logger log = LoggerFactory.getLogger(Http11.class.getName());

    public static void handleHttp11Connection(Socket client) {
        handlerTemplate(client, Handlers::rootHandler);
    }

    public static void handlerTemplate(Socket client, Http11Handler handler) {
        try {
            Request request = Messages.readRequest(client.getInputStream());
            Messages.Response response = new Messages.Response();
            Messages.writeResponse(client.getOutputStream(), response);

            Handlers.ResponseBodyCallback responseBodyCallback;
            if (!validateHeader(request)) {
                response.status = 400;
                responseBodyCallback = null;
            } else {
                responseBodyCallback = handler.handle(request, response, client.getInputStream());
            }

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
