package test;

import org.junit.Assert;
import org.junit.Test;
import propolis.server.Hpack;

/**
 * These tests are taken from Appendix C (Examples) of RFC-7541 [1]
 *
 * [1] Peon, R. and H. Ruellan,
 * "HPACK: Header Compression for HTTP/2",
 * RFC 7541, DOI 10.17487/RFC7541,
 * May 2015,
 * <http://www.rfc-editor.org/info/rfc7541>.
 */
public class HpackTests {

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
    public void shouldEncode1337UsingA5BitPrefix() {
        hpack.encode(1337, 5);
    }

    /**
     * C.1.3.  Example 3: Encoding 42 Starting at an Octet Boundary
     */
    public void shouldEncode42StartingAtAnOctetBinary() {
    }

    /**
     * C.2.1.  Literal Header Field with Indexing
     */
    public void shouldDecodeListeralHeaderFieldWithIndexing() {
    }

    /**
     * C.2.2.  Literal Header Field without Indexing
     */
    public void shouldDecodeListeralHeaderFieldWithoutIndexing() {
    }

    /**
     * C.2.3.  Literal Header Field Never Indexed
     */
    public void shouldDecodeListeralHeaderFieldNeverIndexed() {
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