package com.starrainnotes.media.controller;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.service.MediaAccessService;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import javax.imageio.ImageIO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Bounded raster variants. Asset authorization is performed on every request, including cache hits. */
@Controller
public class MediaThumbnailController {
    private final MediaAccessService access;
    private final Semaphore generators = new Semaphore(2);
    private final Map<String, Variant> cache = new LinkedHashMap<>(32, .75f, true);
    private long cacheBytes;

    public MediaThumbnailController(MediaAccessService access) { this.access = access; }

    @GetMapping("/api/media/assets/{id}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable Long id, @RequestParam(defaultValue = "720") int width) throws Exception {
        if (!Set.of(360, 720, 1080).contains(width)) throw new ApiException("MEDIA_VARIANT_INVALID", "图片尺寸无效", 400);
        var asset = access.open(id, null);
        // Always close the authorized source even for 304, cached variants and unsupported formats.
        try (var stream = asset.getStream()) {
            if (!Set.of("image/png", "image/jpeg").contains(asset.getMimeType())) {
                return ResponseEntity.status(302).header("Location", "/api/media/assets/" + id + "/content")
                        .header("Cache-Control", "private, no-store").build();
            }
            String key = asset.getSha256() + ":" + width;
            Variant variant;
            synchronized (cache) { variant = cache.get(key); }
            if (variant == null) {
                if (!generators.tryAcquire()) throw new ApiException("MEDIA_VARIANT_BUSY", "图片正在处理中，请稍后重试", 503);
                try {
                    try (var imageInput = ImageIO.createImageInputStream(stream)) {
                        var readers = ImageIO.getImageReaders(imageInput);
                        if (!readers.hasNext()) throw new ApiException("MEDIA_VARIANT_INVALID", "图片无法读取", 400);
                        var reader = readers.next();
                        try {
                            reader.setInput(imageInput, true, true);
                            int sourceWidth = reader.getWidth(0), sourceHeight = reader.getHeight(0);
                            if (sourceWidth <= 0 || sourceHeight <= 0 || (long) sourceWidth * sourceHeight > 40000000) {
                                throw new ApiException("MEDIA_VARIANT_TOO_LARGE", "图片像素数量超过缩略处理上限", 400);
                            }
                            var parameters = reader.getDefaultReadParam();
                            int sample = Math.max(1, Math.max(sourceWidth / width, sourceHeight / 2160));
                            parameters.setSourceSubsampling(sample, sample, 0, 0);
                            BufferedImage image = reader.read(0, parameters);
                            double scale = Math.min(1, Math.min((double) width / image.getWidth(), 2160.0 / image.getHeight()));
                            int targetWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
                            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
                            boolean alpha = image.getColorModel().hasAlpha();
                            BufferedImage scaled = new BufferedImage(targetWidth, height, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
                            var graphics = scaled.createGraphics();
                            try {
                                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                                graphics.drawImage(image, 0, 0, targetWidth, height, null);
                            } finally { graphics.dispose(); }
                            var bytes = new ByteArrayOutputStream();
                            ImageIO.write(scaled, alpha ? "png" : "jpeg", bytes);
                            variant = new Variant(bytes.toByteArray(), alpha ? "image/png" : "image/jpeg");
                        } finally { reader.dispose(); }
                    }
                    synchronized (cache) {
                        var old = cache.put(key, variant); cacheBytes += variant.bytes().length - (old == null ? 0 : old.bytes().length);
                        while (cacheBytes > 32L * 1024 * 1024 || cache.size() > 128) {
                            var first = cache.entrySet().iterator(); var removed = first.next(); cacheBytes -= removed.getValue().bytes().length; first.remove();
                        }
                    }
                } finally { generators.release(); }
            }
            return ResponseEntity.ok().header("Content-Type", variant.type()).header("X-Content-Type-Options", "nosniff")
                    .header("Cache-Control", "PUBLIC".equals(asset.getAccessLevel()) ? "private, no-cache, must-revalidate" : "private, no-store")
                    .eTag('"' + key + '"').body(variant.bytes());
        }
    }

    private record Variant(byte[] bytes, String type) { }
}
