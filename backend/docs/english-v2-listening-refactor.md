# English V2 E02: Listening boundary

Branch: `codex/english-v2-listening-refactor`. Base: E01 Reading refactor.

## Method migration

| Old responsibility | New owner |
| --- | --- |
| Listening item commands and lifecycle entry points | `ListeningCommandService` |
| Item reads, lists, home data and navigation | `ListeningQueryService` and `ListeningRepository` |
| Segment reads/writes and batch replacement | `ListeningSegmentService` and `ListeningSegmentRepository` |
| Segment range and replacement checks | `SegmentTimelinePolicy` |
| Reading ↔ Listening relation reads/writes | `ListeningRelationService` and `ListeningRelationRepository` |
| Pronunciation rule reads/writes | `PronunciationRuleQueryService`, `PronunciationRuleCommandService`, `PronunciationRuleRepository` |
| Existing publication prerequisites | `ListeningPublishPolicy` |
| Listening exercise persistence and answer checks | `ListeningExerciseApplicationService` and `ListeningExerciseRepository` |
| Audio/cover asset type validation | `MediaAssetPort` |

`ListeningItemService` and `ListeningExerciseService` were deleted after callers moved. Exercise ownership checks use `ListeningContentPort`; Listening no longer imports the Reading module's exercise DTOs.

## Shared exercise contract

Added the seven DTOs under `english/shared/exercise/dto` and moved Reading, Listening and Writing controllers and services to them. The record fields retain the old JSON names. The seven old Reading DTOs remain temporarily with `@Deprecated`, as directed by the plan.

## Compatibility

- Listening REST mappings and existing request/response record fields remain unchanged.
- Existing error codes, status transitions, media rules, segment ordering/range checks, batch replacement transaction, and pronunciation rule SEO annotations are retained.
- The admin published-item review path remains connected through `ContentReviewService`.
- No database or migration changes were made.

## Tests

- `mvn -q -DskipTests compile` passed.
- `mvn -q -Dtest=ListeningIntegrationTest test` passed.
- `mvn test -q` passed on 2026-09-23: 55 suites, 373 tests, 0 failures, 0 errors, 1 skipped.

The Listening integration suite runs against the existing test MySQL schema at migration V35 and covers item lifecycle, segment operations, relations, pronunciation rules, exercise validation/scoring, API routes and public answer sanitization.

## Follow-up

The deprecated Reading DTO compatibility layer can be removed after confirming no external Java consumers use those classes. `ListeningRepository` still owns item CRUD, item projections, tag projection and the SQL-backed publication fact gathering; the extracted policies own the validation decisions.
