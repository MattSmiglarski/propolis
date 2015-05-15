package test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.server.HttpComponents;
import propolis.server.HttpIOStream;
import propolis.server.TcpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class Http11Tests {

    private static final Logger log = LoggerFactory.getLogger(SocketTests.class);

    @Test(timeout = 3000)
    public void shouldMakeHttpRequest() throws IOException, InterruptedException {
        final Object mutex = new Object();
        AtomicReference<HttpIOStream> serverStreamReference = new AtomicReference<>();
        ServerSocket serverSocket = new ServerSocket(0);

        TcpServer server;
        HttpIOStream clientStream;
        server = new TcpServer(serverSocket, client -> {
            serverStreamReference.set(new HttpIOStream(client));
            synchronized (mutex) { mutex.notify(); try { mutex.wait(); } catch (InterruptedException e) {}}
        });

        server.waitForStart();

        synchronized (mutex) {
            Socket socket = new Socket("localhost", server.waitForPort());
            clientStream = new HttpIOStream(socket);
            mutex.wait();
        }

        HttpIOStream serverStream = serverStreamReference.get();

        HashMap<String, String> headers = new HashMap<>();
        headers.put("header1", "value1");
        headers.put("header2", "value2");
        HttpComponents.Request request = new HttpComponents.Request("GET", "/", headers);

        clientStream.writeHttpRequest(request);

        HttpComponents.Request serverRequest = serverStream.readHttpRequest();
        HttpComponents.Response serverResponse = new HttpComponents.Response(200, "OK");
        serverStream.writeHttpResponse(serverResponse);

        HttpComponents.Response clientResponse = clientStream.readHttpResponse();

        synchronized (mutex) { mutex.notify(); } // Because the connection can be closed.

        server.stop();
        server.waitForStop();

        assertNotNull(serverRequest);
        assertNotNull(clientResponse);
        assertEquals(200, clientResponse.status);
    }
}
