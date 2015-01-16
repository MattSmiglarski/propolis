package test.server.applications;

import propolis.server.Server;
import propolis.server.http2.Http2;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.applications.PushApplication;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class PushTests {

    private static final Logger log = LoggerFactory.getLogger(PushTests.class);

    @Test(timeout = 10000)
    public void pushShouldWork() throws IOException, URISyntaxException {
        Server.TcpServer server = new Server.TcpServer(
                0, client -> {
            PushApplication application = new PushApplication();
            Http2.handleHttp2Connection(client, application);
        });
        Server.Daemon daemon = new Server.Daemon(server);
        daemon.start();

        URI uri = new URI(String.format("h2c-14://localhost:%d/", server.port));
//        Client.requestHttp11(uri, "GET", null, null, socket -> {
//            InputStream is = socket.getInputStream();
//            OutputStream os = socket.getOutputStream();
//
//            Messages.readResponse(is);
//
//            log.info("Writing preface");
//            os.write(Http2.PREFACE);
//            new Frames.SettingsFrame().write(os);
//
//            Frames.SettingsFrame settingsFrame = (Frames.SettingsFrame) Frames.Frame.readSync(is);
//            log.info("Received: " + settingsFrame);
//
//            Frames.PingFrame pong = (Frames.PingFrame) Frames.Frame.readSync(is);
//            log.info("Received: " + pong);
//            return null;
//        });

        daemon.stop();
    }
}
