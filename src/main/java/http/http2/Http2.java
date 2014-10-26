package http.http2;

import http.Client;
import http.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import static http.Utils.closeQuietly;

public class Http2 {

    private final static Logger log = Logger.getLogger(Http2.class.getName());
    public static final byte[] PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes();

    public static void requestHttp2(String url) throws IOException, URISyntaxException {
        URI uri = new URI(url);
        Client.requestHttp11(uri, "GET", null, null, socket -> {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            Messages.Response response = Messages.readResponse(is);
            log.info("Received response " + response);
            os.write(PREFACE);
            Frames.SettingsFrame settings = new Frames.SettingsFrame();

            PingApplication application = new PingApplication();
            application.sendFrame(settings);
            Frames.Frame.read(is, application);
            // read settings?
            application.sendFrame(new Frames.PingFrame());
            Frames.Frame.read(is, application);
            socket.close();
            return (Void) null;
        });
    }

    public static void handleHttp2Connection(Socket client, Application application) {

        log.fine("Handling connection from " + client + ": " + client.isClosed());

        try {
            InputStream is = client.getInputStream();
            Messages.Request request = Messages.readRequest(client.getInputStream());
            log.info("Received request " + request);

            Messages.Response response = new Messages.Response();
            OutputStream os = client.getOutputStream();
            Messages.writeResponse(client.getOutputStream(), response);

            os.write(PREFACE);

            Frames.SettingsFrame settingsFrame = new Frames.SettingsFrame();
            settingsFrame.write(os);

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
