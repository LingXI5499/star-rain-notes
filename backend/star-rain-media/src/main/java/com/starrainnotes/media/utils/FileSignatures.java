/*
 * FileSignatures：媒体模块的工具文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：MediaFileType。
 * 阅读入口：verify（验证文件Signatures）；verifyPlainText（验证纯文本文本）。
 */

package com.starrainnotes.media.utils;

import com.starrainnotes.media.enumeration.MediaFileType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/*
 * 真实文件内容校验：magic bytes 与图片尺寸。
 *
 * 这是“不信任扩展名、不信任浏览器声明的 Content-Type”的最终依据。
 *
 * 调用约定：
 *   图片  —— 传入完整内容（需要解析宽高），默认上限 20MB；
 *   文本  —— 用 verifyPlainText 流式校验完整内容，不能在文件头边界截断 UTF-8 字符；
 *   其他  —— 只需传入文件头（见 HEAD_BYTES），因为签名都在前若干字节内。
 *
 * 只解析头部与容器结构，不做转码、不做图像处理。
 */
/**
 * 文件Signatures，属于媒体模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class FileSignatures {

    // 文件头读取长度：要覆盖 tar 在偏移 257 的 ustar 标记，以及 WebP 的 30 字节头
    /**
     * 字节。
     */
    public static final int HEAD_BYTES = 64 * 1024;

    /**
     * PNG。
     */
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    /**
     * JPEG。
     */
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    /**
     * BMP。
     */
    private static final byte[] BMP = {'B', 'M'};
    /**
     * LE。
     */
    private static final byte[] TIFF_LE = {'I', 'I', 0x2A, 0x00};
    /**
     * BE。
     */
    private static final byte[] TIFF_BE = {'M', 'M', 0x00, 0x2A};
    /**
     * PDF。
     */
    private static final byte[] PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    /**
     * 2。
     */
    private static final byte[] OLE2 = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    /**
     * ZIP。
     */
    private static final byte[] ZIP = {'P', 'K'};
    /**
     * 3。
     */
    private static final byte[] ID3 = "ID3".getBytes(StandardCharsets.US_ASCII);
    /**
     * OGG。
     */
    private static final byte[] OGG = "OggS".getBytes(StandardCharsets.US_ASCII);
    /**
     * FLAC。
     */
    private static final byte[] FLAC = "fLaC".getBytes(StandardCharsets.US_ASCII);
    /**
     * FTYP。
     */
    private static final byte[] FTYP = "ftyp".getBytes(StandardCharsets.US_ASCII);
    /**
     * RIFF。
     */
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    /**
     * WEBP。
     */
    private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);
    /**
     * WAVE。
     */
    private static final byte[] WAVE = "WAVE".getBytes(StandardCharsets.US_ASCII);
    /**
     * AVI。
     */
    private static final byte[] AVI = {'A', 'V', 'I', ' '};
    /**
     * EBML。
     */
    private static final byte[] EBML = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3};
    /**
     * 4。
     */
    private static final byte[] RAR4 = {'R', 'a', 'r', '!', 0x1A, 0x07, 0x00};
    /**
     * 5。
     */
    private static final byte[] RAR5 = {'R', 'a', 'r', '!', 0x1A, 0x07, 0x01, 0x00};
    /**
     * Z。
     */
    private static final byte[] SEVEN_Z = {'7', 'z', (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};
    /**
     * GZIP。
     */
    private static final byte[] GZIP = {0x1F, (byte) 0x8B};
    /**
     * USTAR。
     */
    private static final byte[] USTAR = "ustar".getBytes(StandardCharsets.US_ASCII);
    /**
     * BRANDS。
     */
    private static final Set<String> AVIF_BRANDS = Set.of("avif", "avis", "mif1", "msf1");

    /**
     * 构造FileSignatures，接收本类使用的依赖或初始化参数。
     */
    private FileSignatures() {
    }

    /*
     * 校验真实内容与扩展名一致。
     *
     * @return 图片的 {width, height}；非图片、或该格式不解析尺寸时返回 null
     * @throws IOException 内容与扩展名不匹配或不可解析，由调用方转成 415
     */
    /**
     * 验证文件Signatures。
     * 主要协作调用：<code>AVIF_BRANDS.contains</code>。
     * 实现中的显式失败类型包括 <code>IOException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(!startsWith(content, TIFF_LE) &amp;&amp; !startsWith(content, TIFF_BE))</code>；<code>(content.length &lt; 12 || !matchesAt(content, 8, WEBP))</code>。
     *
     * @param type 类型；类型为 <code>MediaFileType</code>
     * @param content 内容；类型为 <code>byte[]</code>
     * @return 类型为 <code>int[]</code> 的处理结果
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    public static int[] verify(MediaFileType type, byte[] content) throws IOException {
        return switch (type) {
            // ---------- 图片 ----------
            case PNG -> {
                assertMagic(content, PNG, "PNG");
                yield readViaImageIo(content);
            }
            case JPG, JPEG -> {
                assertMagic(content, JPEG, "JPEG");
                yield readViaImageIo(content);
            }
            case GIF -> {
                assertGif(content);
                yield readViaImageIo(content);
            }
            case BMP -> {
                assertMagic(content, BMP, "BMP");
                yield readViaImageIo(content);
            }
            case TIFF, TIF -> {
                if (!startsWith(content, TIFF_LE) && !startsWith(content, TIFF_BE)) {
                    throw new IOException("TIFF signature mismatch");
                }
                yield readViaImageIo(content);
            }
            case WEBP -> {
                assertMagic(content, RIFF, "RIFF");
                if (content.length < 12 || !matchesAt(content, 8, WEBP)) {
                    throw new IOException("WEBP container mismatch");
                }
                yield readWebpDimensions(content);
            }
            case AVIF -> {
                // ISO BMFF：偏移 4 是 ftyp，主品牌标明具体家族
                assertIsoBmff(content, "AVIF");
                String brand = new String(content, 8, 4, StandardCharsets.US_ASCII);
                if (!AVIF_BRANDS.contains(brand)) {
                    throw new IOException("AVIF brand mismatch: " + brand);
                }
                yield null;
            }

            // ---------- 文档 ----------
            case PDF -> {
                assertMagic(content, PDF, "PDF");
                yield null;
            }
            case MD, MARKDOWN, TXT -> {
                assertPlainText(content);
                yield null;
            }
            case DOC, XLS, PPT -> {
                // 旧版 Office 二进制格式，OLE2 复合文档容器
                assertMagic(content, OLE2, "OLE2 (MS Office 97-2003)");
                yield null;
            }
            case DOCX, XLSX, PPTX -> {
                // 新版 Office 是 OOXML，本质是 zip 容器
                assertZip(content);
                yield null;
            }

            // ---------- 音频 ----------
            case MP3 -> {
                boolean id3 = startsWith(content, ID3);
                boolean mpegSync = content.length >= 2 && (content[0] & 0xFF) == 0xFF
                        && (content[1] & 0xE0) == 0xE0;
                if (!id3 && !mpegSync) {
                    throw new IOException("MP3 signature mismatch");
                }
                yield null;
            }
            case M4A -> {
                assertIsoBmff(content, "M4A");
                yield null;
            }
            case AAC -> {
                // ADTS：同步字 0xFFF，第二字节高 4 位为 0xF1 或 0xF9
                boolean adts = content.length >= 2 && (content[0] & 0xFF) == 0xFF
                        && ((content[1] & 0xF6) == 0xF0);
                if (!adts) {
                    throw new IOException("AAC (ADTS) signature mismatch");
                }
                yield null;
            }
            case OGG, OGA, OGV -> {
                assertMagic(content, OGG, "OGG");
                yield null;
            }
            case WAV -> {
                assertMagic(content, RIFF, "RIFF");
                if (content.length < 12 || !matchesAt(content, 8, WAVE)) {
                    throw new IOException("WAV container mismatch");
                }
                yield null;
            }
            case FLAC -> {
                assertMagic(content, FLAC, "FLAC");
                yield null;
            }

            // ---------- 视频 ----------
            case MP4, M4V, MOV -> {
                assertIsoBmff(content, "MP4/MOV");
                yield null;
            }
            case WEBM, MKV -> {
                // Matroska / WebM 同属 EBML 容器
                assertMagic(content, EBML, "EBML (Matroska/WebM)");
                yield null;
            }
            case AVI -> {
                assertMagic(content, RIFF, "RIFF");
                if (content.length < 12 || !matchesAt(content, 8, AVI)) {
                    throw new IOException("AVI container mismatch");
                }
                yield null;
            }

            // ---------- 压缩包 ----------
            case ZIP -> {
                assertZip(content);
                yield null;
            }
            case RAR -> {
                if (!startsWith(content, RAR4) && !startsWith(content, RAR5)) {
                    throw new IOException("RAR signature mismatch");
                }
                yield null;
            }
            case SEVEN_Z -> {
                assertMagic(content, SEVEN_Z, "7z");
                yield null;
            }
            case TAR -> {
                // tar 没有开头魔数，只能在偏移 257 处检查 ustar 标记
                if (!matchesAt(content, 257, USTAR)) {
                    throw new IOException("TAR (ustar) signature mismatch");
                }
                yield null;
            }
            case GZ, TGZ -> {
                assertMagic(content, GZIP, "GZIP");
                yield null;
            }
        };
    }

    // ISO BMFF 家族（mp4/mov/m4a/avif）：偏移 4 起必须是 ftyp box
    /**
     * 校验IsoBmff。
     * 关键处理步骤调用 <code>matchesAt</code>。
     * 实现中的显式失败类型包括 <code>IOException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(content.length &lt; 12 || !matchesAt(content, 4, FTYP))</code>。
     *
     * @param content 内容；类型为 <code>byte[]</code>
     * @param label 标签；类型为 <code>String</code>
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    private static void assertIsoBmff(byte[] content, String label) throws IOException {
        if (content.length < 12 || !matchesAt(content, 4, FTYP)) {
            throw new IOException(label + " (ISO BMFF) signature mismatch");
        }
    }

    // zip 及其派生容器：PK 后跟 03 04（本地文件头）、05 06（空档案）或 07 08（分卷）
    /**
     * 校验Zip。
     * 关键处理步骤调用 <code>startsWith</code>。
     * 实现中的显式失败类型包括 <code>IOException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(content.length &lt; 4 || !startsWith(content, ZIP))</code>；<code>(!ok)</code>。
     *
     * @param content 内容；类型为 <code>byte[]</code>
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    private static void assertZip(byte[] content) throws IOException {
        if (content.length < 4 || !startsWith(content, ZIP)) {
            throw new IOException("ZIP container mismatch");
        }
        int third = content[2] & 0xFF;
        int fourth = content[3] & 0xFF;
        boolean ok = (third == 0x03 && fourth == 0x04)
                || (third == 0x05 && fourth == 0x06)
                || (third == 0x07 && fourth == 0x08);
        if (!ok) {
            throw new IOException("ZIP container mismatch");
        }
    }

    // 纯文本：不能含 NUL，且必须是合法 UTF-8
    /**
     * 校验纯文本文本。
     *
     * @param content 内容；类型为 <code>byte[]</code>
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    private static void assertPlainText(byte[] content) throws IOException {
        verifyPlainText(new ByteArrayInputStream(content));
    }

    /**
     * 校验完整文本流，解码器保留跨缓冲区的字符字节；关闭时同时关闭传入的流。
     */
    public static void verifyPlainText(InputStream content) throws IOException {
        try (Reader reader = new InputStreamReader(content, StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT))) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                for (int index = 0; index < count; index++) {
                    if (buffer[index] == '\0') {
                        throw new IOException("text content contains NUL byte");
                    }
                }
            }
        }
    }

    /**
     * 校验Gif。
     * 实现中的显式失败类型包括 <code>IOException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(content.length &lt; 10)</code>；<code>(!"GIF87a".equals(header) &amp;&amp; !"GIF89a".equals(header))</code>。
     *
     * @param content 内容；类型为 <code>byte[]</code>
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    private static void assertGif(byte[] content) throws IOException {
        if (content.length < 10) {
            throw new IOException("truncated GIF header");
        }
        String header = new String(content, 0, 6, StandardCharsets.US_ASCII);
        if (!"GIF87a".equals(header) && !"GIF89a".equals(header)) {
            throw new IOException("GIF signature mismatch");
        }
    }

    // 用 ImageReader 只取宽高，不触发像素解码
    /**
     * 读取Via图片Io。
     * 关键处理步骤调用 <code>ImageIO.createImageInputStream</code>、<code>ImageIO.getImageReaders</code>、<code>readers.hasNext</code>、<code>readers.next</code>。
     * 实现中的显式失败类型包括 <code>IOException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(stream == null)</code>；<code>(!readers.hasNext())</code>。
     *
     * @param content 内容；类型为 <code>byte[]</code>
     * @return 类型为 <code>int[]</code> 的处理结果
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    private static int[] readViaImageIo(byte[] content) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (stream == null) {
                throw new IOException("image stream unavailable");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IOException("no image reader");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        }
    }

    /*
     * 只解析 VP8 / VP8L / VP8X 容器头，不做图像处理。
     * 布局：0-3 "RIFF"，8-11 "WEBP"，12-15 chunk tag，16-19 chunk size，其后是 chunk 载荷。
     */
    /**
     * 读取WebpDimensions。
     * 实现中的显式失败类型包括 <code>IOException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(h.length &lt; 30)</code>；<code>((h[20] &amp; 255) != 157 || (h[21] &amp; 255) != 1 || (h[22] &amp; 255) != 42)</code>。
     *
     * @param h h；类型为 <code>byte[]</code>
     * @return 类型为 <code>int[]</code> 的处理结果
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    private static int[] readWebpDimensions(byte[] h) throws IOException {
        if (h.length < 30) {
            throw new IOException("truncated webp header");
        }
        String chunk = new String(h, 12, 4, StandardCharsets.US_ASCII);
        return switch (chunk) {
            case "VP8 " -> {
                // 有损：帧标签后是 2 字节小端宽高（各 14 位）
                if ((h[20] & 0xFF) != 0x9D || (h[21] & 0xFF) != 0x01 || (h[22] & 0xFF) != 0x2A) {
                    throw new IOException("invalid VP8 frame tag");
                }
                int width = ((h[23] & 0xFF) | ((h[24] & 0xFF) << 8)) & 0x3FFF;
                int height = ((h[25] & 0xFF) | ((h[26] & 0xFF) << 8)) & 0x3FFF;
                yield new int[]{width, height};
            }
            case "VP8L" -> {
                // 无损：签名 0x2F，随后 4 字节小端，位 0-13 为宽-1，位 14-27 为高-1
                if ((h[20] & 0xFF) != 0x2F) {
                    throw new IOException("invalid VP8L signature");
                }
                int value = (h[21] & 0xFF) | ((h[22] & 0xFF) << 8)
                        | ((h[23] & 0xFF) << 16) | ((h[24] & 0xFF) << 24);
                yield new int[]{(value & 0x3FFF) + 1, ((value >> 14) & 0x3FFF) + 1};
            }
            case "VP8X" -> {
                // 扩展格式：画布宽-1 在 21-23，高-1 在 24-26，小端
                int width = (h[21] & 0xFF) | ((h[22] & 0xFF) << 8) | ((h[23] & 0xFF) << 16);
                int height = (h[24] & 0xFF) | ((h[25] & 0xFF) << 8) | ((h[26] & 0xFF) << 16);
                yield new int[]{width + 1, height + 1};
            }
            default -> throw new IOException("unsupported webp chunk: " + chunk);
        };
    }

    /**
     * 校验Magic。
     * 关键处理步骤调用 <code>startsWith</code>。
     * 实现中的显式失败类型包括 <code>IOException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(!startsWith(content, magic))</code>。
     *
     * @param content 内容；类型为 <code>byte[]</code>
     * @param magic magic；类型为 <code>byte[]</code>
     * @param label 标签；类型为 <code>String</code>
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    private static void assertMagic(byte[] content, byte[] magic, String label) throws IOException {
        if (!startsWith(content, magic)) {
            throw new IOException(label + " signature mismatch");
        }
    }

    /**
     * starts包含处理入口。
     * 关键处理步骤调用 <code>matchesAt</code>。
     *
     * @param content 内容；类型为 <code>byte[]</code>
     * @param magic magic；类型为 <code>byte[]</code>
     * @return 本方法条件是否成立的判断结果
     */
    private static boolean startsWith(byte[] content, byte[] magic) {
        return matchesAt(content, 0, magic);
    }

    /**
     * 判断是否匹配时间。
     * 边界判断包含 <code>(content.length &lt; offset + magic.length)</code>；<code>(content[offset + i] != magic[i])</code>。
     *
     * @param content 内容；类型为 <code>byte[]</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>
     * @param magic magic；类型为 <code>byte[]</code>
     * @return 本方法条件是否成立的判断结果
     */
    private static boolean matchesAt(byte[] content, int offset, byte[] magic) {
        if (content.length < offset + magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (content[offset + i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
