package http;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods which do not belong anywhere.
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
}
