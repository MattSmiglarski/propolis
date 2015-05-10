package propolis.server;

import java.util.HashMap;
import java.util.Map;

public class HttpComponents {
    public static final byte[] PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes();
    public static final Map<Integer, String> statusReason = new HashMap<>();

    enum State {
        IDLE, OPEN, CLOSED, RESERVED_LOCAL, RESERVED_REMOTE, HALF_CLOSED_LOCAL, HALF_CLOSED_REMOTE
    }

    static {
        // TODO: Read from a properties file.
        HttpComponents.statusReason.put(200, "OK");
        HttpComponents.statusReason.put(400, "Bad readRequest");
        HttpComponents.statusReason.put(404, "Not Found");
        HttpComponents.statusReason.put(421, "Misdirected Request");
    }

    public static abstract class Message {

        public String version = "HTTP/1.1";
        public Map<String, String> headers = new HashMap<>();
    }

    public static class Request extends Message {

        public String method;
        public String target;

        @Override
        public String toString() {
            return String.format("%s %s", method, target);
        }
    }

    public static class Response extends Message {

        public int status;
        public String reason;

        public String toString() {
            return String.format("%d: %s", status, reason);
        }
    }
}
