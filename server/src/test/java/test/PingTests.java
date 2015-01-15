package test;

import http.Client;
import http.Messages;
import http.Server;
import http.http2.Frames;
import http.http2.Http2;
import http.http2.PingApplication;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PingTests {

    private static final Logger log = LoggerFactory.getLogger(PingTests.class);

    @Test(timeout = 10000)
    public void pingShouldWork() throws IOException, URISyntaxException {
        Server.TcpServer server = new Server.TcpServer(
                0, client -> {
            PingApplication application = new PingApplication();
            Http2.handleHttp2Connection(client, application);
        });
        Server.Daemon daemon = new Server.Daemon(server);
        daemon.start();

        URI uri = new URI(String.format("h2c-14://localhost:%d/", server.port));
        Long responseTime = Client.requestHttp11(uri, "GET", null, null, socket -> {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            Messages.readResponse(is);

            log.info("Writing preface");
            os.write(Http2.PREFACE);
            new Frames.SettingsFrame().write(os);

            Frames.SettingsFrame settingsFrame = (Frames.SettingsFrame) Frames.Frame.readSync(is);
            log.info("Received: " + settingsFrame);

            log.info("Writing ping");
            Frames.PingFrame ping = new Frames.PingFrame();
            long requestTime = System.currentTimeMillis();
            ping.write(os);

            Frames.PingFrame pong = (Frames.PingFrame) Frames.Frame.readSync(is);
            log.info("Received: " + pong);
            return System.currentTimeMillis() - requestTime;
        });

        daemon.stop();

        Assert.assertNotNull(responseTime);
        log.info("Response time: " + responseTime);
    }

    @Test(timeout = 5000)
    public void assertionsShouldWorkTogether() throws Throwable {
        final ServerSocket server = new ServerSocket(0);
        assertClientIsPingCompliant(
                () -> assertServerHandlesPing("localhost", server.getLocalPort()),
                server);
    }

    public static void assertServerHandlesPing(String host, int port) {
        URI url;
        try {
            url = new URI(String.format("h2c://%s:%d", host, port));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String path = "/";

        Messages.Request requestMessage = new Messages.Request();
        requestMessage.method = "GET";
        requestMessage.target = path;

        try (Socket socket = new Socket(url.getHost(), port)) {
            socket.setSoLinger(true, 10);
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            // Write request header
            os.write(new Messages.RequestBuilder().asBytes(requestMessage));

            // Write preface
            os.write(Http2.PREFACE);

            // Write settings
            Frames.SettingsFrame settingsFrame = new Frames.SettingsFrame();
            settingsFrame.write(os);

            // Write ping frame
            long pingSentTime = System.currentTimeMillis();
            Frames.PingFrame pingFrame = new Frames.PingFrame();
            pingFrame.write(os);

            // Read settings
            Assertions.assertSettings(is);

            // Read ping frame
            Assertions.assertPing(is);
            log.info(String.format("Ping response in %d milliseconds.", System.currentTimeMillis() - pingSentTime));

            socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Failure!", e);
        }

    }

    public void assertClientIsPingCompliant(Runnable clientRequest, ServerSocket server) {
        int port = server.getLocalPort();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {

            Callable<Socket> singleUseServerConnection = () -> {
                try {
                    return server.accept();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };

            Future<Socket> pendingConnection = executor.submit(singleUseServerConnection);
            executor.submit(clientRequest);
            Socket client = pendingConnection.get();

            InputStream is = client.getInputStream();
            OutputStream os = client.getOutputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            // Read request header.
            Messages.Request request = Messages.readRequest(is);
            log.info("Request: " + request);

            // Read request preface.
            for (int i = 0; i < Http2.PREFACE.length; i++) {
                if (Http2.PREFACE[i] == ((byte) '\n')) {
                    String prefaceLine = reader.readLine();
                    if (prefaceLine == null) {
                        throw new RuntimeException("Null preface line. Connection closed?");
                    }
                    log.info("PREFACE: " + prefaceLine);
                }
            }

            Frames.Frame settingsFrame = Frames.Frame.readSync(is);

            // Write settings frame.
            new Frames.SettingsFrame().write(os);

            // Read ping frame.
            Frames.PingFrame pingFrame = (Frames.PingFrame) Frames.Frame.readSync(is);

            // Respond ping frame.
            pingFrame.ack = true;
            pingFrame.write(os);

            server.close();
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
