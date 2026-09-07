package com.starrainnotes.media.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Responsive image variants for portfolio / media delivery.
 *
 * <p>Originals stay as uploaded. For JPG/PNG sources we also write JPEG
 * siblings at 480 / 768 / 1280 CSS pixels when the source is wider. URLs use
 * the {@code .w{width}.jpg} suffix so clients can build {@code srcset} without
 * a separate table. Missing siblings are omitted from srcset.</p>
 */
public final class ImageVariantSupport {

    private static final Logger log = LoggerFactory.getLogger(ImageVariantSupport.class);
    public static final int[] WIDTHS = {480, 768, 1280};
    private static final float JPEG_QUALITY = 0.82f;

    private ImageVariantSupport() {
    }

    public static String variantPublicUrl(String publicUrl, int width) {
        int slash = publicUrl.lastIndexOf('/');
        int dot = publicUrl.lastIndexOf('.');
        if (slash < 0 || dot <= slash) {
            return publicUrl + ".w" + width + ".jpg";
        }
        return publicUrl.substring(0, dot) + ".w" + width + ".jpg";
    }

    public static Path variantPath(Path originalPath, int width) {
        String name = originalPath.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        return originalPath.resolveSibling(stem + ".w" + width + ".jpg");
    }

    public static void writeVariants(Path originalPath, byte[] bytes, String extension, Integer sourceWidth) {
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png")) {
            return;
        }
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException ex) {
            log.warn("Skip image variants; cannot decode {}", originalPath, ex);
            return;
        }
        if (source == null) {
            return;
        }
        int width = sourceWidth != null ? sourceWidth : source.getWidth();
        for (int targetWidth : WIDTHS) {
            if (width <= targetWidth) {
                continue;
            }
            Path target = variantPath(originalPath, targetWidth);
            try {
                writeJpeg(scale(source, targetWidth), target);
            } catch (IOException ex) {
                log.warn("Failed to write image variant {}", target, ex);
            }
        }
    }

    public static void deleteVariants(Path originalPath) {
        for (int width : WIDTHS) {
            Path target = variantPath(originalPath, width);
            try {
                Files.deleteIfExists(target);
            } catch (IOException ex) {
                log.warn("Failed to delete image variant {}", target, ex);
            }
        }
    }

    public static String buildSrcSet(Path storageRoot, String storagePath, String publicUrl, Integer width) {
        if (publicUrl == null || publicUrl.isBlank() || storagePath == null || storagePath.isBlank()) {
            return null;
        }
        Path original = storageRoot.resolve(storagePath).normalize();
        if (!original.startsWith(storageRoot.normalize())) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (int variantWidth : WIDTHS) {
            if (width != null && width <= variantWidth) {
                continue;
            }
            Path variant = variantPath(original, variantWidth);
            if (Files.isRegularFile(variant)) {
                parts.add(variantPublicUrl(publicUrl, variantWidth) + " " + variantWidth + "w");
            }
        }
        int descriptor = width != null && width > 0 ? width : 1920;
        parts.add(publicUrl + " " + descriptor + "w");
        return String.join(", ", parts);
    }

    private static BufferedImage scale(BufferedImage source, int targetWidth) {
        int targetHeight = Math.max(1, (int) Math.round(source.getHeight() * (targetWidth / (double) source.getWidth())));
        BufferedImage dest = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = dest.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return dest;
    }

    private static void writeJpeg(BufferedImage image, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
        }
        try (OutputStream out = Files.newOutputStream(target);
             ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
