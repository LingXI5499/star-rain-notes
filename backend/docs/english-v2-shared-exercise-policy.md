# English V2 E09: Shared exercise policy

Branch: `codex/english-shared-foundation-refactor`.

`EnglishExerciseService` has been renamed to `EnglishExercisePolicy` under the shared exercise domain. Reading, Listening, Writing, the English meta endpoint and `EnglishExerciseSafety` now use that policy. The question type registry, config validation, public answer sanitization and server scoring keep their existing behavior and error codes.

`EnglishExerciseSafety` currently performs only JSON transformations and scoring; it has no database access to move into a repository. The supported question types are the types already exposed by the current API, including module-specific types. No schema or route changes are introduced.

`mvn -q -f backend/pom.xml '-Dtest=EnglishExercisePolicyTest,EnglishExerciseSafetyTest' test` passed.
