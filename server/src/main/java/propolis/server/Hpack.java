package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Hpack {

    private static Logger log = LoggerFactory.getLogger(Hpack.class);

    /**
     * Number encoding.
     *
     * @param n The value to be encoded.
     * @param prefixBits The number of bits in the prefix, no greater than 8.
     * @return A byte array containing the encoding.
     */
    public byte[] encode(long n, int prefixBits) {

        if (prefixBits > 8) throw new UnsupportedOperationException();

        if (n < (1 << prefixBits)) {
            // Since n will fit in the prefix bits, simply return the relevant byte.
            return new byte[] {
                    ByteBuffer.allocate(4)
                            .putInt((int) n)
                            .asReadOnlyBuffer().get(3)
            };
        }

        byte prefix = (byte) ((1 << prefixBits) - 1); // eg. 00011111, when prefixBits=5.
        n -= prefix;

        // Allocate the buffer, with a byte for each 7 bits.
        int i=0;
        do { /* nothing */ } while (n >> (7 * i++) > 1 << 7); // Count the required bytes.
        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + i) // The prefix byte and the rest.
                .put(prefix); // Write the prefix byte.

        // Write the middle bytes, which consist of a continuation flag followed by 7 bits.
        while (n >= (1 << 7)) {
            byteBuffer.put((byte) (
                    (1 << 7) |           /* Continuation flag with... */
                    (n & ((1 << 7) - 1)) /* ...the next 7 bits. */
            ));
            n >>= 7; // Divide by 128.
        }

        // Write the last byte (which fits in 7 bits) and return the whole thing.
        return byteBuffer.put(
                ByteBuffer.allocate(4)
                        .putInt((int) n)
                        .asReadOnlyBuffer().get(3)
        ).array();
    }

    // encode string literal
    // huffman encoding
    // static table
    // dynamic table

    static class DynamicTable {}

    private Map<Integer, StaticTableValue> staticTable = new HashMap<>(64);

    static class StaticTableValue {

        String key;
        String value;
    }
    /**
     * Static table
     *
     *
     | 1     | :authority                  |               |
     | 2     | :method                     | GET           |
     | 3     | :method                     | POST          |
     | 4     | :path                       | /             |
     | 5     | :path                       | /index.html   |
     | 6     | :scheme                     | http          |
     | 7     | :scheme                     | https         |
     | 8     | :status                     | 200           |
     | 9     | :status                     | 204           |
     | 10    | :status                     | 206           |
     | 11    | :status                     | 304           |
     | 12    | :status                     | 400           |
     | 13    | :status                     | 404           |
     | 14    | :status                     | 500           |
     | 15    | accept-charset              |               |
     | 16    | accept-encoding             | gzip, deflate |
     | 17    | accept-language             |               |
     | 18    | accept-ranges               |               |
     | 19    | accept                      |               |
     | 20    | access-control-allow-origin |               |
     | 21    | age                         |               |
     | 22    | allow                       |               |
     | 23    | authorization               |               |
     | 24    | cache-control               |               |
     | 25    | content-disposition         |               |
     | 26    | content-encoding            |               |
     | 27    | content-language            |               |
     | 28    | content-length              |               |
     | 29    | content-location            |               |
     | 30    | content-range               |               |
     | 31    | content-type                |               |
     | 32    | cookie                      |               |
     | 33    | date                        |               |
     | 34    | etag                        |               |
     | 35    | expect                      |               |
     | 36    | expires                     |               |
     | 37    | from                        |               |
     | 38    | host                        |               |
     | 39    | if-match                    |               |
     | 40    | if-modified-since           |               |
     | 41    | if-none-match               |               |
     | 42    | if-range                    |               |
     | 43    | if-unmodified-since         |               |
     | 44    | last-modified               |               |
     | 45    | link                        |               |
     | 46    | location                    |               |
     | 47    | max-forwards                |               |
     | 48    | proxy-authenticate          |               |
     | 49    | proxy-authorization         |               |
     | 50    | range                       |               |
     | 51    | referer                     |               |
     | 52    | refresh                     |               |
     | 53    | retry-after                 |               |
     | 54    | server                      |               |
     | 55    | set-cookie                  |               |
     | 56    | strict-transport-security   |               |
     | 57    | transfer-encoding           |               |
     | 58    | user-agent                  |               |
     | 59    | vary                        |               |
     | 60    | via                         |               |
     | 61    | www-authenticate            |               |
     */
}
