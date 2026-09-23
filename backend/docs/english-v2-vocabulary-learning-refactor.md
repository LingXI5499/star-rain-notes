# English V2 E08: Vocabulary learning and review

Branch: `codex/english-vocabulary-learning-refactor`. Base: E07 Account English ownership.

## Boundary

Account vocabulary study code now lives under `com.starrainnotes.english.vocabulary.learning`. The former `VocabularyStudyService` has been split into query, command, and local progress import services. They delegate persistence to `VocabularyStudyRepository` and `VocabularyReviewLogRepository`. The English Learning facade delegates all account vocabulary endpoints to this boundary, including the simple memory counter and local progress import.

## Frozen behavior

- `VocabularyReviewPolicy` retains all ten intervals from 5 minutes through 60 days, the stage 10 cap, and `NEW`/`EARLY`/`ON_TIME`/`OVERDUE` timing boundaries.
- Review writes still check `review_session_id` before work, lock the memory row with `FOR UPDATE`, insert an immutable log using the existing unique session constraint, and advance memory only after a new log is inserted. A duplicate session returns the prior result; reuse for another word still raises `VOCABULARY_REVIEW_SESSION_REUSED`.
- Queue ordering, daily new and review limits, deterministic mixed direction, display preferences, local import merge rules, and REST routes remain unchanged.
- No schema migration or DTO JSON change was introduced.

## Verification

- `mvn -q -f backend/pom.xml -DskipTests compile` passed.
- Interval and timing parity tests cover all ten stages, boundaries, and the cap.
- Account English integration tests cover settings, queue, duplicate reviews, concurrent reviews of one word, and repeat local import.
- `mvn -q -f backend/pom.xml clean test` passed before the final parity and import cases were added. Final `mvn -q -f backend/pom.xml test` passed: 55 suites, 379 tests, 0 failures, 0 errors, 1 skipped. Test schema remains at V35.
