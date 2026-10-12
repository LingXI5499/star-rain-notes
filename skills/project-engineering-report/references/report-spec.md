# Report specification

Use this structure for a complete current-state engineering report. Adapt labels to the project, but do not omit a section merely because its evidence is distributed across multiple modules.

## 1. Scope and evidence

- Product name, target version, commit SHA, commit time, branch or tag, repository root, remote URLs, and working-tree state.
- Evidence sources and their roles: source, migrations, generated artifacts, executed tests, remote repository, and production site.
- Explicit boundaries: included modules, excluded local changes, shared services, external systems, and unverified runtime claims.
- A traceability note explaining how readers can locate each fact in the repository.

## 2. Requirements analysis

Describe the system from the user's point of view before discussing implementation.

- Product purpose, users, roles, permissions, and primary user journeys.
- A complete functional requirement catalog. For each requirement give its trigger, preconditions, input, processing rules, output, state changes, permission checks, error states, and acceptance criteria.
- Page-level behavior including empty, loading, success, validation, authorization, conflict, and failure states.
- Business rules such as uniqueness, ownership, quotas, ordering, lifecycle transitions, soft deletion, visibility, and cross-module dependencies.
- Non-functional requirements supported by evidence: security, performance, consistency, availability, accessibility, compatibility, observability, backup, and recovery.
- A requirement-to-page, requirement-to-API, and requirement-to-table trace where the system is large enough to need it.

Avoid inventing stakeholder intent. Translate implementation evidence into current behavior and label inferred requirements as inference.

## 3. System design

- Deployment and runtime topology: browser, reverse proxy, frontend assets, backend processes, database, cache, object storage, third-party APIs, and scheduled jobs.
- Module boundaries and ownership of data and behavior.
- Frontend responsibilities: router, layouts, stores, API layer, authentication state, forms, validation, rendering, and shared components.
- Backend responsibilities: HTTP boundary, security pipeline, validation, service transactions, persistence, external gateways, events, and error mapping.
- Authentication, authorization, CSRF or CORS, session or token lifecycle, password handling, rate limiting, and audit behavior.
- Transaction boundaries, concurrency control, idempotency, cache invalidation, consistency guarantees, and failure recovery when present.
- Detailed sequence narratives for important business flows. Each narrative should name the page, endpoint, controller, request model, service, data operations, response model, and visible error behavior.

Use diagrams where they improve comprehension, and accompany every diagram with a precise text explanation so the report remains useful when diagrams cannot be rendered.

## 4. Database design

Start with a database summary by domain and provide the exact total number of tables, columns, foreign keys, indexes, and views found in the target version.

For every table include:

- Physical table name and Chinese business name.
- Purpose and owning module.
- Every column's physical name, SQL type, Chinese meaning, nullability, default, PK or FK role, auto-generation behavior, and constraints.
- Unique keys, ordinary indexes, check constraints, referenced keys, and delete or update actions.
- Creation or modification migration and any storage behavior that code enforces outside the database.

Document bridge tables, runtime support tables, backup or compatibility tables, history tables, and external-boundary entities. Explain cross-table rules implemented by services when the database has no corresponding constraint.

Provide a complete Crow's Foot ER diagram and smaller domain diagrams when the complete graph is dense. The diagram must agree with the field tables.

## 5. Frontend pages and interactions

Give both the total number of page components and the total number of user-reachable routes. Explain why counts differ when routes reuse components, redirect, or provide aliases.

For every page include:

- Component path, route path, route name, layout, access level, and entry points.
- Purpose, visible sections, controls, forms, validation, state sources, APIs used, and permission behavior.
- Navigation and interaction sequence, including modal, drawer, pagination, filtering, sorting, upload, editor, detail, and back-navigation behavior.
- Loading, empty, partial, success, failure, expired-session, forbidden, and not-found states where applicable.

Do not omit detail pages, tutorial or blog pages, administration pages, callback pages, utility pages, or hidden routes merely because they are less prominent in the navigation.

## 6. API and class design

Document API-wide conventions first: base path, authentication, authorization, CSRF or CORS, content types, timestamps, identifiers, pagination, sorting, error envelope, validation errors, and idempotency.

Create a complete endpoint catalog with method, path, Chinese purpose, access level, request model, response model, controller method, service entry point, and major error outcomes. Then provide standard interface definitions for important endpoints:

- Name and business purpose.
- Method and full path.
- Headers, path variables, query parameters, request body, field types, required flags, validation, and examples.
- Success status and response body.
- Error statuses and machine-readable response examples.
- Authorization, transaction, idempotency, and side effects.

Show the actual class chain from route to persistence and response. Include a repository tree using real paths and a one-line Chinese comment for every relevant class or file. Cover controllers, DTOs, view models, services, implementations, repositories or mappers, entities, security, configuration, gateways, events, jobs, exceptions, builders, renderers, caches, and utilities that participate in the documented behavior.

## 7. Development, tests, deployment, and operations

- Real local development chain: required runtimes, configuration sources, database preparation, migration command, backend start, frontend start, build, and smoke check.
- Test inventory by layer, plus the exact commands and observed results from this analysis.
- Packaging and deployment chain from source checkout to dependency installation, migrations, builds, process startup, reverse proxy, TLS, health checks, and post-deploy verification.
- Runtime configuration names without secret values.
- Logs, metrics, alerts, scheduled tasks, database backup, object-storage backup, restore testing, rollback or roll-forward procedure, and incident checks that actually exist.
- Known limits and unverified points, written precisely rather than as generic recommendations.

## Standalone HTML requirements

- UTF-8, semantic headings, stable internal anchors, responsive layout, and print styles.
- Embedded CSS and JavaScript only; no network dependency for presentation.
- A linked catalog index (目录索引): sticky sidebar or equivalent covering report files, sections, modules, and physical tables; searchable; print as the first directory page.
- Search and filters for pages, tables, endpoints, and classes.
- Collapsible detail sections with visible summaries and accessible controls.
- Tables that remain readable on narrow screens and in print.
- SVG diagrams with legends, markers, readable labels, and text descriptions.
- A generated-at timestamp may be included, but the report's project version must come from repository evidence.
