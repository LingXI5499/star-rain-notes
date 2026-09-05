package com.starrainnotes.vocabulary.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.vocabulary.config.VocabularyProperties;
import com.starrainnotes.vocabulary.provider.PronunciationProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Server-side pronunciation proxy with an on-disk cache.
 *
 * <p>The browser never talks to the third-party Youdao endpoint directly. The
 * front end requests {@code GET /api/v1/public/vocabulary/pronunciation}, this
 * service asks the configured {@link PronunciationProvider} for the upstream
 * URL, fetches the audio, caches it by {@code sha256(word|accent)} and returns
 * the bytes. When the provider is disabled or the upstream is unavailable the
 * call fails so the client falls back to browser speech synthesis.</p>
 */
@Service
public class VocabularyPronunciationService {

    /** A single English token: letters plus internal apostrophes/hyphens. */
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z][A-Za-z'\\-]*");
    private static final int MAX_WORD_LENGTH = 64;
    private static final String ACCENT_US = "US";
    private static final String ACCENT_UK = "UK";

    private final PronunciationProvider pronunciationProvider;
    private final VocabularyProperties props;
    private final HttpClient http;

    public VocabularyPronunciationService(PronunciationProvider pronunciationProvider,
                                          VocabularyProperties props,
                                          HttpClient http) {
        this.pronunciationProvider = pronunciationProvider;
        this.props = props;
        this.http = http;
    }

    /** Resolves, fetches (and caches on disk) the pronunciation audio bytes. */
    public byte[] audio(String word, String accent) {
        String clean = normalizeWord(word);
        String acc = normalizeAccent(accent);
        Optional<String> url = pronunciationProvider.pronunciationUrl(clean, acc);
        if (url.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRONUNCIATION_DISABLED",
                    "Pronunciation unavailable", "The pronunciation provider is disabled.");
        }
        Path cache = cachePath(clean, acc);
        byte[] cached = readIfExists(cache);
        if (cached != null) {
            return cached;
        }
        byte[] bytes = fetch(url.get());
        writeCache(cache, bytes);
        return bytes;
    }

    private byte[] fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "Mozilla/5.0 (StarRainNotes yulanlin)")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length == 0) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "PRONUNCIATION_UPSTREAM",
                        "Pronunciation upstream unavailable",
                        "The pronunciation service returned " + response.statusCode() + ".");
            }
            return response.body();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PRONUNCIATION_UPSTREAM",
                    "Pronunciation upstream unavailable", "Could not reach the pronunciation service.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PRONUNCIATION_UPSTREAM",
                    "Pronunciation upstream unavailable", "The pronunciation request was interrupted.");
        }
    }

    private Path cachePath(String word, String accent) {
        return Path.of(props.pronunciation().cacheDir()).resolve(sha256(word + "|" + accent) + ".mp3");
    }

    private byte[] readIfExists(Path path) {
        try {
            if (Files.exists(path) && Files.size(path) > 0) {
                return Files.readAllBytes(path);
            }
        } catch (IOException ignored) {
            // cache read is best effort; fall through to an upstream fetch
        }
        return null;
    }

    private void writeCache(Path path, byte[] bytes) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException ignored) {
            // cache write is best effort; the response is still served
        }
    }

    private String normalizeWord(String word) {
        if (word == null || word.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WORD_REQUIRED", "word required",
                    "The word parameter is required.");
        }
        String trimmed = word.trim();
        if (trimmed.length() > MAX_WORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WORD_TOO_LONG", "word too long",
                    "The word must be at most " + MAX_WORD_LENGTH + " characters.");
        }
        if (!WORD_PATTERN.matcher(trimmed).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WORD_INVALID", "word invalid",
                    "The word must be a single English word.");
        }
        return trimmed;
    }

    private String normalizeAccent(String accent) {
        if (accent == null || accent.isBlank()) {
            return props.pronunciation().defaultAccent();
        }
        String upper = accent.toUpperCase(Locale.ROOT);
        if (!upper.equals(ACCENT_US) && !upper.equals(ACCENT_UK)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACCENT_INVALID", "accent invalid",
                    "The accent must be US or UK.");
        }
        return upper;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
