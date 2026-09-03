# WEB-03 — TV-Like Browse Milestone Index

## Status

This is a non-executable milestone index. Do not assign it to an implementation owner. Execute
[WEB-03A](WEB-03A-contracts-and-shell.md) through [WEB-03F](WEB-03F-integration-gate.md).

WEB-02 is accepted and merged. WEB-03A uses immutable base
`eb5467a661fed227f80f70bd3f8a2e91c52bf102`; later WEB-03 feature tickets remain blocked until WEB-03A publishes its reviewed contract commit.

## Aggregate Goal and Scope

Deliver a distributable TMDB web browsing client covering Home, Search, Library, and Details with Android-TV visual language, browser history,
mouse input, deterministic keyboard focus, direct URL/reload behavior, profile switch/logout entry points, and an explicit temporary Player
destination. Playback remains isolated to WEB-04.

The milestone adds `:feature:home:ui-web`, `:feature:search:ui-web`, `:feature:library:ui-web`, and `:feature:details:ui-web`; extends `:webApp`
navigation/chrome; and adds cross-feature browser tests and visual evidence. It uses shared ViewModels and repository contracts. Web UI must remain
backend-agnostic and must never call TMDB directly or import provider DTOs, responses, SDKs, or client models.

Out of scope: media playback, ClientB web UI, narrow/mobile layout, SEO/SSR, DOM rewrite, Android TV behavior changes, or web-specific repository
implementations.

## Executable Tickets and Merge Wave

| Ticket | Deliverable | Immutable-base rule |
|---|---|---|
| [WEB-03A](WEB-03A-contracts-and-shell.md) | Contracts, module shells, routes, focus/history, chrome, fixtures and placeholder | Accepted WEB-02 merge SHA recorded at dispatch |
| [WEB-03B](WEB-03B-home.md) | Home | Accepted WEB-03A contract SHA |
| [WEB-03C](WEB-03C-search.md) | Search and native input | Same accepted WEB-03A contract SHA |
| [WEB-03D](WEB-03D-library.md) | Library | Same accepted WEB-03A contract SHA |
| [WEB-03E](WEB-03E-details.md) | Details | Same accepted WEB-03A contract SHA |
| [WEB-03F](WEB-03F-integration-gate.md) | B→C→D→E integration and milestone gates | Accepted WEB-03A contract SHA plus reviewed feature commits |

After WEB-03A freezes the contract, run no more than three of B–E concurrently while the root remains active; start the fourth when a slot is free.
The integration owner merges reviewed feature commits strictly B→C→D→E. All build/test processes use the serialized queue.

## Aggregate Acceptance

- Login → Profiles → Home → Search/Library → Details works against TMDB; temporary Player is visibly non-functional and cannot be mistaken for
  playback.
- Browser Back/Forward, direct Details URLs, hard reload, logout protection, and originating-item focus restoration behave correctly.
- Mouse and keyboard journeys pass in the complete Chromium/Firefox/WebKit candidate matrix at 1280×720 and 1920×1080.
- Compose UI Test v2 uses semantic node/state/focus assertions; Playwright uses the canvas-safe selector/input strategy in `docs/kmp/web-testing.md`.
- Loading, content, empty, offline, error, and long-text states have inspected visual evidence. Screenshot presence alone is not approval.
- The production browse distribution builds independently before playback work; Android TMDB and ClientB regression gates remain green.
- No provider DTO, provider SDK, direct network call, credential, or generated production configuration enters web UI/shared boundaries.

No live credential run occurs before WEB-03F's final gate. Any passing, visual-parity, cross-browser, or live claim requires evidence classified under
the ticket README taxonomy.
