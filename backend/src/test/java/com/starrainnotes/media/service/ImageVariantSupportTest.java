package com.starrainnotes.media.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ImageVariantSupportTest {

    @TempDir
    Path temp;

    @Test
    void buildsVariantUrlBesideOriginal() {
        assertThat(ImageVariantSupport.variantPublicUrl("/uploads/2026/09/abcd.png", 480))
                .isEqualTo("/uploads/2026/09/abcd.w480.jpg");
    }

    @Test
    void writesSmallerVariantsAndSrcSet() throws Exception {
        BufferedImage source = new BufferedImage(1600, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = source.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, 1600, 900);
        g.dispose();

        Path original = temp.resolve("shot.png");
        ImageIO.write(source, "png", original.toFile());
        byte[] bytes = Files.readAllBytes(original);

        ImageVariantSupport.writeVariants(original, bytes, "png", 1600);

        assertThat(ImageVariantSupport.variantPath(original, 480)).exists();
        assertThat(ImageVariantSupport.variantPath(original, 768)).exists();
        assertThat(ImageVariantSupport.variantPath(original, 1280)).exists();

        String srcSet = ImageVariantSupport.buildSrcSet(
                temp, "shot.png", "/uploads/shot.png", 1600);
        assertThat(srcSet).contains("/uploads/shot.w480.jpg 480w");
        assertThat(srcSet).contains("/uploads/shot.w1280.jpg 1280w");
        assertThat(srcSet).endsWith("/uploads/shot.png 1600w");

        ImageVariantSupport.deleteVariants(original);
        assertThat(ImageVariantSupport.variantPath(original, 480)).doesNotExist();
    }

    @Test
    void skipsVariantsWhenSourceAlreadySmall() throws Exception {
        BufferedImage source = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
        Path original = temp.resolve("tiny.png");
        ImageIO.write(source, "png", original.toFile());
        ImageVariantSupport.writeVariants(original, Files.readAllBytes(original), "png", 320);
        assertThat(ImageVariantSupport.variantPath(original, 480)).doesNotExist();
        String srcSet = ImageVariantSupport.buildSrcSet(temp, "tiny.png", "/uploads/tiny.png", 320);
        assertThat(srcSet).isEqualTo("/uploads/tiny.png 320w");
    }
}
