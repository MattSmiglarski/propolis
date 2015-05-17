package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public abstract class Frames {

    private static final Logger LOG = LoggerFactory.getLogger(Frames.class);
    public static final int HEADER_SIZE = 9 * 8;
    public static final int SETTINGS_MAX_FRAME_LENGTH = 16384;

    public enum Error {
        NO_ERROR, PROTOCOL_ERROR, INTERNAL_ERROR, FLOW_CONTROL_ERROR, SETTINGS_TIMEOUT,
        STREAM_CLOSED, FRAME_SIZE_ERROR, REFUSED_STREAM, CANCEL, COMPRESSION_ERROR,
        CONNECT_ERROR, ENHANCE_YOUR_CALM, INADEQUATE_SECURITY
    }

    public static final Error[] errors = Error.values();

    public enum Type {
        DATA, HEADERS, PRIORITY, RST_STREAM, SETTINGS, PUSH_PROMISE, PING, GO_AWAY, WINDOW_UPDATE, CONTINUATION
    }

    public static final Type[] types = Type.values();

    public interface Frame {
        HttpFrame asHttpFrame();
    }

    public static class DataFrame implements Frame {

        int padding;
        byte[] data;

        @Override
        public HttpFrame asHttpFrame() {
            throw new UnsupportedOperationException();
        }
    }

    public static class HeadersFrame implements Frame {
        Map<String, String> headers;
        boolean exclusive;
        int weight;

        @Override
        public HttpFrame asHttpFrame() {
            throw new UnsupportedOperationException();
        }
    }

    public static class PriorityFrame implements Frame {

        @Override
        public HttpFrame asHttpFrame() {
            throw new UnsupportedOperationException();
        }
    }

    public static class ResetFrame implements Frame {
        Error error;

        protected void writePayload(DataOutputStream os) throws IOException {
            os.writeInt(error.ordinal());
        }

        @Override
        public HttpFrame asHttpFrame() {
            throw new UnsupportedOperationException();
        }
    }

    public static class SettingsFrame implements Frame {

        public boolean ack;
        public final Map<Setting, Integer> settings;


        public enum Setting {

            SETTINGS_HEADER_TABLE_SIZE,
            SETTINGS_ENABLE_PUSH,
            SETTINGS_MAX_CONCURRENT_STREAMS,
            SETTINGS_INITIAL_WINDOW_SIZE,
            SETTINGS_MAX_FRAME_SIZE,
            SETTINGS_MAX_HEADER_LIST_SIZE;
        }

        public SettingsFrame() {
            this(false, new HashMap<>());
        }

        public SettingsFrame(boolean ack, Map<Setting, Integer> settings) {
            this.ack = ack;
            this.settings = settings;
        }

        @Override
        public HttpFrame asHttpFrame() {
            ByteBuffer buffer = ByteBuffer.allocate(6 * settings.size());
            for (Map.Entry<Frames.SettingsFrame.Setting, Integer> entry : settings.entrySet()) {
                Integer settingIdentifier = entry.getKey().ordinal();
                Integer value = entry.getValue();

                buffer.put((byte) (0x2 & settingIdentifier));
                buffer.put((byte) (0x1 & settingIdentifier));
                buffer.putInt(value);
            }
            return new Frames.HttpFrame(
                    0,
                    Frames.Type.SETTINGS,
                    ack? 1 : 0,
                    buffer.array()
            );
        }
    }

    public static class PushPromiseFrame implements Frame {

        public void writePayload(ByteBuffer buffer) {
            // TODO: Do padding, as required.
            byte b = buffer.get();
            int streamId = b >> 1;
        }

        @Override
        public HttpFrame asHttpFrame() {
            throw new UnsupportedOperationException();
        }
    }

    public static class PingFrame implements Frame {

        public byte[] data;
        public boolean ack;

        public PingFrame() {
            this(false, "12345678".getBytes(Charset.defaultCharset()));
        }

        public PingFrame(String dataString) {
            this(false, dataString.getBytes());
        }

        public PingFrame(boolean ack, String dataString) {
            this(ack, dataString.getBytes());
        }

        public PingFrame(byte[] data) {
            this(false, data);
        }

        public PingFrame(boolean ack, byte[] data) {
            this.data = data;
            this.ack = ack;
        }

        @Override
        public HttpFrame asHttpFrame() {
            if (data.length != 8) {
                throw new RuntimeException("Ping payload must be length 8, not " + data.length);
            }
            return new Frames.HttpFrame(
                    0,
                    Frames.Type.PING,
                    ack? 1 : 0,
                    data
            );
        }

        @Override
        public String toString() {
            return String.format("%s : %s", ack? "ping" : "pong", new String(data));
        }
    }

    public static class GoAwayFrame implements Frame {

        int lastStreamId;
        public Error error;
        byte[] data;

        public void writePayload(ByteBuffer buffer) {
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

        @Override
        public HttpFrame asHttpFrame() {
            throw new UnsupportedOperationException();
        }
    }

    public static class WindowUpdateFrame implements Frame {

        @Override
        public HttpFrame asHttpFrame() {
            throw new UnsupportedOperationException();
        }
    }

    public static class ContinuationFrame implements Frame {

        @Override
        public HttpFrame asHttpFrame() {
            throw new UnsupportedOperationException();
        }
    }

    public static class HttpFrame implements Frame {

        public int streamId;
        public int type;
        public int flags;
        public byte[] payload;

        public HttpFrame(int streamId, Type type, int flags, byte[] payload) {
            this(streamId, type.ordinal(), flags, payload);
        }

        public HttpFrame(int streamId, int type, int flags, byte[] payload) {
            this.streamId = streamId;
            this.type = type;
            this.flags = flags;
            this.payload = payload;
        }

        @Override
        public HttpFrame asHttpFrame() {
            throw new UnsupportedOperationException();
        }
    }
}
