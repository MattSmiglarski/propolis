package http.http2;

import java.util.concurrent.LinkedBlockingDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Application extends FrameReceiver {

    void connectionError(Frames.Error error);

    void streamError(Frames.Error error);

    Frames.Frame nextFrame() throws InterruptedException;

    void sendFrame(Frames.Frame frame);

    @FunctionalInterface
    public static interface FrameHandler<F extends Frames.Frame> {
        void onFrame(F frame);
    }

    public static class Adapter implements Application {

        protected Logger log = LoggerFactory.getLogger(Application.class.getName());
        protected volatile LinkedBlockingDeque<Frames.Frame> sendFrames = new LinkedBlockingDeque<>();

        public Frames.Frame nextFrame() throws InterruptedException {
            return sendFrames.take();
        }

        @Override
        public void sendFrame(Frames.Frame frame) {
            sendFrames.add(frame);
        }

        public void connectionError(Frames.Error error) {
            sendFrames.clear();
            Frames.GoAwayFrame goAwayFrame = new Frames.GoAwayFrame();
            goAwayFrame.streamId = 0; // last successful stream id.
            goAwayFrame.error = error;

            sendFrames.add(goAwayFrame);
            // close connection
        }

        public void streamError(Frames.Error error) {
            sendFrames.clear();
            Frames.ResetFrame resetFrame = new Frames.ResetFrame();
            resetFrame.error = error;
            sendFrames.add(resetFrame);
            // close stream
        }

        @Override
        public void onFrame(Frames.DataFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onFrame(Frames.HeadersFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onFrame(Frames.PriorityFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onFrame(Frames.ResetFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onFrame(Frames.SettingsFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onFrame(Frames.PushPromiseFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onFrame(Frames.PingFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onFrame(Frames.GoAwayFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onFrame(Frames.WindowUpdateFrame frame) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onFrame(Frames.ContinuationFrame frame) {
            throw new UnsupportedOperationException();
        }
    }
}
