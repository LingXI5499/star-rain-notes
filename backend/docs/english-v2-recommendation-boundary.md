# English V2 E09: Recommendation boundary, first pass

Branch: `codex/english-shared-foundation-refactor`.

`RecommendationQueryService` orchestrates the existing source queries. `RecommendationEngine` owns source precedence, content deduplication, the starter fallback and the eight-item cap. `RecommendationRepository` owns the SQL and row mapping previously embedded in `EnglishRecommendationService`. The learning insights route and response DTO are unchanged.

The current product uses discrete priorities: due review (10), continue (20), bundle next (30), paired content (40), matching taxonomy (50), starter (60). The plan's numeric scoring formula does not match the implementation at this point, so this pass preserves the running algorithm. Cross-domain candidate queries still exist inside `RecommendationRepository`; moving those behind domain providers and `EnglishContentRegistry` is the remaining recommendation boundary work.

`RecommendationEngineTest` fixes the current source precedence, duplicate handling, starter fallback and eight-item limit. `RecommendationQueryIntegrationTest` checks a due review, route, deduplication and hiding withdrawn content against the V35 test schema.
