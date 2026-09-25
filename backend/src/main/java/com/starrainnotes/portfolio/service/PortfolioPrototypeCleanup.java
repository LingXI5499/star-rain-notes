package com.starrainnotes.portfolio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.api.PrototypeArchivePolicy;
import com.starrainnotes.media.api.PrototypeCleanupPort;
import com.starrainnotes.portfolio.entity.PortfolioProjectPrototype;
import com.starrainnotes.portfolio.mapper.PortfolioProjectPrototypeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Deletes extracted prototype trees without depending on media upload. */
@Service
public class PortfolioPrototypeCleanup implements PrototypeCleanupPort {
    private static final long MAX_UNPACKED_BYTES = 100L * 1024 * 1024;
    private static final int MAX_FILES = 500;
    private static final int UNIX_SYMLINK_MASK = 0120000;
    private static final Set<String> ALLOWED = Set.of(
            "html", "htm", "css", "js", "json", "svg", "png", "jpg", "jpeg", "webp", "gif", "ico", "woff", "woff2", "ttf");

    private final PortfolioProjectPrototypeMapper mapper;
    private final Path privateRoot;
    private final Path publicRoot;

    public PortfolioPrototypeCleanup(PortfolioProjectPrototypeMapper mapper,
                                     @Value("${app.portfolio-prototype.private-root:./data/portfolio-prototypes}") String privateDir,
                                     @Value("${app.portfolio-prototype.public-root:${MEDIA_STORAGE_DIR:uploads}/prototypes}") String publicDir) {
        this.mapper = mapper;
        this.privateRoot = Path.of(privateDir).toAbsolutePath().normalize();
        this.publicRoot = Path.of(publicDir).toAbsolutePath().normalize();
    }

    @Override
    public void validateArchive(byte[] bytes) {
        if (!PrototypeArchivePolicy.withinLimit(bytes) || !PrototypeArchivePolicy.hasZipMagic(bytes)) {
            throw invalid("Upload a ZIP package no larger than 25MB.");
        }
        Path validationStage = privateRoot.resolve(".validation")
                .resolve(UUID.randomUUID().toString().replace("-", ""));
        try {
            unpack(bytes, validationStage);
        } catch (IOException ex) {
            throw invalid("The ZIP package could not be read.");
        } finally {
            PortfolioPrototypeFiles.deleteTree(validationStage);
        }
    }

    public UnpackStats unpack(byte[] bytes, Path stage) throws IOException {
        assertNoUnixSymlinks(bytes);
        Files.createDirectories(stage);
        int files = 0;
        long totalBytes = 0L;
        Set<String> entryCandidates = new LinkedHashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                Path relative = PortfolioPrototypeFiles.safeRelative(entry.getName());
                String ext = extension(relative.getFileName().toString());
                if (!ALLOWED.contains(ext)) {
                    throw invalid("Unsupported files are not allowed in a prototype package.");
                }
                if (++files > MAX_FILES) {
                    throw invalid("A prototype package may contain at most 500 files.");
                }
                Path target = stage.resolve(relative).normalize();
                if (!target.startsWith(stage)) {
                    throw invalid("Archive path escapes its package.");
                }
                Files.createDirectories(target.getParent());
                long copied = copyLimited(zip, target, totalBytes);
                totalBytes += copied;
                if (totalBytes > MAX_UNPACKED_BYTES) {
                    throw invalid("Unpacked prototype files may not exceed 100MB.");
                }
                String normalizedName = relative.toString().replace('\\', '/');
                if (normalizedName.equals("index.html")
                        || (relative.getNameCount() == 2 && relative.getFileName().toString().equalsIgnoreCase("index.html"))) {
                    entryCandidates.add(normalizedName);
                }
            }
        }
        String entryPath;
        if (entryCandidates.contains("index.html")) {
            entryPath = "index.html";
        } else if (entryCandidates.size() == 1) {
            entryPath = entryCandidates.iterator().next();
        } else if (entryCandidates.isEmpty()) {
            throw invalid("The archive must contain index.html at its root or inside one top-level project folder.");
        } else {
            throw invalid("The archive contains multiple possible project entry pages.");
        }
        return new UnpackStats(files, totalBytes, entryPath);
    }

    public record UnpackStats(int files, long bytes, String entryPath) {
    }

    @Override
    public void cleanupExtractedByMediaAssetId(Long mediaAssetId) {
        PortfolioProjectPrototype prototype = mapper.selectOne(new LambdaQueryWrapper<PortfolioProjectPrototype>()
                .eq(PortfolioProjectPrototype::getMediaAssetId, mediaAssetId));
        if (prototype == null) {
            return;
        }
        PortfolioPrototypeFiles.deleteTree(privateRoot.resolve(prototype.getStorageKey()));
        PortfolioPrototypeFiles.deleteTree(publicRoot.resolve(prototype.getStorageKey()));
        mapper.deleteById(prototype.getId());
    }

    private static long copyLimited(InputStream in, Path target, long already) throws IOException {
        long remaining = MAX_UNPACKED_BYTES - already;
        if (remaining <= 0) {
            throw invalid("Unpacked prototype files may not exceed 100MB.");
        }
        long copied = 0L;
        byte[] buffer = new byte[8192];
        try (OutputStream out = Files.newOutputStream(target)) {
            int read;
            while ((read = in.read(buffer)) >= 0) {
                copied += read;
                if (copied > remaining) {
                    throw invalid("Unpacked prototype files may not exceed 100MB.");
                }
                out.write(buffer, 0, read);
            }
        }
        return copied;
    }

    private static void assertNoUnixSymlinks(byte[] zipBytes) {
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

    private static String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 1 ? "" : name.substring(index + 1).toLowerCase();
    }

    private static ApiException invalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PROTOTYPE_ARCHIVE",
                "Invalid prototype archive", detail);
    }
}
