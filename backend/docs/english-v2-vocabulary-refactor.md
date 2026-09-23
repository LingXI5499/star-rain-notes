# English V2 E06: Vocabulary domain consolidation

Branch: `codex/english-v2-vocabulary-refactor`. Base: E05 Writing boundary.

## Domain ownership

Vocabulary code now lives under `com.starrainnotes.english.vocabulary`, including its controllers, DTOs, entities, provider configuration, pronunciation support, and the existing word family module.

| Responsibility | New owner |
| --- | --- |
| Public catalog and word reads | `VocabularyQueryService` and `VocabularyCatalogRepository` |
| Word updates, examples and deletion | `VocabularyCommandService` and `VocabularyRepository` |
| Theme persistence | `VocabularyThemeRepository` |
| Audio persistence and primary-accent updates | `VocabularyAudioRepository`, called through `VocabularyAudioService` |
| Word response assembly | Existing `VocabularyWordViewAssembler` |
| Legacy global memory counter API | `LegacyVocabularyMemoryService` |
| Pronunciation proxy | Existing `VocabularyPronunciationService` |

The public/admin vocabulary controllers and account study service now depend on English vocabulary application services. The account study package remains in its current location for its separate planned migration stage.

## Compatibility

- Existing vocabulary API paths remain available. Added the plan's `/layers`, `/words/{wordId}` and `/words?ids=...` aliases alongside existing `/themes`, `/words/{wordId}/study` and `/words/batch` routes.
- `POST /api/v1/public/vocabulary/words/{wordId}/memory` remains available and is marked deprecated as a legacy global counter operation. Account study state remains separate.
- Existing theme and word ordering, pagination, remembered filtering, admin word search, audio lifecycle, and pronunciation behavior are retained.
- Word family code remains within the `english.vocabulary` ownership tree. No database migration was added.

## Verification

- `mvn -q -DskipTests compile` passed.
- Vocabulary catalog, pronunciation API, provider and service tests passed.
- `mvn -q test` passed: 55 suites, 374 tests, 0 failures, 0 errors, 1 skipped. The test database remained at migration V35.
