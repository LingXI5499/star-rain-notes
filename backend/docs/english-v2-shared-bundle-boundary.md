# English V2 E09: Bundle content boundary

Branch: `codex/english-shared-foundation-refactor`. Base: E08 Vocabulary learning.

## Boundary

`LearningBundleItemService` now owns bundle rules, ordering, readiness and catalog assembly. Its SQL has moved to `LearningBundleItemRepository`, which reads and writes only bundle metadata and membership. It no longer joins reading, listening or writing tables.

`LearningBundleService` owns lifecycle and validation rules. `LearningBundleRepository` owns bundle SQL, MyBatis mapper calls and row mapping. The public visibility check remains in the service because it combines bundle state with item readiness.

`EnglishContentRegistry` resolves content through domain providers. Reading, Listening and Writing expose catalog descriptors through their query services and repositories. Each domain repository applies status, CEFR and text filters in SQL and returns a count plus the candidate prefix needed for cross-domain pagination. The service merges those candidates in the existing status, type, sort order and ID order. Grammar has a descriptor provider for direct lookup but is not a bundle item type.

## Frozen behavior

- Bundle API paths, response DTOs, validation codes, publish checklist and published bundle lock are unchanged.
- Catalog status, type, CEFR and title/slug/summary text filters retain their previous SQL semantics.
- Public bundle items still require a published bundle and published referenced content.
- No schema migration is introduced.

## Verification

- `LearningBundleIntegrationTest` covers add, publish, public visibility, readiness, catalog selection, filtering and pagination.
- `mvn -q -f backend/pom.xml test` passed: 55 suites, 379 tests, 0 failures, 0 errors, 1 skipped. Test schema remains at V35.

Taxonomy, exercise, recommendation and analytics boundaries are documented in their corresponding E09 notes.
