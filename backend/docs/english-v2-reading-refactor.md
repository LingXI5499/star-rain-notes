# English V2 E01: Reading boundary

Branch: `codex/english-v2-reading-refactor`. Baseline: `e51eaf3af99ece0eb4895c53464dccd37298f0bf`.

## Method migration

| Old entry | New owner |
| --- | --- |
| `ReadingArticleService` create, update, publish, withdraw, delete | `ReadingCommandService` |
| `ReadingArticleService` get, publicGet, list, publicList, home | `ReadingQueryService` |
| Article SQL, row mapping, pagination, navigation, state writes | `ReadingRepository` |
| Tag/grammar relation reads and writes | `ReadingRelationRepository` and `ReadingRelationService` |
| Publish checks and reading-level validation | `ReadingPublishPolicy` |
| `ReadingExerciseService` use cases | `ReadingExerciseApplicationService` |
| Exercise binding, ordering, projections and persistence | `ReadingExerciseRepository` |
| Cover validation and URL lookup | `MediaAssetPort` |

The old Reading article and exercise services were deleted after moving all controller and content-review callers. Exercises use `ReadingContentPort` for article existence and visibility. Article lists batch media URL lookups through the media capability.

## Compatibility

- REST paths, request/response record fields, HTTP statuses and error codes are unchanged. The admin's published-update review flow still runs before the command service.
- `@SeoContentChange` remains on update, publish and withdraw. Reading statistics, exercise config validation, answer sanitization and scoring continue to use the original implementations.
- Publish violation order and status transitions are unchanged. Public detail still includes published-only previous/next navigation. Published content still cannot be deleted directly.
- No migration, schema, Listening, Grammar, Writing, Vocabulary or Learning production logic was changed.

## Tests

Run from `backend`: `mvn test -q` passed on 2026-09-23 (55 suites, 373 tests, 0 failures, 0 errors, 1 skipped). The Reading integration suite covers lifecycle, DTO values, error codes, public visibility, exercise scoring, navigation, cover URL projection and missing-cover list behavior. `ReadingPublishPolicyTest` checks publish violation parity and level error codes; the existing `ReadingTextStatisticsTest` and shared exercise tests cover the original algorithms.

## Remaining boundary work

Reading relations still validate taxonomy and grammar references through their existing tables inside the relation repository. Those ports belong with the later Shared Foundation and Grammar phases. The existing SEO annotation remains until the planned event phase. The current account route discrepancy is recorded in the baseline document.
