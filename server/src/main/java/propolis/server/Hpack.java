package propolis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TODO: Separate encoding from decoding.
 */
public class Hpack {

    private static Logger log = LoggerFactory.getLogger(Hpack.class);

    public static final byte LITERAL_NEVER_INDEXED = (byte) 0x10;

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

    private byte[] huffmanEncodedLiteral(String literal) {
        try {
            return HuffmanEncoder.encode(literal.getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new RuntimeException("Unhandled failure!", e);
        }
    }

    public byte[] encodeLiteralHeaderFieldWithoutIndexing(String name, String value) {
        Integer nameIndex = headerIndex.getIndex(name);
        byte[] valueBytes = value.getBytes(Charset.defaultCharset());

        if (huffmanEncoding) {
            valueBytes = huffmanEncodedLiteral(value);
        }

        return ByteBuffer
                .allocate(2 + valueBytes.length)
                .put((byte) nameIndex.intValue())
                .put((byte) valueBytes.length)
                .put(valueBytes)
                .array();
    }

    public byte[] encodeLiteralHeaderFieldNeverIndexed(String name, String value) {

        byte[] nameBytes;
        byte[] valueBytes;
        if (huffmanEncoding) {
            nameBytes = huffmanEncodedLiteral(name);
            valueBytes = huffmanEncodedLiteral(value);
        } else {
            nameBytes = name.getBytes(Charset.defaultCharset());
            valueBytes = value.getBytes(Charset.defaultCharset());
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
            byte[] valueBytes;
            if (huffmanEncoding) {
                valueBytes = huffmanEncodedLiteral(value);
            } else {
                valueBytes = value.getBytes(Charset.defaultCharset());
            }

            headerIndex.store(name, value);

            return ByteBuffer
                    .allocate(2 + valueBytes.length)
                    .put((byte) (64 | index))
                    .put((byte) ((huffmanEncoding ? 128 : 0) | valueBytes.length))
                    .put(valueBytes)
                    .array();
        } else {
            // Return the name encoding, and the value encoding.
            byte[] nameBytes;
            byte[] valueBytes;
            if (huffmanEncoding) {
                nameBytes = huffmanEncodedLiteral(name);
                valueBytes = huffmanEncodedLiteral(value);
            } else {
                nameBytes = name.getBytes(Charset.defaultCharset());
                valueBytes = value.getBytes(Charset.defaultCharset());
            }

            headerIndex.store(name, value);

            return ByteBuffer
                    .allocate(3 + nameBytes.length + valueBytes.length)
                    .put((byte) 64) // Literal indexed.
                    .put((byte) ((huffmanEncoding ? 128 : 0) | nameBytes.length))
                    .put(nameBytes)
                    .put((byte) ((huffmanEncoding ? 128 : 0) | valueBytes.length))
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

    public HeaderIndex.HeaderEntry decodeHeader(InputStream input) throws IOException {

        int b = input.read();

        if ((b & 0b1000_0000) != 0) { // First bit.
            /*
             * [HPACK] 6.1.  Indexed Header Field Representation
             */

            int index = 0b0111_1111 & b;
            return headerIndex.get(index);

        } else if ((b & 0b0100_0000) != 0) { // Second bit.
            /*
             * 6.2.1.  Literal Header Field with Incremental Indexing
             */

            int index = 0b0011_1111 & b;
            String headerName;
            if (index == 0) {
                headerName = decodeLiteral(input);
            } else {
                headerName = headerIndex.get(index).name;
            }
            String headerValue = decodeLiteral(input);

            HeaderIndex.HeaderEntry entry = new HeaderIndex.HeaderEntry(headerName, headerValue);
            headerIndex.store(entry);
            return entry;

        } else if ((b & 0b0010_0000) != 0) { // Third bit.
            /*
             * 6.3.  Dynamic Table Size Update.
             */
            int maxSize = 0b0001_1111 & b;
            headerIndex.updateMaximumDynamicSize(maxSize);
            return null;

        } else if ((b & 0b0001_0000) != 0) { // Fourth bit.
            /*
             * [HPACK] 6.2.3.  Literal Header Field Never Indexed
             *
             * This is the same as "Literal Header Field without Indexing" below, with the spec adding the qualification:
             * "Intermediaries MUST use the same representation for encoding this header field."
             */

            int index = 0b0000_1111 & b;
            if (index == 0) {
                return new HeaderIndex.HeaderEntry(
                        decodeLiteral(input),
                        decodeLiteral(input));
            } else {
                return new HeaderIndex.HeaderEntry(
                        headerIndex.get(index).name,
                        decodeLiteral(input));
            }

        } else { // No bits.
            /*
             * [HPACK] 6.2.2.  Literal Header Field without Indexing
             */

            int index = 0b0000_1111 & b;
            if (index == 0) {
                return new HeaderIndex.HeaderEntry(
                        decodeLiteral(input),
                        decodeLiteral(input)
                );
            } else {
                return new HeaderIndex.HeaderEntry(
                        headerIndex.get(index).name,
                        decodeLiteral(input)
                );
            }
        }
    }

    /**
     * String decode a Huffman flagged length byte, followed by the (potentially Huffman encoded) value of that length.
     *
     *    +---+---+-----------------------+
     *    | H |     Value Length (7+)     |
     *    +---+---------------------------+
     *
     * @param input The input stream, containing sufficient bytes to read off the value.
     * @return The value as a Java String.
     * @throws IOException Upon error reading from the underlying stream.
     */
    private String decodeLiteral(InputStream input) throws IOException {
        int b = input.read();
        boolean huffmanEncoded = (0b1000_0000 & b) != 0;
        int length = 0b0111_1111 & b;
        byte[] value = new byte[length];
        input.read(value);
        byte[] bytes = huffmanEncoded ? HuffmanEncoder.decode(value) : value;
        return new String(bytes);
    }

    public LinkedHashMap<String, String> decodeHeaderList(byte[] encodedHeaders) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(encodedHeaders);
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        while (input.available() > 0) {
            HeaderIndex.HeaderEntry header = decodeHeader(input);
            headers.put(header.name, header.value);
        }
        return headers;
    }

    /**
     * Flag used when encoding, to decide whether to apply Huffman encoding to literals.
     * NB: This is not used when decoding.
     *
     * @param huffmanEncoding
     */
    public void setHuffmanEncoding(boolean huffmanEncoding) {
        this.huffmanEncoding = huffmanEncoding;
    }
}
