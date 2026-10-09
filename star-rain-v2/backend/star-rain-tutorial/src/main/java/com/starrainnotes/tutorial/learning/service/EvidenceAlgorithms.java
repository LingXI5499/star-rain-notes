package com.starrainnotes.tutorial.learning.service;

import com.starrainnotes.tutorial.learning.entity.EvidenceModels.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure, deterministic business rules. Time orders recommendations, never mastery. */
public final class EvidenceAlgorithms {
    private EvidenceAlgorithms() { }

    public static int score(String rating) {
        return switch (rating) {
            case "FORGOT" -> 0;
            case "FUZZY" -> 1;
            case "REMEMBERED" -> 2;
            default -> throw new IllegalArgumentException("未知回忆评价");
        };
    }

    public static String mastery(int evidenceCount, int recentScore) {
        if (evidenceCount == 0) return "UNLEARNED";
        if (evidenceCount < 3 || recentScore <= 3) return "LEARNING";
        return recentScore == 6 ? "STABLE_MASTERED" : "BASIC_MASTERED";
    }

    public static int level(String mastery) {
        return switch (mastery) {
            case "STABLE_MASTERED" -> 3;
            case "BASIC_MASTERED" -> 2;
            case "LEARNING" -> 1;
            default -> 0;
        };
    }

    public static int priority(CardMastery card) {
        if (Boolean.TRUE.equals(card.getNeedsRevalidation())) return 0;
        if ("FORGOT".equals(card.getLatestRating())) return 1;
        if ("FUZZY".equals(card.getLatestRating())) return 2;
        return switch (card.getMasteryStatus()) {
            case "LEARNING" -> 3;
            case "BASIC_MASTERED" -> 4;
            default -> 5;
        };
    }

    public static List<Task> tasks(List<Chapter> input, int target) {
        List<Chapter> chapters = input.stream().sorted(Comparator.comparing(Chapter::getGroupOrder)
                .thenComparing(Chapter::getChapterOrder).thenComparing(Chapter::getChapterId)).toList();
        List<Task> result = new ArrayList<>();
        Task current = null;
        Chapter previous = null;
        for (Chapter chapter : chapters) {
            boolean adjacent = previous != null && previous.getGroupId().equals(chapter.getGroupId())
                    && chapter.getChapterOrder() == previous.getChapterOrder() + 1;
            boolean combine = current != null && adjacent && current.getCardCount() <= target
                    && chapter.getCardCount() <= target
                    && Math.abs(current.getCardCount() + chapter.getCardCount() - target)
                    <= Math.abs(current.getCardCount() - target);
            if (!combine) {
                current = new Task();
                current.setGroupId(chapter.getGroupId());
                current.setTutorialId(chapter.getTutorialId());
                current.setSequenceNo(result.size() + 1);
                current.setStatus("PENDING");
                current.setCardCount(0);
                current.setQuestionCount(0);
                current.setChapters(new ArrayList<>());
                result.add(current);
            }
            current.getChapters().add(chapter);
            current.setCardCount(current.getCardCount() + chapter.getCardCount());
            current.setQuestionCount(current.getQuestionCount() + chapter.getQuestionCount());
            previous = chapter;
        }
        return result;
    }

    public static List<CardMastery> select(List<CardMastery> candidates, int count, boolean stableAudit) {
        Comparator<CardMastery> order = Comparator.comparing(CardMastery::getLastEvidenceAt)
                .thenComparing(CardMastery::getEvidenceCount).thenComparing(CardMastery::getChapterOrder)
                .thenComparing(CardMastery::getCardOrder).thenComparing(CardMastery::getKnowledgeCardId);
        List<CardMastery> result = new ArrayList<>();
        for (int priority = stableAudit ? 5 : 0; priority <= 5 && result.size() < count; priority++) {
            Map<Long, ArrayDeque<CardMastery>> tutorials = new LinkedHashMap<>();
            final int bucket = priority;
            candidates.stream().filter(card -> priority(card) == bucket).sorted(order)
                    .forEach(card -> tutorials.computeIfAbsent(card.getTutorialId(), ignored -> new ArrayDeque<>()).add(card));
            // Linked insertion order equals the ordering of each tutorial's oldest candidate.
            while (!tutorials.isEmpty() && result.size() < count) {
                var iterator = tutorials.values().iterator();
                while (iterator.hasNext() && result.size() < count) {
                    ArrayDeque<CardMastery> queue = iterator.next();
                    result.add(queue.removeFirst());
                    if (queue.isEmpty()) iterator.remove();
                }
            }
        }
        return result;
    }
}
