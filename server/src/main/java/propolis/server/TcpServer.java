package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Daemon wrapper around ServerSocket.
 */
public class TcpServer implements Runnable {

    private static final int MAX_CONNECTIONS = 5;
    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);

    // Status flags & co-ordination mutex.
    private final Object mutex = new Object();
    private volatile boolean started;
    private volatile boolean terminating;
    private volatile boolean terminated;

    private ServerSocket serverSocket;
    private ExecutorService server;
    private ExecutorService connections;
    private TcpServable connectionHandler;

    public TcpServer(ServerSocket serverSocket, TcpServable connectionHandler) throws IOException {
        this.serverSocket = serverSocket;
        this.connectionHandler = connectionHandler;
        this.server = Executors.newSingleThreadExecutor();
        this.connections = Executors.newFixedThreadPool(MAX_CONNECTIONS);

        server.submit(this);
    }

    public void stop() {
        log.info("Stopping server " + this);
        terminating = true;
        server.shutdown();
        connections.shutdown();
        try {
            connections.awaitTermination(3, TimeUnit.SECONDS);
            server.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Graceful shutdown prevented! Attempting forced shutdown. " + e.getMessage());
            connections.shutdownNow();
            server.shutdownNow();
        }

        synchronized (mutex) {
            terminated = true;
            mutex.notify();
        }
    }

    public int waitForPort() throws InterruptedException {
        waitForStart();
        return serverSocket.getLocalPort();
    }

    public void waitForStart() throws InterruptedException {
        synchronized (mutex) {
            if (!started) {
                mutex.wait(3000);
            }
        }
    }

    public void waitForStop() throws InterruptedException {
        synchronized (mutex) {
            if (!terminated) {
                mutex.wait(3000);
            }
        }
    }

    public String getHttpUrl() {
        return String.format("http://%s:%s", serverSocket.getLocalSocketAddress(), serverSocket.getLocalPort());
    }

    public ServerSocket getRawSocket() {
        return serverSocket;
    }

    @FunctionalInterface
    public interface TcpServable {
        void handleConnection(Socket client) throws IOException;
    }

    class Terminator implements Runnable {

        @Override
        public void run() {
            stop();
            try {
                waitForStop();
            } catch (InterruptedException e) {
                log.error("Failure to shutdown gracefully! " + e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Terminator()));
        while (!terminating) {
            try {
                synchronized (mutex) {
                    started = true;
                    mutex.notify();
                }

                final Socket clientConnection = serverSocket.accept();
                connections.submit(() -> {
                    try {
                        connectionHandler.handleConnection(clientConnection);
                    } catch (IOException e) {
                        log.error("Connection error!", e);
                    } finally {
                        try {
                            clientConnection.close();
                        } catch (IOException e) {
                            log.error("Failure to close socket!" + clientConnection);
                        }
                    }
                });

            } catch (IOException e) {
                throw new RuntimeException("Unhandled failure!");
            }
        }
    }

    public static TcpServer newTcpServer(int port, TcpServable tcpServable) throws IOException {
        return new TcpServer(new ServerSocket(port), tcpServable);
    }

    public static TcpServer newTcpServer(int port) throws IOException {
        return newTcpServer(port, clientSocket -> {
            log.info(String.format("Recieved connection from %s", clientSocket));
        });
    }

    @Override
    public String toString() {
        String status;
        if (terminated) {
            status = "terminated";
        } else if (terminating) {
            status = "terminating";
        } else if (started) {
            status = "started";
        } else {
            status = "starting";
        }
        return String.format("Server %s on %s", status, serverSocket.getLocalSocketAddress());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        TcpServer tcpServer = TcpServer.newTcpServer(8081);
        tcpServer.waitForStart();
        log.info("Press Ctrl-C to exit.");
    }
}
