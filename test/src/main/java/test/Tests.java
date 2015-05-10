package test;

import propolis.server.FrameFactory;
import propolis.server.HttpComponents;
import propolis.server.HttpIOStream;
import propolis.server.Server;
import propolis.server.Frames;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public class Tests {

    private FrameFactory frameFactory;

    public void http2Ping2() throws IOException {
        Object mutex = new Object();
        AtomicReference<Socket> serverClientSocketReference = new AtomicReference<>();
        Server.TcpServer tcpServer = new Server.TcpServer(serverClientSocket -> {
            serverClientSocketReference.set(serverClientSocket);
            synchronized (mutex) {
                mutex.notify();
            }
        });

        Socket clientSocket = new Socket("localhost", tcpServer.port);

        synchronized (mutex) {
            try {
                mutex.wait();
            } catch (InterruptedException e) {
                // whatever.
            }
        }

        HttpIOStream server = new HttpIOStream(serverClientSocketReference.get());
        HttpIOStream client = new HttpIOStream(clientSocket);

        HttpComponents.Request request = new HttpComponents.Request();

        client.writeHttpRequest(request);
        server.readHttpRequest();

        HttpComponents.Response response = new HttpComponents.Response();
        server.writeHttpResponse(response);

        client.readHttpResponse();
        client.writeConnectionPreface();
        server.readConnectionPreface();
        server.writeConnectionPreface();
        client.readConnectionPreface();

        Frames.HttpFrame clientPingFrame = frameFactory.createHttpFrame(new Frames.PingFrame("ping test"));
        client.writeFrame(clientPingFrame);

        Frames.PingFrame serverPingFrame = frameFactory.createPingFrame(server.readFrame());
        Frames.PingFrame pongFrame = new Frames.PingFrame(true, "todo");
        server.writeFrame(frameFactory.createHttpFrame(pongFrame));

        Frames.PingFrame clientPongFrame = frameFactory.createPingFrame(client.readFrame());
        client.close();
        server.close();
    }
}
