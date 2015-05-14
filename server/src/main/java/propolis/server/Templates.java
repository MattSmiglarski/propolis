package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static propolis.server.HttpComponents.Request;
import static propolis.server.Utils.closeQuietly;

public class Templates {

    private static Logger log = LoggerFactory.getLogger(Templates.class.getName());

    public static void handlerTemplate(Socket client, Handlers.Http11Handler handler) {
        try {
            HttpIOStream http = new HttpIOStream(client);

            Request request = http.readHttpRequest();
            HttpComponents.Response response;

            Handlers.ResponseBodyCallback responseBodyCallback;
            if (!validateRequest(request)) {
                response = new HttpComponents.Response(400, "Bad request"); // TODO: Configure default response mesasges.
                responseBodyCallback = null;
            } else {
                response = HttpComponents.Response.ok();
                responseBodyCallback = handler.handle(request, response, client.getInputStream());
            }

            http.writeHttpResponse(response);
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

    public static boolean validateRequest(Request header) {
        List<String> validMethods = Arrays.asList("HEAD", "GET", "POST");
        return validMethods.contains(header.method);
    }
}
