package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Messages {

    private static Logger log = Logger.getLogger(Messages.class.getName());
    private static final Map<Integer, String> statusReason = new HashMap<>();

    static {
        // IMPROVE: Read from a properties file.
        statusReason.put(200, "OK");
        statusReason.put(400, "Bad Request");
        statusReason.put(404, "Not Found");
    }

    static abstract class MessageToRead {

        String version;
        Map<String, String> headers;
    }

    public static class RequestToRead extends MessageToRead {

        String method;
        String target;

        RequestToRead(InputStream inputStream) throws IOException {
            // RFC 7230 Section 3 - Message Format
            MessageParser messageParser = new MessageParser(inputStream);
            String[] startLineComponents = messageParser.requestLineComponents();

            this.method = startLineComponents[0];
            this.target = startLineComponents[1];
            this.version = startLineComponents[2];

            this.headers = messageParser.headers();
        }
    }

    public static class ResponseToRead extends MessageToRead {

        int status;
        String reason;

        ResponseToRead(InputStream inputStream) throws IOException {
            MessageParser messageParser = new MessageParser(inputStream);
            String[] statusLineComponents = messageParser.statusLineComponents();

            this.version = statusLineComponents[0];
            String statusString = statusLineComponents[1].trim();
            status = Integer.parseInt(statusString);
            this.reason = statusLineComponents[2];

            this.headers = messageParser.headers();
        }
    }

    static class MessageParser {

        InputStream inputStream;
        InputStreamReader isReader;
        BufferedReader reader;

        MessageParser(InputStream inputStream) {
            this.inputStream = inputStream;
            this.isReader = new InputStreamReader(inputStream);
            this.reader = new BufferedReader(isReader);
        }

        String startLine() throws IOException {
            Map<String, String> headers = new HashMap<>();
            return reader.readLine();
        }

        String[] requestLineComponents() throws IOException {
            return startLine().split("\\s"); // FIXME: Validation; efficiency.
        }

        String[] statusLineComponents() throws IOException {
            return startLine().split("\\s");
        }

        Map<String, String> headers() throws IOException {
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while ((headerLine = reader.readLine()).trim().length() > 0) {
                // read until delimiter
                // read remaining, discarding whitespace
                int split = headerLine.indexOf(':');
                if (split == -1) {
                    log.warning("Invalid header! " + headerLine);
                    break;
                }
                headers.put(headerLine.substring(0, split), headerLine.substring(split).trim());
            }
            return headers;
        }
    }

    static class RequestToWrite extends MessageToWrite {

        String method;
        String target;

        MessageBuilder startLine() {
            return new MessageBuilder()
                .requestLine(method, target, version);
        }
    }

    static class ResponseToWrite extends MessageToWrite {

        int status;
        Map<String, String> headers = new HashMap<>();

        MessageBuilder startLine() {
            return new MessageBuilder()
                .statusLine(version, status, statusReason.get(status));
        }
    }

    static abstract class MessageToWrite {

        String version = "HTTP/1.1";
        Map<String, String> headers = new HashMap<>();

        abstract MessageBuilder startLine();

        StringBuilder asStringBuilder() {
            return startLine()
                .headers(headers)
                .crlf()
                .internalBuilder;
        }

        String asString() {
            return asStringBuilder().toString();
        }

        byte[] asBytes() {
            return asString().getBytes();
        }
    }

    static class MessageBuilder {

        StringBuilder internalBuilder = new StringBuilder();

        MessageBuilder crlf() {
            internalBuilder
                .append('\r')
                .append('\n');
            return this;
        }

        MessageBuilder requestLine(String method, String requestTarget, String version) {
            internalBuilder
                .append(method)
                .append(' ')
                .append(requestTarget)
                .append(' ')
                .append(version);
            return crlf();
        }

        MessageBuilder statusLine(String version, int status, String reason) {
            internalBuilder
                .append(version)
                .append(' ')
                .append(String.format("%3d", status))
                .append(' ')
                .append(reason);
            return crlf();
        }

        MessageBuilder headers(Map<String, String> headers) {
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    internalBuilder
                            .append(header.getKey())
                            .append(": ")
                            .append(header.getValue())
                            .append("\r\n");
                }
            }
            return this;
        }
    }
}
