package com.starrainnotes.english.reading.support;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backend-only English reading article statistics (方案 §6.2, §9.3, §六-1).
 *
 * <p>The client never submits stats; every save recomputes them here from the
 * Markdown body so they can't be forged. Rules (方案):
 * <ul>
 *   <li>Markdown markers, URLs and fenced code blocks are ignored;</li>
 *   <li>case is normalised for the unique-word set;</li>
 *   <li>apostrophes inside contractions (don't / rock'n'roll) are kept as one
 *       token; words are letters only, so digits/punctuation are dropped;</li>
 *   <li>total and unique word counts, average &amp; max sentence length, and an
 *       estimated reading time are computed server-side.</li>
 * </ul>
 * Reading speed for the estimate is a fixed 200 wpm (方案 doesn't define a
 * per-level reading wpm); the result is at least 1 minute for any text.</p>
 */
public final class ReadingTextStatistics {

    /** Estimated reading speed (words per minute). */
    public static final int READING_WPM = 200;

    private static final Pattern WORD = Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)*");
    private static final Pattern FENCED_CODE = Pattern.compile("(?s)```.*?```|~~~.*?~~~");
    private static final Pattern INLINE_LINK = Pattern.compile("\\[([^\\]]*)\\]\\(([^)]*)\\)");
    private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\([^)]*\\)");
    private static final Pattern RAW_URL = Pattern.compile("https?://\\S+");
    private static final Pattern MARKDOWN_MARKERS = Pattern.compile("[#*_>`~|^]|^\\s*[-+]\\s+|^>\\s?");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern NON_LETTER = Pattern.compile("[^A-Za-z']+");

    private ReadingTextStatistics() { }

    public record Stats(int wordCount, int uniqueWordCount, double averageSentenceWords,
                        int maxSentenceWords, int estimatedMinutes) { }

    public static Stats analyze(String markdown) {
        String body = plainText(markdown);
        String[] sentences = SENTENCE_END.split(body.trim());
        int totalWords = 0;
        int maxSentenceWords = 0;
        Set<String> unique = new HashSet<>();
        for (String sentence : sentences) {
            List<String> words = words(sentence);
            totalWords += words.size();
            maxSentenceWords = Math.max(maxSentenceWords, words.size());
            for (String word : words) {
                unique.add(word.toLowerCase(Locale.ROOT));
            }
        }
        int sentenceCount = Math.max(1, sentences.length);
        double average = totalWords == 0 ? 0.0
                : Math.round((totalWords / (double) sentenceCount) * 100.0) / 100.0;
        int minutes = totalWords == 0 ? 0 : Math.max(1, (int) Math.ceil(totalWords / (double) READING_WPM));
        return new Stats(totalWords, unique.size(), average, maxSentenceWords, minutes);
    }

    /** Strip Markdown markup, code fences, images and URLs, keeping readable text. */
    private static String plainText(String markdown) {
        if (markdown == null) return "";
        String text = markdown;
        text = FENCED_CODE.matcher(text).replaceAll(" ");
        text = IMAGE.matcher(text).replaceAll(" ");
        text = INLINE_LINK.matcher(text).replaceAll("$1");
        text = RAW_URL.matcher(text).replaceAll(" ");
        text = MARKDOWN_MARKERS.matcher(text).replaceAll(" ");
        text = text.replace("\r\n", "\n");
        return text;
    }

    /** Extract English words (letters + internal apostrophes) from a snippet. */
    static List<String> words(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = WORD.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }
}
