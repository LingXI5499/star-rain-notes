# ER and class design conventions

## Physical Crow's Foot ER diagrams

Model the implemented database, not a conceptual approximation.

Each entity box must show:

- Physical table name followed by the Chinese business name.
- Primary keys and foreign keys.
- The fields needed to understand relationships and identity, with SQL types and concise Chinese meanings.
- A visual distinction for PK, FK, unique, nullable, and external-boundary fields.

Each relationship must show:

- Child FK column and referenced parent key.
- Crow's Foot cardinality at both ends: exactly one, zero or one, one or many, or zero or many.
- Identifying or non-identifying status when relevant.
- Database delete and update behavior such as `CASCADE`, `RESTRICT`, `SET NULL`, or the engine default.
- A short Chinese relationship label when it clarifies the business meaning.

Draw bridge tables explicitly. Show self-references and composite keys accurately. Shared tables owned by another module may appear as dashed external-boundary entities, but their keys and relationship direction must remain explicit.

For a large schema, provide one complete relationship graph plus domain diagrams. Domain diagrams improve readability but never replace the complete graph. Follow each diagram with a text description of the entities, cardinalities, lifecycle effects, and constraints enforced only by service code.

Do not use an ER diagram as a substitute for per-table field definitions. The report needs both.

## Class and file structure

Use the real repository hierarchy. A typical backend request chain is:

```text
HTTP route
  -> Controller
  -> Request DTO and validation
  -> Service interface or implementation
  -> Mapper or repository
  -> Entity
  -> View or response DTO
```

Extend the chain with the components that actually participate: authentication filters, permission evaluators, configuration, transaction boundaries, external providers, gateways, error handlers, domain events, scheduled jobs, caches, builders, renderers, and utilities.

For each relevant file show its repository-relative path and a one-line Chinese responsibility comment. Preserve package and directory nesting so readers can locate it directly. Do not infer a class merely because a conventional architecture would contain one.

For frontend code, include routes, views or pages, layouts, API clients, stores, shared components, composables or hooks, validation schemas, and model definitions. State which route uses each page and which API methods it calls.

The endpoint catalog and class tree must agree: every documented controller method should be discoverable in the tree, and every important business flow should identify the concrete service and persistence classes that implement it.
