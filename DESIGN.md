---
name: StreamCoreTV
description: Backend-agnostic Android streaming UI system for mobile, tablet, and TV.
colors:
  primary: "#EB5E28"
  on-primary: "#FFFFFCF2"
  primary-container: "#FFFFDBCE"
  on-primary-container: "#3A0B00"
  secondary: "#635D55"
  secondary-container: "#CCC5B9"
  tertiary: "#403D39"
  tertiary-container: "#E6E1D9"
  background-light: "#FFFFFCF2"
  on-background-light: "#252422"
  surface-light: "#FFFFFCF2"
  surface-variant-light: "#CCC5B9"
  outline-light: "#7F7A72"
  error: "#BA1A1A"
  background-dark: "#1A1918"
  on-background-dark: "#E6E2D9"
  surface-dark: "#252422"
  surface-variant-dark: "#403D39"
  outline-dark: "#968F86"
typography:
  display:
    fontFamily: "Android system default"
    fontSize: "45sp"
    fontWeight: 400
    lineHeight: "52sp"
    letterSpacing: "0sp"
  headline:
    fontFamily: "Android system default"
    fontSize: "28sp"
    fontWeight: 400
    lineHeight: "36sp"
    letterSpacing: "0sp"
  title:
    fontFamily: "Android system default"
    fontSize: "22sp"
    fontWeight: 600
    lineHeight: "28sp"
    letterSpacing: "0sp"
  body:
    fontFamily: "Android system default"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "0.5sp"
  label:
    fontFamily: "Android system default"
    fontSize: "12sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.5sp"
rounded:
  sm: "8dp"
  md: "10dp"
  lg: "12dp"
  xl: "16dp"
  full: "999dp"
spacing:
  xxs: "4dp"
  xs: "8dp"
  sm: "12dp"
  md: "16dp"
  lg: "20dp"
  xl: "24dp"
  screen-tv: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.full}"
    height: "52dp"
    padding: "0 24dp"
    typography: "{typography.label}"
  button-text:
    backgroundColor: "transparent"
    textColor: "{colors.primary}"
    rounded: "{rounded.full}"
    padding: "8dp 12dp"
    typography: "{typography.label}"
  tv-button:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.sm}"
    padding: "10dp 18dp"
    typography: "{typography.title}"
  content-card:
    backgroundColor: "{colors.surface-light}"
    textColor: "{colors.on-background-light}"
    rounded: "{rounded.md}"
    width: "140dp"
  loading-chip:
    backgroundColor: "{colors.primary-container}"
    textColor: "{colors.on-primary-container}"
    rounded: "{rounded.sm}"
    height: "30dp"
    padding: "7dp 10dp"
---

# Design System: StreamCoreTV

## 1. Overview

**Creative North Star: "The Cinematic Control Room"**

StreamCoreTV is a product UI system for a backend-agnostic Android streaming app family. The visual language is premium, cinematic, calm, and
task-focused: content artwork creates the atmosphere, while the app chrome stays restrained enough for browsing, profile management, login, and
details workflows to feel direct.

The system uses familiar streaming-category patterns without cloning competitors. Mobile and tablet screens use Material 3 structure, content-led
rows, compact forms, and adaptive density. TV screens use larger typography, 32dp screen padding, 8dp focus geometry, and explicit D-pad focus states
so the selected object is never ambiguous.

It rejects generic SaaS dashboards, marketing landing-page composition, loud orange-and-black demo styling, white or cream streaming canvases,
social-feed layouts, device mockups inside the UI, busy chrome, and provider-specific branding in shared surfaces.

**Key Characteristics:**

- Content-led hierarchy: posters, backdrops, profile imagery, and row rhythm carry the brand more than accent color.
- Restrained accent: StreamCore orange is for primary action, focus, selected state, and loading emphasis, not decoration.
- Platform-fit composition: touch surfaces stay compact; TV surfaces increase spacing, type scale, and focus affordance.
- Backend-agnostic neutrality: UI language must not expose provider DTOs, SDKs, copy, or client identities.

## 2. Colors

The palette is a restrained Material 3 scheme built around ember orange and warm cinema neutrals.

### Primary

- **Ember Action** (`primary`): StreamCore orange for primary actions, progress, selected state, and TV focus borders.
- **Ember Mist** (`primary-container`): low-intensity accent container for loading chips, fallback artwork, and active emphasis surfaces.
- **Deep Ember Ink** (`on-primary-container`): text or fallback glyphs placed on primary containers.

### Secondary

- **Warm Control Neutral** (`secondary`): subdued supporting tone for secondary emphasis and Material roles.
- **Muted Sand Surface** (`secondary-container`): warm neutral container used sparingly where Material components need a secondary fill.

### Tertiary

- **Charcoal Tertiary** (`tertiary`): dark support color for non-primary classification.
- **Soft Utility Tan** (`tertiary-container`): secondary profile and kid-safe avatar containers.

### Neutral

- **Canvas Light** (`background-light`): existing light theme background. Do not use it as the default streaming browsing canvas.
- **Ink Light** (`on-background-light`): primary readable text in light theme.
- **Surface Light** (`surface-light`): cards and panels in light theme.
- **Divider Taupe** (`outline-light`): outlines and disabled structure in light theme.
- **Cinema Black** (`background-dark`): preferred cinematic app background for mobile and tablet streaming surfaces.
- **Soft Cinema Ink** (`on-background-dark`): readable text on dark streaming backgrounds.
- **Panel Charcoal** (`surface-dark`): dark cards, login panels, and layered content surfaces.
- **Raised Charcoal** (`surface-variant-dark`): secondary dark panels and artwork fallback containers.
- **Focus Taupe** (`outline-dark`): non-primary boundaries on dark surfaces.
- **Failure Red** (`error`): validation and error affordances only.

### Named Rules

**The Artwork First Rule.** Primary orange must remain rare on catalogue and details screens; artwork and typography provide the cinematic weight.

**The No Cream Streaming Canvas Rule.** Light theme tokens exist, but browsing, details, and TV streaming surfaces should not read as white, cream, or
beige product pages.

## 3. Typography

**Display Font:** Android system default
**Body Font:** Android system default
**Label/Mono Font:** Android system default

**Character:** The type system is intentionally familiar and neutral. It should feel like Android-native product UI tuned for streaming density, not a
branded editorial layout.

### Hierarchy

- **Display** (regular, `45sp`, `52sp`): large TV and details titles where distance or artwork scale requires it.
- **Headline** (regular, `28sp`, `36sp`): screen titles, TV row headers, and important section labels.
- **Title** (semibold where needed, `22sp`, `28sp`): row titles, card titles, dialogs, and form headings.
- **Body** (regular, `16sp`, `24sp`, `0.5sp`): descriptions, helper copy, and readable text blocks. Keep long synopsis text short and scannable.
- **Label** (medium, `12sp`, `16sp`, `0.5sp`): metadata, chips, button labels, ratings, and compact status text.

### Named Rules

**The Native Product Rule.** Do not introduce display fonts into labels, buttons, fields, or metadata. System typography is a strength here.

**The Two-Line Streaming Rule.** Descriptions on browse surfaces should cap at two lines; details screens may expand, but TV copy must stay readable
at distance.

## 4. Elevation

StreamCoreTV uses tonal layering first and elevation second. Cards sit on `surface` with low tonal elevation (`2dp`) at rest. Loading chips and TV
focused cards may rise to `8dp`, but heavy shadows are not part of the vocabulary.

### Shadow Vocabulary

- **Card Rest** (`tonalElevation = 2dp`): default content cards and profile cards.
- **Focused / Status Lift** (`tonalElevation = 8dp` or `shadowElevation = 8dp`): TV focus, loading chips, and temporary status surfaces.
- **Login Panel** (`tonalElevation = 6dp` mobile/tablet, `8dp` TV): form container over cinematic background imagery.

### Named Rules

**The Tonal First Rule.** Use Material tonal elevation and surface contrast before adding visible shadow.

**The Focus Earns Lift Rule.** Elevation is allowed when the user moves focus, when async status is active, or when a form panel must separate from
artwork.

## 5. Components

### Buttons

- **Shape:** mobile primary buttons follow Material 3 defaults with a `52dp` minimum height; TV buttons use a calmer `8dp` corner radius.
- **Primary:** `StreamCoreButton` uses Material 3 `Button` with `primary` / `on-primary`, loading spinner support, and full-width form usage.
- **Hover / Focus:** TV buttons use a `2dp` primary border inset by `2dp`; focused scale remains `1f` to avoid layout shifts.
- **Secondary / Ghost / Tertiary:** `StreamCoreTextButton` is the secondary action pattern for forgot password, create profile, edit, delete, back,
  and refresh.

### Chips

- **Style:** `StreamCoreLoadingChip` uses `primary-container` with `on-primary-container`, `8dp` radius, `30dp` minimum height, and compact spinner
  plus label.
- **State:** use only for transient async status such as "Updating"; do not use chips as decorative badges unless the state is meaningful.

### Cards / Containers

- **Corner Style:** content cards use subtle `10dp` to `12dp` radius; profile cards and TV panels use `8dp`.
- **Background:** cards use `surface`; artwork fallback uses `primary-container`, `secondary-container`, or `surface-variant` depending on state.
- **Shadow Strategy:** `2dp` tonal elevation at rest; `8dp` tonal elevation on TV focus.
- **Border:** TV focused cards use a `2dp` primary border. Mobile cards do not use decorative borders.
- **Internal Padding:** mobile card metadata uses `10dp`; TV card metadata uses `14dp`; profile cards use `16dp`.

### Inputs / Fields

- **Style:** login forms use Material 3 `OutlinedTextField` with single-line email and password fields.
- **Focus:** native field focus is acceptable on touch surfaces; TV fields require deterministic focus order through `FocusRequester` /
  `focusProperties`.
- **Error / Disabled:** validation errors use `isError` and supporting text; loading disables input and action controls.

### Navigation

- **Style:** navigation is route-driven with platform-specific destinations for login, profiles, home, editor, and details.
- **Motion:** route transitions use `240ms` slide, `140ms` fade, `90ms` exit fade, and `FastOutSlowInEasing`.
- **Platform treatment:** mobile, tablet, and TV have separate route/screen implementations. Do not collapse TV focus behavior into mobile components
  with flags.

### Content Artwork

- **Style:** `StreamCoreContentImage` fills its bounds with Coil `AsyncImage`, crops content, crossfades by default, and shows a centered text
  fallback.
- **Shape:** shape comes from the consuming card or shared transition clip. Poster cards use `2:3`; landscape and hero artwork use `16:9`.

## 6. Do's and Don'ts

### Do:

- **Do** keep the target architecture backend-agnostic: feature UI consumes app models, UI state, actions, and effects, not provider DTOs or SDK
  types.
- **Do** use `StreamCoreButton`, `StreamCoreTextButton`, `StreamCoreTvButton`, `StreamCoreContentImage`, and other app-owned components before raw
  Material primitives in feature UI.
- **Do** keep StreamCore orange (`primary`) reserved for primary action, selection, focus, and meaningful status.
- **Do** use stable lazy keys and content types for content rows and grids.
- **Do** use TV-specific focus states with a `2dp` primary border and enough spacing for D-pad movement.
- **Do** preserve preview-friendly composables wrapped in `StreamCoreTVTheme`.

### Don't:

- **Don't** build generic SaaS dashboards for subscriber-facing streaming flows.
- **Don't** use marketing landing-page composition for authenticated app screens.
- **Don't** lean into loud orange-and-black demo styling; orange is an accent, not the whole brand.
- **Don't** use white or cream page backgrounds as the default for streaming browse, details, or TV surfaces.
- **Don't** clone Netflix, Disney+, HBO Max, Prime Video, or provider-specific layouts, logos, icon shapes, or copy.
- **Don't** introduce social-feed layouts into catalogue browsing.
- **Don't** place device mockups inside the app UI.
- **Don't** add busy chrome, random notification actions, decorative badges, or non-content UI noise.
- **Don't** ship tiny TV controls or subtle TV focus rings.
- **Don't** put provider-specific branding, DTO language, SDK concepts, or client-specific model names in shared UI.
