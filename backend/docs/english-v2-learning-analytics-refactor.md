# English V2 E09: Learning analytics boundary

Branch: `codex/english-shared-foundation-refactor`.

`EnglishLearningAnalyticsQueryService` now validates filters, calculates the reporting window and completion rates, and assembles the response. `LearningAnalyticsRepository` owns attempt aggregation SQL. Published content counts come from `EnglishContentRegistry` and the Grammar, Reading, Listening and Writing providers, so analytics no longer queries content tables directly.

The 7/30/90 day windows, site timezone conversion, response DTO, authentication, filters and error code are unchanged. No schema migration is introduced.

`EnglishLearningAnalyticsIntegrationTest` covers access rules, filters, trends, module totals and published counts. The test schema remains at V35.
