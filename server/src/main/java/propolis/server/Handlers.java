package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.server.http2.Http2;
import propolis.shared.Application;
import propolis.shared.Frames;
import propolis.shared.Messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static propolis.shared.Messages.Request;
import static propolis.shared.Utils.closeQuietly;

public class Handlers {

    public static final String TEST_HANDLER_MESSAGE = "This server is correctly installed\n";
    private static final Path STATIC_ROOT = Paths.get("/home/zz/html/");
    private static final Logger log = LoggerFactory.getLogger(Handlers.class);

    @FunctionalInterface
    public static interface ResponseBodyCallback {
        void handleResponseBody(OutputStream outputStream) throws IOException;
    }

    public static ResponseBodyCallback rootHandler(Request header, Messages.Response response, InputStream inputStream) {
        String[] pathComponents = header.target.split("/");
        if ("/".equals(header.target)) {
            return testHandler(header, response, inputStream);
        } else if ("static".equals(pathComponents[0])) {
            return staticHandler(header, response, inputStream);
        } else {
            return notFoundHandler(header, response, inputStream);
        }
    }

    private static ResponseBodyCallback notFoundHandler(Request header, Messages.Response response, InputStream inputStream) {
        response.status = 404;
        return outputStream -> payload(String.format("The resource %s could not be found.", header.target), outputStream);
    }

    private static ResponseBodyCallback testHandler(Request header, Messages.Response response, InputStream inputStream) {

        response.status = 200;
        response.headers.put("Server", "Blah-de-blah server.");

        return outputStream -> payload(TEST_HANDLER_MESSAGE, outputStream);
    }

    private static ResponseBodyCallback staticHandler(Request header, Messages.Response response, InputStream inputStream) {

        String target = header.target.substring(header.target.indexOf('/', 1));
        Path path = STATIC_ROOT.resolve(target); // BUG? Can directory traversal happen here?
        if (Files.exists(path)) {
            response.status = 200;
            return outputStream -> payload(path, outputStream);
        } else {
            response.status = 404;
            return outputStream -> payload(String.format("The resource at %s was not found.", target), outputStream);
        }
    }

    static void payload(Path path, OutputStream outputStream) throws IOException {
        InputStream fileInputStream = Files.newInputStream(path, StandardOpenOption.READ);
        byte[] buffer = new byte[256];
        while (fileInputStream.available() > 0) {
            int bytesRead = fileInputStream.read(buffer);
            outputStream.write(buffer, 0, bytesRead);
        }
    }

    static void payload(String message, OutputStream outputStream) throws IOException {
        outputStream.write(message.getBytes());
    }

}
