package test;

import propolis.client.Client;
import propolis.server.Server;
import propolis.server.http2.Http2;
import propolis.shared.Application;
import propolis.shared.Frames;

import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class HttpInputOutputStream {

    Server.Daemon daemon;
    Application serverApp;
    Application clientApp;
    LinkedBlockingQueue<Frames.Frame> clientReceivedFrames = new LinkedBlockingQueue<>();
    LinkedBlockingQueue<Frames.Frame> serverReceivedFrames = new LinkedBlockingQueue<>();

    public HttpInputOutputStream() {
        Server.TcpServer server = new Server.TcpServer(client -> {
            Application application = new Application.UntypedAdapter() {

                @Override
                public void onAnyFrame(Frames.Frame frame) {
                    try {
                        serverReceivedFrames.put(frame);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            Http2.handleHttp2Connection(client, application);
        });
        daemon = new Server.Daemon(server);

        clientApp = new Application.UntypedAdapter() {

            @Override
            public void onAnyFrame(Frames.Frame frame) {
                try {
                    clientReceivedFrames.put(frame);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        daemon.start();

        try {
            Client.requestHttp2("h2://localhost:" + server.port, clientApp);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException();
        }
    }

    public void start() {
        daemon.start();
    }

    public void stop() {
        daemon.stop();
    }

    public void clientSends(Frames.Frame frame) {
        clientApp.sendFrame(frame);
    }

    public void serverSends(Frames.Frame frame) {
        serverApp.sendFrame(frame);
    }

    public Frames.Frame clientReceives() {
        try {
            return clientReceivedFrames.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Frames.Frame serverReceives() {
        try {
            return serverReceivedFrames.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}