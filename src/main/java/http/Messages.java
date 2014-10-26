package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Mechanisms for the reading and writing of requests and responses.
 */
public class Messages {

    private static Logger log = Logger.getLogger(Messages.class.getName());
    private static final Map<Integer, String> statusReason = new HashMap<>();

    static {
        // IMPROVE: Read from a properties file.
        statusReason.put(200, "OK");
        statusReason.put(400, "Bad readRequest");
        statusReason.put(404, "Not Found");
    }

    public static void writeRequest(OutputStream os, Request request) {
        try {
            os.write(new RequestBuilder().asBytes(request));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeResponse(OutputStream os, Response response) {
        try {
            os.write(new ResponseBuilder().asBytes(response));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Response readResponse(InputStream inputStream) throws IOException {
        // RFC 7230 Section 3 - Message Format
        Response response = new Response();
        MessageParser messageParser = new MessageParser(inputStream);
        String[] startLineComponents = messageParser.statusLineComponents();

        response.version = startLineComponents[0];
        response.status = Integer.parseInt(startLineComponents[1]);
        response.reason = startLineComponents[2];
        response.headers = messageParser.headers();

        return response;
    }

    public static Request readRequest(InputStream inputStream) throws IOException {
        // RFC 7230 Section 3 - Message Format
        Request request = new Request();
        MessageParser messageParser = new MessageParser(inputStream);
        String[] startLineComponents = messageParser.requestLineComponents();

        request.method = startLineComponents[0];
        request.target = startLineComponents[1];
        request.version = startLineComponents[2];
        request.headers = messageParser.headers();
        return request;
    }

    static abstract class Message {

        public String version = "HTTP/1.1";
        public Map<String, String> headers = new HashMap<>();
    }

    public static class Request extends Message {

        public String method;
        public String target;

    }

    public static class Response extends Message {

        public int status;
        public String reason;
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
            return reader.readLine();
        }

        String[] requestLineComponents() throws IOException {
            return startLine().split("\\s+"); // FIXME: Validation; efficiency.
        }

        String[] statusLineComponents() throws IOException {
            return startLine().split("\\s+");
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
                headers.put(headerLine.substring(0, split), headerLine.substring(split + 1).trim());
            }
            return headers;
        }
    }

    abstract static class MessageBuilder<M extends Message> {

        StringBuilder internalBuilder = new StringBuilder();

        MessageBuilder<M> crlf() {
            internalBuilder
                    .append('\r')
                    .append('\n');
            return this;
        }

        abstract MessageBuilder<M> startLine(M Message);

        MessageBuilder<M> headers(M message) {
            if (message.headers != null) {
                for (Map.Entry<String, String> header : message.headers.entrySet()) {
                    internalBuilder
                            .append(header.getKey())
                            .append(": ")
                            .append(header.getValue())
                            .append("\r\n");
                }
            }
            return this;
        }

        StringBuilder asStringBuilder(M message) {
            return startLine(message)
                    .headers(message)
                    .crlf()
                    .internalBuilder;
        }

        String asString(M message) {
            return asStringBuilder(message).toString();
        }

        byte[] asBytes(M message) {
            return asString(message).getBytes();
        }
    }

    public static class RequestBuilder extends MessageBuilder<Request> {

        MessageBuilder<Request> startLine(Request request) {
            internalBuilder
                    .append(request.method)
                    .append(' ')
                    .append(request.target)
                    .append(' ')
                    .append(request.version);
            return crlf();
        }
    }

    public static class ResponseBuilder extends MessageBuilder<Response> {

        MessageBuilder<Response> startLine(Response response) {
            internalBuilder
                    .append(response.version)
                    .append(' ')
                    .append(response.status)
                    .append(' ')
                    .append(response.reason != null ? response.reason : statusReason.get(response.status));
            return crlf();
        }
    }
}
