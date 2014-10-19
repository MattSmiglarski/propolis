package http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static http.Messages.RequestToRead;
import static http.Messages.ResponseToWrite;

public class Handlers {

    public static final String TEST_HANDLER_MESSAGE = "This server is correctly installed\n";
    private static final Path STATIC_ROOT = Paths.get("/home/zz/html/");

    @FunctionalInterface
    static interface ResponseBodyCallback {
        void handleResponseBody(OutputStream outputStream) throws IOException;
    }

    public static ResponseBodyCallback rootHandler(RequestToRead header, ResponseToWrite response, InputStream inputStream) {
        String[] pathComponents = header.target.split("/");
        if ("/".equals(header.target)) {
            return testHandler(header, response, inputStream);
        } else if ("static".equals(pathComponents[0])) {
            return staticHandler(header, response, inputStream);
        } else {
            return notFoundHandler(header, response, inputStream);
        }
    }

    private static ResponseBodyCallback notFoundHandler(RequestToRead header, ResponseToWrite response, InputStream inputStream) {

        response.status = 404;

        return outputStream -> payload(String.format("The resource %s could not be found.", header.target), outputStream);
    }

    private static ResponseBodyCallback testHandler(RequestToRead header, ResponseToWrite response, InputStream inputStream) {

        response.status = 200;
        response.headers.put("Server", "Blah-de-blah server.");

        return outputStream -> payload(TEST_HANDLER_MESSAGE, outputStream);
    }

    private static ResponseBodyCallback staticHandler(RequestToRead header, ResponseToWrite response, InputStream inputStream) {

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
