package com.starrainnotes.media.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal WebP container header reader: validates RIFF/WEBP magic and extracts
 * canvas dimensions from VP8 / VP8L / VP8X chunks. Only the header is parsed —
 * this is signature/decodability validation, not an image processing pipeline.
 *
 * <p>Layout: 0-3 "RIFF", 4-7 size, 8-11 "WEBP", 12-15 chunk tag,
 * 16-19 chunk size, then chunk payload.</p>
 */
final class WebpHeaderReader {

    private WebpHeaderReader() {
    }

    static int[] readDimensions(InputStream in) throws IOException {
        byte[] header = new byte[30];
        int read = in.readNBytes(header, 0, header.length);
        if (read < 12) {
            throw new IOException("truncated webp header");
        }
        if (!"RIFF".equals(asAscii(header, 0, 4)) || !"WEBP".equals(asAscii(header, 8, 4))) {
            throw new IOException("not a webp file");
        }
        String chunk = asAscii(header, 12, 4);
        return switch (chunk) {
            case "VP8 " -> vp8(header);
            case "VP8L" -> vp8l(header);
            case "VP8X" -> vp8x(header);
            default -> throw new IOException("unsupported webp chunk: " + chunk);
        };
    }

    private static int[] vp8(byte[] h) throws IOException {
        // VP8 lossy: frame tag at 20-22, then 2-byte LE width and height (14 bits)
        if ((h[20] & 0xFF) != 0x9D || (h[21] & 0xFF) != 0x01 || (h[22] & 0xFF) != 0x2A) {
            throw new IOException("invalid VP8 frame tag");
        }
        int width = ((h[23] & 0xFF) | ((h[24] & 0xFF) << 8)) & 0x3FFF;
        int height = ((h[25] & 0xFF) | ((h[26] & 0xFF) << 8)) & 0x3FFF;
        return new int[]{width, height};
    }

    private static int[] vp8l(byte[] h) throws IOException {
        // VP8L lossless: signature 0x2F at 20, then 4 bytes LE:
        // bits 0-13 = width-1, bits 14-27 = height-1
        if ((h[20] & 0xFF) != 0x2F) {
            throw new IOException("invalid VP8L signature");
        }
        int value = (h[21] & 0xFF)
                | ((h[22] & 0xFF) << 8)
                | ((h[23] & 0xFF) << 16)
                | ((h[24] & 0xFF) << 24);
        int width = (value & 0x3FFF) + 1;
        int height = ((value >> 14) & 0x3FFF) + 1;
        return new int[]{width, height};
    }

    private static int[] vp8x(byte[] h) throws IOException {
        // VP8X extended: flags at 20, canvas width-1 at 21-23, height-1 at 24-26 (LE)
        int width = (h[21] & 0xFF) | ((h[22] & 0xFF) << 8) | ((h[23] & 0xFF) << 16);
        int height = (h[24] & 0xFF) | ((h[25] & 0xFF) << 8) | ((h[26] & 0xFF) << 16);
        return new int[]{width + 1, height + 1};
    }

    private static String asAscii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }
}
