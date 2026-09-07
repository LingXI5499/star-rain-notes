package com.starrainnotes.media.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.dto.MediaAssetView;
import com.starrainnotes.media.dto.MediaPageView;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Secure media management (01 §8, 04 §16).
 *
 * <p>Validation is server-side and never trusts the client MIME: extension
 * allowlist, declared-MIME check, file signature / magic bytes, image
 * decodability (ImageIO for jpg/png, structural WebP header check), size
 * limits (image ≤ 10MB, pdf ≤ 20MB). Files are stored as UUID names under a
 * year/month directory with a RELATIVE storage key; the DB stores that key and
 * the public URL — never an absolute path.</p>
 *
 * <p>If the disk write succeeds but the DB insert fails, the new file is
 * deleted best-effort and the error is logged. Delete removes the DB row (FK
 * SET NULL on covers/avatars/logos) and then the disk file best-effort;
 * markdown references cannot be tracked by the DB, so the UI warns.</p>
 */
@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    private static final long IMAGE_MAX_BYTES = 10L * 1024 * 1024;
    private static final long PDF_MAX_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf",
            "mp3", "m4a", "ogg");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "m4a", "ogg");
    private static final Map<String, String> EXT_MIME = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "pdf", "application/pdf",
            "mp3", "audio/mpeg",
            "m4a", "audio/mp4",
            "ogg", "audio/ogg");
    private static final Map<String, java.util.Set<String>> EXT_ACCEPTED_MIME = Map.of(
            "mp3", java.util.Set.of("audio/mpeg"),
            "m4a", java.util.Set.of("audio/mp4", "audio/x-m4a"),
            "ogg", java.util.Set.of("audio/ogg"));

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final byte[] ID3_MAGIC = "ID3".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final byte[] OGG_MAGIC = "OggS".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final byte[] M4A_FTYP = "ftyp".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    private final MediaAssetMapper mapper;
    private final SiteSettingsTimezone timezone;
    private final Path storageRoot;
    private final String publicBase;
    private final long audioMaxBytes;

    public MediaService(MediaAssetMapper mapper,
                        SiteSettingsTimezone timezone,
                        @Value("${app.media.storage-dir:uploads}") String storageDir,
                        @Value("${app.media.public-base:/uploads}") String publicBase,
                        @Value("${app.media.audio-max-bytes:52428800}") long audioMaxBytes) {
        this.mapper = mapper;
        this.timezone = timezone;
        this.storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
        this.publicBase = publicBase;
        this.audioMaxBytes = audioMaxBytes;
    }

    // ---------------------------------------------------------------
    // upload
    // ---------------------------------------------------------------

    public MediaAssetView upload(MultipartFile file) {
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
        long max = image ? IMAGE_MAX_BYTES : (audio ? audioMaxBytes : PDF_MAX_BYTES);
        if (file.getSize() > max) {
            String unit = audio ? audioMaxBytes / (1024 * 1024) + "MB" : (image ? "10MB" : "20MB");
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    "File too large", "Media must be ≤ " + unit + ".");
        }

        // declared MIME must match the extension (never trusted blindly)
        String expectedMime = EXT_MIME.get(extension);
        String declaredMime = file.getContentType();
        if (declaredMime == null || !declaredMimeMatches(extension, declaredMime)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                    "MIME mismatch", "The declared content type does not match the file extension.");
        }

        // signature / magic bytes + image decodability
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                    "Unreadable file", "The uploaded file could not be read.");
        }
        int[] dimensions = null;
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

        String storedName = UUID.randomUUID() + "." + extension;
        String yearMonth = timezone.atSite(LocalDateTime.now(ZoneOffset.UTC))
                .toLocalDate().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storagePath = yearMonth + "/" + storedName;
        Path target = storageRoot.resolve(yearMonth).resolve(storedName).normalize();
        assertWithinRoot(target);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            if (image && dimensions != null) {
                ImageVariantSupport.writeVariants(target, bytes, extension, dimensions[0]);
            }
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Storage unavailable", "The media file could not be stored.");
        }

        MediaAsset asset = new MediaAsset();
        asset.setAssetType(image ? "IMAGE" : (audio ? "AUDIO" : "DOCUMENT"));
        asset.setOriginalName(originalName);
        asset.setStoredName(storedName);
        asset.setMimeType(expectedMime);
        asset.setExtension(extension);
        asset.setSizeBytes(file.getSize());
        asset.setStoragePath(storagePath);
        asset.setPublicUrl(publicBase + "/" + storagePath);
        if (dimensions != null) {
            asset.setWidth(dimensions[0]);
            asset.setHeight(dimensions[1]);
        }
        try {
            mapper.insert(asset);
        } catch (RuntimeException ex) {
            // failure compensation: disk write succeeded but DB insert failed
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupEx) {
                log.warn("Failed to clean up media file after DB error: {}", target, cleanupEx);
            }
            ImageVariantSupport.deleteVariants(target);
            log.error("Media DB insert failed after file write; cleaned up {}", target, ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Internal server error", "The media asset could not be recorded.");
        }
        return toView(asset);
    }

    // ---------------------------------------------------------------
    // list / delete
    // ---------------------------------------------------------------

    public MediaPageView list(int page, int pageSize, String query, String assetType) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        LambdaQueryWrapper<MediaAsset> wrapper = new LambdaQueryWrapper<MediaAsset>()
                .eq(assetType != null && !assetType.isBlank(), MediaAsset::getAssetType, assetType)
                .like(query != null && !query.isBlank(), MediaAsset::getOriginalName, query)
                .orderByDesc(MediaAsset::getId);

        Long total = mapper.selectCount(wrapper);
        wrapper.last("LIMIT " + safeSize + " OFFSET " + ((safePage - 1) * safeSize));
        List<MediaAssetView> items = mapper.selectList(wrapper).stream().map(this::toView).toList();
        long safeTotal = total == null ? 0 : total;
        int totalPages = safeTotal == 0 ? 0 : (int) ((safeTotal + safeSize - 1) / safeSize);
        return new MediaPageView(items, safePage, safeSize, safeTotal, totalPages);
    }

    public void delete(Long mediaId) {
        MediaAsset asset = mapper.selectById(mediaId);
        if (asset == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND",
                    "Media not found", "The media asset does not exist.");
        }
        // DB row first: covers/avatars/logos/resumes reference it with ON DELETE
        // SET NULL (frozen FKs), so removing the row clears those references.
        mapper.deleteById(mediaId);
        // best-effort disk file removal; markdown references are the UI's concern
        Path target = storageRoot.resolve(asset.getStoragePath()).normalize();
        if (target.startsWith(storageRoot)) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ex) {
                log.warn("Failed to delete media file {} (row already removed)", target, ex);
            }
            ImageVariantSupport.deleteVariants(target);
        }
    }

    public String srcSetOf(MediaAsset asset) {
        if (asset == null || !"IMAGE".equals(asset.getAssetType())) {
            return null;
        }
        return ImageVariantSupport.buildSrcSet(
                storageRoot, asset.getStoragePath(), asset.getPublicUrl(), asset.getWidth());
    }

    // ---------------------------------------------------------------
    // validation helpers
    // ---------------------------------------------------------------

    private int[] readImage(byte[] bytes, String extension) throws IOException {
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

    private void assertSignature(byte[] bytes, byte[] magic, String label) throws IOException {
        if (bytes.length < magic.length) {
            throw new IOException(label + " too short");
        }
        for (int i = 0; i < magic.length; i++) {
            if (bytes[i] != magic[i]) {
                throw new IOException(label + " mismatch");
            }
        }
    }

    /**
     * Validate a declared audio container against its magic bytes. MP3 accepts
     * an ID3 tag or an MPEG frame sync; M4A checks the ISO BMFF 'ftyp' box;
     * OGG checks the 'OggS' capture pattern. This never trusts extension or
     * browser MIME alone (方案 §13.1).
     */
    private void assertAudioSignature(byte[] bytes, String extension) throws IOException {
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

    private boolean startsWith(byte[] bytes, byte[] magic) {
        return bytes.length >= magic.length && startsWithAt(bytes, 0, magic);
    }

    private boolean startsWithAt(byte[] bytes, int offset, byte[] magic) {
        if (bytes.length < offset + magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (bytes[offset + i] != magic[i]) return false;
        }
        return true;
    }

    private boolean declaredMimeMatches(String extension, String declaredMime) {
        String normalized = normalizeMime(declaredMime);
        java.util.Set<String> accepted = EXT_ACCEPTED_MIME.get(extension);
        return accepted != null ? accepted.contains(normalized) : normalized.equals(EXT_MIME.get(extension));
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeMime(String mime) {
        int semicolon = mime.indexOf(';');
        return (semicolon >= 0 ? mime.substring(0, semicolon) : mime).trim().toLowerCase(java.util.Locale.ROOT);
    }

    private void assertWithinRoot(Path target) {
        if (!target.startsWith(storageRoot)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Storage error", "Resolved path escapes the media root.");
        }
    }

    private MediaAssetView toView(MediaAsset asset) {
        return new MediaAssetView(
                asset.getId(), asset.getAssetType(), asset.getOriginalName(), asset.getMimeType(),
                asset.getExtension(), asset.getSizeBytes(), asset.getWidth(), asset.getHeight(),
                asset.getPublicUrl(), srcSetOf(asset), asset.getCreatedAt());
    }
}
