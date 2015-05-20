package propolis.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HeaderIndex {

    /**
     * As specified by RFC7541 - Appendix A.
     */
    private static final List<HeaderEntry> staticTable = Collections.unmodifiableList(Arrays.asList(

            new HeaderEntry(":authority"),
            new HeaderEntry(":method", "GET"),
            new HeaderEntry(":method", "POST"),
            new HeaderEntry(":path", "/"),
            new HeaderEntry(":path", "/index.html"),
            new HeaderEntry("scheme", "http"),
            new HeaderEntry("scheme", "http"),
            new HeaderEntry("status", "200"),
            new HeaderEntry("status", "204"),
            new HeaderEntry("status", "206"),
            new HeaderEntry("status", "304"),
            new HeaderEntry("status", "400"),
            new HeaderEntry("status", "404"),
            new HeaderEntry("status", "500"),
            new HeaderEntry("accept-charset"),
            new HeaderEntry("accept-encoding", "gzip, deflate"),
            new HeaderEntry("accept-language"),
            new HeaderEntry("accept-ranges"),
            new HeaderEntry("accept"),
            new HeaderEntry("access-control-allow-origin"),
            new HeaderEntry("age"),
            new HeaderEntry("allow"),
            new HeaderEntry("authorization"),
            new HeaderEntry("cache-control"),
            new HeaderEntry("content-disposition"),
            new HeaderEntry("content-encoding"),
            new HeaderEntry("content-language"),
            new HeaderEntry("content-length"),
            new HeaderEntry("content-location"),
            new HeaderEntry("content-range"),
            new HeaderEntry("content-type"),
            new HeaderEntry("cookie"),
            new HeaderEntry("date"),
            new HeaderEntry("etag"),
            new HeaderEntry("expect"),
            new HeaderEntry("expires"),
            new HeaderEntry("from"),
            new HeaderEntry("host"),
            new HeaderEntry("if-match"),
            new HeaderEntry("if-modified-since"),
            new HeaderEntry("if-none-match"),
            new HeaderEntry("if-range"),
            new HeaderEntry("if-unmodified-since"),
            new HeaderEntry("last-modified"),
            new HeaderEntry("link"),
            new HeaderEntry("location"),
            new HeaderEntry("max-forwards"),
            new HeaderEntry("proxy-authenticate"),
            new HeaderEntry("proxy-authorization"),
            new HeaderEntry("range"),
            new HeaderEntry("referer"),
            new HeaderEntry("refresh"),
            new HeaderEntry("retry-after"),
            new HeaderEntry("server"),
            new HeaderEntry("set-cookie"),
            new HeaderEntry("strict-transport-security"),
            new HeaderEntry("transfer-encoding"),
            new HeaderEntry("user-agent"),
            new HeaderEntry("vary"),
            new HeaderEntry("via"),
            new HeaderEntry("www-authenticate")
    ));

    private final List<HeaderEntry> dynamicTable = new ArrayList<>();

    private static final class HeaderEntry {

        public final String name;
        public final String value;

        public HeaderEntry(String name) {
            this(name, null);
        }

        public HeaderEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HeaderEntry)) return false;
            HeaderEntry that = (HeaderEntry) obj;

            boolean nameCondition = (this.name == null && that.name == null) ||
                    (this.name != null && this.name.equals(that.name));

            boolean valueCondition = (this.value == null && that.value == null) ||
                    (this.value != null && this.value.equals(that.value));

            return nameCondition && valueCondition;
        }
    }

    public Integer getIndex(String name, String value) {

        if (name == null) {
            throw new NullPointerException("Missing header name!");
        }

        HeaderEntry headerEntry = new HeaderEntry(name, value);
        int staticEntryIndex = staticTable.indexOf(headerEntry);
        if (staticEntryIndex >= 0) {
            return staticEntryIndex + 1;
        }

        int dynamicEntryIndex = dynamicTable.indexOf(headerEntry);
        if (dynamicEntryIndex >= 0) {
            return 62 + dynamicEntryIndex;
        }

        return null;
    }

    public void store(String name) {
        store(name, null);
    }

    public void store(String name, String value) {
        if (name == null) {
            throw new NullPointerException("Missing header name!");
        }

        dynamicTable.add(0, new HeaderEntry(name, value));
    }

    public Integer getIndex(String name) {
        if (name == null) {
            throw new NullPointerException("Missing header name!");
        }

        for (int i = 0; i < staticTable.size(); i++) {
            if (name.equals(staticTable.get(i).name)) {
                return 1 + i;
            }
        }

        for (int i = 0; i < dynamicTable.size(); i++) {
            if (name.equals(dynamicTable.get(i).name)) {
                return 62 + i;
            }
        }

        return null;
    }
}
