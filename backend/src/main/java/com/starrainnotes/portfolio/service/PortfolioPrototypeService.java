package com.starrainnotes.portfolio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.api.PrototypeArchivePolicy;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.service.MediaService;
import com.starrainnotes.portfolio.dto.ProjectPrototypeView;
import com.starrainnotes.portfolio.entity.PortfolioProjectPrototype;
import com.starrainnotes.portfolio.mapper.PortfolioProjectPrototypeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Stores a small static-site package per project, backed by an ARCHIVE media asset. */
@Service
public class PortfolioPrototypeService {

    private final PortfolioProjectPrototypeMapper mapper;
    private final MediaService mediaService;
    private final PortfolioPrototypeCleanup packages;
    private final Path privateRoot;
    private final Path publicRoot;

    public PortfolioPrototypeService(PortfolioProjectPrototypeMapper mapper,
                                     MediaService mediaService,
                                     PortfolioPrototypeCleanup packages,
                                     @Value("${app.portfolio-prototype.private-root:./data/portfolio-prototypes}") String privateDir,
                                     @Value("${app.portfolio-prototype.public-root:${MEDIA_STORAGE_DIR:uploads}/prototypes}") String publicDir) {
        this.mapper = mapper;
        this.mediaService = mediaService;
        this.packages = packages;
        this.privateRoot = Path.of(privateDir).toAbsolutePath().normalize();
        this.publicRoot = Path.of(publicDir).toAbsolutePath().normalize();
    }

    public ProjectPrototypeView upload(Long projectId, MultipartFile archive, boolean published) {
        if (archive == null || archive.isEmpty() || archive.getSize() > PrototypeArchivePolicy.MAX_BYTES || !isZip(archive)) {
            throw invalid("Upload a ZIP package no larger than 25MB.");
        }

        byte[] bytes;
        try {
            bytes = archive.getBytes();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROTOTYPE_STORAGE_FAILED",
                    "Prototype storage failed", "The prototype package could not be stored.");
        }

        try {
            String sourceName = archive.getOriginalFilename() == null ? "prototype.zip" : archive.getOriginalFilename();
            MediaAsset mediaAsset = mediaService.createArchiveAsset(sourceName, bytes);
            try {
                return bind(projectId, mediaAsset, bytes, published);
            } catch (RuntimeException ex) {
                try {
                    mediaService.delete(mediaAsset.getId());
                } catch (RuntimeException ignored) {
                    // best-effort compensation
                }
                throw ex;
            }
        } catch (ApiException ex) {
            throw ex;
        }
    }

    public ProjectPrototypeView attach(Long projectId, Long mediaAssetId, boolean published) {
        MediaAsset mediaAsset = mediaService.requireAsset(mediaAssetId);
        if (!"ARCHIVE".equals(mediaAsset.getAssetType())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_TYPE_INVALID",
                    "Archive required", "The selected media asset must be a ZIP archive.");
        }
        PortfolioProjectPrototype bound = mapper.selectOne(new LambdaQueryWrapper<PortfolioProjectPrototype>()
                .eq(PortfolioProjectPrototype::getMediaAssetId, mediaAssetId));
        if (bound != null && !projectId.equals(bound.getProjectId())) {
            throw new ApiException(HttpStatus.CONFLICT, "PROTOTYPE_MEDIA_ALREADY_BOUND",
                    "Archive already bound", "This ZIP archive is already attached to another project.");
        }
        try {
            byte[] bytes = Files.readAllBytes(mediaService.resolveStoragePath(mediaAsset));
            return bind(projectId, mediaAsset, bytes, published);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROTOTYPE_STORAGE_FAILED",
                    "Prototype storage failed", "The selected ZIP archive could not be read.");
        }
    }

    /** Validates a media-library ZIP before it is persisted as an unbound archive. */
    public void validateArchive(byte[] bytes) {
        packages.validateArchive(bytes);
    }

    private ProjectPrototypeView bind(Long projectId, MediaAsset mediaAsset, byte[] bytes, boolean published) {
        if (!PrototypeArchivePolicy.withinLimit(bytes) || !PrototypeArchivePolicy.hasZipMagic(bytes)) {
            throw invalid("Upload a ZIP package no larger than 25MB.");
        }
        PortfolioProjectPrototype previous = find(projectId);
        String previousStorageKey = previous == null ? null : previous.getStorageKey();
        Long previousMediaAssetId = previous == null ? null : previous.getMediaAssetId();
        String revision = UUID.randomUUID().toString().replace("-", "");
        String storageKey = projectId + "/" + revision;
        Path stage = privateRoot.resolve(".staging").resolve(revision);
        Path destination = privateRoot.resolve(storageKey);
        Path publicDestination = publicRoot.resolve(storageKey);
        try {
            PortfolioPrototypeCleanup.UnpackStats stats = packages.unpack(bytes, stage);
            Files.createDirectories(destination.getParent());
            try {
                Files.move(stage, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(stage, destination);
            }
            if (published) {
                PortfolioPrototypeFiles.copyTree(destination, publicDestination);
            }

            PortfolioProjectPrototype row = previous == null ? new PortfolioProjectPrototype() : previous;
            row.setProjectId(projectId);
            row.setMediaAssetId(mediaAsset.getId());
            row.setRevision(revision);
            row.setEntryPath(stats.entryPath());
            row.setStorageKey(storageKey);
            row.setSourceName(mediaAsset.getOriginalName());
            row.setFileCount(stats.files());
            row.setTotalBytes(stats.bytes());
            if (previous == null) {
                mapper.insert(row);
            } else {
                mapper.updateById(row);
            }

            if (previousStorageKey != null && !storageKey.equals(previousStorageKey)) {
                PortfolioPrototypeFiles.deleteTree(privateRoot.resolve(previousStorageKey));
                PortfolioPrototypeFiles.deleteTree(publicRoot.resolve(previousStorageKey));
                if (!mediaAsset.getId().equals(previousMediaAssetId)) {
                    try {
                        mediaService.delete(previousMediaAssetId);
                    } catch (RuntimeException ignored) {
                        // The new binding is already valid; old media cleanup is best-effort.
                    }
                }
            }
            return view(row, mediaAsset.getSizeBytes(), true, published);
        } catch (ApiException ex) {
            PortfolioPrototypeFiles.deleteTree(stage);
            PortfolioPrototypeFiles.deleteTree(destination);
            PortfolioPrototypeFiles.deleteTree(publicDestination);
            throw ex;
        } catch (RuntimeException ex) {
            PortfolioPrototypeFiles.deleteTree(stage);
            PortfolioPrototypeFiles.deleteTree(destination);
            PortfolioPrototypeFiles.deleteTree(publicDestination);
            throw ex;
        } catch (IOException ex) {
            PortfolioPrototypeFiles.deleteTree(stage);
            PortfolioPrototypeFiles.deleteTree(destination);
            PortfolioPrototypeFiles.deleteTree(publicDestination);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROTOTYPE_STORAGE_FAILED",
                    "Prototype storage failed", "The prototype package could not be stored.");
        }
    }

    public void publish(Long projectId) {
        PortfolioProjectPrototype prototype = find(projectId);
        if (prototype == null) {
            return;
        }
        Path source = privateRoot.resolve(prototype.getStorageKey()).normalize();
        Path target = publicRoot.resolve(prototype.getStorageKey()).normalize();
        if (!source.startsWith(privateRoot) || !target.startsWith(publicRoot)) {
            throw invalid("Invalid prototype storage path.");
        }
        try {
            PortfolioPrototypeFiles.copyTree(source, target);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROTOTYPE_PUBLICATION_FAILED",
                    "Prototype publication failed", "The public prototype files could not be prepared.");
        }
    }

    public void delete(Long projectId) {
        PortfolioProjectPrototype prototype = find(projectId);
        if (prototype == null) {
            return;
        }
        Long mediaAssetId = prototype.getMediaAssetId();
        cleanupExtracted(prototype);
        mapper.deleteById(prototype.getId());
        if (mediaAssetId != null) {
            try {
                mediaService.delete(mediaAssetId);
            } catch (ApiException ex) {
                if (ex.getStatus() != HttpStatus.NOT_FOUND) {
                    throw ex;
                }
            }
        }
    }

    /** Removes extracted trees and prototype row for a media asset; does not delete the media row. */
    public void cleanupExtractedByMediaAssetId(Long mediaAssetId) {
        PortfolioProjectPrototype prototype = mapper.selectOne(new LambdaQueryWrapper<PortfolioProjectPrototype>()
                .eq(PortfolioProjectPrototype::getMediaAssetId, mediaAssetId));
        if (prototype == null) {
            return;
        }
        cleanupExtracted(prototype);
        mapper.deleteById(prototype.getId());
    }

    public boolean exists(Long projectId) {
        return find(projectId) != null;
    }

    public String publicEntry(Long projectId, boolean published) {
        PortfolioProjectPrototype prototype = find(projectId);
        return prototype != null && published
                ? "/uploads/prototypes/" + prototype.getStorageKey() + "/" + prototype.getEntryPath()
                : null;
    }

    public ProjectPrototypeView view(Long projectId, boolean published) {
        PortfolioProjectPrototype prototype = find(projectId);
        if (prototype == null) {
            return null;
        }
        long sizeBytes = 0L;
        if (prototype.getMediaAssetId() != null) {
            try {
                sizeBytes = mediaService.requireAsset(prototype.getMediaAssetId()).getSizeBytes();
            } catch (ApiException ignored) {
                sizeBytes = 0L;
            }
        }
        return view(prototype, sizeBytes, true, published);
    }

    public Resource previewResource(Long projectId, String requestedPath) {
        PortfolioProjectPrototype prototype = find(projectId);
        if (prototype == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROTOTYPE_NOT_FOUND",
                    "Prototype not found", "This project has no static prototype.");
        }
        String path = requestedPath == null || requestedPath.isBlank()
                ? prototype.getEntryPath()
                : requestedPath.replaceFirst("^/", "");
        Path file = privateRoot.resolve(prototype.getStorageKey()).resolve(PortfolioPrototypeFiles.safeRelative(path)).normalize();
        if (!file.startsWith(privateRoot) || !Files.isRegularFile(file)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROTOTYPE_FILE_NOT_FOUND",
                    "Prototype file not found", "The requested prototype file does not exist.");
        }
        return new FileSystemResource(file);
    }

    private void cleanupExtracted(PortfolioProjectPrototype prototype) {
        PortfolioPrototypeFiles.deleteTree(privateRoot.resolve(prototype.getStorageKey()));
        PortfolioPrototypeFiles.deleteTree(publicRoot.resolve(prototype.getStorageKey()));
    }

    private ProjectPrototypeView view(PortfolioProjectPrototype prototype, long sizeBytes, boolean preview, boolean published) {
        return new ProjectPrototypeView(
                prototype.getMediaAssetId(),
                prototype.getSourceName(),
                sizeBytes,
                "VALID",
                prototype.getRevision(),
                prototype.getFileCount(),
                prototype.getTotalBytes(),
                preview ? "/api/v1/admin/portfolio/projects/" + prototype.getProjectId()
                        + "/prototype-preview/" + prototype.getEntryPath() : null,
                published ? "/uploads/prototypes/" + prototype.getStorageKey() + "/" + prototype.getEntryPath() : null);
    }

    private PortfolioProjectPrototype find(Long projectId) {
        return mapper.selectOne(new LambdaQueryWrapper<PortfolioProjectPrototype>()
                .eq(PortfolioProjectPrototype::getProjectId, projectId));
    }

    private static boolean isZip(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.read() == 'P' && in.read() == 'K';
        } catch (IOException ex) {
            return false;
        }
    }

    private static ApiException invalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PROTOTYPE_ARCHIVE",
                "Invalid prototype archive", detail);
    }

}
