/*
 * TestMediaFiles：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：png（png处理入口）；jpeg（jpeg处理入口）；gif（gif处理入口）；bmp（bmp处理入口）。
 */

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
/**
 * 测试媒体Files，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
public final class TestMediaFiles {

    /**
     * 构造TestMediaFiles，接收本类使用的依赖或初始化参数。
     */
    private TestMediaFiles() {
    }

    // ---------- 图片 ----------

    /**
     * png处理入口。
     * 关键处理步骤调用 <code>encode</code>。
     *
     * @param width 宽度；类型为 <code>int</code>
     * @param height 高度；类型为 <code>int</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    public static byte[] png(int width, int height) throws IOException {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png");
    }

    /**
     * jpeg处理入口。
     * 关键处理步骤调用 <code>encode</code>。
     *
     * @param width 宽度；类型为 <code>int</code>
     * @param height 高度；类型为 <code>int</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    public static byte[] jpeg(int width, int height) throws IOException {
        // JPEG 不支持透明通道，必须用不含 alpha 的图像类型
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg");
    }

    /**
     * gif处理入口。
     * 关键处理步骤调用 <code>encode</code>。
     *
     * @param width 宽度；类型为 <code>int</code>
     * @param height 高度；类型为 <code>int</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    public static byte[] gif(int width, int height) throws IOException {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "gif");
    }

    /**
     * bmp处理入口。
     * 关键处理步骤调用 <code>encode</code>。
     *
     * @param width 宽度；类型为 <code>int</code>
     * @param height 高度；类型为 <code>int</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    public static byte[] bmp(int width, int height) throws IOException {
        return encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "bmp");
    }

    /**
     * 编码测试媒体Files。
     * 关键处理步骤调用 <code>ImageIO.write</code>、<code>out.toByteArray</code>。
     *
     * @param image 图片；类型为 <code>BufferedImage</code>
     * @param format 格式；类型为 <code>String</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    // VP8X 扩展容器：0-3 RIFF，8-11 WEBP，12-15 VP8X，21-23 宽-1，24-26 高-1（小端）
    /**
     * webpVp8x处理入口。
     * 关键处理步骤调用 <code>write</code>、<code>putLittleEndian24</code>。
     *
     * @param width 宽度；类型为 <code>int</code>
     * @param height 高度；类型为 <code>int</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
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
    /**
     * isoBmff处理入口。
     * 关键处理步骤调用 <code>write</code>。
     *
     * @param brand brand；类型为 <code>String</code>
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] isoBmff(String brand) {
        byte[] bytes = new byte[32];
        bytes[3] = 24;
        write(bytes, 4, "ftyp");
        write(bytes, 8, brand);
        return bytes;
    }

    /**
     * avif处理入口。
     * 关键处理步骤调用 <code>isoBmff</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] avif() {
        return isoBmff("avif");
    }

    // ---------- 文档 ----------

    /**
     * pdf处理入口。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] pdf() {
        return "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n".getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Markdown处理入口。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] markdown() {
        return "# 标题\n\n正文内容，含中文与 UTF-8 字符。\n".getBytes(StandardCharsets.UTF_8);
    }

    // 旧版 Office：OLE2 复合文档容器
    /**
     * ole2文档处理入口。
     * 关键处理步骤调用 <code>System.arraycopy</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] ole2Document() {
        byte[] bytes = new byte[512];
        byte[] magic = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
        System.arraycopy(magic, 0, bytes, 0, magic.length);
        return bytes;
    }

    // 新版 Office：OOXML 本质是 zip 容器
    /**
     * ooxml文档处理入口。
     * 关键处理步骤调用 <code>zip</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] ooxmlDocument() {
        return zip();
    }

    // ---------- 音频 ----------

    /**
     * mp3处理入口。
     * 关键处理步骤调用 <code>write</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] mp3() {
        byte[] bytes = new byte[64];
        write(bytes, 0, "ID3");
        bytes[3] = 3;
        return bytes;
    }

    /**
     * m4a处理入口。
     * 关键处理步骤调用 <code>isoBmff</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] m4a() {
        return isoBmff("M4A ");
    }

    /**
     * aac处理入口。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] aac() {
        // ADTS 同步字
        byte[] bytes = new byte[64];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xF1;
        return bytes;
    }

    /**
     * ogg处理入口。
     * 关键处理步骤调用 <code>write</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] ogg() {
        byte[] bytes = new byte[64];
        write(bytes, 0, "OggS");
        return bytes;
    }

    /**
     * wav处理入口。
     * 关键处理步骤调用 <code>write</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] wav() {
        byte[] bytes = new byte[44];
        write(bytes, 0, "RIFF");
        write(bytes, 8, "WAVE");
        return bytes;
    }

    /**
     * flac处理入口。
     * 关键处理步骤调用 <code>write</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] flac() {
        byte[] bytes = new byte[64];
        write(bytes, 0, "fLaC");
        return bytes;
    }

    // ---------- 视频 ----------

    /**
     * mp4处理入口。
     * 关键处理步骤调用 <code>isoBmff</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] mp4() {
        return isoBmff("isom");
    }

    /**
     * mov处理入口。
     * 关键处理步骤调用 <code>isoBmff</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] mov() {
        return isoBmff("qt  ");
    }

    /**
     * webm处理入口。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] webm() {
        // EBML 头，Matroska / WebM 通用
        byte[] bytes = new byte[64];
        bytes[0] = 0x1A;
        bytes[1] = 0x45;
        bytes[2] = (byte) 0xDF;
        bytes[3] = (byte) 0xA3;
        return bytes;
    }

    /**
     * avi处理入口。
     * 关键处理步骤调用 <code>write</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] avi() {
        byte[] bytes = new byte[64];
        write(bytes, 0, "RIFF");
        write(bytes, 8, "AVI ");
        return bytes;
    }

    // ---------- 压缩包 ----------

    /**
     * zip处理入口。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] zip() {
        byte[] bytes = new byte[64];
        bytes[0] = 'P';
        bytes[1] = 'K';
        bytes[2] = 0x03;
        bytes[3] = 0x04;
        return bytes;
    }

    /**
     * rar处理入口。
     * 关键处理步骤调用 <code>System.arraycopy</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] rar() {
        byte[] bytes = new byte[64];
        byte[] magic = {'R', 'a', 'r', '!', 0x1A, 0x07, 0x00};
        System.arraycopy(magic, 0, bytes, 0, magic.length);
        return bytes;
    }

    /**
     * sevenZip处理入口。
     * 关键处理步骤调用 <code>System.arraycopy</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] sevenZip() {
        byte[] bytes = new byte[64];
        byte[] magic = {'7', 'z', (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};
        System.arraycopy(magic, 0, bytes, 0, magic.length);
        return bytes;
    }

    /**
     * tar处理入口。
     * 关键处理步骤调用 <code>write</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] tar() {
        // tar 没有开头魔数，只能在偏移 257 处写 ustar 标记
        byte[] bytes = new byte[512];
        write(bytes, 257, "ustar");
        return bytes;
    }

    /**
     * gzip处理入口。
     * @return 类型为 <code>byte[]</code> 的处理结果
     */
    public static byte[] gzip() {
        byte[] bytes = new byte[64];
        bytes[0] = 0x1F;
        bytes[1] = (byte) 0x8B;
        return bytes;
    }

    // 内容与扩展名不符：扩展名叫 png，实际是 JPEG 字节
    /**
     * mislabeledPng处理入口。
     * 关键处理步骤调用 <code>jpeg</code>。
     * @return 类型为 <code>byte[]</code> 的处理结果
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    public static byte[] mislabeledPng() throws IOException {
        return jpeg(4, 4);
    }

    /**
     * 写入测试媒体Files。
     * 关键处理步骤调用 <code>ascii.getBytes</code>、<code>System.arraycopy</code>。
     *
     * @param target 目标；类型为 <code>byte[]</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>
     * @param ascii ascii；类型为 <code>String</code>
     */
    private static void write(byte[] target, int offset, String ascii) {
        byte[] source = ascii.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(source, 0, target, offset, source.length);
    }

    /**
     * putLittleEndian24处理入口。
     *
     * @param target 目标；类型为 <code>byte[]</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>
     * @param value 值；类型为 <code>int</code>
     */
    private static void putLittleEndian24(byte[] target, int offset, int value) {
        target[offset] = (byte) (value & 0xFF);
        target[offset + 1] = (byte) ((value >> 8) & 0xFF);
        target[offset + 2] = (byte) ((value >> 16) & 0xFF);
    }
}
