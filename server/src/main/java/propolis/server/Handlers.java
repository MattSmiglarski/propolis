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

    private static final Logger log = LoggerFactory.getLogger(Handlers.class);

    @FunctionalInterface
    public interface ResponseBodyCallback {
        void handleResponseBody(OutputStream outputStream) throws IOException;
    }

    @FunctionalInterface
    public interface Http11Handler {

        ResponseBodyCallback handle(
                Request header,
                HttpComponents.Response response,
                InputStream inputStream);
    }
}
