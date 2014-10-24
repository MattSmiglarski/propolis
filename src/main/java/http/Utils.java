package http;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * Utility methods which do not belong anywhere.
 */
public abstract class Utils {

    public static Logger log = Logger.getLogger(Utils.class.getName());

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            log.severe("Failed to close stream! " + e.getMessage());
        }
    }

    public static String inputStream2String(InputStream inputStream) {
        StringBuffer buffer = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                buffer.append(nextLine);
            }
            return buffer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
