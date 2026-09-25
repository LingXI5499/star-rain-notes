package com.starrainnotes.media.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.dto.MediaAssetView;
import com.starrainnotes.media.image.ImageVariantSupport;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.api.PrototypeArchivePolicy;
import com.starrainnotes.media.api.PrototypeCleanupPort;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    private final MediaAssetMapper mapper;
    private final SiteSettingsTimezone timezone;
    private final PrototypeCleanupPort prototypeCleanup;
    private final TransactionTemplate transactions;
    private final Path storageRoot;
    private final Path privateStorageRoot;
    private final String publicBase;
    private final long audioMaxBytes;

    public MediaService(MediaAssetMapper mapper,
                        SiteSettingsTimezone timezone,
                        PrototypeCleanupPort prototypeCleanup,
                        PlatformTransactionManager transactionManager,
                        @Value("${app.media.storage-dir:uploads}") String storageDir,
                        @Value("${app.media.private-storage-dir:./data/private-media}") String privateStorageDir,
                        @Value("${app.media.public-base:/uploads}") String publicBase,
                        @Value("${app.media.audio-max-bytes:52428800}") long audioMaxBytes) {
        this.mapper = mapper;
        this.timezone = timezone;
        this.prototypeCleanup = prototypeCleanup;
        this.transactions = new TransactionTemplate(transactionManager);
        this.storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
        this.privateStorageRoot = Path.of(privateStorageDir).toAbsolutePath().normalize();
        this.publicBase = publicBase;
        this.audioMaxBytes = audioMaxBytes;
    }

    // ---------------------------------------------------------------
    // upload
    // ---------------------------------------------------------------

    public MediaAssetView upload(MultipartFile file) {
        MediaContentCheck.CheckedUpload checked = MediaContentCheck.inspect(file, audioMaxBytes);
        if (checked.archive()) {
            prototypeCleanup.validateArchive(checked.bytes());
            return toView(createArchiveAsset(checked.originalName(), checked.bytes()));
        }

        String storedName = UUID.randomUUID() + "." + checked.extension();
        String yearMonth = timezone.atSite(LocalDateTime.now(ZoneOffset.UTC))
                .toLocalDate().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storagePath = yearMonth + "/" + storedName;
        Path target = storageRoot.resolve(yearMonth).resolve(storedName).normalize();
        MediaFileStore.assertWithinRoot(target, storageRoot);
        MediaFileStore.write(target, checked.bytes());
        if (checked.image() && checked.dimensions() != null) {
            ImageVariantSupport.writeVariants(target, checked.bytes(), checked.extension(), checked.dimensions()[0]);
        }

        MediaAsset asset = new MediaAsset();
        asset.setAssetType(checked.image() ? "IMAGE" : (checked.audio() ? "AUDIO" : "DOCUMENT"));
        asset.setOriginalName(checked.originalName());
        asset.setStoredName(storedName);
        asset.setMimeType(checked.mime());
        asset.setExtension(checked.extension());
        asset.setSizeBytes(file.getSize());
        asset.setStoragePath(storagePath);
        asset.setPublicUrl(publicBase + "/" + storagePath);
        if (checked.dimensions() != null) {
            asset.setWidth(checked.dimensions()[0]);
            asset.setHeight(checked.dimensions()[1]);
        }
        try {
            mapper.insert(asset);
        } catch (RuntimeException ex) {
            MediaFileStore.deleteIfExists(target, log, "Failed to clean up media file after DB error: {}");
            ImageVariantSupport.deleteVariants(target);
            log.error("Media DB insert failed after file write; cleaned up {}", target, ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Internal server error", "The media asset could not be recorded.");
        }
        return toView(asset);
    }

    // ---------------------------------------------------------------
    // archive support / deletion
    // ---------------------------------------------------------------

    /**
     * Creates an ARCHIVE media asset from an already-validated ZIP payload.
     * Used by both portfolio prototype upload and the admin media-library ZIP workflow.
     */
    public MediaAsset createArchiveAsset(String originalName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE",
                    "Empty file", "The uploaded file is empty.");
        }
        if (!PrototypeArchivePolicy.withinLimit(bytes)) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    "File too large", "Prototype ZIP must be ≤ 25MB.");
        }
        if (!PrototypeArchivePolicy.hasZipMagic(bytes)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PROTOTYPE_ARCHIVE",
                    "Invalid prototype archive", "Upload a ZIP package no larger than 25MB.");
        }
        String cleaned = MediaContentCheck.archiveName(originalName);
        String storedName = UUID.randomUUID() + ".zip";
        String yearMonth = timezone.atSite(LocalDateTime.now(ZoneOffset.UTC))
                .toLocalDate().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storagePath = yearMonth + "/" + storedName;
        Path target = privateStorageRoot.resolve(yearMonth).resolve(storedName).normalize();
        MediaFileStore.assertWithinRoot(target, privateStorageRoot);
        MediaFileStore.write(target, bytes);
        MediaAsset asset = new MediaAsset();
        asset.setAssetType("ARCHIVE");
        asset.setOriginalName(cleaned);
        asset.setStoredName(storedName);
        asset.setMimeType("application/zip");
        asset.setExtension("zip");
        asset.setSizeBytes((long) bytes.length);
        asset.setStoragePath(storagePath);
        asset.setPublicUrl("/api/v1/admin/media-assets/archive/pending");
        try {
            transactions.executeWithoutResult(status -> {
                mapper.insert(asset);
                asset.setPublicUrl("/api/v1/admin/media-assets/" + asset.getId() + "/download");
                mapper.updateById(asset);
            });
        } catch (RuntimeException ex) {
            MediaFileStore.deleteIfExists(target, log, "Failed to clean up archive file after DB error: {}");
            if (ex instanceof ApiException apiException) {
                throw apiException;
            }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Internal server error", "The media asset could not be recorded.");
        }
        return asset;
    }

    public MediaAsset requireAsset(Long mediaId) {
        MediaAsset asset = mapper.selectById(mediaId);
        if (asset == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND",
                    "Media not found", "The media asset does not exist.");
        }
        return asset;
    }

    public Path resolveStoragePath(MediaAsset asset) {
        Path root = "ARCHIVE".equals(asset.getAssetType()) ? privateStorageRoot : storageRoot;
        Path target = root.resolve(asset.getStoragePath()).normalize();
        MediaFileStore.assertWithinRoot(target, root);
        return target;
    }

    public Resource downloadArchive(Long mediaId) {
        MediaAsset asset = requireAsset(mediaId);
        if (!"ARCHIVE".equals(asset.getAssetType())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ARCHIVE_NOT_FOUND",
                    "Archive not found", "The requested media asset is not an archive.");
        }
        Path path = resolveStoragePath(asset);
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ARCHIVE_NOT_FOUND",
                    "Archive not found", "The archive file is unavailable.");
        }
        return new FileSystemResource(path);
    }

    public void delete(Long mediaId) {
        MediaAsset asset = mapper.selectById(mediaId);
        if (asset == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEDIA_NOT_FOUND",
                    "Media not found", "The media asset does not exist.");
        }
        if ("ARCHIVE".equals(asset.getAssetType())) {
            prototypeCleanup.cleanupExtractedByMediaAssetId(mediaId);
        }
        mapper.deleteById(mediaId);
        Path root = "ARCHIVE".equals(asset.getAssetType()) ? privateStorageRoot : storageRoot;
        Path target = root.resolve(asset.getStoragePath()).normalize();
        if (target.startsWith(root)) {
            MediaFileStore.deleteStoredFile(target, log);
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

    private MediaAssetView toView(MediaAsset asset) {
        return new MediaAssetView(
                asset.getId(), asset.getAssetType(), asset.getOriginalName(), asset.getMimeType(),
                asset.getExtension(), asset.getSizeBytes(), asset.getWidth(), asset.getHeight(),
                asset.getPublicUrl(), srcSetOf(asset), asset.getCreatedAt());
    }
}
