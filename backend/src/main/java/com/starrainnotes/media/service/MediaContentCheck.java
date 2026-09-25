package com.starrainnotes.media.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.api.PrototypeArchivePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Extension, declared MIME, and magic-byte checks for an uploaded media file. */
final class MediaContentCheck {
    private static final long IMAGE_MAX_BYTES = 10L * 1024 * 1024;
    private static final long PDF_MAX_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf",
            "mp3", "m4a", "ogg", "zip");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "m4a", "ogg");
    private static final Map<String, String> EXT_MIME = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("m4a", "audio/mp4"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("zip", "application/zip"));
    private static final Map<String, Set<String>> EXT_ACCEPTED_MIME = Map.of(
            "mp3", Set.of("audio/mpeg"),
            "m4a", Set.of("audio/mp4", "audio/x-m4a"),
            "ogg", Set.of("audio/ogg"),
            "zip", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream"));

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ID3_MAGIC = "ID3".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] OGG_MAGIC = "OggS".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] M4A_FTYP = "ftyp".getBytes(StandardCharsets.US_ASCII);

    private MediaContentCheck() {
    }

    static CheckedUpload inspect(MultipartFile file, long audioMaxBytes) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE",
                    "Empty file", "The uploaded file is empty.");
        }
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                    "Unsupported file type", "Allowed types: jpg, jpeg, png, webp, pdf, mp3, m4a, ogg.");
        }
        boolean image = IMAGE_EXTENSIONS.contains(extension);
        boolean audio = AUDIO_EXTENSIONS.contains(extension);
        boolean archive = "zip".equals(extension);
        long max = archive ? PrototypeArchivePolicy.MAX_BYTES : (image ? IMAGE_MAX_BYTES : (audio ? audioMaxBytes : PDF_MAX_BYTES));
        if (file.getSize() > max) {
            String unit = audio ? audioMaxBytes / (1024 * 1024) + "MB" : (archive ? "25MB" : (image ? "10MB" : "20MB"));
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    "File too large", "Media must be ≤ " + unit + ".");
        }
        String declaredMime = file.getContentType();
        if (declaredMime == null || !declaredMimeMatches(extension, declaredMime)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                    "MIME mismatch", "The declared content type does not match the file extension.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                    "Unreadable file", "The uploaded file could not be read.");
        }
        int[] dimensions = null;
        if (!archive) {
            try {
                if (image) {
                    dimensions = readImage(bytes, extension);
                } else if (audio) {
                    assertAudioSignature(bytes, extension);
                } else {
                    assertSignature(bytes, PDF_MAGIC, "PDF signature");
                }
            } catch (IOException ex) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                        "Invalid file content", "The file content does not match its type.");
            }
        }
        return new CheckedUpload(originalName, extension, EXT_MIME.get(extension), image, audio, archive, bytes, dimensions);
    }

    static String archiveName(String originalName) {
        String cleaned = StringUtils.cleanPath(originalName == null || originalName.isBlank() ? "prototype.zip" : originalName);
        return cleaned.toLowerCase(Locale.ROOT).endsWith(".zip") ? cleaned : cleaned + ".zip";
    }

    private static int[] readImage(byte[] bytes, String extension) throws IOException {
        switch (extension) {
            case "png" -> assertSignature(bytes, PNG_MAGIC, "PNG signature");
            case "jpg", "jpeg" -> assertSignature(bytes, JPEG_MAGIC, "JPEG signature");
            case "webp" -> {
                return WebpHeaderReader.readDimensions(new ByteArrayInputStream(bytes));
            }
            default -> throw new IOException("unsupported image extension");
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IOException("image not decodable");
        }
        return new int[]{image.getWidth(), image.getHeight()};
    }

    private static void assertSignature(byte[] bytes, byte[] magic, String label) throws IOException {
        if (bytes.length < magic.length) {
            throw new IOException(label + " too short");
        }
        for (int i = 0; i < magic.length; i++) {
            if (bytes[i] != magic[i]) {
                throw new IOException(label + " mismatch");
            }
        }
    }

    private static void assertAudioSignature(byte[] bytes, String extension) throws IOException {
        switch (extension) {
            case "mp3" -> {
                boolean id3 = startsWith(bytes, ID3_MAGIC);
                boolean mpeg = bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF
                        && (bytes[1] & 0xE0) == 0xE0;
                if (!id3 && !mpeg) {
                    throw new IOException("MP3 signature mismatch");
                }
            }
            case "m4a" -> {
                if (bytes.length < 12 || !startsWithAt(bytes, 4, M4A_FTYP)) {
                    throw new IOException("M4A (ISO BMFF) signature mismatch");
                }
            }
            case "ogg" -> {
                if (!startsWith(bytes, OGG_MAGIC)) {
                    throw new IOException("OGG signature mismatch");
                }
            }
            default -> throw new IOException("unsupported audio extension");
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] magic) {
        return bytes.length >= magic.length && startsWithAt(bytes, 0, magic);
    }

    private static boolean startsWithAt(byte[] bytes, int offset, byte[] magic) {
        if (bytes.length < offset + magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (bytes[offset + i] != magic[i]) return false;
        }
        return true;
    }

    private static boolean declaredMimeMatches(String extension, String declaredMime) {
        String normalized = normalizeMime(declaredMime);
        Set<String> accepted = EXT_ACCEPTED_MIME.get(extension);
        return accepted != null ? accepted.contains(normalized) : normalized.equals(EXT_MIME.get(extension));
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String normalizeMime(String mime) {
        int semicolon = mime.indexOf(';');
        return (semicolon >= 0 ? mime.substring(0, semicolon) : mime).trim().toLowerCase(Locale.ROOT);
    }

    record CheckedUpload(String originalName, String extension, String mime, boolean image, boolean audio,
                         boolean archive, byte[] bytes, int[] dimensions) {
    }
}
