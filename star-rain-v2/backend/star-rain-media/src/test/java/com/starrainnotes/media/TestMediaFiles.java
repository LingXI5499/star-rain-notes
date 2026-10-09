package com.starrainnotes.media;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

/*
 * 测试用的真实文件字节构造器。
 *
 * 图片用 ImageIO 真实编码，保证 magic bytes 与内容都真实，而不是拼几个字节糊弄校验。
 * WebP / AVIF 与各类容器格式 JDK 没有编码器，按各格式的规范头部手工构造。
 */
public final class TestMediaFiles {

    private TestMediaFiles() {
    }

    // ---------- 图片 ----------

    public static byte[] png(int width, int height) throws IOException {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png");
    }

    public static byte[] jpeg(int width, int height) throws IOException {
        // JPEG 不支持透明通道，必须用不含 alpha 的图像类型
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg");
    }

    public static byte[] gif(int width, int height) throws IOException {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "gif");
    }

    public static byte[] bmp(int width, int height) throws IOException {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "bmp");
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    // VP8X 扩展容器：0-3 RIFF，8-11 WEBP，12-15 VP8X，21-23 宽-1，24-26 高-1（小端）
    public static byte[] webpVp8x(int width, int height) {
        byte[] bytes = new byte[30];
        write(bytes, 0, "RIFF");
        write(bytes, 8, "WEBP");
        write(bytes, 12, "VP8X");
        bytes[16] = 10;
        putLittleEndian24(bytes, 21, width - 1);
        putLittleEndian24(bytes, 24, height - 1);
        return bytes;
    }

    // ISO BMFF：4-7 ftyp，8-11 主品牌
    public static byte[] isoBmff(String brand) {
        byte[] bytes = new byte[32];
        bytes[3] = 24;
        write(bytes, 4, "ftyp");
        write(bytes, 8, brand);
        return bytes;
    }

    public static byte[] avif() {
        return isoBmff("avif");
    }

    // ---------- 文档 ----------

    public static byte[] pdf() {
        return "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n".getBytes(StandardCharsets.US_ASCII);
    }

    public static byte[] markdown() {
        return "# 标题\n\n正文内容，含中文与 UTF-8 字符。\n".getBytes(StandardCharsets.UTF_8);
    }

    // 旧版 Office：OLE2 复合文档容器
    public static byte[] ole2Document() {
        byte[] bytes = new byte[512];
        byte[] magic = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
        System.arraycopy(magic, 0, bytes, 0, magic.length);
        return bytes;
    }

    // 新版 Office：OOXML 本质是 zip 容器
    public static byte[] ooxmlDocument() {
        return zip();
    }

    // ---------- 音频 ----------

    public static byte[] mp3() {
        byte[] bytes = new byte[64];
        write(bytes, 0, "ID3");
        bytes[3] = 3;
        return bytes;
    }

    public static byte[] m4a() {
        return isoBmff("M4A ");
    }

    public static byte[] aac() {
        // ADTS 同步字
        byte[] bytes = new byte[64];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xF1;
        return bytes;
    }

    public static byte[] ogg() {
        byte[] bytes = new byte[64];
        write(bytes, 0, "OggS");
        return bytes;
    }

    public static byte[] wav() {
        byte[] bytes = new byte[44];
        write(bytes, 0, "RIFF");
        write(bytes, 8, "WAVE");
        return bytes;
    }

    public static byte[] flac() {
        byte[] bytes = new byte[64];
        write(bytes, 0, "fLaC");
        return bytes;
    }

    // ---------- 视频 ----------

    public static byte[] mp4() {
        return isoBmff("isom");
    }

    public static byte[] mov() {
        return isoBmff("qt  ");
    }

    public static byte[] webm() {
        // EBML 头，Matroska / WebM 通用
        byte[] bytes = new byte[64];
        bytes[0] = 0x1A;
        bytes[1] = 0x45;
        bytes[2] = (byte) 0xDF;
        bytes[3] = (byte) 0xA3;
        return bytes;
    }

    public static byte[] avi() {
        byte[] bytes = new byte[64];
        write(bytes, 0, "RIFF");
        write(bytes, 8, "AVI ");
        return bytes;
    }

    // ---------- 压缩包 ----------

    public static byte[] zip() {
        byte[] bytes = new byte[64];
        bytes[0] = 'P';
        bytes[1] = 'K';
        bytes[2] = 0x03;
        bytes[3] = 0x04;
        return bytes;
    }

    public static byte[] rar() {
        byte[] bytes = new byte[64];
        byte[] magic = {'R', 'a', 'r', '!', 0x1A, 0x07, 0x00};
        System.arraycopy(magic, 0, bytes, 0, magic.length);
        return bytes;
    }

    public static byte[] sevenZip() {
        byte[] bytes = new byte[64];
        byte[] magic = {'7', 'z', (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};
        System.arraycopy(magic, 0, bytes, 0, magic.length);
        return bytes;
    }

    public static byte[] tar() {
        // tar 没有开头魔数，只能在偏移 257 处写 ustar 标记
        byte[] bytes = new byte[512];
        write(bytes, 257, "ustar");
        return bytes;
    }

    public static byte[] gzip() {
        byte[] bytes = new byte[64];
        bytes[0] = 0x1F;
        bytes[1] = (byte) 0x8B;
        return bytes;
    }

    // 内容与扩展名不符：扩展名叫 png，实际是 JPEG 字节
    public static byte[] mislabeledPng() throws IOException {
        return jpeg(4, 4);
    }

    private static void write(byte[] target, int offset, String ascii) {
        byte[] source = ascii.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(source, 0, target, offset, source.length);
    }

    private static void putLittleEndian24(byte[] target, int offset, int value) {
        target[offset] = (byte) (value & 0xFF);
        target[offset + 1] = (byte) ((value >> 8) & 0xFF);
        target[offset + 2] = (byte) ((value >> 16) & 0xFF);
    }
}
