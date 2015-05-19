package test;

import org.junit.Assert;
import org.junit.Test;
import propolis.server.Frames;
import propolis.server.Hpack;

/**
 * These tests are taken from Appendix C (Examples) of RFC-7541 [1]
 * The comments indicate the subsection each test is taken from.
 *
 * [1] Peon, R. and H. Ruellan,
 * "HPACK: Header Compression for HTTP/2",
 * RFC 7541, DOI 10.17487/RFC7541,
 * May 2015,
 * <http://www.rfc-editor.org/info/rfc7541>.
 */
public class HpackRfcExampleTests {

    private Hpack hpack = new Hpack();

    /**
     * C.1.1.  Example 1: Encoding 10 Using a 5-Bit Prefix
     */
    @Test
    public void shouldEncode10Using5BitPrefix() {
        byte[] expected = new byte[] { 0b00001010 };
        byte[] actual = hpack.encode(10, 5);
        Assert.assertArrayEquals(expected, actual);
    }

    /**
     * C.1.2.  Example 2: Encoding 1337 Using a 5-Bit Prefix
     */
    @Test
    public void shouldEncode1337UsingA5BitPrefix() {
        byte[] expected = new byte[] { 0b00011111, (byte) 0b10011010, 0b00001010 };
        byte[] actual = hpack.encode(1337, 5);
        Assert.assertArrayEquals(expected, actual);
    }

    /**
     * C.1.3.  Example 3: Encoding 42 Starting at an Octet Boundary
     */
    @Test
    public void shouldEncode42StartingAtAnOctetBinary() {
        byte[] expected = new byte[] { 0b0010_1010 };
        byte[] actual = hpack.encode(42, 8);
        Assert.assertArrayEquals(expected, actual);
    }

    /**
     * C.2.1.  Literal Header Field with Indexing
     */
    @Test
    public void shouldDecodeLiteralHeaderFieldWithIndexing() {
        byte[] expected = new byte[] {
                0x40,0x0a, 0x63,0x75, 0x73,0x74, 0x6f,0x6d,  0x2d,0x6b, 0x65,0x79, 0x0d,0x63, 0x75,0x73,
                0x74,0x6f, 0x6d,0x2d, 0x68,0x65, 0x61,0x64,  0x65,0x72
        };

        byte[] actual = hpack.encodeLiteralHeaderFieldWithIndexing("custom-key", "custom-header");
        Assert.assertArrayEquals(expected, actual);
    }

    /**
     * C.2.2.  Literal Header Field without Indexing
     */
    public void shouldDecodeLiteralHeaderFieldWithoutIndexing() {
        byte[] expected = new byte[] {
                0x04,0x0c, 0x2f,0x73, 0x61,0x6d, 0x70,0x6c,  0x65,0x2f, 0x70,0x61, 0x74,0x68
        };

        byte[] actual = hpack.encodeLiteralHeaderFieldWithoutIndexing(":path", "/sample/path");
        Assert.assertArrayEquals(expected, actual);
    }

    /**
     * C.2.3.  Literal Header Field Never Indexed
     */
    public void shouldDecodeLiteralHeaderFieldNeverIndexed() {
        byte[] expected = new byte[] {
                0x10,0x08, 0x70,0x61, 0x73,0x73, 0x77,0x6f,   0x72,0x64, 0x06,0x73, 0x65,0x63, 0x72,0x65,
                0x74
        };

        byte[] actual = hpack.encodeLiteralHeaderFieldNeverIndexed("password", "secret");
        Assert.assertArrayEquals(expected, actual);
    }

    /**
     * C.3.1. First Request
     */
    public void shouldEncodeHeaderList() {

    }

    /**
     * C.3.2.  Second Request
     */
    public void shouldEncodeAnotherHeaderList() {

    }

    /**
     * C.3.3.  Third Request
     */
    public void shouldEncodeYetAnotherHeaderList() {

    }

    /**
     * C.4.1. First Request
     */
    public void shouldEncodeHeaderListWithHuffmanEncoding() {

    }

    /**
     * C.4.2.  Second Request
     */
    public void shouldEncodeAnotherHeaderListWithHuffmanEncoding() {

    }

    /**
     * C.4.3.  Third Request
     */
    public void shouldEncodeYetAnotherHeaderListWithHuffmanEncoding() {

    }

    /**
     * C.5.1.  First Response
     */
    public void shouldEncodeResponseHeaders() {

    }

    /**
     * C.5.2.  Second Response
     */
    public void shouldEncodeSomeMoreResponseHeaders() {

    }

    /**
     * C.5.3.  Third Response
     */
    public void shouldEncodeStillSomeMoreResponseHeaders() {

    }

    /**
     * C.6.1.  First Response
     */
    public void shouldEncodeResponseHeadersWithHuffmanEncoding() {

    }

    /**
     * C.6.2.  Second Response
     */
    public void shouldEncodeSomeMoreResponseHeadersWithHuffmanEncoding() {

    }

    /**
     * C.6.3.  Third Response
     */
    public void shouldEncodeStillSomeMoreResponseHeadersWithHuffmanEncoding() {

    }
}