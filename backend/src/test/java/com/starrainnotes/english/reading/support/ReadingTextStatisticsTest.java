package com.starrainnotes.english.reading.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReadingTextStatisticsTest {

    @Test
    void plainProseCountsWordsAndSentences() {
        ReadingTextStatistics.Stats stats =
                ReadingTextStatistics.analyze("The quick brown fox jumps. It is a sunny day!");
        assertThat(stats.wordCount()).isEqualTo(10);
        assertThat(stats.uniqueWordCount()).isGreaterThan(0);
        assertThat(stats.maxSentenceWords()).isEqualTo(5);
        assertThat(stats.averageSentenceWords()).isEqualTo(5.0);
    }

    @Test
    void ignoresMarkdownMarkersUrlsImagesAndCodeFences() {
        String md = """
                # Title
                ## Heading
                - list item one
                - list item two
                See https://example.com/page for details.

                ![alt](img.png)

                ```java
                public void x() {}
                ```
                """;
        ReadingTextStatistics.Stats stats = ReadingTextStatistics.analyze(md);
        // words: Title, Heading, list, item, one, list, item, two, See, for, details
        assertThat(stats.wordCount()).isEqualTo(11);
    }

    @Test
    void contractionsAreSingleTokens() {
        ReadingTextStatistics.Stats stats = ReadingTextStatistics.analyze("Don't stop. It's fine.");
        assertThat(stats.wordCount()).isEqualTo(4);
    }

    @Test
    void estimatedMinutesIsAtLeastOneAndScalesWithWords() {
        ReadingTextStatistics.Stats shortText = ReadingTextStatistics.analyze("Short text.");
        assertThat(shortText.estimatedMinutes()).isEqualTo(1);
        ReadingTextStatistics.Stats longText = ReadingTextStatistics.analyze(
                ("word ".repeat(500)).trim());
        assertThat(longText.estimatedMinutes()).isGreaterThan(2);
    }
}
