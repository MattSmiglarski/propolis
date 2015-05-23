package test;

import org.junit.Assert;
import org.junit.Test;
import propolis.server.HuffmanEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HuffmanTests {

    private HuffmanEncoder huffmanEncoder = new HuffmanEncoder();

    @Test
    public void shouldEncodeSampleString() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        huffmanEncoder.encode("www.example.com".getBytes(), os);
        Assert.assertArrayEquals(new byte[]{
                (byte) 0xf1, (byte) 0xe3, (byte) 0xc2, (byte) 0xe5,
                (byte) 0xf2, (byte) 0x3a, (byte) 0x6b, (byte) 0xa0,
                (byte) 0xab, (byte) 0x90, (byte) 0xf4, (byte) 0xff
        }, os.toByteArray());
    }

    @Test
    public void encodeDecodeEverything() throws IOException {
        byte[] testBytes = new byte[256];
        for (int i=0; i<256; i++) {
            testBytes[i] = (byte) i;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        huffmanEncoder.encode(testBytes, baos);
        byte[] encoding = baos.toByteArray();
        baos.close();
        byte[] decoded = huffmanEncoder.decode(encoding);
        Assert.assertArrayEquals(testBytes, decoded);
    }
}
