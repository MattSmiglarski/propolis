package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

public class Hpack {

    private static Logger log = LoggerFactory.getLogger(Hpack.class);

    public static final byte LITERAL_NEVER_INDEXED = (byte) 0x10;
    public static final byte LITERAL_INDEXED = (byte) 0x40;
    public static final byte LITERAL_NAME = (byte) 0x0a;

    private HeaderIndex headerIndex = new HeaderIndex();
    private boolean huffmanEncoding;

    public Hpack() {
        this(false);
    }

    public Hpack(boolean huffmanEncoding) {
        this.setHuffmanEncoding(huffmanEncoding);
    }

    /**
     * Byte encode a number.
     *
     * @param n The value to be encoded.
     * @param prefixBits The number of bits in the prefix, but no greater than 8.
     * @return A new byte array containing the encoding.
     */
    public byte[] encode(final long n, final int prefixBits) {

        // TODO: Check prefix bits is positive.
        // TODO: Find out if the spec supports multibyte prefixes.
        if (prefixBits > 8) throw new UnsupportedOperationException("Multibyte prefixes are not supported.");

        if (n < (1 << prefixBits)) {
            // Since remainder will fit in the prefix bits, simply return the relevant byte.
            return new byte[] {
                    ByteBuffer
                            .allocate(4)
                            .putInt((int) n)
                            .asReadOnlyBuffer().get(3)
            };
        }

        byte prefix = (byte) ((1 << prefixBits) - 1); // eg. 00011111, when prefixBits=5.
        long remainder = n - prefix; // Assign to a new variable for readibility.

        // Count the continuation bytes.
        // This is the number of 7 bit non-terminating blocks after subtracting the prefix..
        int continuationBytes=0;
        while (remainder >> (7 * continuationBytes) > 128) {
            continuationBytes++;
        }

        // Allocate
        ByteBuffer byteBuffer = ByteBuffer
                .allocate(1 + continuationBytes + 1) // Account for the prefix byte, continuation bytes, and the final byte.
                .put(prefix); // Write the prefix byte.

        // Write the middle bytes, which consist of a continuation flag followed by 7 bits.
        while (remainder >= (1 << 7)) {
            byteBuffer.put((byte) (
                    (1 << 7) |           /* Continuation flag OR-ed with... */
                    (remainder & ((1 << 7) - 1)) /* ...the next 7 bits of the remainder. */
            ));
            remainder >>= 7; // Divide by 128.
        }

        return byteBuffer
                // Write the last non-continuation byte (which fits in 7 bits).
                .put((byte) remainder)
                //  Return the whole thing.
                .array();
    }

    /**
     * Decode a byte representation of a number.
     *
     * @param bis The input containing the number.
     * @param prefixBits An encoding setting.
     * @return The decoded number.
     */
    public long decode(ByteArrayInputStream bis, int prefixBits) {
        // TODO: Check the following on page 12 of the spec is correct w.r.t the exclusive comparison: if I < 2^N - 1, return I

        int value = bis.read() & ((1 << prefixBits) - 1); // What comes before the prefix is not being trusted.
        if (value >= (1 << prefixBits) - 1) {
            int i = 0;

            // Subsequent bytes are a 7-bit encoding, plus a continuation flag.
            // Terminate the loop when the flag becomes unset.
            int b;
            do {
                b = bis.read();
                int increment = (b & 127) << (7 * i);
                value += increment;
                i++; // Next time, multiply the increment by another 128.
            } while ((b & 128) == 128); // Check for the continuation flag.
        }

        return value;
    }

    public long decode(byte[] encoding, int prefixBits) {
        return decode(new ByteArrayInputStream(encoding), prefixBits);
    }

    public byte[] encodeLiteralHeaderFieldWithIndexing(String name, String value) {
        byte[] nameBytes = name.getBytes(Charset.defaultCharset());
        byte[] valueBytes = value.getBytes(Charset.defaultCharset());

        if (huffmanEncoding) {
            try {
                nameBytes = new HuffmanEncoder().encode(nameBytes);
                valueBytes = new HuffmanEncoder().encode(valueBytes);
            } catch (IOException e) {
                throw new RuntimeException("Unhandled failure!", e);
            }
        }

        // TODO: Ensure the header name length fits into 1 byte.
        // TODO: Ensure the header value length fits into 1 byte.

        return ByteBuffer
                .allocate(3 + nameBytes.length + valueBytes.length)
                .put(LITERAL_INDEXED)
                .put(LITERAL_NAME)
                .put(nameBytes)
                .put((byte) valueBytes.length)
                .put(valueBytes)
                .array();
    }

    public byte[] encodeLiteralHeaderFieldWithoutIndexing(String name, String value) {
        Integer nameIndex = headerIndex.getIndex(name);
        byte[] valueBytes = value.getBytes(Charset.defaultCharset());

        if (huffmanEncoding) {
            try {
                valueBytes = new HuffmanEncoder().encode(valueBytes);
            } catch (IOException e) {
                throw new RuntimeException("Unhandled failure!", e);
            }
        }

        return ByteBuffer
                .allocate(2 + valueBytes.length)
                .put((byte) nameIndex.intValue())
                .put((byte) valueBytes.length)
                .put(valueBytes)
                .array();
    }

    public byte[] encodeLiteralHeaderFieldNeverIndexed(String name, String value) {
        byte[] nameBytes = name.getBytes(Charset.defaultCharset());
        byte[] valueBytes = value.getBytes(Charset.defaultCharset());

        if (huffmanEncoding) {
            try {
                nameBytes = new HuffmanEncoder().encode(nameBytes);
                valueBytes = new HuffmanEncoder().encode(valueBytes);
            } catch (IOException e) {
                throw new RuntimeException("Unhandled failure!", e);
            }
        }

        return ByteBuffer
                .allocate(3 + nameBytes.length + valueBytes.length)
                .put(LITERAL_NEVER_INDEXED)
                .put((byte) nameBytes.length)
                .put(nameBytes)
                .put((byte) valueBytes.length)
                .put(valueBytes)
                .array();
    }

    public byte[] encodeHeader(String name, String value) {
        // TODO: Check the name and value lengths will fit in 7 bits.

        Integer index;

        if ((index = headerIndex.getIndex(name, value)) != null) {
            // Return the name and value index.
            return new byte[] { (byte) (128 | index) };
        } else if ((index = headerIndex.getIndex(name)) != null) {
            // Return the name index, and the value encoding.
            byte[] valueBytes = value.getBytes(Charset.defaultCharset());
            if (huffmanEncoding) {
                try {
                    valueBytes = new HuffmanEncoder().encode(valueBytes);
                } catch (IOException e) {
                    throw new RuntimeException("Unhandled failure!", e);
                }
            }

            headerIndex.store(name, value);

            return ByteBuffer
                    .allocate(2 + valueBytes.length)
                    .put((byte) (64 | index))
                    .put((byte) (128 | valueBytes.length))
                    .put(valueBytes)
                    .array();
        } else {
            // Return the name encoding, and the value encoding.
            byte[] nameBytes = name.getBytes(Charset.defaultCharset());
            byte[] valueBytes = value.getBytes(Charset.defaultCharset());
            if (huffmanEncoding) {
                try {
                    nameBytes = new HuffmanEncoder().encode(nameBytes);
                    valueBytes = new HuffmanEncoder().encode(valueBytes);
                } catch (IOException e) {
                    throw new RuntimeException("Unhandled failure!", e);
                }
            }

            headerIndex.store(name, value);

            return ByteBuffer
                    .allocate(3 + nameBytes.length + valueBytes.length)
                    .put((byte) 64) // Literal indexed.
                    .put((byte) (128 | nameBytes.length))
                    .put(nameBytes)
                    .put((byte) (128 | valueBytes.length))
                    .put(valueBytes)
                    .array();
        }
    }

    public byte[] encodeHeaderList(LinkedHashMap<String, String> headerList) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (Map.Entry<String, String> entry : headerList.entrySet()) {
                byte[] encodedHeader = encodeHeader(entry.getKey(), entry.getValue());
                baos.write(encodedHeader);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unhandled exception!", e);
        }
    }

    public void setHuffmanEncoding(boolean huffmanEncoding) {
        this.huffmanEncoding = huffmanEncoding;
    }
}
