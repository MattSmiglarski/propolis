package test;

import http.Client;
import http.Messages;
import http.Server;
import http.http2.Frames;
import http.http2.Http2;
import http.http2.PingApplication;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class PingTests {

    @Test
    public void pingShouldWork() throws IOException, URISyntaxException {
        Server.Daemon daemon = new Server.Daemon(new Server.TcpServer(
                8003, client -> {
            PingApplication application = new PingApplication();
            Http2.handleHttp2Connection(client, application);
        }));
        daemon.start();

        URI uri = new URI("h2c-14://localhost:8003/");
        Long responseTime = Client.requestHttp11(uri, "GET", null, null, socket -> {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            Messages.readResponse(is);
            os.write(Http2.PREFACE);
            new Frames.SettingsFrame().write(os);
            Frames.SettingsFrame settingsFrame = (Frames.SettingsFrame) Frames.Frame.readSync(is);
            System.out.println("Received: " + settingsFrame);
            Frames.PingFrame ping = new Frames.PingFrame();
            long requestTime = System.currentTimeMillis();
            ping.write(os);
            Frames.PingFrame pong = (Frames.PingFrame) Frames.Frame.readSync(is);
            System.out.println("Received: " + pong);
            return System.currentTimeMillis() - requestTime;
        });

        daemon.stop();

        Assert.assertNotNull(responseTime);
    }
}
