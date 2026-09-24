# English V2 E11: Content change events

Branch: `codex/english-content-events`. Base: E10 English API facades.

## Event flow

```text
English Command Service
  → @EnglishContentChange
  → EnglishContentChangeAspect
  → EnglishContentStateRepository (before/after persistence state)
  → EnglishContentChangedEvent
  → EnglishContentSeoListener
  → SeoContentChangedEvent
  → existing SEO cache, sitemap and notification listeners
```

The command annotation identifies the content kind and change type (updated, published or withdrawn). The state repository uses a fixed kind-to-table mapping; Command Services and the event aspect contain no SQL or table names. Only content that was publicly visible before or after a successful change emits an event. Draft-only edits do not. SEO receives the same public URL that the old annotation emitted, and its existing listeners still run after transaction commit.

Reading, Listening, Grammar lessons, Writing prompts/resources, pronunciation rules and learning bundles use this path. Their former `@SeoContentChange` annotations have been removed from English repositories. Other site modules continue using the existing SEO annotation.

Global search currently queries published database rows directly, so it has no search index or cache to refresh. The English domain event is available for a future search index or knowledge index subscriber; this stage does not add a placeholder listener or an LLM integration.

## Compatibility and verification

Existing API routes, DTOs and database schema are unchanged. Failed mutations publish no event. Focused event/SEO integration tests verify withdrawal, rejected draft withdrawal and the SEO bridge. The full backend suite passed on the local MySQL V35 test database: 61 suites, 397 tests, 0 failures, 0 errors, 1 skipped.
