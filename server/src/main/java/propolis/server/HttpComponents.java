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

        public static final String HTTP_11 = "HTTP/1.1";

        public final String version;
        public final Map<String, String> headers;

        public Message() {
            this(HTTP_11, new HashMap<>());
        }

        public Message(Map<String, String> headers) {
            this(HTTP_11, headers);
        }

        public Message(String version, Map<String, String> headers) {
            this.version = version;
            this.headers = headers;
        }
    }

    public static final class Request extends Message {

        public final String method;
        public final String target;

        public Request(String method, String target) {
            super();
            this.method = method;
            this.target = target;
        }

        public Request(String method, String target, Map<String, String> headers) {
            super(headers);
            this.method = method;
            this.target = target;
        }

        public Request(String version, String method, String target, Map<String, String> headers) {
            super(version, headers);
            this.method = method;
            this.target = target;
        }

        @Override
        public String toString() {
            return String.format("%s %s", method, target);
        }
    }

    public static final class Response extends Message {

        public final int status;
        public final String reason;

        public Response(int status, String reason) {
            super();
            this.status = status;
            this.reason = reason;
        }

        public Response(int status, String reason, Map<String, String> headers) {
            super(headers);
            this.status = status;
            this.reason = reason;
        }

        public Response(String version, int status, String reason, Map<String, String> headers) {
            super(version, headers);
            this.status = status;
            this.reason = reason;
        }

        public static Response ok() {
            return new Response(200, "OK");
        }

        public String toString() {
            return String.format("%d: %s", status, reason);
        }
    }
}
