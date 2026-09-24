# English V2 E09: Recommendation boundary

Branch: `codex/english-shared-foundation-refactor`.

`RecommendationQueryService` assembles review, bundle path, reading/listening pair, taxonomy match and starter candidates. `RecommendationEngine` owns source precedence, content deduplication, the starter fallback and the eight-item cap. `RecommendationRepository` reads learning records only. Bundle membership, reading/listening pairs and tags are read from their owning modules' repositories through query services and `EnglishContentRegistry`. The learning insights route and response DTO are unchanged.

The current product uses discrete priorities: due review (10), continue (20), bundle next (30), paired content (40), matching taxonomy (50), starter (60). The plan's numeric scoring formula does not match the implementation, so this refactor preserves the running algorithm. Recommendation queries catch unavailable content while assembling candidates. The outer query service has no transaction wrapper so a nested repository's expected not-found exception cannot mark an enclosing transaction rollback-only.

`RecommendationEngineTest` fixes source precedence, duplicate handling, starter fallback and eight-item limit. `RecommendationQueryIntegrationTest` checks due review, route, deduplication, withdrawn content, bundle next step and cross-module tag matching against the V35 test schema. The account English integration test checks that recommendations can be included in learning insights.
