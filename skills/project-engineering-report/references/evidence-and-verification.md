# Evidence and verification

## Evidence hierarchy

Prefer the source closest to enforced behavior:

1. Applied database migrations for the physical schema.
2. Controllers plus security configuration for the HTTP surface and access rules.
3. Service implementations for transactions and cross-table business rules.
4. Router definitions and page components for reachable UI behavior.
5. Executed tests and builds for behavior that was actually verified.
6. Read-only production observations for deployed availability and visible behavior.
7. README files and existing reports as leads that must be checked against the sources above.

Generated ORM entities can omit indexes, constraints, bridge tables, triggers, or later migration changes. Do not treat them as the sole database authority.

## Repository checks

Capture the following before writing:

```text
git status --short --branch
git rev-parse --show-toplevel
git rev-parse HEAD
git show -s --format=%cI HEAD
git describe --tags --always --dirty
git remote -v
```

When network access is authorized, fetch remote refs without changing the working tree and compare the target commit with the relevant GitHub or Gitee branch. Record the comparison result. A remote display name or release page does not override the checked-out source unless the user selected that remote version.

Separate committed version evidence from uncommitted files. Do not reset, clean, stash, or rewrite user changes while preparing documentation.

## Production checks

Use read-only requests and browser inspection. Record the URL, observation time, response status, redirect chain, visible version marker, and relevant headers. Inspect public assets or API responses only when access is expected and no credentials or personal data are exposed.

Production availability proves deployment state, not source equivalence. Claim that production matches a commit only when a build identifier, asset hash, release marker, or other verifiable link establishes it.

## Precise language

Use evidence-qualified statements:

- "Migration V12 defines..."
- "The router exposes 18 reachable routes backed by 15 page components."
- "The build passed with command ..."
- "A read-only request on the observation date returned HTTP 200."
- "The source suggests ..., but runtime behavior was not verified."

Avoid "fully secure", "high performance", "production ready", or "all tests pass" unless the performed checks justify the entire claim.

## Final consistency checks

- Recount route definitions, unique page components, endpoints, Java or backend source files, tables, real columns, indexes, and FK relations from source.
- Ensure report totals exclude SQL constraint lines accidentally parsed as columns.
- Ensure all ER child and parent tables exist; confirm child columns and referenced parent columns.
- Confirm prefixes and base paths are not duplicated when joining controller and method routes.
- Confirm every class tree entry is a real path and every comment matches the class responsibility.
- Check balanced HTML tags, unique anchors, valid internal links, parseable JavaScript, and non-empty SVG output.
- Search the deliverables for `TODO`, `TBD`, `placeholder`, fabricated example domains, and stale version strings.
- Open each report at its first section, database section, API section, and class tree; test search, filters, expanders, and print preview.
