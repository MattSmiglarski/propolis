package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static propolis.server.HttpComponents.Request;

public class Handlers {

    public static final String TEST_HANDLER_MESSAGE = "This server is correctly installed\n";
    private static final Path STATIC_ROOT = Paths.get("/home/zz/html/");
    private static final Logger log = LoggerFactory.getLogger(Handlers.class);

    @FunctionalInterface
    public interface ResponseBodyCallback {
        void handleResponseBody(OutputStream outputStream) throws IOException;
    }

    public static ResponseBodyCallback rootHandler(Request header, HttpComponents.Response response, InputStream inputStream) {
        log.info("Received request " + header);
        String[] pathComponents = header.target.split("/");
        if ("/".equals(header.target)) {
            return testHandler(header, response, inputStream);
        } else {
            return notFoundHandler(header, response, inputStream);
        }
    }

    private static ResponseBodyCallback notFoundHandler(Request header, HttpComponents.Response response, InputStream inputStream) {
        response.status = 404;
        return outputStream -> payload(String.format("The resource %s could not be found.", header.target), outputStream);
    }

    private static ResponseBodyCallback testHandler(Request header, HttpComponents.Response response, InputStream inputStream) {

        response.status = 200;
        response.headers.put("Server", "Blah-de-blah server.");

        return outputStream -> payload(TEST_HANDLER_MESSAGE, outputStream);
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

    @FunctionalInterface
    public interface Http11Handler {

        ResponseBodyCallback handle(
                Request header,
                HttpComponents.Response response,
                InputStream inputStream);
    }
}
