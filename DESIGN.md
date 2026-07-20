---
name: StreamCoreTV
description: Backend-agnostic Android streaming UI system for mobile, tablet, and TV.
colors:
  primary: "#EB5E28"
  scrim: "#000000"
  on-primary-light: "#2B1004"
  primary-container-light: "#FFE0D2"
  on-primary-container-light: "#2B1004"
  inverse-primary-light: "#FFB59A"
  secondary-light: "#5B626B"
  on-secondary-light: "#FFFFFF"
  secondary-container-light: "#D8DEE6"
  on-secondary-container-light: "#171C22"
  tertiary-light: "#4F635D"
  on-tertiary-light: "#FFFFFF"
  tertiary-container-light: "#D3E5DE"
  on-tertiary-container-light: "#0B1F1A"
  background-light: "#F5F6F8"
  on-background-light: "#191C20"
  surface-light: "#FAFAFB"
  on-surface-light: "#191C20"
  surface-variant-light: "#DDE1E7"
  on-surface-variant-light: "#41464D"
  inverse-surface-light: "#2F343A"
  inverse-on-surface-light: "#F0F2F4"
  surface-dim-light: "#D8DCE2"
  surface-bright-light: "#FAFAFB"
  surface-container-lowest-light: "#FFFFFF"
  surface-container-low-light: "#F0F2F5"
  surface-container-light: "#E9ECEF"
  surface-container-high-light: "#E1E5EA"
  surface-container-highest-light: "#D8DDE3"
  outline-light: "#727982"
  outline-variant-light: "#C1C7CF"
  error-light: "#BA1A1A"
  on-error-light: "#FFFFFF"
  error-container-light: "#FFDAD6"
  on-error-container-light: "#410002"
  on-primary-dark: "#3A1404"
  primary-container-dark: "#682500"
  on-primary-container-dark: "#FFE1D6"
  inverse-primary-dark: "#A63D0F"
  secondary-dark: "#C3CAD3"
  on-secondary-dark: "#2C3137"
  secondary-container-dark: "#444B54"
  on-secondary-container-dark: "#DCE2EA"
  tertiary-dark: "#B7CFC6"
  on-tertiary-dark: "#24332F"
  tertiary-container-dark: "#354A44"
  on-tertiary-container-dark: "#D5EBE2"
  background-dark: "#101214"
  on-background-dark: "#E4E7EB"
  surface-dark: "#171A1D"
  on-surface-dark: "#E4E7EB"
  surface-variant-dark: "#3F454D"
  on-surface-variant-dark: "#C2C8D0"
  inverse-surface-dark: "#E4E7EB"
  inverse-on-surface-dark: "#1B1E22"
  surface-dim-dark: "#101214"
  surface-bright-dark: "#3A3F46"
  surface-container-lowest-dark: "#090A0B"
  surface-container-low-dark: "#14171A"
  surface-container-dark: "#1C2024"
  surface-container-high-dark: "#262B30"
  surface-container-highest-dark: "#31373D"
  outline-dark: "#8E969F"
  outline-variant-dark: "#3F454D"
  error-dark: "#FFB4AB"
  on-error-dark: "#690005"
  error-container-dark: "#93000A"
  on-error-container-dark: "#FFDAD6"
typography:
  display-large:
    fontFamily: "Android system default"
    fontSize: "45sp"
    fontWeight: 400
    lineHeight: "52sp"
    letterSpacing: "0sp"
  display-medium:
    fontFamily: "Android system default"
    fontSize: "36sp"
    fontWeight: 400
    lineHeight: "44sp"
    letterSpacing: "0sp"
  display-small:
    fontFamily: "Android system default"
    fontSize: "32sp"
    fontWeight: 400
    lineHeight: "40sp"
    letterSpacing: "0sp"
  headline-large:
    fontFamily: "Android system default"
    fontSize: "28sp"
    fontWeight: 400
    lineHeight: "36sp"
    letterSpacing: "0sp"
  headline-medium:
    fontFamily: "Android system default"
    fontSize: "24sp"
    fontWeight: 400
    lineHeight: "32sp"
    letterSpacing: "0sp"
  headline-small:
    fontFamily: "Android system default"
    fontSize: "20sp"
    fontWeight: 400
    lineHeight: "28sp"
    letterSpacing: "0sp"
  title-large:
    fontFamily: "Android system default"
    fontSize: "22sp"
    fontWeight: 600
    lineHeight: "28sp"
    letterSpacing: "0sp"
  title-medium:
    fontFamily: "Android system default"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "24sp"
    letterSpacing: "0sp"
  title-small:
    fontFamily: "Android system default"
    fontSize: "14sp"
    fontWeight: 600
    lineHeight: "20sp"
    letterSpacing: "0sp"
  body-large:
    fontFamily: "Android system default"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "0.5sp"
  body-medium:
    fontFamily: "Android system default"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
    letterSpacing: "0.25sp"
  body-small:
    fontFamily: "Android system default"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "16sp"
    letterSpacing: "0.4sp"
  label-large:
    fontFamily: "Android system default"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.1sp"
  label-medium:
    fontFamily: "Android system default"
    fontSize: "12sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.5sp"
  label-small:
    fontFamily: "Android system default"
    fontSize: "11sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.5sp"
  mobile-top-ten-rank:
    fontFamily: "Android system default"
    fontSize: "118sp"
    fontWeight: 800
    lineHeight: "96sp"
    letterSpacing: "0sp"
  tablet-top-ten-rank:
    fontFamily: "Android system default"
    fontSize: "92sp"
    fontWeight: 800
    lineHeight: "82sp"
    letterSpacing: "0sp"
rounded:
  xs: "6dp"
  sm: "8dp"
  md: "10dp"
  lg: "12dp"
  xl: "16dp"
  full: "999dp"
spacing:
  tiny: "4dp"
  small: "8dp"
  medium: "12dp"
  large: "16dp"
  extra-large: "24dp"
  mobile-screen-horizontal: "20dp"
  tablet-screen-horizontal: "32dp"
  tv-screen: "32dp"
components:
  button-primary-light:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary-light}"
    rounded: "{rounded.full}"
    height: "52dp"
    padding: "0 24dp"
    typography: "{typography.label-large}"
  button-primary-dark:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary-dark}"
    rounded: "{rounded.full}"
    height: "52dp"
    padding: "0 24dp"
    typography: "{typography.label-large}"
  button-compact-light:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary-light}"
    rounded: "{rounded.full}"
    height: "38dp"
    padding: "0 16dp"
    typography: "{typography.label-large}"
  button-compact-dark:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary-dark}"
    rounded: "{rounded.full}"
    height: "38dp"
    padding: "0 16dp"
    typography: "{typography.label-large}"
  button-text-light:
    backgroundColor: "transparent"
    textColor: "{colors.primary}"
    rounded: "{rounded.full}"
    padding: "8dp 12dp"
    typography: "{typography.label-large}"
  button-text-dark:
    backgroundColor: "transparent"
    textColor: "{colors.primary}"
    rounded: "{rounded.full}"
    padding: "8dp 12dp"
    typography: "{typography.label-large}"
  tv-button-dark:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary-dark}"
    rounded: "{rounded.sm}"
    padding: "10dp 18dp"
  mobile-hero:
    rounded: "{rounded.xl}"
    padding: "16dp"
    size: "available width x 16:9"
  mobile-continue-watching:
    rounded: "{rounded.md}"
    width: "192dp"
    size: "192dp x 108dp artwork"
  mobile-poster:
    rounded: "{rounded.md}"
    width: "120dp"
    size: "120dp x 180dp"
  mobile-landscape:
    rounded: "{rounded.md}"
    width: "192dp"
    size: "192dp x 108dp"
  mobile-top-ten:
    rounded: "{rounded.md}"
    width: "160dp"
    height: "180dp"
    size: "120dp x 180dp poster"
  mobile-pager-indicator:
    size: "5dp; selected 18dp x 5dp"
  input-outlined:
    backgroundColor: "transparent"
    textColor: "{colors.on-surface-light}"
    rounded: "{rounded.xs}"
    height: "52dp"
    padding: "0 14dp"
  tv-content-card-focused:
    backgroundColor: "{colors.surface-dark}"
    textColor: "{colors.on-surface-dark}"
    rounded: "{rounded.lg}"
    width: "250dp"
  profile-card:
    backgroundColor: "{colors.surface-light}"
    textColor: "{colors.on-surface-light}"
    rounded: "{rounded.sm}"
    width: "180dp"
    padding: "16dp"
  loading-chip-light:
    backgroundColor: "{colors.primary-container-light}"
    textColor: "{colors.on-primary-container-light}"
    rounded: "{rounded.sm}"
    height: "32dp"
    padding: "8dp 12dp"
  loading-chip-dark:
    backgroundColor: "{colors.primary-container-dark}"
    textColor: "{colors.on-primary-container-dark}"
    rounded: "{rounded.sm}"
    height: "32dp"
    padding: "8dp 12dp"
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

The shared spacing scale is `4dp`, `8dp`, `12dp`, `16dp`, and `24dp`. Platform layout tokens intentionally add `20dp` mobile horizontal padding and
`32dp` tablet/TV horizontal padding; these are screen-layout values, not additional shared spacing steps.

## 2. Colors

The palette is a restrained Material 3 scheme built around ember orange and smoky cinema neutrals.

### Primary

- **Ember Action** (`primary`): StreamCore orange for primary actions, progress, selected state, and TV focus borders.
- **Ember Mist** (`primary-container-light` / `primary-container-dark`): theme-specific low-intensity accent containers for loading chips, fallback
  artwork, and active emphasis surfaces.
- **Deep Ember Ink** (`on-primary-light` / `on-primary-dark`): theme-specific foregrounds for primary actions.
- **Container Ink** (`on-primary-container-light` / `on-primary-container-dark`): theme-specific text or fallback glyphs placed on primary containers.

### Secondary

- **Control Neutral** (`secondary-light` / `secondary-dark`): theme-specific smoky support tones for secondary emphasis and Material roles.
- **Control Container** (`secondary-container-light` / `secondary-container-dark`): theme-specific neutral containers used sparingly where Material
  components need a secondary fill.

### Tertiary

- **Muted Utility Green** (`tertiary-light` / `tertiary-dark`): theme-specific utility support colors for non-primary classification.
- **Soft Utility Container** (`tertiary-container-light` / `tertiary-container-dark`): theme-specific profile and kid-safe avatar containers.

### Neutral

- **Cinematic Light Canvas** (`background-light`): smoky light theme background for mobile and tablet surfaces.
- **Ink Light** (`on-background-light`): primary readable text in light theme.
- **Surface Light** (`surface-light`): cards and panels in light theme.
- **Supporting Ink Light** (`on-surface-variant-light`): metadata and secondary content in light theme.
- **Divider Smoke** (`outline-light`): outlines and disabled structure in light theme.
- **Cinema Black** (`background-dark`): preferred cinematic app background for dark mobile/tablet and all TV streaming surfaces.
- **Soft Cinema Ink** (`on-background-dark`): readable text on dark streaming backgrounds.
- **Panel Charcoal** (`surface-dark`): dark cards, login panels, and layered content surfaces.
- **Raised Charcoal** (`surface-variant-dark`): secondary dark panels and artwork fallback containers.
- **Supporting Ink Dark** (`on-surface-variant-dark`): metadata and secondary content in dark theme.
- **Focus Taupe** (`outline-dark`): non-primary boundaries on dark surfaces.
- **Tonal Surface Ladders** (`surface-container-lowest-*` through `surface-container-highest-*`): theme-specific Material tonal elevation without
  decorative shadows.
- **Failure Red** (`error-light` / `error-dark`): theme-specific validation and error affordances only.

### Named Rules

**The Artwork First Rule.** Primary orange must remain rare on catalogue and details screens; artwork and typography provide the cinematic weight.

**The No Cream Streaming Canvas Rule.** Light theme exists for mobile and tablet, but it must read smoky and cinematic, not white, cream, or beige.
TV always uses the dark scheme.

**The Theme Pairing Rule.** Every container and foreground must come from the same theme suffix. Never place a `*-light` foreground on a `*-dark`
container or the reverse. Feature code consumes `MaterialTheme.colorScheme` roles instead of raw palette values.

## 3. Typography

**Display Font:** Android system default
**Body Font:** Android system default
**Label/Mono Font:** Android system default

**Character:** The type system is intentionally familiar and neutral. It should feel like Android-native product UI tuned for streaming density, not a
branded editorial layout.

### Hierarchy

- **Display Large** (regular, `45sp`, `52sp`): large TV and details titles where distance or artwork scale requires it.
- **Display Medium** (regular, `36sp`, `44sp`): secondary display moments on large surfaces.
- **Display Small** (regular, `32sp`, `40sp`): mobile hero titles; use bold weight locally when artwork needs stronger hierarchy.
- **Headline Large** (regular, `28sp`, `36sp`): screen titles, TV row headers, and important section labels.
- **Headline Medium** (regular, `24sp`, `32sp`): secondary screen and panel headings.
- **Headline Small** (regular, `20sp`, `28sp`): compact section headings.
- **Title Large** (semibold, `22sp`, `28sp`): dialogs and prominent form headings.
- **Title Medium** (semibold, `16sp`, `24sp`): mobile browse shelf titles and compact card headings.
- **Title Small** (semibold, `14sp`, `20sp`): dense headings where `Title Medium` is too prominent.
- **Body Large** (regular, `16sp`, `24sp`, `0.5sp`): primary descriptions and readable text blocks.
- **Body Medium** (regular, `14sp`, `20sp`, `0.25sp`): supporting descriptions and helper copy.
- **Body Small** (regular, `12sp`, `16sp`, `0.4sp`): compact browse hero descriptions.
- **Label Large** (medium, `14sp`, `20sp`, `0.1sp`): button labels and prominent compact actions.
- **Label Medium** (medium, `12sp`, `16sp`, `0.5sp`): hero classification and prominent metadata.
- **Label Small** (medium, `11sp`, `16sp`, `0.5sp`): card titles, card metadata, ratings, and dense status text.
- **Mobile Top Ten Rank** (extra bold, `118sp`, `96sp`): oversized outlined ranking numerals on mobile browse shelves only.
- **Tablet Top Ten Rank** (extra bold, `92sp`, `82sp`): oversized outlined ranking numerals on tablet browse shelves only.

### Named Rules

**The Native Product Rule.** Do not introduce display fonts into labels, buttons, fields, or metadata. System typography is a strength here.

**The Exact Role Rule.** New screens must consume the named Material typography role that matches the intended hierarchy. Do not substitute a generic
`Display`, `Title`, `Body`, or `Label` token for the concrete `Large`, `Medium`, or `Small` role.

**The Two-Line Streaming Rule.** Descriptions on browse surfaces should cap at two lines; details screens may expand, but TV copy must stay readable
at distance.

## 4. Elevation

StreamCoreTV uses tonal layering first and elevation second. Mobile/tablet browse artwork stays flat; elevated card surfaces use low tonal elevation
(`2dp`) at rest. Loading chips and TV focused cards may rise to `8dp`, but heavy shadows are not part of the vocabulary.

### Shadow Vocabulary

- **Card Rest** (`tonalElevation = 2dp`): elevated recommendation surfaces and profile cards at rest.
- **Focused / Status Lift** (`tonalElevation = 8dp` or `shadowElevation = 8dp`): TV focus, loading chips, and temporary status surfaces.
- **Login Panel** (`tonalElevation = 8dp`): mobile, tablet, and TV form containers over cinematic background imagery.

### Named Rules

**The Tonal First Rule.** Use Material tonal elevation and surface contrast before adding visible shadow.

**The Focus Earns Lift Rule.** Elevation is allowed when the user moves focus, when async status is active, or when a form panel must separate from
artwork.

## 5. Components

### Buttons

- **Shape:** standard mobile primary buttons follow Material 3 defaults with a `52dp` minimum height; compact browse CTAs retain the same pill shape
  at `38dp`; TV buttons use a calmer `8dp` corner radius.
- **Primary:** `StreamCoreButton` uses Material 3 `Button` with the active theme's `primary` / `onPrimary` roles, loading spinner support, and
  full-width form usage.
- **Compact:** `StreamCoreButton(size = StreamCoreButtonSize.Compact)` is reserved for artwork-contained browse actions such as the home hero
  "Details" CTA. It uses a `38dp` minimum height and `16dp` horizontal padding; it is not a replacement for `52dp` form actions.
- **Hover / Focus:** TV buttons use a `2dp` primary border inset by `2dp`; focused scale remains `1f` to avoid layout shifts.
- **Secondary / Ghost / Tertiary:** `StreamCoreTextButton` is the secondary action pattern for forgot password, create profile, edit, delete, back,
  and refresh.

### Chips

- **Style:** `StreamCoreLoadingChip` uses `primary-container` with `on-primary-container`, `8dp` radius, `32dp` minimum height, `12dp` horizontal
  padding, `8dp` vertical padding, and a compact spinner plus label.
- **State:** use only for transient async status such as "Updating"; do not use chips as decorative badges unless the state is meaningful.

### Cards / Containers

- **Corner Style:** content cards use subtle `10dp` to `12dp` radius; profile cards and TV panels use `8dp`.
- **Background:** cards use `surface`; artwork fallback uses `primary-container`, `secondary-container`, or `surface-variant` depending on state.
- **Shadow Strategy:** browse artwork cards are flat on mobile/tablet; elevated card surfaces use `2dp` at rest and `8dp` on TV focus.
- **Border:** TV focused cards use a `2dp` primary border. Mobile cards do not use decorative borders.
- **Internal Padding:** mobile/tablet artwork overlays use `8dp`; TV card metadata uses `16dp` horizontal and `12dp` vertical padding; profile cards
  use
  `16dp`.

### Inputs / Fields

- **Style:** login forms use Material 3 `OutlinedTextField` with single-line email and password fields.
- **Focus:** native field focus is acceptable on touch surfaces; TV fields require deterministic focus order through `FocusRequester` /
  `focusProperties`.
- **Error / Disabled:** validation errors use `isError` and supporting text; loading disables input and action controls.

### Navigation

- **Style:** navigation is route-driven with platform-specific destinations for login, profiles, home, editor, and details.
- **Motion:** route transitions use `240ms` slide, `140ms` fade, `90ms` exit fade, and `FastOutSlowInEasing`.
- **Content continuity:** home-to-details navigation shares the selected artwork and title using the stable `contentId + row` identity. Shared bounds
  use a `90ms` fade, `scaleToBounds(ContentScale.Crop)`, and shape clipping in the transition overlay. Only the selected content key participates.
- **Platform treatment:** mobile, tablet, and TV have separate route/screen implementations. Do not collapse TV focus behavior into mobile components
  with flags.

**The Home-to-Details Continuity Rule.** A selected card's artwork and title must visually resolve into the details surface. Preserve the shared
identity and clip shape; never attach the transition to every duplicate content instance simultaneously.

### Content Artwork

- **Style:** `StreamCoreContentImage` fills its bounds with Coil `AsyncImage`, crops content, crossfades by default, and shows a centered text
  fallback.
- **Shape:** shape comes from the consuming card or shared transition clip. Poster cards use `StreamCoreDimens.Artwork.PosterAspectRatio` (`2:3`);
  landscape and hero artwork use `StreamCoreDimens.Artwork.LandscapeAspectRatio` (`16:9`). Current poster sizes are `120dp x 180dp` on mobile and
  `92dp x 138dp` on tablet; TV poster cards use a `220dp` width with the shared `2:3` ratio.

### Mobile Browse

- **Screen rhythm:** `20dp` horizontal screen padding, `24dp` between major sections, and `12dp` between a shelf title and its row.
- **Top bar:** `StreamCoreBrowseTopBar` occupies a `48dp`-high row. Brand and actions stay visually compact; interactive actions must retain a
  `48dp` touch target even when the visible icon or avatar is `32dp`.
- **Hero pager:** fills the available width inside `20dp` horizontal pager padding, derives height from the shared `16:9` ratio, uses `12dp` page
  spacing, a `16dp` corner radius, and `16dp` internal overlay padding. The description is capped at two lines.
- **Pager indicator:** inactive dots are `5dp`; the selected indicator is `18dp x 5dp`; spacing is `5dp`.
- **Continue Watching:** each item is `192dp` wide with `16:9` artwork (`192dp x 108dp`), `10dp` corners, and `8dp` between artwork and metadata.
  Playback progress is a `3dp` bar inset `8dp` horizontally and uses `primary` only for the completed fraction.
- **Poster shelf:** cards are `120dp x 180dp` (`2:3`) with `10dp` corners and an `8dp` artwork overlay inset.
- **Landscape shelf:** cards are `192dp x 108dp` (`16:9`) with `10dp` corners and an `8dp` artwork overlay inset.
- **Top 10 shelf:** each composition is `160dp x 180dp`; its poster is `120dp x 180dp`; shelf padding and item spacing are both `16dp`. Ranking
  numerals use `Mobile Top Ten Rank` and must remain secondary to the poster artwork.
- **Refresh:** pull-to-refresh is the mobile browse refresh affordance. Keep loading state in the existing content when rows are already available.

**The Browse Density Rule.** Preserve the exact card dimensions and aspect ratios above across new mobile browse-like surfaces. Change density through
platform-specific components, never by adding boolean tablet/TV flags to the mobile card.

## 6. Do's and Don'ts

### Do:

- **Do** keep the target architecture backend-agnostic: feature UI consumes app models, UI state, actions, and effects, not provider DTOs or SDK
  types.
- **Do** use `StreamCoreButton`, `StreamCoreTextButton`, `StreamCoreTvButton`, `StreamCoreContentImage`, and other app-owned components before raw
  Material primitives in feature UI.
- **Do** keep StreamCore orange (`primary`) reserved for primary action, selection, focus, and meaningful status.
- **Do** use stable lazy keys and content types for content rows and grids.
- **Do** use TV-specific focus states with a `2dp` primary border and enough spacing for D-pad movement.
- **Do** preserve preview-friendly composables wrapped in `StreamCoreTheme`.

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
