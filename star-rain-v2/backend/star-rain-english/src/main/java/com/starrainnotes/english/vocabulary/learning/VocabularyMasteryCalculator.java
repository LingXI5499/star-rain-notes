package com.starrainnotes.english.vocabulary.learning;

import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.ModeMemory;
import com.starrainnotes.english.vocabulary.utils.VocabularySiteTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Pure product scoring model; it estimates self-rated recall, not language proficiency. */
public final class VocabularyMasteryCalculator {
    public static final List<String> DIRECTIONS = List.of("EN_TO_ZH", "ZH_TO_EN", "AUDIO_TO_BOTH");
    private VocabularyMasteryCalculator() { }

    public static double updatedEma(int previousCount, double previousScore, String rating) {
        int score = switch (rating) {
            case "FORGOT" -> 0;
            case "UNCERTAIN" -> 60;
            case "KNOW" -> 100;
            default -> throw new IllegalArgumentException("Unknown rating");
        };
        return previousCount == 0 ? score : Math.round((0.4 * previousScore + 0.6 * score) * 10000) / 10000.0;
    }

    public static int score(List<ModeMemory> modes) {
        double sum = 0;
        for (String direction : DIRECTIONS) {
            ModeMemory mode = modes.stream().filter(item -> direction.equals(item.getDirection())).findFirst().orElse(null);
            if (mode != null) {
                sum += mode.getEmaScore() * Math.min(1, mode.getRatingCount() / 3.0);
            }
        }
        return (int) Math.max(0, Math.min(100, Math.round(sum / 3)));
    }

    public static boolean graduated(List<ModeMemory> modes, LocalDateTime firstRatedAt,
                                    LocalDateTime now, int totalDays, int recentDays) {
        if (firstRatedAt == null || score(modes) < 80 || totalDays < 3 || recentDays < 2
                || siteDay(firstRatedAt).plusDays(7).isAfter(siteDay(now))) {
            return false;
        }
        for (String direction : DIRECTIONS) {
            ModeMemory mode = modes.stream().filter(item -> direction.equals(item.getDirection())).findFirst().orElse(null);
            if (mode == null || mode.getRatingCount() < 3 || !"KNOW".equals(mode.getLastRating())
                    || ("AUDIO_TO_BOTH".equals(direction) && !mode.isAudioVerified())) {
                return false;
            }
        }
        return true;
    }

    public static String rank(int score, boolean graduated, boolean rated) {
        if (!rated) { return null; }
        if (graduated) { return "MASTERED"; }
        return score < 25 ? "BEGINNER" : score < 50 ? "LEARNER" : "SKILLED";
    }

    public static LocalDate siteDay(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(VocabularySiteTime.SITE_ZONE).toLocalDate();
    }
}
