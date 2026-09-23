# English V2 E03: Shared exercise contract

Branch: `codex/english-v2-exercise-contract`. Base: E02 Listening refactor.

## Changes

- The shared DTO contract lives under `english/shared/exercise/dto`: `ExerciseRequest`, `ExerciseView`, `ExercisePublicView`, `ExerciseMoveRequest`, `CheckAnswerRequest`, `CheckResultView`, and `CheckItemView`.
- Reading, Listening and Writing controllers, application services and repositories now use these shared types. Listening and Writing have no imports from `english.reading`.
- Removed the seven deprecated DTO definitions from `english/reading/dto` after confirming the repository had no remaining production or test references.
- Kept the record component names and order aligned with the previous DTOs, preserving API JSON field names and constructors' serialized shapes.

## Compatibility

The migration only changes internal Java type ownership. Route mappings, validation annotations, record fields, exercise configuration validation, answer sanitization, scoring, and content binding remain in their existing implementations. No schema or migration changes were made.

## Verification

`mvn test -q` passed on 2026-09-23: 55 suites, 373 tests, 0 failures, 0 errors, 1 skipped. The run includes Reading, Listening, and Writing integration suites against the test database at migration V35.
