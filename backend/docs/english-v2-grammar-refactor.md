# English V2 E04: Grammar boundary

Branch: `codex/english-v2-grammar-refactor`. Base: E03 shared exercise contract.

## Method migration

| Responsibility | New owner |
| --- | --- |
| Course, curriculum and lesson reads | `GrammarQueryService` and `GrammarRepository` |
| Course, section and lesson writes | `GrammarCommandService` and `GrammarRepository` |
| Section/lesson ordering and reassignment | `GrammarOrderingService` with repository ordering primitives |
| Draft withdrawal lifecycle rules | `GrammarPublishPolicy` |
| SQL, row mapping, curriculum assembly and public navigation | `GrammarRepository` |
| Cover image validation and URL lookup | `MediaAssetPort` |

The controllers now depend on Grammar query/command services. The published lesson review path in `ContentReviewService` still calls the command service. The old `EnglishGrammarService` was removed.

## Compatibility

- The singleton course remains `id=1`; no multi-course model or schema change was introduced.
- Existing admin and public routes, DTOs, error codes, section and lesson ordering, and explicit lesson reassign behavior remain in place.
- Public curriculum includes only published lessons and omits sections with no published lessons. Public lesson previous/next navigation still spans the published course ordered by section and lesson order.
- Lesson update, publish and withdraw retain their `@SeoContentChange` annotations. Draft course/lesson withdrawal continues to return `INVALID_PUBLISH_TRANSITION`.
- Grammar course queries no longer join `media_asset`; cover URLs and image type checks go through `MediaAssetPort`.

## Verification

- `mvn -q -DskipTests compile` passed.
- `mvn -q -Dtest=EnglishGrammarIntegrationTest test` passed.
- `mvn test -q` passed on 2026-09-23: 55 suites, 373 tests, 0 failures, 0 errors, 1 skipped. The test database remained at migration V35.
