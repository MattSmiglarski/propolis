package http;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
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
    static interface ResponseHandler<T> {
        T handleResponse(Socket socket);
    }

    /**
     * A trivial HTTP request.
     *
     * @param url The URL to be retrieved.
     * @return The response body.
     */
    public static String quickGetResponseBody(String url) {
        try {
            return requestHttp11(new URL(url), "GET", null, null, socket -> {
                InputStream inputStream = null;
                try {
                    inputStream = socket.getInputStream();
                    Messages.readResponse(inputStream); // Throw away the headers and that.
                    new Messages.Response(inputStream);
                    return Utils.inputStream2String(inputStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T requestHttp11(URL url, String method, Map<String, String> headers, InputStream requestBody, ResponseHandler<T> responseHandler) {

        int port = url.getPort() >= 0 ? url.getPort() : 80;
        String path = url.getPath().length() > 0 ? url.getPath() : "/";

        Messages.RequestToWrite requestMessage = new Messages.RequestToWrite();
        requestMessage.method = method;
        requestMessage.headers = headers;
        requestMessage.target = path;

        byte[] request = requestMessage.asBytes();

        Socket socket = null;
        try {
            socket = new Socket(url.getHost(), port);
            socket.getOutputStream().write(request);

            return responseHandler.handleResponse(socket);
        } catch (ConnectException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Failure!", e);
        } finally {
            Utils.closeQuietly(socket);
        }
    }

    private static String handleResponse(Socket socket) throws IOException {
        byte[] buffer = new byte[256];
        StringBuilder response = new StringBuilder();

        InputStream is = socket.getInputStream();
        while (socket.isConnected()) { // BUG? What happens when the server pauses while responding?
            int bytesRead = is.read(buffer);
            if (bytesRead < 0) {
                break; // WHY? How come the socket is still connected after EOF?
            }
            response.append(new String(buffer, 0, bytesRead));
        }

        return response.toString();
    }
}
