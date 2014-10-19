package http;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class Utils {

    static Logger log = Logger.getLogger(Server.class.getName());

    static void closeQuietly(Closeable closeable) {
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
