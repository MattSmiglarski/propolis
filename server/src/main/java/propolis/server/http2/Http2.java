package propolis.server.http2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.server.Handlers;
import propolis.server.Http11;
import propolis.server.Http11Handler;
import propolis.shared.Application;
import propolis.shared.Frames;
import propolis.shared.Messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import static propolis.shared.Utils.closeQuietly;

public class Http2 {

    private final static Logger log = LoggerFactory.getLogger(Http2.class);
    public static final byte[] PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes();

    public static void handleHttp2Connection(Socket client, Application application) {
        Http11.handlerTemplate(client, (header, response, inputStream) -> {
            Settings setting = new Settings();

            try {
                while (client.isConnected()) {
                    if (inputStream.available() > 0) {
                        application.sendFrame(Frames.Frame.readSync(inputStream));
                    }

                    Frames.Frame serverFrame = application.nextFrame();
                    if (serverFrame != null) {
                        serverFrame.write(client.getOutputStream());
                    }
                }
            } catch (IOException e) {
                log.error("Failure!", e);
            }

            return outputStream -> {
                response.status = 200;
            };
        });
    }
}
