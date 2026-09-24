# English V2 E10: Stable API facades

Branch: `codex/english-api-facades`. Base: E09 shared foundation.

## Boundaries

| Facade | Owner | Capability |
| --- | --- | --- |
| `EnglishLearningFacade` | Learning | Records, summary, insights, writing submissions and legacy progress migration |
| `EnglishVocabularyFacade` | Vocabulary | Word reads, queue, review, memory, settings, display and local import |
| `EnglishContentFacade` | Shared content | Published descriptors, public availability and taxonomy references |

The three interfaces live in `com.starrainnotes.english.api`. Their implementations delegate to application services and the content registry. They contain no SQL or Mapper access. `AccountEnglishController` depends on the learning and vocabulary interfaces plus account session access; it retains the existing HTTP paths, validation and response types.

The learning facade also exposes typed `getRecord`, `getWritingSubmission` and `saveWritingSubmission` methods for internal callers. Legacy map-returning methods remain for the current HTTP JSON contract.

`ContentReadiness` reports whether a content item can currently be read publicly. `CONTENT_UNPUBLISHED` means its own status is not published; `DEPENDENCY_UNPUBLISHED` covers an unpublished parent such as the grammar course. This is a public availability query, not a draft publication checklist. `taxonomy` returns the current content tag references; grammar currently has no tagged relations.

## Compatibility

No database migration or API route change. The account vocabulary endpoints now delegate through `EnglishVocabularyFacade`; their underlying study services and review policy remain unchanged. The existing learning facade interface moved to the stable `english.api` package, and its implementation no longer imports vocabulary study services.

## Verification

Maven compilation and focused account/content facade integration tests passed. The full backend suite passed on the local MySQL V35 test database: 60 suites, 395 tests, 0 failures, 0 errors, 1 skipped.
