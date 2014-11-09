package http.http2;

import http.Messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static http.Utils.closeQuietly;

public class Http2 {

    private final static Logger log = LoggerFactory.getLogger(Http2.class);
    public static final byte[] PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes();

    public static void handleHttp2Connection(Socket client, Application application) {

        log.debug("Handling connection from " + client + ": " + client.isClosed());

        try {
            InputStream is = client.getInputStream();
            Messages.Request request = Messages.readRequest(client.getInputStream());
            log.info("Received request " + request);

            Messages.Response response = new Messages.Response();
            OutputStream os = client.getOutputStream();
            Messages.writeResponse(client.getOutputStream(), response);

            // Write preface
            Frames.SettingsFrame settingsFrame = new Frames.SettingsFrame();
            settingsFrame.write(os);

            // Read preface
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(is));
            for (int i = 0; i < Http2.PREFACE.length; i++) {
                if (Http2.PREFACE[i] == ((byte) '\n')) {
                    String prefaceLine = lineReader.readLine();
                    if (prefaceLine == null) {
                        throw new RuntimeException("Null preface line. Connection closed?");
                    }
                    log.info("PREFACE: " + prefaceLine);
                }
            }

            Frames.Frame.read(is, new Application.Adapter() {
                public void onFrame(Frames.SettingsFrame frame) {
                    log.info("Received settings");
                }
            });

            Thread applicationFrameListeningThread = new Thread(() -> {
                while (client.isConnected()) {
                    try {
                        application.nextFrame().write(os);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.info("The thread has been interrupted.");
                        break;
                    }
                }
            });
            applicationFrameListeningThread.start();

            while (client.isConnected()) {
                if (is.available() > 0) {
                    Frames.Frame.read(is, application);
                }
            }
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(client);
        }
    }
}
