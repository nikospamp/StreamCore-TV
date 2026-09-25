# StreamCore showcase

Five screens across **Phone, Tablet, Android TV, and Web**: Profiles, Home, Search, Details, and Library. The target architecture is backend-agnostic.

## Open the dashboard

After cloning or pulling this repository, open [index.html](index.html) in a browser. It works directly from disk, without a server, dependencies, login, or network access.

Choose a screen tab to compare all four clients. Select an image to inspect it, switch to actual pixels, or open the full-size original. Keyboard navigation, Escape, and responsive layouts are supported.

GitHub displays the HTML source rather than running the dashboard. Use the image links below to browse on GitHub, or download/clone the repository and open `docs/showcase/index.html` locally. Keep this folder together so the relative image and metadata links work.

## Boards and screenshots

| Screen | Comparison board | Phone | Tablet | Android TV | Web |
| --- | --- | --- | --- | --- | --- |
| Profiles | [Board](boards/profiles-board.png) | [Screenshot](screenshots/phone/profiles.png) | [Screenshot](screenshots/tablet/profiles.png) | [Screenshot](screenshots/tv/profiles.png) | [Screenshot](screenshots/web/profiles.png) |
| Home | [Board](boards/home-board.png) | [Screenshot](screenshots/phone/home.png) | [Screenshot](screenshots/tablet/home.png) | [Screenshot](screenshots/tv/home.png) | [Screenshot](screenshots/web/home.png) |
| Search | [Board](boards/search-board.png) | [Screenshot](screenshots/phone/search.png) | [Screenshot](screenshots/tablet/search.png) | [Screenshot](screenshots/tv/search.png) | [Screenshot](screenshots/web/search.png) |
| Details | [Board](boards/details-board.png) | [Screenshot](screenshots/phone/details.png) | [Screenshot](screenshots/tablet/details.png) | [Screenshot](screenshots/tv/details.png) | [Screenshot](screenshots/web/details.png) |
| Library | [Board](boards/library-board.png) | [Screenshot](screenshots/phone/library.png) | [Screenshot](screenshots/tablet/library.png) | [Screenshot](screenshots/tv/library.png) | [Screenshot](screenshots/web/library.png) |

## Capture context

Captured on **24 September 2026**, from source commit `8478e2c1dc8050b8d43f97dc62470ef8b4febfb4`, using certified TMDB Android and production web builds.

| Client | Capture environment | Original resolution |
| --- | --- | --- |
| Phone | Medium Phone emulator, Android API 36 | 1080 × 2400 |
| Tablet | Medium Tablet emulator, Android API 36 | 2560 × 1600 |
| Android TV | Television 1080p emulator, Android API 36 | 1920 × 1080 |
| Web | Chrome 153.0.8010.53, headless | 1920 × 1080 |

- The selected profile is Nikos. Phone and Tablet use their existing light theme; Android TV and Web use their dark theme.
- Home hero carousels were captured at different moments, so featured titles differ.
- Search uses **Dune** on every client. Two Web results show title-letter artwork fallbacks; these captures do not establish the cause.
- Details shows **Dune: Part Two (2024)** on all clients.
- Library shows the existing empty Continue Watching, Liked, and My List sections. Saved lists were not populated for these captures.

These are static showcase images, not an interactive app session or a claim that every application behavior was tested.

## Package and image integrity

The folder contains one offline dashboard, 20 original PNG screenshots, five comparison PNG boards, and portable metadata. Screenshot bytes are unchanged from the visually inspected originals. Boards preserve each screenshot in full, with display scaling only.

[manifest.json](manifest.json) records the source commit, capture context, image dimensions, and SHA-256 hashes of all 25 PNGs. Its paths are relative to this folder. Each `boards/*-board.json` records source hashes and display scaling; paths in those sidecars are relative to the sidecar file.

This curated showcase is intentionally tracked in Git. Credentials, browser profiles, local configuration, build logs, device dumps, and diagnostic/intermediate captures remain outside it under ignored local directories. Publishing the dashboard to a website is not required to use this package.
