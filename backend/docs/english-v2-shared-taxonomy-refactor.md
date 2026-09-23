# English V2 E09: Taxonomy boundary

Branch: `codex/english-shared-foundation-refactor`.

`EnglishTaxonomyService` is now a compatibility facade for the existing admin and meta controllers. `TaxonomyQueryService` handles tree and flat views and lookups. `TaxonomyCommandService` owns transactional create, update, move and delete operations. `TaxonomyPolicy` validates dimensions, hierarchy, sort order, slug conflicts and deletion rules. `TaxonomyRepository` owns MyBatis and SQL access, including content references and two-phase sibling order normalization.

The table, routes, response shapes and error codes are unchanged. A negative move index is clamped to the first sibling.

`mvn -q -f backend/pom.xml -Dtest=EnglishMetaAndTaxonomyIntegrationTest test` passed, including new update checks for a root with children, self-parenting and duplicate slugs. Test schema remains at V35.
