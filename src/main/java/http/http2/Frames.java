package http.http2;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class Frames {

    enum Error {
        NO_ERROR, PROTOCOL_ERROR, INTERNAL_ERROR, FLOW_CONTROL_ERROR, SETTINGS_TIMEOUT,
        STREAM_CLOSED, FRAME_SIZE_ERROR, REFUSED_STREAM, CANCEL, COMPRESSION_ERROR,
        CONNECT_ERROR, ENHANCE_YOUR_CALM, INADEQUATE_SECURITY;
    }
    
    public static void writeFrame(OutputStream os, Frame frame) {

    }

    public static class DataFrame extends Frame {
        int padding;
        byte[] data;
        int streamId;
    }

    public static class HeadersFrame extends Frame {
        Map<String, String> headers;
        boolean exclusive;
        int streamId;
        int weight;
        // header block fragment

    }

    public static class ResetFrame extends Frame {
        int errorCode;
    }

    public static class SettingsFrame extends Frame {

    }

    public static class PushPromiseFrame extends Frame {

    }

    public static class PingFrame extends Frame {

    }

    public static class GoAwayFrame extends Frame {

    }

    public static class WindowUpdateFrame extends Frame {

    }

    public static class ContinuationFrame extends Frame {

    }

    static abstract class Frame {


    }

    public static Frame readFrame(InputStream is) {
        return null;
    }

}
