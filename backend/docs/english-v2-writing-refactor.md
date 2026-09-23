# English V2 E05: Writing boundary

Branch: `codex/english-v2-writing-refactor`. Base: E04 Grammar boundary.

## Method migration

| Responsibility | New owner |
| --- | --- |
| Prompt and resource reads | `WritingPromptQueryService`, `WritingResourceQueryService` |
| Prompt and resource writes | `WritingPromptCommandService`, `WritingResourceCommandService` |
| Shared exercise use cases | `WritingExerciseApplicationService` |
| Prompt input and publish rules | `WritingPromptPolicy` |
| Resource shape, template schema and publish rules | `WritingResourcePolicy` |
| SQL, persistence, row mapping and tags | `WritingPromptRepository`, `WritingResourceRepository`, `WritingExerciseRepository` |
| Prompt capabilities needed by exercises | `WritingPromptPort` |
| Cover URL and image validation | `MediaAssetPort` |

Admin and public controllers now use the application services. `ContentReviewService` applies accepted prompt and resource edits through their command services. The prior `WritingPromptService`, `WritingResourceService` and `WritingExerciseService` classes were removed.

## Compatibility

- Existing admin/public routes and DTOs are preserved, and exercises continue using the shared exercise DTO contract.
- Prompt word bounds, CEFR checks, cover checks, template/model resource kinds, rubric and checklist constraints, tag validation, and publication prerequisites remain enforced.
- Resource kinds, expression levels, template schema shape, word/time bounds, tags and publication prerequisites remain enforced.
- Published content delete restrictions, resource-in-use restrictions, withdrawal transitions, slug handling and ordering remain unchanged.
- SEO change annotations remain on prompt/resource update, publish and withdraw operations.
- Prompt and resource cover data is resolved through `MediaAssetPort`; their repositories no longer join `media_asset` directly. No database migration was added.

## Verification

- `mvn -q -DskipTests compile` passed.
- `mvn -q -Dtest=WritingIntegrationTest test` passed.
- `mvn -q test` passed: 55 suites, 373 tests, 0 failures, 0 errors, 1 skipped. The test database remained at migration V35.
