package http.http2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class Frames {

    private static final Logger log = LoggerFactory.getLogger(Frames.class);
    public static final int HEADER_SIZE = 9 * 8;
    public static final int SETTINGS_MAX_FRAME_LENGTH = 16384;

    enum Error {
        NO_ERROR, PROTOCOL_ERROR, INTERNAL_ERROR, FLOW_CONTROL_ERROR, SETTINGS_TIMEOUT,
        STREAM_CLOSED, FRAME_SIZE_ERROR, REFUSED_STREAM, CANCEL, COMPRESSION_ERROR,
        CONNECT_ERROR, ENHANCE_YOUR_CALM, INADEQUATE_SECURITY
    }

    public static final Error[] errors = Error.values();

    public static class FrameHeader {
        int length;
        int type;
        int flags;
        boolean reserved;
        int streamId;
    }

    public static enum Type {
        DATA, HEADERS, PRIORITY, RST_STREAM, SETTINGS, PUSH_PROMISE, PING, GO_AWAY, WINDOW_UPDATE, CONTINUATION
    }

    public static final Type[] types = Type.values();

    public static class DataFrame extends Frame {
        int padding;
        byte[] data;

        public DataFrame() {
            super(Type.DATA);
        }
    }

    public static class HeadersFrame extends Frame {
        Map<String, String> headers;
        boolean exclusive;
        int weight;

        public HeadersFrame() {
            super(Type.HEADERS);
        }
    }

    public static class PriorityFrame extends Frame {
        public PriorityFrame() {
            super(Type.PRIORITY);
        }
    }

    public static class ResetFrame extends Frame {
        Error error;

        public ResetFrame() {
            super(Type.RST_STREAM);
        }

        protected void writePayload(DataOutputStream os) throws IOException {
            os.writeInt(error.ordinal());
        }
    }

    public static class SettingsFrame extends Frame {

        public boolean ack;
        Map<Integer, Integer> settings = new HashMap<>();

        public SettingsFrame() {
            super(Type.SETTINGS);
        }

        @Override
        protected void writePayload(ByteBuffer buffer) {
            for (Map.Entry<Integer, Integer> entry : settings.entrySet()) {
                Integer settingIdentifier = entry.getKey();
                Integer value = entry.getValue();

                buffer.put((byte) (0x2 & settingIdentifier));
                buffer.put((byte) (0x1 & settingIdentifier));
                buffer.putInt(value);
            }
        }

        public void read(byte[] payload) {
            for (int i = 0; i < payload.length; i += 6) {
                int settingIdentifier = payload[i]
                        & payload[i + 1] << 1;
                int settingValue = payload[i + 2] << 3
                        & payload[i + 3] << 2
                        & payload[i + 4] << 1
                        & payload[i + 5];
                settings.put(settingIdentifier, settingValue);
            }
        }
    }

    public static class PushPromiseFrame extends Frame {

        public PushPromiseFrame() {
            super(Type.PUSH_PROMISE);
        }
    }

    public static class PingFrame extends Frame {

        public byte[] data;
        public boolean ack;

        public PingFrame() {
            this(new byte[]{
                    (byte) (0xDEADBEEF >>> 24),
                    (byte) (0xDEADBEEF >>> 16),
                    (byte) (0xDEADBEEF >>> 8),
                    (byte) 0xDEADBEEF,
                    (byte) (0xDEADBEEF >>> 24),
                    (byte) (0xDEADBEEF >>> 16),
                    (byte) (0xDEADBEEF >>> 8),
                    (byte) 0xDEADBEEF
            });
        }

        public PingFrame(byte[] data) {
            super(Type.PING);
            this.data = data;
        }

        @Override
        protected void writePayload(ByteBuffer buffer) {
            if (data.length != 8) {
                throw new RuntimeException("Ping payload must be length 8, not " + data.length);
            }
            buffer.put(data);
        }

        public void read(byte[] payload) {
            data = payload;
        }
    }

    public static class GoAwayFrame extends Frame {

        int lastStreamId;
        public Error error;
        byte[] data;

        public GoAwayFrame() {
            super(Type.GO_AWAY);
        }

        @Override
        protected void writePayload(ByteBuffer buffer) {
            buffer.putInt(lastStreamId);
            buffer.putInt(error.ordinal());
            buffer.put(data);
        }

        public void read(byte[] payloadData) {
            ByteBuffer payload = ByteBuffer.wrap(payloadData);
            lastStreamId = payload.getInt();
            error = errors[payload.getInt()];
            data = new byte[payload.limit() - payload.position()];
            payload.get(data);
        }
    }

    public static class WindowUpdateFrame extends Frame {

        public WindowUpdateFrame() {
            super(Type.WINDOW_UPDATE);
        }
    }

    public static class ContinuationFrame extends Frame {

        public ContinuationFrame() {
            super(Type.CONTINUATION);
        }
    }

    public static abstract class Frame {

        public int streamId;
        public final int type;
        public int flags;

        public Frame(Type type) {
            this.type = type.ordinal();
        }

        public final void write(OutputStream outputStream) {
            try {
                int maxLength = 16_384;

                ByteBuffer buffer = ByteBuffer.allocate(2048);

                buffer.position(HEADER_SIZE);
                writePayload(buffer);
                buffer.flip();
                int payloadLength = buffer.limit() - HEADER_SIZE;
                log.info(String.format("Writing frame with length: %X", payloadLength));
                if (payloadLength > maxLength) {
                    throw new RuntimeException("Length exceeds maximum length. Has a SETTINGS_MAX_FRAME_SIZE setting been sent and not parsed?");
                }
                byte[] typeReservedAndFlags = new byte[5];
                typeReservedAndFlags[0] = (byte) ((0xFF0000 & type) >> 16);
                typeReservedAndFlags[1] = (byte) ((0x00FF00 & type) >> 8);
                typeReservedAndFlags[2] = (byte) (0x0000FF & type);

                typeReservedAndFlags[4] = (byte) flags;

                buffer.putInt(payloadLength)
                        .put(typeReservedAndFlags)
                        .putInt(streamId)
                        .rewind();

                byte[] array = buffer.array();
                outputStream.write(array, 0, buffer.limit());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static Frame readSync(InputStream is) throws IOException {
            CompletableFuture<Frame> receivedFrame = new CompletableFuture<>();
            try {
                FrameReceiver receiver = new FrameReceiver() {
                    @Override
                    public void onFrame(DataFrame frame) {
                        receivedFrame.complete(frame);
                    }

                    @Override
                    public void onFrame(HeadersFrame frame) {
                        receivedFrame.complete(frame);
                    }

                    @Override
                    public void onFrame(PriorityFrame frame) {
                        receivedFrame.complete(frame);
                    }

                    @Override
                    public void onFrame(ResetFrame frame) {
                        receivedFrame.complete(frame);
                    }

                    @Override
                    public void onFrame(SettingsFrame frame) {
                        receivedFrame.complete(frame);
                    }

                    @Override
                    public void onFrame(PushPromiseFrame frame) {
                        receivedFrame.complete(frame);
                    }

                    @Override
                    public void onFrame(PingFrame frame) {
                        receivedFrame.complete(frame);
                    }

                    @Override
                    public void onFrame(GoAwayFrame frame) {
                        receivedFrame.complete(frame);
                    }

                    @Override
                    public void onFrame(WindowUpdateFrame frame) {
                        receivedFrame.complete(frame);
                    }

                    @Override
                    public void onFrame(ContinuationFrame frame) {
                        receivedFrame.complete(frame);
                    }
                };
                read(is, receiver);
                return receivedFrame.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public static void read(InputStream is, FrameReceiver receiver) throws IOException {
            byte[] header = new byte[HEADER_SIZE];
            is.read(header);
            ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
            final int length = buffer.getInt();

            byte[] typeReservedAndFlags = new byte[5];
            buffer.get(typeReservedAndFlags);
            int streamId = buffer.getInt();

            int type = typeReservedAndFlags[0] << 16
                    | typeReservedAndFlags[1] << 8
                    | typeReservedAndFlags[2];

            int flags = typeReservedAndFlags[4];

            log.info(String.format("Received frame with length: %d, type: %s, flags: %d", length, Type.values()[type], flags));

            if (length > SETTINGS_MAX_FRAME_LENGTH) {
                throw new RuntimeException(String.format("Payload of size %d exceeded the maximum length of %d.", length, SETTINGS_MAX_FRAME_LENGTH));
            }

            // Should we delay reading the payload?
            byte[] payload = new byte[length];
            if (length > 0) {
                int bytesRead = is.read(payload);
                if (bytesRead < 0) {
                    throw new RuntimeException("Failed to read anything from the input stream!");
                } else if (bytesRead != length) {
                    throw new RuntimeException(String.format("Failed to consume the entire payload! Expected %d, actual %d.", length, bytesRead));
                }
            } else {
                log.info("Empty payload. Continuing...");
            }

            switch (types[type]) {
                case SETTINGS: {
                    SettingsFrame frame = new SettingsFrame();
                    frame.streamId = streamId;
                    frame.ack = (flags & 0x1) == 1;
                    frame.read(payload);
                    receiver.onFrame(frame);
                    break;
                }
                case GO_AWAY: {
                    GoAwayFrame frame = new GoAwayFrame();
                    frame.streamId = streamId;
                    frame.read(payload);
                    receiver.onFrame(frame);
                    break;
                }
                case PING: {
                    PingFrame frame = new PingFrame();
                    frame.streamId = streamId;
                    frame.ack = (flags & 0x1) == 1;
                    frame.read(payload);
                    receiver.onFrame(frame);
                    break;
                }
                default: {
                    throw new RuntimeException("Unknown frame type " + types[type]);
                }
            }
        }

        protected void writePayload(ByteBuffer buffer) {
            throw new UnsupportedOperationException();
        }
    }
}
