package test;

import org.junit.Assert;
import org.junit.Test;
import propolis.server.Hpack;

import java.util.Arrays;
import java.util.LinkedHashMap;

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
        assertHpackEncodingEquals(new byte[]{0b00001010}, 10, 5);
    }

    /**
     * C.1.2.  Example 2: Encoding 1337 Using a 5-Bit Prefix
     */
    @Test
    public void shouldEncode1337UsingA5BitPrefix() {
        assertHpackEncodingEquals(new byte[]{0b00011111, (byte) 0b10011010, 0b00001010}, 1337, 5);
    }

    /**
     * C.1.3.  Example 3: Encoding 42 Starting at an Octet Boundary
     */
    @Test
    public void shouldEncode42StartingAtAnOctetBinary() {
        assertHpackEncodingEquals(new byte[]{0b0010_1010}, 42, 8);
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

        // TODO: Assert the key/value is indexed.
    }

    /**
     * C.2.2.  Literal Header Field without Indexing
     */
    @Test
    public void shouldDecodeLiteralHeaderFieldWithoutIndexing() {
        byte[] expected = new byte[] {
                0x04,0x0c, 0x2f,0x73, 0x61,0x6d, 0x70,0x6c,  0x65,0x2f, 0x70,0x61, 0x74,0x68
        };

        byte[] actual = hpack.encodeLiteralHeaderFieldWithoutIndexing(":path", "/sample/path");
        Assert.assertArrayEquals(expected, actual);

        // TODO: Assert the key/value is not indexed.
    }

    /**
     * C.2.3.  Literal Header Field Never Indexed
     */
    @Test
    public void shouldDecodeLiteralHeaderFieldNeverIndexed() {
        byte[] expected = new byte[] {
                0x10,0x08, 0x70,0x61, 0x73,0x73, 0x77,0x6f,   0x72,0x64, 0x06,0x73, 0x65,0x63, 0x72,0x65,
                0x74
        };

        byte[] actual = hpack.encodeLiteralHeaderFieldNeverIndexed("password", "secret");
        Assert.assertArrayEquals(expected, actual);
    }

    /**
     * C.3.  Request Examples without Huffman Coding
     */
    @Test
    public void shouldEncodeHeaderListWithoutHuffmanEncoding() {

        // TODO: stop adding the huffman bit flag when the value is not huffman encoded.
        // C.3.1. First Request
        LinkedHashMap<String, String> firstHeaders = new LinkedHashMap<>();
        firstHeaders.put(":method", "GET");
        firstHeaders.put(":scheme", "http");
        firstHeaders.put(":path", "/");
        firstHeaders.put(":authority", "www.example.com");

        assertHpackEncodingEquals(new int[]{
                0x82, 0x86, 0x84, 0x41, 0x0f, 0x77, 0x77, 0x77, 0x2e, 0x65, 0x78, 0x61, 0x6d, 0x70, 0x6c, 0x65,
                0x2e, 0x63, 0x6f, 0x6d
        }, firstHeaders, hpack);

        /**
         * C.3.2.  Second Request
         */
        LinkedHashMap<String, String> secondHeaders = new LinkedHashMap<>();
        secondHeaders.put(":method", "GET");
        secondHeaders.put(":scheme", "http");
        secondHeaders.put(":path", "/");
        secondHeaders.put(":authority", "www.example.com");
        secondHeaders.put("cache-control", "no-cache");

        assertHpackEncodingEquals(new int[] {
                0x82,0x86, 0x84,0xbe, 0x58,0x08, 0x6e,0x6f,  0x2d,0x63, 0x61,0x63, 0x68,0x65
        }, secondHeaders, hpack);

        /**
         * C.3.3.  Third Request
         */
        LinkedHashMap<String, String> thirdHeaders = new LinkedHashMap<>();
        thirdHeaders.put(":method", "GET");
        thirdHeaders.put(":scheme", "https");
        thirdHeaders.put(":path", "/index.html");
        thirdHeaders.put(":authority", "www.example.com");
        thirdHeaders.put("custom-key", "custom-value");

        assertHpackEncodingEquals(new int[] {
                0x82,0x87, 0x85,0xbf, 0x40,0x0a, 0x63,0x75,  0x73,0x74, 0x6f,0x6d, 0x2d,0x6b, 0x65,0x79,
                0x0c,0x63, 0x75,0x73, 0x74,0x6f, 0x6d,0x2d,  0x76,0x61, 0x6c,0x75, 0x65
        }, thirdHeaders, hpack);
    }

    /**
     * C.4.  Request Examples with Huffman Coding
     *
     * Notice that this test uses the hpackHuffman instance.
     */
    @Test
    public void shouldEncodeHeaderListWithHuffmanEncoding() {

        hpack.setHuffmanEncoding(true);

        /**
         * C.4.1. First Request
         */
        LinkedHashMap<String, String> firstHeaders = new LinkedHashMap<>();
        firstHeaders.put(":method", "GET");
        firstHeaders.put(":scheme", "http");
        firstHeaders.put(":path", "/");
        firstHeaders.put(":authority", "www.example.com");

        assertHpackEncodingEquals(new int[] {
                0x82,0x86, 0x84,0x41, 0x8c,0xf1, 0xe3,0xc2,  0xe5,0xf2, 0x3a,0x6b, 0xa0,0xab, 0x90,0xf4,
                0xff
        }, firstHeaders, hpack);

        /**
         * C.4.2.  Second Request
         */
        LinkedHashMap<String, String> secondHeaders = new LinkedHashMap<>();
        secondHeaders.put(":method", "GET");
        secondHeaders.put(":scheme", "http");
        secondHeaders.put(":path", "/");
        secondHeaders.put(":authority", "www.example.com");
        secondHeaders.put("cache-control", "no-cache");

        assertHpackEncodingEquals(new int[] {
               0x82,0x86,0x84,0xbe, 0x58,0x86,0xa8,0xeb, 0x10, 0x64,0x9c,0xbf
        }, secondHeaders, hpack);

        /**
         * C.4.3.  Third Request
         */
        LinkedHashMap<String, String> thirdHeaders = new LinkedHashMap<>();
        thirdHeaders.put(":method", "GET");
        thirdHeaders.put(":scheme", "https");
        thirdHeaders.put(":path", "/index.html");
        thirdHeaders.put(":authority", "www.example.com");
        thirdHeaders.put("custom-key", "custom-value");

        assertHpackEncodingEquals(new int[] {
               0x82,0x87,0x85,0xbf, 0x40,0x88, 0x25,0xa8, 0x49,0xe9, 0x5b,0xa9, 0x7d, 0x7f,0x89, 0x25,
               0xa8, 0x49,0xe9, 0x5b,0xb8,0xe8,0xb4,0xbf
        }, thirdHeaders, hpack);
    }


    /**
     * C.5.  Response Examples without Huffman Coding
     */
    @Test
    public void shouldEncodeResponseHeaders() {

        /**
         * C.5.1.  First Response
         */
        LinkedHashMap<String, String> firstHeaders = new LinkedHashMap<>();
        firstHeaders.put(":status", "302");
        firstHeaders.put("cache-control", "private");
        firstHeaders.put("date", "Mon, 21 Oct 2013 20:13:21 GMT");
        firstHeaders.put("location", "https://www.example.com");

        assertHpackEncodingEquals(new int[] {
                0x48,0x03 ,0x33,0x30 ,0x32,0x58 ,0x07,0x70  ,0x72,0x69 ,0x76,0x61 ,0x74,0x65 ,0x61,0x1d, // H.302X.privatea.
                0x4d,0x6f ,0x6e,0x2c ,0x20,0x32 ,0x31,0x20  ,0x4f,0x63 ,0x74,0x20 ,0x32,0x30 ,0x31,0x33, // Mon, 21 Oct 2013
                0x20,0x32 ,0x30,0x3a ,0x31,0x33 ,0x3a,0x32  ,0x31,0x20 ,0x47,0x4d ,0x54,0x6e ,0x17,0x68, //  20:13:21 GMTn.h
                0x74,0x74 ,0x70,0x73 ,0x3a,0x2f ,0x2f,0x77  ,0x77,0x77 ,0x2e,0x65 ,0x78,0x61 ,0x6d,0x70, // ttps://www.examp
                0x6c,0x65 ,0x2e,0x63 ,0x6f,0x6d                                                          // le.com
        }, firstHeaders, hpack);

        /**
         * C.5.2.  Second Response
         */
        LinkedHashMap<String, String> secondHeaders = new LinkedHashMap<>();
        secondHeaders.put(":status", "307");
        secondHeaders.put("cache-control", "private");
        secondHeaders.put("date", "Mon, 21 Oct 2013 20:13:21 GMT");
        secondHeaders.put("location", "https://www.example.com");

        assertHpackEncodingEquals(new int[] {
                0x48,0x03 ,0x33,0x30 ,0x37,0xc1 ,0xc0,0xbf                     // H.307...
        }, secondHeaders, hpack);

        /**
         * C.5.3.  Third Response
         */
        LinkedHashMap<String, String> thirdHeaders = new LinkedHashMap<>();
        thirdHeaders.put(":status", "200");
        thirdHeaders.put("cache-control", "private");
        thirdHeaders.put("date", "Mon, 21 Oct 2013 20:13:22 GMT");
        thirdHeaders.put("location", "https://www.example.com");
        thirdHeaders.put("content-encoding", "gzip");
        thirdHeaders.put("set-cookie", "foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1");

        assertHpackEncodingEquals(new int[] {
                0x88,0xc1 ,0x61,0x1d ,0x4d,0x6f ,0x6e,0x2c ,0x20,0x32 ,0x31,0x20 ,0x4f,0x63 ,0x74,0x20, // ..a.Mon, 21 Oct
                0x32,0x30 ,0x31,0x33 ,0x20,0x32 ,0x30,0x3a ,0x31,0x33 ,0x3a,0x32 ,0x32,0x20 ,0x47,0x4d, // 2013 20:13:22 GM
                0x54,0xc0 ,0x5a,0x04 ,0x67,0x7a ,0x69,0x70 ,0x77,0x38 ,0x66,0x6f ,0x6f,0x3d ,0x41,0x53, // T.Z.gzipw8foo=AS
                0x44,0x4a ,0x4b,0x48 ,0x51,0x4b ,0x42,0x5a ,0x58,0x4f ,0x51,0x57 ,0x45,0x4f ,0x50,0x49, // DJKHQKBZXOQWEOPI
                0x55,0x41 ,0x58,0x51 ,0x57,0x45 ,0x4f,0x49 ,0x55,0x3b ,0x20,0x6d ,0x61,0x78 ,0x2d,0x61, // UAXQWEOIU; max-a
                0x67,0x65 ,0x3d,0x33 ,0x36,0x30 ,0x30,0x3b ,0x20,0x76 ,0x65,0x72 ,0x73,0x69 ,0x6f,0x6e, // ge=3600; version
                0x3d,0x31                                                                               // =1
        }, thirdHeaders, hpack);
    }


    /**
     * C.6.  Response Examples with Huffman Coding
     */
    @Test
    public void shouldEncodeResponseHeadersWithHuffmanEncoding() {

        hpack.setHuffmanEncoding(true);

        /**
         * C.6.1.  First Response
         */
        LinkedHashMap<String, String> firstHeaders = new LinkedHashMap<>();
        firstHeaders.put(":status", "302");
        firstHeaders.put("cache-control", "private");
        firstHeaders.put("date", "Mon, 21 Oct 2013 20:13:21 GMT");
        firstHeaders.put("location", "https://www.example.com");

        assertHpackEncodingEquals(new int[] {
                0x48,0x82 ,0x64,0x02 ,0x58,0x85 ,0xae,0xc3 ,0x77,0x1a ,0x4b,0x61 ,0x96,0xd0 ,0x7a,0xbe, // H.d.X...w.Ka..z.
                0x94,0x10 ,0x54,0xd4 ,0x44,0xa8 ,0x20,0x05 ,0x95,0x04 ,0x0b,0x81 ,0x66,0xe0 ,0x82,0xa6, // ..T.D. .....f...
                0x2d,0x1b ,0xff,0x6e ,0x91,0x9d ,0x29,0xad ,0x17,0x18 ,0x63,0xc7 ,0x8f,0x0b ,0x97,0xc8, // -..n..)...c.....
                0xe9,0xae ,0x82,0xae ,0x43,0xd3                                                         // ....C.
        }, firstHeaders, hpack);

        /**
         * C.6.2.  Second Response
         */
        LinkedHashMap<String, String> secondHeaders = new LinkedHashMap<>();
        secondHeaders.put(":status", "307");
        secondHeaders.put("cache-control", "private");
        secondHeaders.put("date", "Mon, 21 Oct 2013 20:13:21 GMT");
        secondHeaders.put("location", "https://www.example.com");

        assertHpackEncodingEquals(new int[]{
                0x48, 0x83, 0x64, 0x0e, 0xff, 0xc1, 0xc0, 0xbf                     // H.d.....
        }, secondHeaders, hpack);

        /**
         * C.6.3.  Third Response
         */
        LinkedHashMap<String, String> thirdHeaders = new LinkedHashMap<>();
        thirdHeaders.put(":status", "200");
        thirdHeaders.put("cache-control", "private");
        thirdHeaders.put("date", "Mon, 21 Oct 2013 20:13:22 GMT");
        thirdHeaders.put("location", "https://www.example.com");
        thirdHeaders.put("content-encoding", "gzip");
        thirdHeaders.put("set-cookie", "foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1");

        assertHpackEncodingEquals(new int[] {
                0x88,0xc1 ,0x61,0x96 ,0xd0,0x7a ,0xbe,0x94 ,0x10,0x54 ,0xd4,0x44 ,0xa8,0x20 ,0x05,0x95, // ..a..z...T.D. ..
                0x04,0x0b ,0x81,0x66 ,0xe0,0x84 ,0xa6,0x2d ,0x1b,0xff ,0xc0,0x5a ,0x83,0x9b ,0xd9,0xab, //...f...-...Z....
                0x77,0xad ,0x94,0xe7 ,0x82,0x1d ,0xd7,0xf2 ,0xe6,0xc7 ,0xb3,0x35 ,0xdf,0xdf ,0xcd,0x5b, // w..........5...[
                0x39,0x60 ,0xd5,0xaf ,0x27,0x08 ,0x7f,0x36 ,0x72,0xc1 ,0xab,0x27 ,0x0f,0xb5 ,0x29,0x1f, // 9`..'..6r..'..).
                0x95,0x87 ,0x31,0x60 ,0x65,0xc0 ,0x03,0xed ,0x4e,0xe5 ,0xb1,0x06 ,0x3d,0x50 ,0x07       // ..1`e...N...=P.
        }, thirdHeaders, hpack);
    }

    /**
     * Helper to test a value encodes to a known byte representation, and decodes back to the original value.
     *
     * @param expected The known byte representation.
     * @param n The test value.
     * @param prefixBits An encoding setting.
     */
    private static void assertHpackEncodingEquals(byte[] expected, int n, int prefixBits) {
        Hpack hpack = new Hpack();
        byte[] actual = hpack.encode(n, prefixBits);
        Assert.assertArrayEquals("Incorrect encoding.", expected, actual);
        Assert.assertEquals("Unexpected change in value after encoding and decoding.", n, hpack.decode(actual, prefixBits));
    }

    /**
     * Helper for assertions and formatting of failures when testing that an encoding matches what was expected.
     *
     * Since a byte is between -127 and 127, an integer outside these bounds requires a cast when converting to a byte.
     * To improve readibility in these tests it makes sense to first create an integer array and then convert it.
     *
     * @param expectedInts The expected byte representation (expressed as an integer array for readibility.)
     * @param headers The headers for the test case (as a LinkedHashMap, since ordering matters).
     * @param hpack The current hpack instance, to provide the context for the encoding.
     */
    public static void assertHpackEncodingEquals(int[] expectedInts, LinkedHashMap<String, String> headers, Hpack hpack) {
        byte[] expected = new byte[expectedInts.length];
        for (int i=0; i<expectedInts.length; i++) {
            expected[i] = (byte) expectedInts[i];
        }
        byte[] actual = hpack.encodeHeaderList(headers);
        Assert.assertArrayEquals(
                String.format("%s\n%s", Arrays.toString(expected), Arrays.toString(actual)),
                expected, actual);
    }
}