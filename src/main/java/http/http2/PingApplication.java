package http.http2;

import java.nio.ByteBuffer;

public class PingApplication extends Application.Adapter {

    public PingApplication() {
        Frames.PingFrame ping = new Frames.PingFrame();
        ping.streamId = 0x0;
        ping.ack = false;
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putInt(0xDEADBEEF);
        byteBuffer.putInt(0xDEADBEEF);
        ping.data = byteBuffer.array();
        sendFrames.add(ping);
    }

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
}
