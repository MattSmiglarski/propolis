package http.http2;

public interface FrameReceiver {
    void onFrame(Frames.DataFrame frame);

    void onFrame(Frames.HeadersFrame frame);

    void onFrame(Frames.PriorityFrame frame);

    void onFrame(Frames.ResetFrame frame);

    void onFrame(Frames.SettingsFrame frame);

    void onFrame(Frames.PushPromiseFrame frame);

    void onFrame(Frames.PingFrame frame);

    void onFrame(Frames.GoAwayFrame frame);

    void onFrame(Frames.WindowUpdateFrame frame);

    void onFrame(Frames.ContinuationFrame frame);
}
