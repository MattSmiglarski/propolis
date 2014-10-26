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
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class PingTests {

    @Test
    public void pingShouldWork() throws IOException, URISyntaxException {
        Server.Daemon daemon = new Server.Daemon(new Server.TcpServer(
                8003, client -> {
            PingApplication application = new PingApplication();
            Http2.handleHttp2Connection(client, application);
        }));
        daemon.start();

        final Map<String, Long> pingRequests = new HashMap<>();
        final Map<String, Long> pingResponses = new HashMap<>();
        URI uri = new URI("h2c-14://localhost:8003/");
        Long responseTime = Client.requestHttp11(uri, "GET", null, null, socket -> {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            Messages.Response response = Messages.readResponse(is);
            os.write(Http2.PREFACE);
            Frames.SettingsFrame settings = new Frames.SettingsFrame();

            PingApplication application = new PingApplication() {
                @Override
                public void onFrame(Frames.PingFrame frame) {
                    super.onFrame(frame);
                    ByteBuffer buffer = ByteBuffer.wrap(frame.data).asReadOnlyBuffer();
                    StringBuilder stringBuilder = new StringBuilder();
                    while (buffer.position() < buffer.limit()) {
                        stringBuilder.append(buffer.getChar());
                    }
                    String pingString = stringBuilder.toString();
                    if (pingResponses.get(pingString) != null) {
                        throw new RuntimeException("There is already a response time set for " + pingString);
                    }
                    pingResponses.put(pingString, System.currentTimeMillis());
                }

                @Override
                public void onFrame(Frames.SettingsFrame frame) {
                    System.out.println("Received settings, ignoring...");
                }
            };

            application.sendFrame(settings);
            Frames.Frame.read(is, application);
            // read settings?
            Frames.PingFrame pingFrame = new Frames.PingFrame();

            long requestTime = System.currentTimeMillis();
            pingRequests.put("", requestTime);
            application.sendFrame(pingFrame);
            Frames.Frame.read(is, application);
            socket.close();
            Map.Entry<String, Long> entry = pingRequests.entrySet().iterator().next();
            Map.Entry<String, Long> responseEntry = pingResponses.entrySet().iterator().next();

            return responseEntry.getValue() - entry.getValue();
        });

        daemon.stop();

        Assert.assertNotNull(responseTime);
    }
}
