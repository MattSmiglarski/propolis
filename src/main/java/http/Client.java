package http;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Utilities for making HTTP requests.
 */
public class Client {

    /**
     * Interface for callbacks upon which to call when receiving a server response.
     * @param <T> The return type of the callback, which may be the result of a conversion of the response body.
     */
    @FunctionalInterface
    public static interface ResponseHandler<T> {
        T handleResponse(Socket socket) throws IOException;
    }

    /**
     * A trivial HTTP request.
     *
     * @param url The URL to be retrieved.
     * @return The response body.
     */
    public static String quickGetResponseBody(String url) {
        try {
            URI uri = new URI(url);
            return requestHttp11(uri, "GET", null, null, socket -> {
                InputStream inputStream = socket.getInputStream();
                Messages.readResponse(inputStream); // Throw away the headers and that.
                return Utils.inputStream2String(inputStream);
            });
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T requestHttp11(URI url, String method, Map<String, String> headers, InputStream requestBody, ResponseHandler<T> responseHandler) {

        int port = url.getPort() >= 0 ? url.getPort() : 80;
        String path = url.getPath().length() > 0 ? url.getPath() : "/";

        Messages.Request requestMessage = new Messages.Request();
        requestMessage.method = method;
        if (headers != null) {
            requestMessage.headers = headers;
        }
        requestMessage.target = path;

        Socket socket = null;
        try {
            socket = new Socket(url.getHost(), port);
            Messages.writeRequest(socket.getOutputStream(), requestMessage);

            return responseHandler.handleResponse(socket);
        } catch (ConnectException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Failure!", e);
        } finally {
            Utils.closeQuietly(socket);
        }
    }
}
