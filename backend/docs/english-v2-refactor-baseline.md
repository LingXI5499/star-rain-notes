# English V2 refactor baseline (E00)

Baseline commit: `e51eaf3af99ece0eb4895c53464dccd37298f0bf` (`main`, 2026-09-23). Java 21, Spring Boot 3.5.16. The latest Flyway migration is `V35__replace_vocabulary_with_cet4.sql`; existing V8–V35 files are immutable. E00/E01 require no schema change.

## Reading REST contract

Admin prefix: `/api/v1/admin/english/reading`.

| Method | Path | Handler |
| --- | --- | --- |
| GET, POST | `/articles` | list, create |
| GET, PUT, DELETE | `/articles/{id}` | detail, update, delete |
| POST | `/articles/{id}/publish`, `/articles/{id}/withdraw` | lifecycle |
| GET, POST | `/articles/{id}/exercises` | list, create |
| PUT, DELETE | `/articles/{id}/exercises/{exerciseId}` | update, delete |
| POST | `/articles/{id}/exercises/{exerciseId}/move` | reorder |

Public prefix: `/api/v1/public/english/reading`.

| Method | Path | Handler |
| --- | --- | --- |
| GET | `/` | home |
| GET | `/articles` | published list |
| GET | `/articles/{slug}` | published detail with previous/next |
| GET | `/articles/{slug}/exercises` | published exercises without answers |
| POST | `/articles/{slug}/check` | server-side scoring |

`ContentReviewService` intercepts non-SUPER_ADMIN edits of published articles and returns `202` with a review; direct updates retain the SEO change annotation. Public reads require `PUBLISHED`.

## Other English route families

The remaining domain's current controller prefixes are `/api/v1/public/english`, `/api/v1/admin/english`, `/api/v1/{public,admin}/english/grammar`, `/api/v1/{public,admin}/english/listening`, `/api/v1/{public,admin}/english/writing`, `/api/v1/{public,admin}/english/bundles`, `/api/v1/admin/english/taxonomy`, `/api/v1/public/english/learning`, `/api/v1/admin/english/analytics`, `/api/v1/{public,admin}/english/vocabulary/families`, `/api/v1/{public,admin}/vocabulary`, and `/api/v1/account/english`. Their individual method mappings remain in the corresponding controllers and are outside E01's production changes.

The plan calls the account route `/api/v1/account/me/english/**`, but the checked-in `AccountEnglishController` currently maps `/api/v1/account/english/**` (for example `/learning/records` and `/learning/summary`). The implementation must preserve the live route during any later account-domain work; the discrepancy needs a separate API decision.

## DTO and frontend contract

`ReadingArticleRequest` fields: `title`, `slug`, `summary`, `bodyMarkdown`, `coverMediaId`, `readingLevel`, `cefrLevel`, `sourceName`, `sourceUrl`, `copyrightNote`, `sortOrder`, `topicTagIds`, `genreTagIds`, `abilityTagIds`, `grammarLessonIds`. Statistics are computed on the server. `ReadingArticleView` additionally returns `id`, `coverUrl`, `wordCount`, `uniqueWordCount`, `averageSentenceWords`, `maxSentenceWords`, `estimatedMinutes`, `publishStatus`, `publishedAt`, `updatedAt`, `tags`, `grammarLessons`, `previous`, `next`. Lists use `ReadingPageView`; exercises retain the existing admin, public, check and move DTOs. `frontend/src/api/reading.ts` calls all routes above and expects these response shapes.

## Behavior and error codes

- Reading levels are 1–3. CEFR and tag dimensions must exist. Cover assets must be images. The first topic tag is `PRIMARY`; subsequent tags are `TAG`.
- Publish requires title, slug, summary, body, level, CEFR, one enabled TOPIC and one enabled GENRE tag, and a valid image cover if present. Statuses are DRAFT, PUBLISHED, WITHDRAWN; a draft cannot be withdrawn and a published article cannot be deleted directly. `publishedAt` is set only on first publish.
- The statistics algorithm in `ReadingTextStatistics` ignores Markdown markers, images, URLs and code fences, counts contractions as one word, and estimates at 200 words per minute.
- Exercises validate config, hide answers on public reads, reject duplicate submissions and score against stored answers. Public detail navigation orders published articles by `sort_order,id`.
- Frozen reading errors: `ENGLISH_READING_ARTICLE_NOT_FOUND`, `ENGLISH_CONTENT_NOT_PUBLISHED`, `ENGLISH_CONTENT_SLUG_CONFLICT`, `ENGLISH_READING_LEVEL_INVALID`, `ENGLISH_CEFR_INVALID`, `INVALID_COVER_MEDIA`, `ENGLISH_TAXONOMY_NOT_FOUND`, `ENGLISH_TAXONOMY_DEPTH_INVALID`, `ENGLISH_READING_GRAMMAR_LESSON_INVALID`, `ENGLISH_READING_PUBLISH_INVALID`, `ENGLISH_INVALID_PUBLISH_TRANSITION`, `ENGLISH_READING_PUBLISHED_DELETE_FORBIDDEN`, `ENGLISH_READING_EXERCISE_NOT_FOUND`, `ENGLISH_EXERCISE_CONFIG_INVALID`, `ENGLISH_READING_ANSWER_INVALID`.

## Regression entry points

`mvn test` in `backend`. Existing parity coverage: `ReadingTextStatisticsTest`, `ReadingIntegrationTest`, shared exercise tests and account review integration tests. Record the exact run and result in the E01 handoff.
