package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.shared.Utils;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Server {

    private static Logger log = LoggerFactory.getLogger(Server.class.getName());

    @FunctionalInterface
    public static interface TcpServable {
        void handleConnection(Socket client);
    }

    public static class Daemon {

        private TcpServer server;

        public Daemon(TcpServer server) {
            this.server = server;
        }

        public void start() {

            if (TcpServer.Lifecycle.STOPPING == server.lifecycle) {
                do {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        log.warn("Thread interrupted! " + e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                } while (server.lifecycle == TcpServer.Lifecycle.STOPPING);
            }

            if (server.lifecycle != null && server.lifecycle.compareTo(TcpServer.Lifecycle.STOPPING) < 0) {
                throw new RuntimeException("Cannot start the same daemon twice.");
            }

            log.info("Starting server");
            Thread thread = new Thread(null, server, "Server");
            thread.start();

            do { // Wait until startup.
                try {
                    Thread.sleep(20); // Pause time.
                } catch (InterruptedException e) {
                    log.error("Thread interrupted!");
                    Thread.currentThread().interrupt();
                }
            } while (server.lifecycle.compareTo(TcpServer.Lifecycle.STARTED) < 0);
        }

        public void stop() {
            log.info("Stopping server");
            server.lifecycle = TcpServer.Lifecycle.STOPPING; // BUG? What if the lifecycle is already STOPPED?
        }
    }

    public static class TcpServer implements Runnable {

        public int port;
        TcpServable callback;
        ServerSocket socket;
        ExecutorService clientConnectionExecutor = Executors.newFixedThreadPool(10);

        static enum Lifecycle {
            STARTING, STARTED, STOPPING, STOPPED
        }
        volatile Lifecycle lifecycle;

        public TcpServer(TcpServable tcpServable) {
            this(0, tcpServable);
        }

        public TcpServer(int port, TcpServable tcpServable) {
            this.port = port;
            this.callback = tcpServable;
        }

        public void run() {
            lifecycle = Lifecycle.STARTING;
            try {
                socket = new ServerSocket(port);
                lifecycle = Lifecycle.STARTED;
                this.port = socket.getLocalPort();
                log.info("Server started on port " + port);
                while (lifecycle.compareTo(Lifecycle.STOPPING) < 0) {
                    // INEFFICIENT: Accept on multiple threads. Is there an alternative mechanism?
                    Socket nextClient = socket.accept();
                    clientConnectionExecutor.execute(
                            () -> callback.handleConnection(nextClient)
                    );
                }
            } catch (BindException e) {
                throw new RuntimeException(e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                Utils.closeQuietly(socket);
                lifecycle = Lifecycle.STOPPED;
                log.info("Server stopped");
            }
        }
    }

    public static void main(String[] args) {
        Daemon daemon = new Daemon(new TcpServer(8000, Http11::handleHttp11Connection));
        Runtime.getRuntime().addShutdownHook(new Thread(daemon::stop));
        daemon.start();
        log.info("Press Control-C to exit");
    }
}
