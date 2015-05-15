package test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.server.TcpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SocketTests {

    private static final Logger log = LoggerFactory.getLogger(SocketTests.class);

    @Test(timeout=1500)
    public void readLineOverSocket() throws InterruptedException, IOException {
        final Object serverReadyMutex = new Object();
        AtomicReference<String> requestLine = new AtomicReference<>();
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        TcpServer server = new TcpServer(serverSocket, client -> {
            log.info("Received connection " + client);

            InputStreamReader isr = new InputStreamReader(client.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(isr);
            requestLine.set(bufferedReader.readLine());

            synchronized (serverReadyMutex) {
                serverReadyMutex.notify(); // because message is ready
            }
        });

        server.waitForStart();
        Socket socket = new Socket("localhost", port);
        socket.getOutputStream().write("foobar\r\n".getBytes(StandardCharsets.UTF_8));

        synchronized (serverReadyMutex) {
            serverReadyMutex.wait(); // For message to be read.
        }

        String actual = requestLine.get();
        assertNotNull("Completely failed to read the string!", actual);
        assertEquals("Expected the request line to be echoed back from the server handler!", "foobar", actual);
        server.stop();
        server.waitForStop();
    }
}
