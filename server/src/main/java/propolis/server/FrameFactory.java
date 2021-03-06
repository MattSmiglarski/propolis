package propolis.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FrameFactory {

    public Frames.DataFrame createDataFrame(Frames.HttpFrame httpFrame) {
        Frames.DataFrame dataFrame = new Frames.DataFrame();
        dataFrame.streamId = httpFrame.streamId;
        dataFrame.flagEndStream = ((httpFrame.flags & 0x1) == 1);
        dataFrame.flagPadded = ((httpFrame.flags & 0x8) == 1);

        if (dataFrame.flagPadded) {
            ByteBuffer payload = ByteBuffer.wrap(httpFrame.payload);
            dataFrame.padLength = Byte.toUnsignedInt(payload.get());
            dataFrame.data = new byte[httpFrame.payload.length - dataFrame.padLength - 1];
            payload.get(dataFrame.data);
        } else {
            dataFrame.data = httpFrame.payload;
        }

        return dataFrame;
    }

    public Frames.HeadersFrame createHeadersFrame(Frames.HttpFrame httpFrame) {
        Frames.HeadersFrame headersFrame = new Frames.HeadersFrame();
        headersFrame.flagEndStream = (httpFrame.flags & 0x1) != 0;
        headersFrame.flagEndHeaders = (httpFrame.flags & 0x4) != 0;
        headersFrame.flagPadded = (httpFrame.flags & 0x8) != 0;
        headersFrame.flagPriority = (httpFrame.flags & 0x20) != 0;

        ByteArrayInputStream bais = new ByteArrayInputStream(httpFrame.payload);
        int paddingLength = 0;
        if (headersFrame.flagPadded) {
            paddingLength = bais.read();
        }
        if (headersFrame.flagPriority) {
            byte b = (byte) bais.read();
            headersFrame.exclusive = (b & 127) != 0;
            headersFrame.streamDependency =
                    (b & 0b0111_1111) << 24
                            | (bais.read() << 16)
                            | (bais.read() << 8)
                            | (bais.read());
            headersFrame.weight = bais.read();
        }
        Hpack hpack = new Hpack();
        byte[] headersFramePayload = new byte[bais.available() - paddingLength];
        try {
            bais.read(headersFramePayload);
            headersFrame.headers = hpack.decodeHeaderList(headersFramePayload);
            return headersFrame;
        } catch (IOException e) {
            throw new RuntimeException("Unhandled failure!");
        }
    }

    public Frames.PriorityFrame createPriorityFrame(Frames.HttpFrame httpFrame) {
        Frames.PriorityFrame priorityFrame = new Frames.PriorityFrame();
        priorityFrame.streamId = httpFrame.streamId;
        priorityFrame.exclusive = (httpFrame.payload[0] & 0x8000_0000) != 0;
        priorityFrame.streamDependency = (0x7f & httpFrame.payload[0])
                | httpFrame.payload[1]
                | httpFrame.payload[2]
                | httpFrame.payload[3];

        priorityFrame.weight = httpFrame.payload[4];
        return priorityFrame;
    }

    public Frames.ResetFrame createResetStreamFrame(Frames.HttpFrame httpFrame) {
        Frames.ResetFrame resetFrame = new Frames.ResetFrame();
        int ordinal = (httpFrame.payload[0] << 24)
                | (httpFrame.payload[1] << 16)
                | (httpFrame.payload[2] << 8)
                | httpFrame.payload[3];
        resetFrame.error = Frames.Error.values()[ordinal];
        return resetFrame;
    }

    public Frames.SettingsFrame createSettingsFrame(Frames.HttpFrame httpFrame) {
        Map<Frames.SettingsFrame.Setting, Integer> settings = new HashMap<>();
        byte[] payload = httpFrame.payload;
        for (int i = 0; i < payload.length; i += 6) {
            int settingIdentifier = payload[i]
                    & payload[i + 1] << 1;
            int settingValue = payload[i + 2] << 3
                    & payload[i + 3] << 2
                    & payload[i + 4] << 1
                    & payload[i + 5];
            settings.put(Frames.SettingsFrame.Setting.values()[settingIdentifier], settingValue);
        }

        return new Frames.SettingsFrame(
                (httpFrame.flags & 0x1) == 1,
                settings
        );
    }

    public Frames.PushPromiseFrame createPushPromiseFrame(Frames.HttpFrame httpFrame) {

        Frames.PushPromiseFrame pushPromiseFrame = new Frames.PushPromiseFrame();
        pushPromiseFrame.flagPadded = (httpFrame.flags & 0x8) != 0;
        pushPromiseFrame.flagEndHeaders = (httpFrame.flags & 0x4) != 0;

        ByteBuffer byteBuffer = ByteBuffer.wrap(httpFrame.payload);
        if (pushPromiseFrame.flagPadded) {
            pushPromiseFrame.padLength = byteBuffer.get();
        }

        pushPromiseFrame.promisedStreamId = 0x7fffffff & byteBuffer.getInt();
        int headerBlockFragmentLength = httpFrame.payload.length - byteBuffer.position() - pushPromiseFrame.padLength;
        byte[] headerBlockFragment = new byte[headerBlockFragmentLength];
        byteBuffer.get(headerBlockFragment);
        try {
            // TODO: Use a Hpack decoding context scoped to the stream.
            pushPromiseFrame.headers = new Hpack().decodeHeaderList(headerBlockFragment);
        } catch (IOException e) {
            throw new RuntimeException("Unhandled failuer!", e);
        }

        return pushPromiseFrame;
    }

    public Frames.PingFrame createPingFrame(Frames.HttpFrame httpFrame) {
        return new Frames.PingFrame(
                (httpFrame.flags & 0x1) == 1,
                httpFrame.payload
        );
    }

    public Frames.GoAwayFrame createGoAwayFrame(Frames.HttpFrame httpFrame) {
        Frames.GoAwayFrame goAwayFrame = new Frames.GoAwayFrame();
        ByteBuffer byteBuffer = ByteBuffer.wrap(httpFrame.payload);
        goAwayFrame.lastStreamId = byteBuffer.getInt() & 0x7fffffff;
        goAwayFrame.error = Frames.Error.values()[byteBuffer.getInt()];
        goAwayFrame.data = new byte[httpFrame.payload.length - 8];
        byteBuffer.get(goAwayFrame.data);
        return goAwayFrame;
    }

    public Frames.WindowUpdateFrame createWindowUpdateFrame(Frames.HttpFrame httpFrame) {
        Frames.WindowUpdateFrame windowUpdateFrame = new Frames.WindowUpdateFrame();
        windowUpdateFrame.streamId = httpFrame.streamId;
        windowUpdateFrame.canTransmit = ByteBuffer.wrap(httpFrame.payload).getInt();
        return windowUpdateFrame;
    }

    public Frames.ContinuationFrame createContinuationFrame(Frames.HttpFrame httpFrame) {
        Frames.ContinuationFrame continuationFrame = new Frames.ContinuationFrame();
        try {
            // TODO: Reuse the decoding context.
            continuationFrame.headers = new Hpack().decodeHeaderList(httpFrame.payload);
        } catch (IOException e) {
            throw new RuntimeException("Unhandled failure!", e);
        }
        return continuationFrame;
    }
}
