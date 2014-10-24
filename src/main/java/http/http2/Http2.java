package http.http2;

import http.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static http.Utils.closeQuietly;

public class Http2 {

    private final static Logger log = Logger.getLogger(Http2.class.getName());

    // "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n"
    private final static String preface = "0x505249202a20485454502f322e300d0a0d0a534d0d0a0d0a";

    static interface Client {
        void sendPreface();
        void sendSettingsFrame();
    }

    static interface Server {

        void sendPreface();
        void sendGoAwayFrame();
    }

    public static void handleHttp2Connection(Socket client) {

        log.fine("Handling connection from " + client + ": " + client.isClosed());

        try {
            InputStream is = client.getInputStream();
            Messages.Request request = Messages.readRequest(client.getInputStream());
            Messages.Response response = new Messages.Response();
            OutputStream os = client.getOutputStream();
            Messages.writeResponse(client.getOutputStream(), response);

            Map<Integer, Stream> streams = new HashMap<>();
            while (client.isConnected()) {
                if (is.available() > 0) {
                    Frames.Frame frame = Frames.readFrame(is);
                }
                Frames.Frame frame = new Frames.SettingsFrame();
                Frames.writeFrame(os, frame);
                Frames.writeFrame(os, new Frames.PushPromiseFrame());
            }
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(client);
        }
    }
}
