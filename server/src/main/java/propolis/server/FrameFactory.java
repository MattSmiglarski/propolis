package propolis.server;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FrameFactory {

    public Frames.HttpFrame createHttpFrame(Frames.Type type) {
        return new Frames.HttpFrame(0, type.ordinal(), 0, new byte[] {});
    }

    public Frames.HttpFrame createHttpFrame(Frames.Type type, byte[] payload) {
        return new Frames.HttpFrame(0, type.ordinal(), 0, payload);
    }

    public Frames.HttpFrame createHttpFrame(Frames.SettingsFrame settingsFrame) {
        ByteBuffer buffer = ByteBuffer.allocate(6 * settingsFrame.settings.size());
        for (Map.Entry<Frames.SettingsFrame.Setting, Integer> entry : settingsFrame.settings.entrySet()) {
            Integer settingIdentifier = entry.getKey().ordinal();
            Integer value = entry.getValue();

            buffer.put((byte) (0x2 & settingIdentifier));
            buffer.put((byte) (0x1 & settingIdentifier));
            buffer.putInt(value);
        }
        return new Frames.HttpFrame(
                0,
                Frames.Type.SETTINGS,
                settingsFrame.ack? 1 : 0,
                 buffer.array()
        );
    }

    public Frames.HttpFrame createHttpFrame(Frames.PingFrame pingFrame) {
        if (pingFrame.data.length != 8) {
            throw new RuntimeException("Ping payload must be length 8, not " + pingFrame.data.length);
        }
        return new Frames.HttpFrame(
                0,
                Frames.Type.PING,
                pingFrame.ack? 1 : 0,
                pingFrame.data
        );
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

    public Frames.PingFrame createPingFrame(Frames.HttpFrame httpFrame) {
        return new Frames.PingFrame(
                (httpFrame.flags & 0x1) == 1,
                httpFrame.payload
        );
    }
}
