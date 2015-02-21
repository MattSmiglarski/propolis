package propolis.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
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

    public static String inputStream2String(InputStream inputStream) {
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                builder.append(nextLine);
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

    public static String requestResponseOverSocket(Socket client, String input) throws IOException {
        InputStream is = client.getInputStream();
        OutputStream os = client.getOutputStream();
        os.write(input.getBytes());
        return Utils.inputStream2String(is);
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
