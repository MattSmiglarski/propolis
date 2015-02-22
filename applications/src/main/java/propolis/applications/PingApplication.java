package propolis.applications;

import org.slf4j.LoggerFactory;
import propolis.server.Server;
import propolis.server.http2.Http2;
import propolis.shared.Application;
import propolis.shared.Frames;

import java.net.Socket;

public class PingApplication extends Application.Adapter {

    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(PingApplication.class);

    public void onFrame(Frames.PingFrame ping) {
        if (ping.streamId != 0) {
            connectionError(Frames.Error.PROTOCOL_ERROR);
        } else if (ping.data.length != 8) {
            streamError(Frames.Error.FRAME_SIZE_ERROR);
        } else if (ping.ack) {
            log.info("Received ping acknowledgement: " + ping);
        } else {
            Frames.PingFrame pong = new Frames.PingFrame();
            pong.streamId = 0x0;
            pong.data = ping.data;
            pong.ack = true;
            sendFrames.add(pong);
        }
    }

    public void onFrame(Frames.ResetFrame resetFrame) {
        if (resetFrame.streamId == 0) {
            connectionError(Frames.Error.PROTOCOL_ERROR);
        }
    }

    public static void main(String[] args) {
        Server.TcpServer server = new Server.TcpServer(8001, client -> Http2.handleHttp2Connection(client, new PingApplication()));
        Server.Daemon daemon = new Server.Daemon(server);
        Runtime.getRuntime().addShutdownHook(new Thread(daemon::stop));
        daemon.start();
        LOG.info("Press Control-C to exit");
    }
}
