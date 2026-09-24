# English V2 E09: Controller, Service, Repository boundary

Branch: `codex/english-shared-foundation-refactor`.

| Layer | Responsibility |
| --- | --- |
| Controller | HTTP routing, input binding, validation and response status |
| Application Service | Use case orchestration, publication rules, candidate ranking and transactions |
| Repository / Mapper | SQL, MyBatis operations and database row mapping |

The final E09 audit found no `JdbcTemplate` or SQL statements in English Controller and Service classes. Two small single-table services, `EnglishService` and `CefrService`, still call their MyBatis-Plus Mappers, as allowed by the plan. `LearningBundleService` delegates bundle persistence to `LearningBundleRepository`. `WordFamilyService` delegates family and member persistence to `WordFamilyRepository`. Recommendation reads learning records through `RecommendationRepository`, bundle membership through the bundle repository, pairs through Listening, and tags through Reading, Listening and Writing providers. The registry is the content lookup boundary.

Existing HTTP routes, DTO fields and V35 database schema are unchanged. Expected missing or withdrawn content is skipped during recommendation assembly. Recommendation priority and top-eight selection stay in the application/domain layer.

Verification: Maven compilation, focused recommendation/account/word-family integration tests, and the full backend test suite (59 suites, 394 tests, 0 failures, 0 errors, 1 skipped). The suite uses the local MySQL V35 test database.
