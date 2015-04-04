package propolis.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.server.Server;
import propolis.shared.Utils;

import java.util.HashSet;
import java.util.Set;

public class Application extends javax.ws.rs.core.Application {

    private static Logger logger = LoggerFactory.getLogger(Application.class);

    @Override
    public Set<Object> getSingletons() {
        HashSet<Object> singletons = new HashSet<>();
        Server.TcpServer server = new Server.TcpServer(Utils::tcpEcho);
        Server.Daemon daemon = new Server.Daemon(server);
        daemon.start();

        singletons.add(server);
        singletons.add(daemon);
        return singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        logger.info("Getting classes...");
        HashSet<Class<?>> singletons = new HashSet<>();
        singletons.add(Api.class);
        return singletons;
    }
}
