package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

        public int streamId;
        public int padLength;
        public byte[] data;

        public boolean flagEndStream;
        public boolean flagPadded;

        @Override
        public HttpFrame asHttpFrame() {
            int flags = (flagEndStream? 0x1 : 0)
                    | (flagPadded? 0x8 : 0);

            if (flagPadded) {
                return new HttpFrame(streamId, 0x0, flags,
                        ByteBuffer.allocate(1 + data.length + padLength)
                            .put(integerToByte(padLength))
                            .put(data)
                            .put(new byte[padLength])
                            .array());
            } else {
                return new HttpFrame(streamId, 0x0, flags, data);
            }
        }
    }

    public static class HeadersFrame implements Frame {

        public int streamId;
        public int padLength;
        public LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        public boolean exclusive;
        public int streamDependency;
        public int weight;

        public boolean flagEndStream;
        public boolean flagEndHeaders;
        public boolean flagPadded;
        public boolean flagPriority;

        @Override
        public HttpFrame asHttpFrame() {
            int flags = (flagEndStream? 0x1 : 0)
                    | (flagEndHeaders? 0x4 : 0)
                    | (flagPadded? 0x8 : 0)
                    | (flagPriority? 0x20 : 0);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Hpack hpackEncoder = new Hpack();
            byte[] headerBlockFragment = hpackEncoder.encodeHeaderList(headers);

            if (flagPadded) {
                baos.write(padLength);
            }

            if (flagPriority) {
                baos.write((exclusive? 127 : 0) | streamDependency >>> 24);
                baos.write(streamDependency >>> 16);
                baos.write(streamDependency >>> 8);
                baos.write(streamDependency);
                baos.write(weight);
            }

            try {
                baos.write(headerBlockFragment);
                baos.write(new byte[padLength]);
            } catch (IOException e) {
                throw new RuntimeException("Unhandled exception!", e);
            }

            return new HttpFrame(streamId, 0x1, flags, baos.toByteArray());
        }
    }

    public static class PriorityFrame implements Frame {

        public int streamId;
        public boolean exclusive;
        public int streamDependency;
        public int weight;

        @Override
        public HttpFrame asHttpFrame() {
            int flags = 0;
            return new HttpFrame(streamId, 0x2, flags, ByteBuffer.allocate(5)
                    .putInt((exclusive? (1 << 31) : 0) | streamDependency)
                    .put((byte) weight)
                    .array()
            );
        }
    }

    public static class ResetFrame implements Frame {

        public int streamId;
        public Error error;

        @Override
        public HttpFrame asHttpFrame() {
            int flags = 0;
            return new HttpFrame(streamId, 0x3, flags, ByteBuffer.allocate(4).putInt(error.ordinal()).array());
        }
    }

    public static class SettingsFrame implements Frame {

        public int streamId;
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

        public int streamId;
        public int padLength;
        public boolean reserved;
        public int promisedStreamId;
        public Map<String, String> headers;
        public boolean flagEndHeaders;
        public boolean flagPadded;

        @Override
        public HttpFrame asHttpFrame() {
            int flags = (flagEndHeaders? 0x4 : 0)
                    | (flagPadded? 0x8 : 0);

            byte[] headerBlockFragment = new byte[0];
            int payloadLength = (flagPadded? padLength : 0)
                    + 4 + headerBlockFragment.length + padLength;

            ByteBuffer payloadBuffer = ByteBuffer.allocate(payloadLength);

            if (flagPadded) {
                payloadBuffer.put((byte) padLength);
            }

            byte[] payload = payloadBuffer
                    .putInt(promisedStreamId)
                    .put(headerBlockFragment)
                    .array();

            return new HttpFrame(streamId, 0x5, flags, payload);
        }
    }

    public static class  PingFrame implements Frame {

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

        public int lastStreamId;
        public Error error;
        public byte[] data;

        @Override
        public HttpFrame asHttpFrame() {
            int flags = 0;
            byte[] payload = ByteBuffer.allocate(8 + data.length)
                    .putInt(0x80000000 | lastStreamId)
                    .putInt(error.ordinal())
                    .put(data)
                    .array();
            return new HttpFrame(0, 0x7, flags, payload);
        }
    }

    public static class WindowUpdateFrame implements Frame {

        public int streamId;
        public int canTransmit;

        @Override
        public HttpFrame asHttpFrame() {
            int flags = 0;
            byte[] payload = ByteBuffer.allocate(4).putInt(canTransmit).array();
            return new HttpFrame(streamId, 0x8, flags, payload);
        }
    }

    public static class ContinuationFrame implements Frame {

        public int streamId;
        public LinkedHashMap<String, String> headers;
        public boolean flagEndHeaders;

        // TODO: Reuse the context.

        @Override
        public HttpFrame asHttpFrame() {
            int flags = flagEndHeaders? 0x4 : 0;
            byte[] payload = new Hpack().encodeHeaderList(headers);
            return new HttpFrame(streamId, 0x9, flags, payload);
        }
    }

    public static class HttpFrame {

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
    }

    /**
     * Convert an integer into a byte.
     *
     * @param x An integer between 0 and 255.
     * @return The least significant byte of the integer x.
     */
    private static byte integerToByte(int x) {
        ByteBuffer buffer = ByteBuffer.allocate(4).putInt(x & 0xff);
        buffer.rewind();
        return buffer.get(3);
    }
}
