package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

/**
 * Utility functions.
 */
public abstract class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class.getName());

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            log.error("Failed to close stream! " + e.getMessage());
        }
    }

    /**
     * Read the data currently in the InputStream as a String.
     * Does not account for pending data.
     *
     * @param inputStream
     * @return
     */
    public static String inputStream2String(InputStream inputStream) {
        StringBuilder builder = new StringBuilder();
        try {
            int available;
            while ((available = inputStream.available()) > 0) {
                byte[] buffer = new byte[available];
                int bytesRead = inputStream.read(buffer);
                byte[] choppedBuffer = Arrays.copyOf(buffer, bytesRead);
                builder.append(new String(choppedBuffer));
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final void tcpEcho(Socket client) {
        try {
            // What is OOBInline?
            InputStream is = client.getInputStream();
            OutputStream os = client.getOutputStream();

            int next;
            // TODO: Note a warning around the omission of pending data.
            while (is.available() > 0) {
                next = is.read();
                os.write(next);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * String request-response over a socket.
     * This method features a call to sleep() due to not knowing the content length of the response,
     * so should be avoided by callers who do know the expeted content length.
     *
     * @param client
     * @param input
     * @return
     * @throws IOException
     */
    public static String requestResponseOverSocket(Socket client, String input) throws IOException {
        InputStream is = client.getInputStream();
        OutputStream os = client.getOutputStream();
        os.write(input.getBytes("UTF-8"));
        os.flush();
        // There is no data at the moment, but allow the server some time for further response data,
        // before hanging up.

        StringBuilder builder = new StringBuilder();
        try {
            while (!client.isClosed()) {
                int available = is.available();

                if (available > 0) {
                    byte[] buffer = new byte[available];
                    int bytesRead = is.read(buffer);
                    byte[] choppedBuffer = Arrays.copyOf(buffer, bytesRead);
                    builder.append(new String(choppedBuffer));
                } else if (available < 0) {
                    throw new RuntimeException("IO Failure!" + available);
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        log.warn("Interrupted!", e);
                    }
                    if (is.available() <= 0) {
                        client.close();
                    } // otherwise, carry on.
                }
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateString(int length) {
        StringBuilder builder = new StringBuilder(length);
        Random random = new Random();
        for (int i=0; i<length; i++) {
            builder.append((char)(random.nextInt(26) + (int)'a'));
        }
        return builder.toString();
    }
}
