
package propolis.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class HuffmanEncoder {

    private static final Map<Byte, EntryValue> mapping = new HashMap<>();
    private static Map<Integer, Byte> decodingTable = new HashMap<>();
    private static BinaryTreeValue decodingTree;

    private static class EntryValue {

        public final int representation;
        public final int bitLength;

        public EntryValue(int representation, int bitLength) {
            this.representation = representation;
            this.bitLength = bitLength;
        }

        @Override
        public String toString() {
            return String.format("%d 1s, length %d", Integer.bitCount(representation), bitLength);
        }
    }

    public static class BinaryTreeValue {

        public BinaryTreeValue left;
        public BinaryTreeValue right;
        public EntryValue entry;

        public boolean isLeaf() {
            return entry != null;
        }

    }

    public static byte[] decode(byte[] data) throws IOException {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte hold = 0x00;
            int byteCursor = 0;
            int bitCursor = 0;
            while (byteCursor < data.length) {
                BinaryTreeValue value = decodingTree;

                while (!value.isLeaf()) {
                    if (bitCursor % 8 == 0) {
                        bitCursor = 0;
                        hold = data[byteCursor++];
                    }
                    boolean bitIsZero = (hold & ((1 << (7 - bitCursor++)))) == 0;
                    value = bitIsZero ? value.left : value.right;
                }

                baos.write(decodingTable.get(value.entry.representation));
            }

            baos.flush();
            return baos.toByteArray();
        }
    }

    public static byte[] encode(byte[] bytes) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            encode(bytes, baos);
            return baos.toByteArray();
        }
    }

    public static void encode(byte[] bytes, OutputStream os) throws IOException {
        byte bitBuffer = 0x00;
        int holdOffset = 0;

        for (int i = 0; i < bytes.length; i++) {
            EntryValue value = mapping.get(bytes[i]);
            int bitsLeft = value.bitLength;

            while (bitsLeft > 0) {
                int availableBits = Math.min(bitsLeft, 8 - holdOffset);
                int bitString = value.representation << 32 - bitsLeft >>> 24 + holdOffset;

                bitBuffer |= bitString;
                holdOffset += availableBits;
                bitsLeft -= availableBits;
                holdOffset %= 8;

                if (holdOffset == 0) {
                    os.write(bitBuffer);
                    bitBuffer = 0x00;
                }
            }
        }

        if (holdOffset > 0) {
            bitBuffer |= (1 << (8 - holdOffset)) - 1;
            os.write(bitBuffer);
        }
    }

    static {
        mapping.put((byte) 0, new EntryValue(0x1ff8, 13));
        mapping.put((byte) 1, new EntryValue(0x7fffd8, 23));
        mapping.put((byte) 2, new EntryValue(0xfffffe2, 28));
        mapping.put((byte) 3, new EntryValue(0xfffffe3, 28));
        mapping.put((byte) 4, new EntryValue(0xfffffe4, 28));
        mapping.put((byte) 5, new EntryValue(0xfffffe5, 28));
        mapping.put((byte) 6, new EntryValue(0xfffffe6, 28));
        mapping.put((byte) 7, new EntryValue(0xfffffe7, 28));
        mapping.put((byte) 8, new EntryValue(0xfffffe8, 28));
        mapping.put((byte) 9, new EntryValue(0xffffea, 24));
        mapping.put((byte) 10, new EntryValue(0x3ffffffc, 30));
        mapping.put((byte) 11, new EntryValue(0xfffffe9, 28));
        mapping.put((byte) 12, new EntryValue(0xfffffea, 28));
        mapping.put((byte) 13, new EntryValue(0x3ffffffd, 30));
        mapping.put((byte) 14, new EntryValue(0xfffffeb, 28));
        mapping.put((byte) 15, new EntryValue(0xfffffec, 28));
        mapping.put((byte) 16, new EntryValue(0xfffffed, 28));
        mapping.put((byte) 17, new EntryValue(0xfffffee, 28));
        mapping.put((byte) 18, new EntryValue(0xfffffef, 28));
        mapping.put((byte) 19, new EntryValue(0xffffff0, 28));
        mapping.put((byte) 20, new EntryValue(0xffffff1, 28));
        mapping.put((byte) 21, new EntryValue(0xffffff2, 28));
        mapping.put((byte) 22, new EntryValue(0x3ffffffe, 30));
        mapping.put((byte) 23, new EntryValue(0xffffff3, 28));
        mapping.put((byte) 24, new EntryValue(0xffffff4, 28));
        mapping.put((byte) 25, new EntryValue(0xffffff5, 28));
        mapping.put((byte) 26, new EntryValue(0xffffff6, 28));
        mapping.put((byte) 27, new EntryValue(0xffffff7, 28));
        mapping.put((byte) 28, new EntryValue(0xffffff8, 28));
        mapping.put((byte) 29, new EntryValue(0xffffff9, 28));
        mapping.put((byte) 30, new EntryValue(0xffffffa, 28));
        mapping.put((byte) 31, new EntryValue(0xffffffb, 28));
        mapping.put((byte) 32, new EntryValue(0x14, 6));
        mapping.put((byte) 33, new EntryValue(0x3f8, 10));
        mapping.put((byte) 34, new EntryValue(0x3f9, 10));
        mapping.put((byte) 35, new EntryValue(0xffa, 12));
        mapping.put((byte) 36, new EntryValue(0x1ff9, 13));
        mapping.put((byte) 37, new EntryValue(0x15, 6));
        mapping.put((byte) 38, new EntryValue(0xf8, 8));
        mapping.put((byte) 39, new EntryValue(0x7fa, 11));
        mapping.put((byte) 40, new EntryValue(0x3fa, 10));
        mapping.put((byte) 41, new EntryValue(0x3fb, 10));
        mapping.put((byte) 42, new EntryValue(0xf9, 8));
        mapping.put((byte) 43, new EntryValue(0x7fb, 11));
        mapping.put((byte) 44, new EntryValue(0xfa, 8));
        mapping.put((byte) 45, new EntryValue(0x16, 6));
        mapping.put((byte) 46, new EntryValue(0x17, 6));
        mapping.put((byte) 47, new EntryValue(0x18, 6));
        mapping.put((byte) 48, new EntryValue(0x0, 5));
        mapping.put((byte) 49, new EntryValue(0x1, 5));
        mapping.put((byte) 50, new EntryValue(0x2, 5));
        mapping.put((byte) 51, new EntryValue(0x19, 6));
        mapping.put((byte) 52, new EntryValue(0x1a, 6));
        mapping.put((byte) 53, new EntryValue(0x1b, 6));
        mapping.put((byte) 54, new EntryValue(0x1c, 6));
        mapping.put((byte) 55, new EntryValue(0x1d, 6));
        mapping.put((byte) 56, new EntryValue(0x1e, 6));
        mapping.put((byte) 57, new EntryValue(0x1f, 6));
        mapping.put((byte) 58, new EntryValue(0x5c, 7));
        mapping.put((byte) 59, new EntryValue(0xfb, 8));
        mapping.put((byte) 60, new EntryValue(0x7ffc, 15));
        mapping.put((byte) 61, new EntryValue(0x20, 6));
        mapping.put((byte) 62, new EntryValue(0xffb, 12));
        mapping.put((byte) 63, new EntryValue(0x3fc, 10));
        mapping.put((byte) 64, new EntryValue(0x1ffa, 13));
        mapping.put((byte) 65, new EntryValue(0x21, 6)); // A
        mapping.put((byte) 66, new EntryValue(0x5d, 7));
        mapping.put((byte) 67, new EntryValue(0x5e, 7));
        mapping.put((byte) 68, new EntryValue(0x5f, 7));
        mapping.put((byte) 69, new EntryValue(0x60, 7));
        mapping.put((byte) 70, new EntryValue(0x61, 7));
        mapping.put((byte) 71, new EntryValue(0x62, 7));
        mapping.put((byte) 72, new EntryValue(0x63, 7));
        mapping.put((byte) 73, new EntryValue(0x64, 7));
        mapping.put((byte) 74, new EntryValue(0x65, 7));
        mapping.put((byte) 75, new EntryValue(0x66, 7));
        mapping.put((byte) 76, new EntryValue(0x67, 7));
        mapping.put((byte) 77, new EntryValue(0x68, 7));
        mapping.put((byte) 78, new EntryValue(0x69, 7));
        mapping.put((byte) 79, new EntryValue(0x6a, 7));
        mapping.put((byte) 80, new EntryValue(0x6b, 7));
        mapping.put((byte) 81, new EntryValue(0x6c, 7));
        mapping.put((byte) 82, new EntryValue(0x6d, 7));
        mapping.put((byte) 83, new EntryValue(0x6e, 7));
        mapping.put((byte) 84, new EntryValue(0x6f, 7));
        mapping.put((byte) 85, new EntryValue(0x70, 7));
        mapping.put((byte) 86, new EntryValue(0x71, 7));
        mapping.put((byte) 87, new EntryValue(0x72, 7));
        mapping.put((byte) 88, new EntryValue(0xfc, 8));
        mapping.put((byte) 89, new EntryValue(0x73, 7));
        mapping.put((byte) 90, new EntryValue(0xfd, 8)); // A
        mapping.put((byte) 91, new EntryValue(0x1ffb, 13));
        mapping.put((byte) 92, new EntryValue(0x7fff0, 19));
        mapping.put((byte) 93, new EntryValue(0x1ffc, 13));
        mapping.put((byte) 94, new EntryValue(0x3ffc, 14));
        mapping.put((byte) 95, new EntryValue(0x22, 6));
        mapping.put((byte) 96, new EntryValue(0x7ffd, 15));
        mapping.put((byte) 97, new EntryValue(0x3, 5)); // a
        mapping.put((byte) 98, new EntryValue(0x23, 6));
        mapping.put((byte) 99, new EntryValue(0x4, 5));
        mapping.put((byte) 100, new EntryValue(0x24, 6));
        mapping.put((byte) 101, new EntryValue(0x5, 5));
        mapping.put((byte) 102, new EntryValue(0x25, 6));
        mapping.put((byte) 103, new EntryValue(0x26, 6));
        mapping.put((byte) 104, new EntryValue(0x27, 6));
        mapping.put((byte) 105, new EntryValue(0x6, 5));
        mapping.put((byte) 106, new EntryValue(0x74, 7));
        mapping.put((byte) 107, new EntryValue(0x75, 7));
        mapping.put((byte) 108, new EntryValue(0x28, 6));
        mapping.put((byte) 109, new EntryValue(0x29, 6));
        mapping.put((byte) 110, new EntryValue(0x2a, 6));
        mapping.put((byte) 111, new EntryValue(0x7, 5));
        mapping.put((byte) 112, new EntryValue(0x2b, 6));
        mapping.put((byte) 113, new EntryValue(0x76, 7));
        mapping.put((byte) 114, new EntryValue(0x2c, 6));
        mapping.put((byte) 115, new EntryValue(0x8, 5));
        mapping.put((byte) 116, new EntryValue(0x9, 5));
        mapping.put((byte) 117, new EntryValue(0x2d, 6));
        mapping.put((byte) 118, new EntryValue(0x77, 7));
        mapping.put((byte) 119, new EntryValue(0x78, 7));
        mapping.put((byte) 120, new EntryValue(0x79, 7));
        mapping.put((byte) 121, new EntryValue(0x7a, 7));
        mapping.put((byte) 122, new EntryValue(0x7b, 7)); // z
        mapping.put((byte) 123, new EntryValue(0x7ffe, 15));
        mapping.put((byte) 124, new EntryValue(0x7fc, 11));
        mapping.put((byte) 125, new EntryValue(0x3ffd, 14));
        mapping.put((byte) 126, new EntryValue(0x1ffd, 13));
        mapping.put((byte) 127, new EntryValue(0xffffffc, 28));
        mapping.put((byte) 128, new EntryValue(0xfffe6, 20));
        mapping.put((byte) 129, new EntryValue(0x3fffd2, 22));
        mapping.put((byte) 130, new EntryValue(0xfffe7, 20));
        mapping.put((byte) 131, new EntryValue(0xfffe8, 20));
        mapping.put((byte) 132, new EntryValue(0x3fffd3, 22));
        mapping.put((byte) 133, new EntryValue(0x3fffd4, 22));
        mapping.put((byte) 134, new EntryValue(0x3fffd5, 22));
        mapping.put((byte) 135, new EntryValue(0x7fffd9, 23));
        mapping.put((byte) 136, new EntryValue(0x3fffd6, 22));
        mapping.put((byte) 137, new EntryValue(0x7fffda, 23));
        mapping.put((byte) 138, new EntryValue(0x7fffdb, 23));
        mapping.put((byte) 139, new EntryValue(0x7fffdc, 23));
        mapping.put((byte) 140, new EntryValue(0x7fffdd, 23));
        mapping.put((byte) 141, new EntryValue(0x7fffde, 23));
        mapping.put((byte) 142, new EntryValue(0xffffeb, 24));
        mapping.put((byte) 143, new EntryValue(0x7fffdf, 23));
        mapping.put((byte) 144, new EntryValue(0xffffec, 24));
        mapping.put((byte) 145, new EntryValue(0xffffed, 24));
        mapping.put((byte) 146, new EntryValue(0x3fffd7, 22));
        mapping.put((byte) 147, new EntryValue(0x7fffe0, 23));
        mapping.put((byte) 148, new EntryValue(0xffffee, 24));
        mapping.put((byte) 149, new EntryValue(0x7fffe1, 23));
        mapping.put((byte) 150, new EntryValue(0x7fffe2, 23));
        mapping.put((byte) 151, new EntryValue(0x7fffe3, 23));
        mapping.put((byte) 152, new EntryValue(0x7fffe4, 23));
        mapping.put((byte) 153, new EntryValue(0x1fffdc, 21));
        mapping.put((byte) 154, new EntryValue(0x3fffd8, 22));
        mapping.put((byte) 155, new EntryValue(0x7fffe5, 23));
        mapping.put((byte) 156, new EntryValue(0x3fffd9, 22));
        mapping.put((byte) 157, new EntryValue(0x7fffe6, 23));
        mapping.put((byte) 158, new EntryValue(0x7fffe7, 23));
        mapping.put((byte) 159, new EntryValue(0xffffef, 24));
        mapping.put((byte) 160, new EntryValue(0x3fffda, 22));
        mapping.put((byte) 161, new EntryValue(0x1fffdd, 21));
        mapping.put((byte) 162, new EntryValue(0xfffe9, 20));
        mapping.put((byte) 163, new EntryValue(0x3fffdb, 22));
        mapping.put((byte) 164, new EntryValue(0x3fffdc, 22));
        mapping.put((byte) 165, new EntryValue(0x7fffe8, 23));
        mapping.put((byte) 166, new EntryValue(0x7fffe9, 23));
        mapping.put((byte) 167, new EntryValue(0x1fffde, 21));
        mapping.put((byte) 168, new EntryValue(0x7fffea, 23));
        mapping.put((byte) 169, new EntryValue(0x3fffdd, 22));
        mapping.put((byte) 170, new EntryValue(0x3fffde, 22));
        mapping.put((byte) 171, new EntryValue(0xfffff0, 24));
        mapping.put((byte) 172, new EntryValue(0x1fffdf, 21));
        mapping.put((byte) 173, new EntryValue(0x3fffdf, 22));
        mapping.put((byte) 174, new EntryValue(0x7fffeb, 23));
        mapping.put((byte) 175, new EntryValue(0x7fffec, 23));
        mapping.put((byte) 176, new EntryValue(0x1fffe0, 21));
        mapping.put((byte) 177, new EntryValue(0x1fffe1, 21));
        mapping.put((byte) 178, new EntryValue(0x3fffe0, 22));
        mapping.put((byte) 179, new EntryValue(0x1fffe2, 21));
        mapping.put((byte) 180, new EntryValue(0x7fffed, 23));
        mapping.put((byte) 181, new EntryValue(0x3fffe1, 22));
        mapping.put((byte) 182, new EntryValue(0x7fffee, 23));
        mapping.put((byte) 183, new EntryValue(0x7fffef, 23));
        mapping.put((byte) 184, new EntryValue(0xfffea, 20));
        mapping.put((byte) 185, new EntryValue(0x3fffe2, 22));
        mapping.put((byte) 186, new EntryValue(0x3fffe3, 22));
        mapping.put((byte) 187, new EntryValue(0x3fffe4, 22));
        mapping.put((byte) 188, new EntryValue(0x7ffff0, 23));
        mapping.put((byte) 189, new EntryValue(0x3fffe5, 22));
        mapping.put((byte) 190, new EntryValue(0x3fffe6, 22));
        mapping.put((byte) 191, new EntryValue(0x7ffff1, 23));
        mapping.put((byte) 192, new EntryValue(0x3ffffe0, 26));
        mapping.put((byte) 193, new EntryValue(0x3ffffe1, 26));
        mapping.put((byte) 194, new EntryValue(0xfffeb, 20));
        mapping.put((byte) 195, new EntryValue(0x7fff1, 19));
        mapping.put((byte) 196, new EntryValue(0x3fffe7, 22));
        mapping.put((byte) 197, new EntryValue(0x7ffff2, 23));
        mapping.put((byte) 198, new EntryValue(0x3fffe8, 22));
        mapping.put((byte) 199, new EntryValue(0x1ffffec, 25));
        mapping.put((byte) 200, new EntryValue(0x3ffffe2, 26));
        mapping.put((byte) 201, new EntryValue(0x3ffffe3, 26));
        mapping.put((byte) 202, new EntryValue(0x3ffffe4, 26));
        mapping.put((byte) 203, new EntryValue(0x7ffffde, 27));
        mapping.put((byte) 204, new EntryValue(0x7ffffdf, 27));
        mapping.put((byte) 205, new EntryValue(0x3ffffe5, 26));
        mapping.put((byte) 206, new EntryValue(0xfffff1, 24));
        mapping.put((byte) 207, new EntryValue(0x1ffffed, 25));
        mapping.put((byte) 208, new EntryValue(0x7fff2, 19));
        mapping.put((byte) 209, new EntryValue(0x1fffe3, 21));
        mapping.put((byte) 210, new EntryValue(0x3ffffe6, 26));
        mapping.put((byte) 211, new EntryValue(0x7ffffe0, 27));
        mapping.put((byte) 212, new EntryValue(0x7ffffe1, 27));
        mapping.put((byte) 213, new EntryValue(0x3ffffe7, 26));
        mapping.put((byte) 214, new EntryValue(0x7ffffe2, 27));
        mapping.put((byte) 215, new EntryValue(0xfffff2, 24));
        mapping.put((byte) 216, new EntryValue(0x1fffe4, 21));
        mapping.put((byte) 217, new EntryValue(0x1fffe5, 21));
        mapping.put((byte) 218, new EntryValue(0x3ffffe8, 26));
        mapping.put((byte) 219, new EntryValue(0x3ffffe9, 26));
        mapping.put((byte) 220, new EntryValue(0xffffffd, 28));
        mapping.put((byte) 221, new EntryValue(0x7ffffe3, 27));
        mapping.put((byte) 222, new EntryValue(0x7ffffe4, 27));
        mapping.put((byte) 223, new EntryValue(0x7ffffe5, 27));
        mapping.put((byte) 224, new EntryValue(0xfffec, 20));
        mapping.put((byte) 225, new EntryValue(0xfffff3, 24));
        mapping.put((byte) 226, new EntryValue(0xfffed, 20));
        mapping.put((byte) 227, new EntryValue(0x1fffe6, 21));
        mapping.put((byte) 228, new EntryValue(0x3fffe9, 22));
        mapping.put((byte) 229, new EntryValue(0x1fffe7, 21));
        mapping.put((byte) 230, new EntryValue(0x1fffe8, 21));
        mapping.put((byte) 231, new EntryValue(0x7ffff3, 23));
        mapping.put((byte) 232, new EntryValue(0x3fffea, 22));
        mapping.put((byte) 233, new EntryValue(0x3fffeb, 22));
        mapping.put((byte) 234, new EntryValue(0x1ffffee, 25));
        mapping.put((byte) 235, new EntryValue(0x1ffffef, 25));
        mapping.put((byte) 236, new EntryValue(0xfffff4, 24));
        mapping.put((byte) 237, new EntryValue(0xfffff5, 24));
        mapping.put((byte) 238, new EntryValue(0x3ffffea, 26));
        mapping.put((byte) 239, new EntryValue(0x7ffff4, 23));
        mapping.put((byte) 240, new EntryValue(0x3ffffeb, 26));
        mapping.put((byte) 241, new EntryValue(0x7ffffe6, 27));
        mapping.put((byte) 242, new EntryValue(0x3ffffec, 26));
        mapping.put((byte) 243, new EntryValue(0x3ffffed, 26));
        mapping.put((byte) 244, new EntryValue(0x7ffffe7, 27));
        mapping.put((byte) 245, new EntryValue(0x7ffffe8, 27));
        mapping.put((byte) 246, new EntryValue(0x7ffffe9, 27));
        mapping.put((byte) 247, new EntryValue(0x7ffffea, 27));
        mapping.put((byte) 248, new EntryValue(0x7ffffeb, 27));
        mapping.put((byte) 249, new EntryValue(0xffffffe, 28));
        mapping.put((byte) 250, new EntryValue(0x7ffffec, 27));
        mapping.put((byte) 251, new EntryValue(0x7ffffed, 27));
        mapping.put((byte) 252, new EntryValue(0x7ffffee, 27));
        mapping.put((byte) 253, new EntryValue(0x7ffffef, 27));
        mapping.put((byte) 254, new EntryValue(0x7fffff0, 27));
        mapping.put((byte) 255, new EntryValue(0x3ffffee, 26));

        // Setup the decoding information.
        for (int i = 0; i < 256; i++) {
            EntryValue entryValue = mapping.get((byte) i);
            decodingTable.put(entryValue.representation, (byte) i);
        }

        // Setup a tree for decoding.
        decodingTree = new BinaryTreeValue();
        for (int i = 0; i < 256; i++) {
            EntryValue entryValue = mapping.get((byte) i);
            BinaryTreeValue node = decodingTree;
            for (int j=entryValue.bitLength - 1; j>=0; j--) {
                if (((entryValue.representation >> j) & 1) == 0) {
                    if (node.left == null) {
                        node.left = new BinaryTreeValue();
                    }

                    node = node.left;
                } else {
                    if (node.right == null) {
                        node.right = new BinaryTreeValue();
                    }

                    node = node.right;
                }

            }
            node.entry = entryValue;
        }
    }
}
