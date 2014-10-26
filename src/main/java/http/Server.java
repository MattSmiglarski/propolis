package http;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server {

    private static Logger log = Logger.getLogger(Server.class.getName());

    @FunctionalInterface
    public static interface TcpServable {
        void handleConnection(Socket client);
    }

    public static class Daemon {

        TcpServer server;
        Thread thread;

        public Daemon(TcpServer server) {
            this.server = server;
            this.thread = new Thread(server);
        }

        public void start() {
            log.info("Starting server");
            thread.start(); // BUG: Don't allow starting twice.

            do { // Wait until startup.
                try {
                    Thread.sleep(20); // Pause time.
                } catch (InterruptedException e) {
                    log.severe("Thread interrupted!");
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

        int port;
        TcpServable callback;
        ServerSocket socket;
        ExecutorService clientConnectionExecutor = Executors.newFixedThreadPool(10);

        static enum Lifecycle {
            STARTING, STARTED, STOPPING, STOPPED
        }
        volatile Lifecycle lifecycle;

        public TcpServer(int port, TcpServable tcpServable) {
            this.port = port;
            this.callback = tcpServable;
        }

        public void run() {
            lifecycle = Lifecycle.STARTING;
            try {
                socket = new ServerSocket(port);
                lifecycle = Lifecycle.STARTED;
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
