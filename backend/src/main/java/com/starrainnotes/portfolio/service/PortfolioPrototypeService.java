package com.starrainnotes.portfolio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Stores a small static-site package per project, backed by an ARCHIVE media asset. */
@Service
public class PortfolioPrototypeService {

    private static final long MAX_ARCHIVE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_UNPACKED_BYTES = 40L * 1024 * 1024;
    private static final int MAX_FILES = 200;
    private static final int UNIX_SYMLINK_MASK = 0120000;
    private static final Set<String> ALLOWED = Set.of(
            "html", "htm", "css", "js", "json", "svg", "png", "jpg", "jpeg", "webp", "gif", "ico", "woff", "woff2", "ttf");

    private final PortfolioProjectPrototypeMapper mapper;
    private final MediaService mediaService;
    private final Path privateRoot;
    private final Path publicRoot;

    public PortfolioPrototypeService(PortfolioProjectPrototypeMapper mapper,
                                     MediaService mediaService,
                                     @Value("${app.portfolio-prototype.private-root:./data/portfolio-prototypes}") String privateDir,
                                     @Value("${app.portfolio-prototype.public-root:${MEDIA_STORAGE_DIR:uploads}/prototypes}") String publicDir) {
        this.mapper = mapper;
        this.mediaService = mediaService;
        this.privateRoot = Path.of(privateDir).toAbsolutePath().normalize();
        this.publicRoot = Path.of(publicDir).toAbsolutePath().normalize();
    }

    public ProjectPrototypeView upload(Long projectId, MultipartFile archive, boolean published) {
        if (find(projectId) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "PROTOTYPE_ALREADY_BOUND",
                    "Prototype already bound", "Delete the existing ZIP before uploading a new one.");
        }
        if (archive == null || archive.isEmpty() || archive.getSize() > MAX_ARCHIVE_BYTES || !isZip(archive)) {
            throw invalid("Upload a ZIP package no larger than 10MB.");
        }

        byte[] bytes;
        try {
            bytes = archive.getBytes();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROTOTYPE_STORAGE_FAILED",
                    "Prototype storage failed", "The prototype package could not be stored.");
        }

        String revision = UUID.randomUUID().toString().replace("-", "");
        Path stage = privateRoot.resolve(".staging").resolve(revision);
        Path destination = privateRoot.resolve(String.valueOf(projectId)).resolve(revision);
        MediaAsset mediaAsset = null;
        try {
            UnpackStats stats = unpack(bytes, stage);
            Files.createDirectories(destination.getParent());
            Files.move(stage, destination, StandardCopyOption.ATOMIC_MOVE);

            String sourceName = archive.getOriginalFilename() == null ? "prototype.zip" : archive.getOriginalFilename();
            mediaAsset = mediaService.createArchiveAsset(sourceName, bytes);

            PortfolioProjectPrototype row = new PortfolioProjectPrototype();
            row.setProjectId(projectId);
            row.setMediaAssetId(mediaAsset.getId());
            row.setRevision(revision);
            row.setEntryPath("index.html");
            row.setStorageKey(projectId + "/" + revision);
            row.setSourceName(sourceName);
            row.setFileCount(stats.files());
            row.setTotalBytes(stats.bytes());
            mapper.insert(row);

            if (published) {
                publish(projectId);
            }
            return view(row, mediaAsset.getSizeBytes(), true, published);
        } catch (ApiException ex) {
            deleteTree(stage);
            deleteTree(destination);
            if (mediaAsset != null) {
                mediaService.delete(mediaAsset.getId());
            }
            throw ex;
        } catch (IOException ex) {
            deleteTree(stage);
            deleteTree(destination);
            if (mediaAsset != null) {
                mediaService.delete(mediaAsset.getId());
            }
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
            copyTree(source, target);
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
        Path file = privateRoot.resolve(prototype.getStorageKey()).resolve(safeRelative(path)).normalize();
        if (!file.startsWith(privateRoot) || !Files.isRegularFile(file)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROTOTYPE_FILE_NOT_FOUND",
                    "Prototype file not found", "The requested prototype file does not exist.");
        }
        return new FileSystemResource(file);
    }

    private UnpackStats unpack(byte[] bytes, Path stage) throws IOException {
        assertNoUnixSymlinks(bytes);
        Files.createDirectories(stage);
        int files = 0;
        long totalBytes = 0L;
        boolean entryFound = false;
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                Path relative = safeRelative(entry.getName());
                String ext = extension(relative.getFileName().toString());
                if (!ALLOWED.contains(ext)) {
                    throw invalid("Unsupported files are not allowed in a prototype package.");
                }
                if (++files > MAX_FILES) {
                    throw invalid("A prototype package may contain at most 200 files.");
                }
                Path target = stage.resolve(relative).normalize();
                if (!target.startsWith(stage)) {
                    throw invalid("Archive path escapes its package.");
                }
                Files.createDirectories(target.getParent());
                long copied = copyLimited(zip, target, totalBytes);
                totalBytes += copied;
                if (totalBytes > MAX_UNPACKED_BYTES) {
                    throw invalid("Unpacked prototype files may not exceed 40MB.");
                }
                if (relative.toString().replace('\\', '/').equals("index.html")) {
                    entryFound = true;
                }
            }
        }
        if (!entryFound) {
            throw invalid("The archive must contain index.html at its root.");
        }
        return new UnpackStats(files, totalBytes);
    }

    private static long copyLimited(InputStream in, Path target, long already) throws IOException {
        long remaining = MAX_UNPACKED_BYTES - already;
        if (remaining <= 0) {
            throw invalid("Unpacked prototype files may not exceed 40MB.");
        }
        long copied = 0L;
        byte[] buffer = new byte[8192];
        try (OutputStream out = Files.newOutputStream(target)) {
            int read;
            while ((read = in.read(buffer)) >= 0) {
                copied += read;
                if (copied > remaining) {
                    throw invalid("Unpacked prototype files may not exceed 40MB.");
                }
                out.write(buffer, 0, read);
            }
        }
        return copied;
    }

    private void cleanupExtracted(PortfolioProjectPrototype prototype) {
        deleteTree(privateRoot.resolve(prototype.getStorageKey()));
        deleteTree(publicRoot.resolve(prototype.getStorageKey()));
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

    private static void assertNoUnixSymlinks(byte[] zipBytes) {
        // Central directory signature PK\x01\x02; external attrs at +38.
        for (int i = 0; i < zipBytes.length - 42; i++) {
            if (zipBytes[i] == 0x50 && zipBytes[i + 1] == 0x4B && zipBytes[i + 2] == 0x01 && zipBytes[i + 3] == 0x02) {
                long attrs = (zipBytes[i + 38] & 0xFFL)
                        | ((zipBytes[i + 39] & 0xFFL) << 8)
                        | ((zipBytes[i + 40] & 0xFFL) << 16)
                        | ((zipBytes[i + 41] & 0xFFL) << 24);
                if (((attrs >> 16) & UNIX_SYMLINK_MASK) == UNIX_SYMLINK_MASK) {
                    throw invalid("Symbolic links are not allowed in a prototype package.");
                }
            }
        }
    }

    private static Path safeRelative(String name) {
        if (name == null || name.contains("\\") || name.startsWith("/") || name.contains(":")) {
            throw invalid("Invalid archive path.");
        }
        Path path = Path.of(name).normalize();
        if (path.isAbsolute() || path.startsWith("..") || path.toString().contains("..")) {
            throw invalid("Invalid archive path.");
        }
        return path;
    }

    private static String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 1 ? "" : name.substring(index + 1).toLowerCase();
    }

    private static ApiException invalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PROTOTYPE_ARCHIVE",
                "Invalid prototype archive", detail);
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path from : paths.toList()) {
                Path to = target.resolve(source.relativize(from));
                if (Files.isDirectory(from)) {
                    Files.createDirectories(to);
                } else {
                    Files.createDirectories(to.getParent());
                    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteTree(Path root) {
        try {
            if (!Files.exists(root)) {
                return;
            }
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // best effort
                    }
                });
            }
        } catch (IOException ignored) {
            // best effort
        }
    }

    private record UnpackStats(int files, long bytes) {
    }
}
