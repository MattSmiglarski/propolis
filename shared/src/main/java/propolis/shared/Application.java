package propolis.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public interface Application extends FrameReceiver {

    void connectionError(Frames.Error error);

    void streamError(Frames.Error error);

    Frames.Frame nextFrame();

    void sendFrame(Frames.Frame frame);

    @FunctionalInterface
    public static interface FrameHandler<F extends Frames.Frame> {
        void onFrame(F frame);
    }

    public abstract static class UntypedAdapter implements Application {

        protected volatile LinkedBlockingQueue<Frames.Frame> sendFrames = new LinkedBlockingQueue<>();

        @Override
        public void connectionError(Frames.Error error) {
            throw new RuntimeException();
        }

        @Override
        public void streamError(Frames.Error error) {
            throw new RuntimeException();
        }

        @Override
        public Frames.Frame nextFrame() {
            if (sendFrames.peek() != null) {
                try {
                    return sendFrames.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public void sendFrame(Frames.Frame frame) {
            sendFrames.add(frame);
        }

        public abstract void onAnyFrame(Frames.Frame frame);

        @Override
        public void onFrame(Frames.DataFrame frame) {
            onAnyFrame(frame);
        }

        @Override
        public void onFrame(Frames.HeadersFrame frame) {
            onAnyFrame(frame);
        }

        @Override
        public void onFrame(Frames.PriorityFrame frame) {
            onAnyFrame(frame);
        }

        @Override
        public void onFrame(Frames.ResetFrame frame) {
            onAnyFrame(frame);
        }

        @Override
        public void onFrame(Frames.SettingsFrame frame) {
            onAnyFrame(frame);
        }

        @Override
        public void onFrame(Frames.PushPromiseFrame frame) {
            onAnyFrame(frame);
        }

        @Override
        public void onFrame(Frames.PingFrame frame) {
            onAnyFrame(frame);
        }

        @Override
        public void onFrame(Frames.GoAwayFrame frame) {
            onAnyFrame(frame);
        }

        @Override
        public void onFrame(Frames.WindowUpdateFrame frame) {
            onAnyFrame(frame);
        }

        @Override
        public void onFrame(Frames.ContinuationFrame frame) {
            onAnyFrame(frame);
        }
    }

    public static class Adapter implements Application {

        protected Logger log = LoggerFactory.getLogger(Application.class.getName());
        protected volatile LinkedBlockingDeque<Frames.Frame> sendFrames = new LinkedBlockingDeque<>();

        public Frames.Frame nextFrame() {
            if (sendFrames.peek() != null) {
                try {
                    return sendFrames.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } else {
                return null;
            }
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
