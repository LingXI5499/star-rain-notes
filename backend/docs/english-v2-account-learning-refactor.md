# English V2 E07: Account English ownership

Branch: `codex/english-learning-refactor`. Base: E06 Vocabulary boundary.

## Ownership

`AccountEnglishService` has been removed. The account English controller retains the existing `/api/v1/account/english/**` routes and delegates to `EnglishLearningFacade`; `AccountSessionService` supplies the authenticated account ID. Account code no longer reads or writes English learning or vocabulary study tables.

The English learning package now owns learner profiles, learning record reads and simple writes, insights, writing submission access, legacy progress import, and the account vocabulary study API. `EnglishLearningService` was removed: account batch reads, summary and insights now use focused application services and repositories, while writing submission persistence resides in `WritingSubmissionRepository`. The former anonymous learning endpoints already return 410 and retain that behavior. The existing admin analytics and recommendation services were rehomed under `english.learning` without changing their behavior. The vocabulary review implementation was moved into English ownership ahead of its planned E08 extraction.

## Compatibility

- Existing REST paths and DTO JSON names remain unchanged. The plan's `/api/v1/account/me/english/**` example differs from the current code; the current path was preserved.
- Existing error codes and status for missing records, missing writing submissions, invalid memory counts, and unpublished writing prompts remain in place.
- Local progress import still defaults a missing or nonnumeric `memoryCount` to 1 and clamps negative counts to 0.
- No Flyway migration or algorithm constants changed.

## Remaining staged work

`VocabularyStudyService` still contains review SQL and policy, which E08 will separate. Admin analytics and recommendations still have cross-domain SQL; E09 will route these through the planned content registry.

## Verification

- `mvn -q -f backend/pom.xml -DskipTests compile` passed.
- Account English, English Learning, analytics, and vocabulary study tests passed.
- `mvn -q -f backend/pom.xml test` passed after removing `EnglishLearningService`: 55 current suites, 374 tests, 0 failures, 0 errors, 1 skipped. Test schema remains at migration V35.
- Account English import parity coverage was then added and run separately: 8 tests passed.
