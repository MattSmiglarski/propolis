package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import propolis.server.Frames.HttpFrame;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * A bi-directional stream wrapper around a socket, with operations for reading and writing HTTP components.
 */
public class HttpIOStream implements Closeable {

    private static Logger log = LoggerFactory.getLogger(HttpIOStream.class);
    private static final int HEADER_SIZE = 9 * 8;

    private final Socket socket;
    public final HttpInputStream input = new HttpInputStream();
    public final HttpOutputStream output = new HttpOutputStream();

    public HttpIOStream(String url) throws IOException {
        this(new URL(url));
    }

    public HttpIOStream(URL uri) throws IOException {
        socket = new Socket(uri.getHost(), uri.getPort() > 0? uri.getPort() : 80);
    }

    public HttpIOStream(Socket socket) {
        this.socket = socket;
    }

    public Socket getRawSocket() {
        return socket;
    }

    public String readPreface() throws IOException {
        return input.readPreface();
    }

    public void writePreface() throws IOException {
        output.writePreface();
    }

    public HttpFrame readFrame() throws IOException {
        return input.readFrame();
    }

    public void writeFrame(HttpFrame frame) throws IOException {
        output.write(frame);
    }

    public void writeHttpRequest(HttpComponents.Request request) {
        output.writeHttpRequest(request);
    }

    public HttpComponents.Request readHttpRequest() throws IOException {
        return input.readHttpRequest();
    }

    public HttpComponents.Response readHttpResonse() throws IOException {
        return input.readHttpResponse();
    }

    public void writeHttpResponse(HttpComponents.Response response) throws IOException {
        output.writeHttpResponse(response);
    }

    public HttpComponents.Response readHttpResponse() throws IOException {
        return input.readHttpResponse();
    }

    public void writeConnectionPreface() throws IOException {
        output.writePreface();
    }

    public void readConnectionPreface() throws IOException {
        input.readPreface();
    }

    /**
     * Closing this HTTPIOStream will also close the underlying socket.
     * @throws IOException - if an I/O error occurs when closing this HttpIOStream.
     */
    public void close() throws IOException {
        socket.close();
    }

    private class HttpInputStream {

        BufferedReader reader;

        public BufferedReader getReader() {
            if (reader == null) {
                try {
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (IOException e) {
                    throw new RuntimeException();
                }
            }
            return reader;
        }

        /**
         * Read the request preface.
         *
         * @return request preface.
         * @throws IOException
         */
        public String readPreface() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            for (int i = 0; i < HttpComponents.PREFACE.length; i++) {
                if (HttpComponents.PREFACE[i] == ((byte) '\n')) {
                    String prefaceLine = reader.readLine();
                    if (prefaceLine == null) {
                        throw new IOException("Null preface line. Connection closed?");
                    }
                    return prefaceLine;
                }
            }
            throw new IOException("Failed to read preface!");
        }

        public HttpFrame readFrame() throws IOException {
            byte[] header = new byte[HEADER_SIZE];
            socket.getInputStream().read(header);
            ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
            final int length = buffer.getInt();

            byte[] typeReservedAndFlags = new byte[5];
            buffer.get(typeReservedAndFlags);
            int streamId = buffer.getInt();

            int type = typeReservedAndFlags[0] << 16
                    | typeReservedAndFlags[1] << 8
                    | typeReservedAndFlags[2];

            int flags = typeReservedAndFlags[4];

            log.info(String.format("Received frame with length: %d, type: %s, flags: %d", length, Frames.Type.values()[type], flags));

            if (length > Frames.SETTINGS_MAX_FRAME_LENGTH) {
                throw new RuntimeException(String.format("Payload of size %d exceeded the maximum length of %d.", length, Frames.SETTINGS_MAX_FRAME_LENGTH));
            }

            // Should we delay reading the payload?
            byte[] payload = new byte[length];
            if (length > 0) {
                int bytesRead = socket.getInputStream().read(payload);
                if (bytesRead < 0) {
                    throw new RuntimeException("Failed to read anything from the input stream!");
                } else if (bytesRead != length) {
                    throw new RuntimeException(String.format("Failed to consume the entire payload! Expected %d, actual %d.", length, bytesRead));
                }
            } else {
                log.info("Empty payload. Continuing...");
            }
            return new Frames.HttpFrame(streamId, type, flags, payload);
        }

        public HttpComponents.Request readHttpRequest() throws IOException {
            // RFC 7230 Section 3 - Message Format
            MessageParser messageParser = new MessageParser();
            String[] startLineComponents = messageParser.requestLineComponents();

            return new HttpComponents.Request(
                    startLineComponents[0],
                    startLineComponents[1],
                    startLineComponents[2],
                    messageParser.headers()
            );
        }

        public HttpComponents.Response readHttpResponse() throws IOException {
            // RFC 7230 Section 3 - Message Format
            MessageParser messageParser = new MessageParser();
            String[] startLineComponents = messageParser.statusLineComponents();

            return new HttpComponents.Response(
                    startLineComponents[0],
                    Integer.parseInt(startLineComponents[1]),
                    startLineComponents[2],
                    messageParser.headers()
            );
        }

        private class MessageParser {

            String startLine() throws IOException {
                return getReader().readLine();
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

                while ((headerLine = getReader().readLine()).trim().length() > 0) {
                    // read until delimiter
                    // read remaining, discarding whitespace
                    log.debug("Header: " + headerLine);

                    int split = headerLine.indexOf(':');
                    if (split == -1) {
                        log.warn("Invalid header! " + headerLine);
                        break;
                    }
                    headers.put(headerLine.substring(0, split), headerLine.substring(split + 1).trim());
                }
                return headers;
            }
        }
    }

    private class HttpOutputStream {

        public void write(HttpFrame frame) throws IOException {
            int maxLength = 16_384;
            int payloadLength = frame.payload.length;
            log.info(String.format("Writing frame with length: %X", payloadLength));
            if (payloadLength > maxLength) {
                throw new IOException("Length exceeds maximum length. Has a SETTINGS_MAX_FRAME_SIZE setting been sent and not parsed?");
            }
            byte[] typeReservedAndFlags = new byte[5];
            typeReservedAndFlags[0] = (byte) ((0xFF0000 & frame.type) >> 16);
            typeReservedAndFlags[1] = (byte) ((0x00FF00 & frame.type) >> 8);
            typeReservedAndFlags[2] = (byte) (0x0000FF & frame.type);

            typeReservedAndFlags[4] = (byte) frame.flags;

            ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + payloadLength);
            buffer.putInt(payloadLength)
                    .put(typeReservedAndFlags)
                    .putInt(frame.streamId)
                    .put(frame.payload);

            socket.getOutputStream().write(buffer.array(), 0, buffer.limit());
        }

        public void writeHttpResponse(HttpComponents.Response response) {
            try {
                socket.getOutputStream().write(new ResponseBuilder().asBytes(response));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void writeHttpRequest(HttpComponents.Request request) {
            try {
                socket.getOutputStream().write(new RequestBuilder().asBytes(request));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void writePreface() throws IOException {
            socket.getOutputStream().write(HttpComponents.PREFACE);
        }

        abstract class MessageBuilder<M extends HttpComponents.Message> {

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

            public byte[] asBytes(M message) {
                return asString(message).getBytes(Charset.defaultCharset());
            }
        }

        public class RequestBuilder extends MessageBuilder<HttpComponents.Request> {

            MessageBuilder<HttpComponents.Request> startLine(HttpComponents.Request request) {
                internalBuilder
                        .append(request.method)
                        .append(' ')
                        .append(request.target)
                        .append(' ')
                        .append(request.version);
                return crlf();
            }
        }

        public class ResponseBuilder extends MessageBuilder<HttpComponents.Response> {

            MessageBuilder<HttpComponents.Response> startLine(HttpComponents.Response response) {
                internalBuilder
                        .append(response.version)
                        .append(' ')
                        .append(response.status)
                        .append(' ')
                        .append(response.reason != null ? response.reason : HttpComponents.statusReason.get(response.status));
                return crlf();
            }
        }
    }
}
