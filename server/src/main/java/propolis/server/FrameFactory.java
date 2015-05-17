package propolis.server;

import java.util.HashMap;
import java.util.Map;

public class FrameFactory {

    public Frames.DataFrame createDataFrame(Frames.HttpFrame httpFrame) {
        throw new UnsupportedOperationException();
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
