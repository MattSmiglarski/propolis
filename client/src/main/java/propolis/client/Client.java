package propolis.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.shared.Application;
import propolis.shared.Frames;
import propolis.shared.Messages;
import propolis.shared.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Utilities for making HTTP requests.
 */
public class Client {

    private final static Logger log = LoggerFactory.getLogger(Client.class);
    public static final byte[] PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes();
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

    public static void requestHttp2(String url, Application application) throws IOException, URISyntaxException {
        URI uri = new URI(url);

        int port = uri.getPort() >= 0 ? uri.getPort() : 80;
        String path = uri.getPath().length() > 0 ? uri.getPath() : "/";

        Messages.Request requestMessage = new Messages.Request();
        requestMessage.method = "GET";
        requestMessage.target = path;

        Socket socket = null;
        try {
            socket = new Socket(uri.getHost(), port);
            Messages.writeRequest(socket.getOutputStream(), requestMessage);

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            Messages.Response response = Messages.readResponse(is);
            log.info("Received response " + response);
            os.write(PREFACE);
            Frames.SettingsFrame settings = new Frames.SettingsFrame();

            application.sendFrame(settings);
            Frames.Frame.read(is, application);
            // read settings?

            application.sendFrame(new Frames.PingFrame());
            Frames.Frame.read(is, application);
        } catch (ConnectException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Failure!", e);
        } finally {
            Utils.closeQuietly(socket);
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

            Thread.currentThread().setName("Client");
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
