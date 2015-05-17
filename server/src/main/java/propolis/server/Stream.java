package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.CommandAPDU;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class Stream {

    private static Logger log = LoggerFactory.getLogger(Stream.class);

    private HttpIOStream stream;
    private Consumer<Frames.HttpFrame> frameConsumer;
    private LinkedBlockingQueue<Frames.HttpFrame> outputFrames;
    private State state = State.IDLE;
    private FrameFactory frameFactory = new FrameFactory();

    public Stream(Consumer<Frames.HttpFrame> frameConsumer) {
        this.frameConsumer = frameConsumer;
    }

    public State getState() {
        return state;
    }

    public enum State {

        /**
         * send or receive headers => open, half-closed
         * send push_promise on another stream => reserved local
         * receive push_promise on another stream => reserved remote
         * any frame other than headers or priority => PROTOCOL_ERROR
         */
        IDLE {

            @Override
            void onSendHeaderFrame(Frames.HeadersFrame frame) {
                // send or receive headers => open, half-closed
            }

            @Override
            void onReceiveHeaderFrame(Frames.HttpFrame frame) {
                // send or receive headers => open, half-closed
            }
        },

        /**
         * send headers => halfclosed_remote
         * send rst_stream => closed
         * receive rst_stream => closed
         * <p>
         * can send: headers, rst_stream or priority
         * can receive: rst_stream, priority, window_update (otherwise protocol_error)
         */
        RESERVED_LOCAL {

            @Override
            void onSendHeaderFrame(Frames.HeadersFrame frame) {
                super.onSendHeaderFrame(frame);
            }

            @Override
            void onSendResetFrame(Frames.ResetFrame frame) {
                super.onSendResetFrame(frame);
            }

            @Override
            void onSendPriorityFrame(Frames.PriorityFrame frame) {
                super.onSendPriorityFrame(frame);
            }

            @Override
            void onReceiveResetFrame(Frames.HttpFrame frame) {
                super.onReceiveResetFrame(frame);
            }

            @Override
            void onReceivePriorityFrame(Frames.HttpFrame frame) {
                super.onReceivePriorityFrame(frame);
            }

            @Override
            void onReceiveWindowUpdateFrame(Frames.HttpFrame frame) {
                super.onReceiveWindowUpdateFrame(frame);
            }
        },

        /**
         * receive headers => halfclosedLocal
         * send rst_stream => closed
         * receive rst_stream => closed
         * <p>
         * can send: headers, rst_stream, priority
         * can receive: headers, rst_stream, priority
         */
        RESERVED_REMOTE {

            @Override
            void onSendHeaderFrame(Frames.HeadersFrame frame) {
                super.onSendHeaderFrame(frame);
            }

            @Override
            void onSendResetFrame(Frames.ResetFrame frame) {
                super.onSendResetFrame(frame);
            }

            @Override
            void onSendPriorityFrame(Frames.PriorityFrame frame) {
                super.onSendPriorityFrame(frame);
            }

            @Override
            void onReceiveHeaderFrame(Frames.HttpFrame frame) {
                super.onReceiveHeaderFrame(frame);
            }

            @Override
            void onReceiveResetFrame(Frames.HttpFrame frame) {
                super.onReceiveResetFrame(frame);
            }

            @Override
            void onReceivePriorityFrame(Frames.HttpFrame frame) {
                super.onReceivePriorityFrame(frame);
            }
        },

        /**
         * can send: any type
         * can receive: any type
         * <p>
         * send frame with the end_stream flag set => halfclosedLocal
         * receive frame with the end_stream flag set => halfclosedRemote
         * <p>
         * send rst_stream => closed
         * receive rst_stream => closed
         */
        OPEN {
            @Override
            void onSendDataFrame(Frames.DataFrame frame) {
                super.onSendDataFrame(frame);
            }

            @Override
            void onSendHeaderFrame(Frames.HeadersFrame frame) {
                super.onSendHeaderFrame(frame);
            }

            @Override
            void onSendPriorityFrame(Frames.PriorityFrame frame) {
                super.onSendPriorityFrame(frame);
            }

            @Override
            void onSendSettingsFrame(Frames.SettingsFrame frame) {
                super.onSendSettingsFrame(frame);
            }

            @Override
            void onSendPushPromiseFrame(Frames.PushPromiseFrame frame) {
                super.onSendPushPromiseFrame(frame);
            }

            @Override
            void onSendPingFrame(Frames.PingFrame frame) {
                super.onSendPingFrame(frame);
            }

            @Override
            void onSendGoAwayFrame(Frames.GoAwayFrame frame) {
                super.onSendGoAwayFrame(frame);
            }

            @Override
            void onSendWindowUpdateFrame(Frames.WindowUpdateFrame frame) {
                super.onSendWindowUpdateFrame(frame);
            }

            @Override
            void onSendContinuationFrame(Frames.ContinuationFrame frame) {
                super.onSendContinuationFrame(frame);
            }

            @Override
            void onReceiveDataFrame(Frames.HttpFrame frame) {
                super.onReceiveDataFrame(frame);
            }

            @Override
            void onReceiveHeaderFrame(Frames.HttpFrame frame) {
                super.onReceiveHeaderFrame(frame);
            }

            @Override
            void onReceivePriorityFrame(Frames.HttpFrame frame) {
                super.onReceivePriorityFrame(frame);
            }

            @Override
            void onReceiveResetFrame(Frames.HttpFrame frame) {
                super.onReceiveResetFrame(frame);
            }

            @Override
            void onReceiveSettingsFrame(Frames.HttpFrame frame) {
                super.onReceiveSettingsFrame(frame);
            }

            @Override
            void onReceivePushPromiseFrame(Frames.HttpFrame frame) {
                super.onReceivePushPromiseFrame(frame);
            }

            @Override
            void onReceivePingFrame(Frames.HttpFrame frame) {
                super.onReceivePingFrame(frame);
            }

            @Override
            void onReceiveGoAwayFrame(Frames.HttpFrame frame) {
                super.onReceiveGoAwayFrame(frame);
            }

            @Override
            void onReceiveWindowUpdateFrame(Frames.HttpFrame frame) {
                super.onReceiveWindowUpdateFrame(frame);
            }

            @Override
            void onReceiveContinuationFrame(Frames.HttpFrame frame) {
                super.onReceiveContinuationFrame(frame);
            }
        },

        /**
         * can send window_update, priority, rst_stream
         * <p>
         * frame received with end_stream flag => closed
         * rst_stream received => closed
         * rst_stream send => closed
         * <p>
         * receive any type of frame.
         * <p>
         * window and priority frames seem to be particularly relevant.
         */
        HALF_CLOSED_LOCAL {

            @Override
            void onSendWindowUpdateFrame(Frames.WindowUpdateFrame frame) {
                super.onSendWindowUpdateFrame(frame);
            }

            @Override
            void onSendPriorityFrame(Frames.PriorityFrame frame) {
                super.onSendPriorityFrame(frame);
            }

            @Override
            void onSendResetFrame(Frames.ResetFrame frame) {
                super.onSendResetFrame(frame);
            }

            @Override
            void onReceiveDataFrame(Frames.HttpFrame frame) {
                super.onReceiveDataFrame(frame);
            }

            @Override
            void onReceiveHeaderFrame(Frames.HttpFrame frame) {
                super.onReceiveHeaderFrame(frame);
            }

            @Override
            void onReceivePriorityFrame(Frames.HttpFrame frame) {
                super.onReceivePriorityFrame(frame);
            }

            @Override
            void onReceiveResetFrame(Frames.HttpFrame frame) {
                super.onReceiveResetFrame(frame);
            }

            @Override
            void onReceiveSettingsFrame(Frames.HttpFrame frame) {
                super.onReceiveSettingsFrame(frame);
            }

            @Override
            void onReceivePushPromiseFrame(Frames.HttpFrame frame) {
                super.onReceivePushPromiseFrame(frame);
            }

            @Override
            void onReceivePingFrame(Frames.HttpFrame frame) {
                super.onReceivePingFrame(frame);
            }

            @Override
            void onReceiveGoAwayFrame(Frames.HttpFrame frame) {
                super.onReceiveGoAwayFrame(frame);
            }

            @Override
            void onReceiveWindowUpdateFrame(Frames.HttpFrame frame) {
                super.onReceiveWindowUpdateFrame(frame);
            }

            @Override
            void onReceiveContinuationFrame(Frames.HttpFrame frame) {
                super.onReceiveContinuationFrame(frame);
            }
        },

        /**
         * no need to maintain a flowcontrol window
         * can receive window_update, priority, rst_stream.
         * other frames => STREAM_CLOSED
         * can send frames of any type
         * send frame with end_stream flag => closed
         * send rst_stream => closed
         * receive rst_stream => closed
         */
        HALF_CLOSED_REMOTE {

            void onSendDataFrame(Frames.DataFrame frame) {
                super.onSendDataFrame(frame);
            }

            @Override
            void onSendHeaderFrame(Frames.HeadersFrame frame) {
                super.onSendHeaderFrame(frame);
            }

            @Override
            void onSendPriorityFrame(Frames.PriorityFrame frame) {
                super.onSendPriorityFrame(frame);
            }

            @Override
            void onSendSettingsFrame(Frames.SettingsFrame frame) {
                super.onSendSettingsFrame(frame);
            }

            @Override
            void onSendPushPromiseFrame(Frames.PushPromiseFrame frame) {
                super.onSendPushPromiseFrame(frame);
            }

            @Override
            void onSendPingFrame(Frames.PingFrame frame) {
                super.onSendPingFrame(frame);
            }

            @Override
            void onSendGoAwayFrame(Frames.GoAwayFrame frame) {
                super.onSendGoAwayFrame(frame);
            }

            @Override
            void onSendWindowUpdateFrame(Frames.WindowUpdateFrame frame) {
                super.onSendWindowUpdateFrame(frame);
            }

            @Override
            void onSendContinuationFrame(Frames.ContinuationFrame frame) {
                super.onSendContinuationFrame(frame);
            }

            @Override
            void onReceiveWindowUpdateFrame(Frames.HttpFrame frame) {
                super.onReceiveWindowUpdateFrame(frame);
            }

            @Override
            void onReceivePriorityFrame(Frames.HttpFrame frame) {
                super.onReceivePriorityFrame(frame);
            }

            @Override
            void onReceiveResetFrame(Frames.HttpFrame frame) {
                super.onReceiveResetFrame(frame);
            }
        },

        /**
         * Can send priority frames.
         * Can receive priority frames.
         * <p>
         * window_update or rst_stream can be received for a short period after a data/headers with the end_stream flag set. Theses must be ignored.
         * after a longer time, consider returning protocol_error error
         * <p>
         * <p>
         * Any other frames => stream_closed error
         */
        CLOSED {
            @Override
            void onSendPriorityFrame(Frames.PriorityFrame frame) {
                super.onSendPriorityFrame(frame);
            }

            @Override
            void onReceivePriorityFrame(Frames.HttpFrame frame) {
                super.onReceivePriorityFrame(frame);
            }

            @Override
            void onReceiveWindowUpdateFrame(Frames.HttpFrame frame) {
                super.onReceiveWindowUpdateFrame(frame);
            }

            @Override
            void onReceiveResetFrame(Frames.HttpFrame frame) {
                super.onReceiveResetFrame(frame);
            }
        };

        private void protocolError() {}

        private void illegalSend() {
            throw new UnsupportedOperationException();
        }

        void onReceiveDataFrame(Frames.HttpFrame frame) {
            protocolError();
        }

        void onReceiveHeaderFrame(Frames.HttpFrame frame) {
            protocolError();
        }

        void onReceivePriorityFrame(Frames.HttpFrame frame) {
            protocolError();
        }

        void onReceiveResetFrame(Frames.HttpFrame frame) {
            protocolError();
        }

        void onReceiveSettingsFrame(Frames.HttpFrame frame) {
            protocolError();
        }

        void onReceivePushPromiseFrame(Frames.HttpFrame frame) {
            protocolError();
        }

        void onReceivePingFrame(Frames.HttpFrame frame) {
            protocolError();
        }

        void onReceiveGoAwayFrame(Frames.HttpFrame frame) {
            protocolError();
        }

        void onReceiveWindowUpdateFrame(Frames.HttpFrame frame) {
            protocolError();
        }

        void onReceiveContinuationFrame(Frames.HttpFrame frame) {
            protocolError();
        }

        void onSendDataFrame(Frames.DataFrame frame) {
            illegalSend();
        }

        void onSendHeaderFrame(Frames.HeadersFrame frame) {
            illegalSend();
        }

        void onSendPriorityFrame(Frames.PriorityFrame frame) {
            illegalSend();
        }

        void onSendResetFrame(Frames.ResetFrame frame) {
            illegalSend();
        }

        void onSendSettingsFrame(Frames.SettingsFrame frame) {
            illegalSend();
        }

        void onSendPushPromiseFrame(Frames.PushPromiseFrame frame) {
            illegalSend();
        }

        void onSendPingFrame(Frames.PingFrame frame) {
            illegalSend();
        }

        void onSendGoAwayFrame(Frames.GoAwayFrame frame) {
            illegalSend();
        }

        void onSendWindowUpdateFrame(Frames.WindowUpdateFrame frame) {
            illegalSend();
        }

        void onSendContinuationFrame(Frames.ContinuationFrame frame) {
            illegalSend();
        }
    }
}
