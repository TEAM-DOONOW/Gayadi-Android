# Gemini Code Review Style Guide

This guide contains repository-specific review rules for the Gayadi Android project.
Only report actionable issues introduced or exposed by the pull request. Explain the
impact and suggest the smallest appropriate fix. Do not request broad refactoring that
is unrelated to the change.

## Review Priority

Prioritize issues that can affect correctness, stability, maintainability, security, or user experience.

Avoid excessive comments about minor formatting or personal style preferences.

Review priority:

1. Bugs and incorrect behavior
2. State management issues
3. Coroutine and lifecycle issues
4. Jetpack Compose performance issues
5. Security and sensitive data exposure
6. Architecture violations
7. Maintainability
8. Minor style improvements

---

## Kotlin

Follow Kotlin idiomatic conventions.

- Prefer immutable values (`val`) over mutable values (`var`).
- Avoid unnecessary nullable types.
- Avoid unsafe `!!` unless absolutely necessary.
- Prefer safe calls and explicit null handling.
- Avoid deeply nested control flow.
- Prefer clear and descriptive names.
- Avoid unnecessary scope functions (`let`, `run`, `apply`, etc.).
- Flag duplicated business logic.
- Flag magic numbers or strings when they should be constants.
- Prefer sealed interfaces/classes for finite UI states or events.

Do not suggest refactoring solely for stylistic preference when the existing code is clear and correct.

---

## Jetpack Compose

Review Compose code with recomposition and state management in mind.

- Composable functions should remain as stateless as reasonably possible.
- State should be hoisted when ownership belongs to a parent or ViewModel.
- Do not keep business logic inside Composable functions.
- Avoid unnecessary recompositions.
- Flag unstable or unnecessarily recreated objects passed to Composables.
- Use `remember` only when the value should survive recomposition.
- Use `rememberSaveable` when UI state should survive configuration changes.
- Avoid creating expensive objects directly during recomposition.
- Prefer immutable UI state.
- Avoid modifying state directly from multiple Composables.
- Flag side effects executed directly inside a Composable body.
- Use appropriate side-effect APIs such as `LaunchedEffect` and `DisposableEffect`.
- Verify that `LazyColumn` / `LazyRow` items use stable keys when necessary.

---

## State Management

Use unidirectional data flow.

Recommended flow:

UI Event
→ ViewModel
→ UseCase
→ Repository
→ DataSource

State flows in the opposite direction back to the UI.

- ViewModels own screen-level state.
- Expose immutable `StateFlow` to the UI.
- Keep `MutableStateFlow` private.
- UI should send events instead of directly modifying ViewModel state.
- Avoid storing the same state in multiple layers.
- Avoid unnecessary duplicated state between Compose and ViewModel.

Example:

```kotlin
private val _uiState = MutableStateFlow(ScreenUiState())
val uiState: StateFlow<ScreenUiState> = _uiState.asStateFlow()
```

---

## Coroutines and Lifecycle

- Use structured concurrency and lifecycle-aware scopes.
- ViewModels should launch screen work in `viewModelScope` unless another owner is explicit.
- Composables should collect flows with lifecycle-aware APIs such as
  `collectAsStateWithLifecycle`.
- Flag work that can outlive its intended owner or leak an Activity, Context, View, or
  Composable.
- Do not swallow `CancellationException` or convert coroutine cancellation into a normal
  failure state.
- Check dispatcher usage for blocking I/O or CPU-heavy work on the main thread.
- Flag races, duplicate requests, stale responses, and effects whose keys do not match
  their dependencies.

---

## Architecture and Module Boundaries

Follow the dependency rules documented in `ARCHITECTURE.md`.

- `domain` must not depend on Android APIs or other project modules.
- `data` implements repository contracts declared by `domain` and maps DTOs or entities
  to domain models.
- `feature/*` contains feature UI, `UiState`, UI events, and ViewModels. Feature modules
  should depend on domain abstractions, not concrete data implementations.
- Presentation code must not access Firebase SDKs, data sources, or repository
  implementations directly.
- `di` is the composition root that wires data implementations to domain contracts and
  use cases.
- `app` owns application entry points, navigation, and ViewModel lifecycle integration.
- Feature modules must not depend directly on one another. Connect screens through
  navigation owned by `app`.
- Keep business rules in domain use cases and avoid duplicating them in UI or data code.

---

## UI and Design System

For every new or changed Compose page, verify compliance with `design.md`.

- Reuse components and tokens from `core/designsystem` and `core/ui` before accepting a
  feature-local implementation.
- Use `GayadiTheme`, existing color tokens, and `MaterialTheme.typography`; flag arbitrary
  colors, typography, spacing, or shapes that duplicate established tokens.
- Preserve the standard 20dp horizontal content padding unless the documented design
  pattern requires otherwise.
- Verify loading, empty, error, and success states for asynchronous screens.
- Interactive elements should provide at least a 48dp touch target.
- Meaningful icons and images require an appropriate Korean `contentDescription`.
  Decorative images should use `null`.
- Check system-bar insets, keyboard behavior, long text, and overlap with fixed bottom
  navigation or actions.
- If a pull request introduces a reusable design decision, it must update `design.md` in
  the same change.

---

## Security and Sensitive Data

- Never request, read, log, expose, copy, or commit values from `.env` or `.env.*` files.
  `.env.example` is the only permitted environment-file reference.
- Never expose private keys, signing files, credentials, tokens, or files under
  `secrets/`. This includes `*.key`, `*.pem`, `*.p12`, `*.jks`, and `*.keystore`.
- Flag hardcoded credentials, API keys, private endpoints, personal data, or sensitive
  values in source code, tests, logs, screenshots, and generated artifacts.
- Do not recommend logging sensitive request or response bodies as a debugging fix.
- Review input validation, authorization boundaries, exported Android components,
  pending intents, deep links, WebViews, and local storage when affected by a change.
- Prefer placeholders from `.env.example` when configuration structure must be shown.

---

## Error Handling and Data Integrity

- Errors should be represented explicitly and converted to user-facing states at the
  appropriate layer.
- Do not silently ignore failures or catch broad exceptions without a recovery action.
- Preserve the original cause when mapping exceptions.
- Validate nullable, empty, malformed, duplicate, and partial data at system boundaries.
- Check that retries and repeated UI actions are idempotent where duplicate writes are
  possible.

---

## Tests

- Require tests for new or changed business rules, bug fixes, mappings, repository
  behavior, and non-trivial ViewModel state transitions.
- Prefer deterministic tests that do not depend on real Firebase services, wall-clock
  timing, the network, or execution order.
- Verify success, failure, empty, boundary, and cancellation paths when relevant.
- A bug fix should include a regression test when the behavior can be tested reasonably.
- Do not request low-value tests that merely duplicate implementation details.

---

## Review Comment Quality

- Comment only when there is a concrete correctness, security, architecture,
  performance, accessibility, or maintainability impact.
- Point to the affected code and describe the failure scenario or violated repository
  rule.
- Distinguish confirmed defects from questions or uncertain risks.
- Avoid repeating the same root cause across multiple comments.
- Do not comment on generated files, build outputs, binaries, local configuration, or
  ignored paths.
- Do not approve secrets merely because they appear in test or sample code; safe sample
  values must be obvious placeholders.
