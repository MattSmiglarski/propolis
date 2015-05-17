package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Session {

    private static final Logger log = LoggerFactory.getLogger(Session.class);

    private HttpIOStream httpIOStream;
    private Map<Integer, Stream> streams = new HashMap<>();
    private LinkedBlockingQueue<Frames.HttpFrame> sendFrames = new LinkedBlockingQueue<>();
    private Settings settings = new Settings();

    public static Session createSession(Socket client) {
        HttpIOStream httpIOStream = new HttpIOStream(client);
        try {
            httpIOStream.readConnectionPreface();
            httpIOStream.writeConnectionPreface();
        } catch (IOException e) {
            throw new RuntimeException("Unhandled failure while setting up HTTP2 strean!", e);
        }
        return new Session(httpIOStream);
    }

    private void writeHandler() {
        while (true) { // TODO: Terminate condition.
            try {
                httpIOStream.writeFrame(sendFrames.take());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Unhandled failure!", e);
            }
        }
    }

    private void readHandler() {
        try {
            Frames.HttpFrame httpFrame = httpIOStream.readFrame();
            onFrame(httpFrame);
        } catch (IOException e) {
            log.error("Failure!", e);
            // TODO: Set an appropriate stream error.
        }
    }

    public Session(HttpIOStream httpIOStream) {
        this.httpIOStream = httpIOStream;
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(this::readHandler);
        service.submit(this::writeHandler);
    }

    private void sendFrame(Frames.HttpFrame frame) {
        try {
            sendFrames.put(frame);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unhandled failure!", e); // TODO: Handle.
        }
    }

    /**
     * HttpFrames are decoded from a Socket's InputStream and then sent here.
     *
     * @param frame - Decoded HttpFrame.
     */
    public void onFrame(Frames.HttpFrame frame) {

        Stream stream;
        if (streams.containsKey(frame.streamId)) {
            stream = streams.get(frame.streamId);
        } else {
            // TODO: validate stream id
            stream = new Stream(this::sendFrame);
            streams.put(frame.streamId, stream);
        }

        switch (frame.type) {
            case 0x0: stream.getState().onReceiveDataFrame(frame); break;
            case 0x1: stream.getState().onReceiveHeaderFrame(frame); break;
            case 0x2: stream.getState().onReceivePriorityFrame(frame); break;
            case 0x3: stream.getState().onReceiveResetFrame(frame); break;
            case 0x4: stream.getState().onReceiveSettingsFrame(frame); break;
            case 0x5: stream.getState().onReceivePushPromiseFrame(frame); break;
            case 0x6: stream.getState().onReceivePingFrame(frame); break;
            case 0x7: stream.getState().onReceiveGoAwayFrame(frame); break;
            case 0x8: stream.getState().onReceiveWindowUpdateFrame(frame); break;
            case 0x9: stream.getState().onReceiveContinuationFrame(frame); break;
            default: {
                // TODO: Handle unknown frame type.
            }
        }
    }
}
