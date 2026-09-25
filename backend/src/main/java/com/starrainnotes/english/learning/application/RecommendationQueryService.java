package com.starrainnotes.english.learning.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.learning.domain.RecommendationEngine;
import com.starrainnotes.english.learning.dto.LearningRecommendationView;
import com.starrainnotes.english.learning.infrastructure.RecommendationRepository;
import com.starrainnotes.english.listening.application.ListeningPairQueryService;
import com.starrainnotes.english.shared.bundle.api.PublishedPathMember;
import com.starrainnotes.english.shared.bundle.service.LearningBundleItemService;
import com.starrainnotes.english.shared.content.ContentDescriptor;
import com.starrainnotes.english.shared.content.EnglishContentRegistry;
import com.starrainnotes.english.shared.content.EnglishContentType;
import com.starrainnotes.english.shared.content.TagMatchCandidate;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RecommendationQueryService {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final List<EnglishContentType> TYPES = List.of(
            EnglishContentType.GRAMMAR, EnglishContentType.READING,
            EnglishContentType.LISTENING, EnglishContentType.WRITING);
    private final RecommendationRepository repository;
    private final RecommendationEngine engine;
    private final EnglishContentRegistry content;
    private final LearningBundleItemService bundles;
    private final ListeningPairQueryService pairs;
    private final SiteSettingsTimezone timezone;

    public RecommendationQueryService(RecommendationRepository repository, RecommendationEngine engine,
                                      EnglishContentRegistry content, LearningBundleItemService bundles,
                                      ListeningPairQueryService pairs, SiteSettingsTimezone timezone) {
        this.repository = repository;
        this.engine = engine;
        this.content = content;
        this.bundles = bundles;
        this.pairs = pairs;
        this.timezone = timezone;
    }

    public List<LearningRecommendationView> recommendations(long learnerId) {
        return engine.select(reviewAndContinue(learnerId),
                bundleNextSteps(learnerId),
                pairedContent(learnerId),
                tagMatches(learnerId),
                () -> starters(learnerId));
    }

    private List<LearningRecommendationView> reviewAndContinue(long learnerId) {
        List<LearningRecommendationView> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<RecommendationRepository.ReviewRecord> records = repository.reviewRecords(learnerId).stream()
                .filter(row -> "IN_PROGRESS".equals(row.completionStatus()) || due(row, now))
                .sorted(Comparator.comparingInt((RecommendationRepository.ReviewRecord row) ->
                                due(row, now) ? 10 : 20)
                        .thenComparing(RecommendationRepository.ReviewRecord::nextReviewAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(RecommendationRepository.ReviewRecord::mastery,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(RecommendationRepository.ReviewRecord::updatedAt,
                                Comparator.reverseOrder()))
                .toList();
        for (RecommendationRepository.ReviewRecord row : records) {
            ContentDescriptor descriptor;
            try {
                descriptor = content.requirePublished(EnglishContentType.valueOf(row.contentType()), row.contentId());
            } catch (ApiException ex) {
                continue;
            }
            String nextReview = row.nextReviewAt() == null ? null
                    : timezone.atSite(row.nextReviewAt()).format(ISO);
            boolean due = due(row, now);
            result.add(new LearningRecommendationView(row.contentType(), row.contentId(),
                    descriptor.slug(), descriptor.title(), route(row.contentType(), descriptor.slug()),
                    due ? "到期复习" : "继续学习",
                    descriptor.cefrLevel(), row.mastery(), nextReview,
                    due ? "REVIEW" : "CONTINUE", due ? 10 : 20, null));
            if (result.size() >= 8) break;
        }
        return result;
    }

    private boolean due(RecommendationRepository.ReviewRecord row, LocalDateTime now) {
        return row.nextReviewAt() != null && !row.nextReviewAt().isAfter(now);
    }

    private List<LearningRecommendationView> starters(long learnerId) {
        Set<RecommendationRepository.ContentKey> studied = repository.studiedContent(learnerId);
        List<LearningRecommendationView> result = new ArrayList<>();
        for (EnglishContentType type : TYPES) {
            content.publishedCandidates(type).stream()
                    .filter(descriptor -> !studied.contains(
                            new RecommendationRepository.ContentKey(type.name(), descriptor.id())))
                    .min(Comparator.comparingLong(ContentDescriptor::id))
                    .ifPresent(descriptor -> result.add(new LearningRecommendationView(
                            type.name(), descriptor.id(), descriptor.slug(), descriptor.title(),
                            route(type.name(), descriptor.slug()), "建议开始", descriptor.cefrLevel(),
                            null, null, "STARTER", 60, null)));
        }
        return result;
    }

    private List<LearningRecommendationView> bundleNextSteps(long learnerId) {
        Map<RecommendationRepository.ContentKey, String> statuses = repository.completionStatuses(learnerId);
        Map<Long, List<PathRow>> grouped = new LinkedHashMap<>();
        for (PublishedPathMember member : bundles.publishedPathMembers()) {
            ContentDescriptor descriptor;
            try {
                descriptor = content.requirePublished(
                        EnglishContentType.valueOf(member.type()), member.contentId());
            } catch (ApiException ex) {
                continue;
            }
            String status = statuses.get(new RecommendationRepository.ContentKey(member.type(), member.contentId()));
            grouped.computeIfAbsent(member.bundleId(), ignored -> new ArrayList<>())
                    .add(new PathRow(member.bundleTitle(), descriptor, status));
        }
        List<LearningRecommendationView> result = new ArrayList<>();
        for (List<PathRow> path : grouped.values()) {
            boolean started = path.stream().anyMatch(row ->
                    row.status() != null && !"NOT_STARTED".equals(row.status()));
            if (!started) continue;
            path.stream().filter(row -> !"COMPLETED".equals(row.status())).findFirst().ifPresent(row -> {
                ContentDescriptor descriptor = row.descriptor();
                result.add(new LearningRecommendationView(descriptor.type().name(), descriptor.id(),
                        descriptor.slug(), descriptor.title(),
                        route(descriptor.type().name(), descriptor.slug()), "学习路径下一步",
                        descriptor.cefrLevel(), null, null, "BUNDLE_NEXT", 30, row.bundleTitle()));
            });
        }
        return result;
    }

    private List<LearningRecommendationView> pairedContent(long learnerId) {
        Map<RecommendationRepository.ContentKey, String> statuses = repository.completionStatuses(learnerId);
        List<LearningRecommendationView> result = new ArrayList<>();
        for (RecommendationRepository.CompletedSource source : repository.completedPairSources(learnerId)) {
            EnglishContentType sourceType = EnglishContentType.valueOf(source.type());
            ContentDescriptor sourceContent;
            try {
                sourceContent = content.require(sourceType, source.id());
            } catch (ApiException ex) {
                continue;
            }
            EnglishContentType targetType = sourceType == EnglishContentType.READING
                    ? EnglishContentType.LISTENING : EnglishContentType.READING;
            List<Long> targetIds = sourceType == EnglishContentType.READING
                    ? pairs.listeningIdsForReading(source.id()) : pairs.readingIdsForListening(source.id());
            for (Long targetId : targetIds) {
                if ("COMPLETED".equals(statuses.get(
                        new RecommendationRepository.ContentKey(targetType.name(), targetId)))) continue;
                ContentDescriptor target;
                try {
                    target = content.requirePublished(targetType, targetId);
                } catch (ApiException ex) {
                    continue;
                }
                result.add(new LearningRecommendationView(targetType.name(), target.id(),
                        target.slug(), target.title(), route(targetType.name(), target.slug()),
                        "读听配对强化", target.cefrLevel(), null, null, "PAIRED", 40, sourceContent.title()));
                if (result.size() >= 6) return result;
            }
        }
        return result;
    }

    private List<LearningRecommendationView> tagMatches(long learnerId) {
        Map<Long, java.time.LocalDateTime> termRecency = new LinkedHashMap<>();
        for (RecommendationRepository.CompletedSource source : repository.completedTaggedSources(learnerId)) {
            EnglishContentType type = EnglishContentType.valueOf(source.type());
            for (Long termId : content.tagIds(type, source.id())) {
                termRecency.putIfAbsent(termId, source.updatedAt());
            }
        }
        List<Long> terms = termRecency.keySet().stream().limit(20).toList();
        if (terms.isEmpty()) return List.of();
        Set<RecommendationRepository.ContentKey> studied = repository.studiedContent(learnerId);
        List<TagMatchCandidate> candidates = new ArrayList<>();
        for (EnglishContentType type : List.of(EnglishContentType.READING,
                EnglishContentType.LISTENING, EnglishContentType.WRITING)) {
            candidates.addAll(content.tagMatches(type, terms));
        }
        return candidates.stream()
                .filter(candidate -> !studied.contains(new RecommendationRepository.ContentKey(
                        candidate.content().type().name(), candidate.content().id())))
                .sorted(Comparator.comparingInt(TagMatchCandidate::matchingTerms).reversed()
                        .thenComparing(candidate -> candidate.content().type().name())
                        .thenComparingLong(candidate -> candidate.content().id()))
                .limit(6)
                .map(candidate -> {
                    ContentDescriptor descriptor = candidate.content();
                    return new LearningRecommendationView(descriptor.type().name(), descriptor.id(),
                            descriptor.slug(), descriptor.title(),
                            route(descriptor.type().name(), descriptor.slug()), "同主题跨模块练习",
                            descriptor.cefrLevel(), null, null, "TAG_MATCH", 50, null);
                }).toList();
    }

    private String route(String type, String slug) {
        return switch (type) {
            case "GRAMMAR" -> "/english/grammar/" + slug;
            case "READING" -> "/english/reading/" + slug;
            case "LISTENING" -> "/english/listening/" + slug;
            default -> "/english/writing/practice/" + slug;
        };
    }

    private record PathRow(String bundleTitle, ContentDescriptor descriptor, String status) { }
}
