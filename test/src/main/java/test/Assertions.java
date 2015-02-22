package test;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.shared.Frames;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public abstract class Assertions {

    private static final Logger log = LoggerFactory.getLogger(Assertions.class);

    public static void assertPing(InputStream is) throws IOException {
        Frames.PingFrame pingFrame = new Frames.PingFrame();

        byte[] header = new byte[Frames.HEADER_SIZE];
        is.read(header);
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
        final int length = buffer.getInt();
        log.info(String.format("Received frame with length: %d %X", length, length));
        byte[] typeReservedAndFlags = new byte[5];
        buffer.get(typeReservedAndFlags);
        pingFrame.streamId = buffer.getInt();
        pingFrame.data = new byte[length];
        int bytesRead = is.read(pingFrame.data);
        if (bytesRead != length) {
            throw new RuntimeException(String.format("Failed to consume the entire payload! Expected %d %X, actual %d %X.", length, length, bytesRead, bytesRead));
        }

        int type = typeReservedAndFlags[0] << 16
                | typeReservedAndFlags[1] << 8
                | typeReservedAndFlags[2];

        Assert.assertEquals(4, type);

        pingFrame.flags = typeReservedAndFlags[4];
    }

    public static void assertSettings(InputStream is) throws IOException {
        byte[] header = new byte[Frames.HEADER_SIZE];
        is.read(header);
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
        final int length = buffer.getInt();
        log.info(String.format("Received frame with length: %d %X", length, length));
        byte[] typeReservedAndFlags = new byte[5];
        buffer.get(typeReservedAndFlags);
        int streamId = buffer.getInt();
        byte[] payload = new byte[length];
        int bytesRead = is.read(payload);
        if (bytesRead != length) {
            throw new RuntimeException(String.format("Failed to consume the entire payload! Expected %d %X, actual %d %X.", length, length, bytesRead, bytesRead));
        }

        int type = typeReservedAndFlags[0] << 16
                | typeReservedAndFlags[1] << 8
                | typeReservedAndFlags[2];

        Assert.assertEquals(4, type);

        int flags = typeReservedAndFlags[4];
        Assert.assertEquals("", 1, flags & 0x1); // ack
        Map<Integer, Integer> settings = new HashMap<>();
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
