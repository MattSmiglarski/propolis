package propolis.server;

import java.nio.ByteBuffer;
import java.util.HashMap;
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
        throw new UnsupportedOperationException();
    }

    public Frames.PriorityFrame createPriorityFrame(Frames.HttpFrame httpFrame) {
        throw new UnsupportedOperationException();
    }

    public Frames.ResetFrame createResetStreamFrame(Frames.HttpFrame httpFrame) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    public Frames.PingFrame createPingFrame(Frames.HttpFrame httpFrame) {
        return new Frames.PingFrame(
                (httpFrame.flags & 0x1) == 1,
                httpFrame.payload
        );
    }

    public Frames.GoAwayFrame createGoAwayFrame(Frames.HttpFrame httpFrame) {
        throw new UnsupportedOperationException();
    }

    public Frames.WindowUpdateFrame createWindowUpdateFrame(Frames.HttpFrame httpFrame) {
        throw new UnsupportedOperationException();
    }

    public Frames.ContinuationFrame createContinuationFrame(Frames.HttpFrame httpFrame) {
        throw new UnsupportedOperationException();
    }
}
