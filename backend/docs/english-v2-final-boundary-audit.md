# English V2 final boundary audit

Branch: `codex/english-final-boundary-audit`. Base: E11 content events.

## Changes

The account content review service now asks `EnglishReviewContentPort` whether an English item is published. The English implementation maps review content types to owned content kinds and reads state through `EnglishContentStateRepository`. Account code no longer names or queries English content tables.

English repositories and assemblers no longer query or join the media table. English code depends on its own `MediaPort`; `MediaPortAdapter` delegates to the Media module's `MediaAssetPort` for types and public URLs. Catalog reads in Reading, Listening and Writing fetch content rows, then resolve cover URLs in one batch. The vocabulary response assembler delegates audio and family SQL to `VocabularyWordRelationRepository`; it only assembles the response.

## Layer check

- English Controller and Service classes contain no SQL or `JdbcTemplate`.
- The vocabulary word view assembler contains no SQL.
- The account review service still owns its own review-request persistence; English publication state is accessed through the English API.
- No English Java SQL references `media_asset` as a table. Existing `media_asset_id` foreign-key columns remain unchanged.
- API paths, JSON fields and Flyway history are unchanged.
- Vocabulary study tables retain their historical `account_vocabulary_*` names for data compatibility; their repository and business rules are owned by English.

## Verification

Maven compilation and focused content, bundle, vocabulary, reading, listening and writing integration tests passed. The full backend suite uses local MySQL at V35. Final command: `mvn -q -f backend/pom.xml test`.

61 suites, 397 tests: 0 failures, 0 errors, 1 skipped.
