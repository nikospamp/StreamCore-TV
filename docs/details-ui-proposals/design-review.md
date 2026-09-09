# Details directions: tablet and TV

Recommend **Cinematic split**. It gives the title and its controls a clear home, lets the artwork carry the atmosphere, and keeps the approved mobile vocabulary recognizable. The target architecture remains backend-agnostic; this is a presentation proposal only.

## Keep in every direction

- `#101214` canvas, white/system-sans hierarchy, restrained orange primary pill, rounded artwork, and the existing icon-over-label secondary actions.
- Play/Resume first, then Like, My List, Trailer, Share; preserve current availability and toggle semantics. Do not invent active Share behavior.
- Overview heading, orange genre text, readable description, Cast, then portrait recommendations with ratings inside the artwork.
- Same content and artwork across all three proposals so composition is the variable. No new branding, tabs, decorative panels, badges, or persistent navigation.
- Tablet uses touch; TV uses explicit D-pad focus and readable controls. Use one vertical details surface and a horizontal recommendation rail, not independently scrolling text panels.

Geometry below uses a **1024×768 tablet** and **960×540 TV logical canvas**. TV can be rendered at 2× resolution for the comparison. Treat dimensions as composition targets; long titles, font scaling, and expanded descriptions grow vertically rather than shrink text.

## Cinematic split — recommended

**Composition:** Title, metadata, and the complete action cluster on the left; a rounded landscape still on the right. Overview and Cast occupy a quiet band below both columns. Recommendations follow at full content width.

**Tablet:** 32px margins, 24px gutter, approximately 396px identity column and 540×304px artwork. Title 32px, body 16/24px, Play 180–220×52px. The secondary actions sit immediately below Play. Below the hero, give the synopsis roughly two thirds of the width and Cast the remaining third. Use 120×180px posters with 16px gaps.

**TV:** 32dp safe margins, 24dp gutter, approximately 400dp identity column and 472×266dp artwork. Title 36–40sp, body 18/26sp, action labels at least 14sp. Keep the title and controls vertically centered against the image. The hero, synopsis, and recommendation rail form successive vertical destinations. A long synopsis pushes recommendations below the first viewport; do not squeeze them in.

**Why:** Artwork and the viewing decision have equal importance without distributing one action sequence across the screen. The lower content band removes the current tall, busy right column.

**Tradeoff:** Less image scale than Panorama; long cast lists need a bounded summary. Best balance of fidelity, readability, and platform fit.

## Panorama

**Composition:** One wide rounded hero, title and metadata at its lower-left edge over a controlled black scrim. A compact primary pill and the familiar secondary action group sit directly beneath it. Overview, Cast, and recommendations continue below.

**Tablet:** Approximately 960×340px image within 32px margins; 24px inset for title. Controls occupy one horizontal row below the image. Synopsis and Cast divide the next band about 2:1. Touch scroll reveals portrait recommendations.

**TV:** Approximately 896×280dp image with a 32dp title inset. Title 40sp; keep all controls outside the image for consistent contrast and focus geometry. Initial focus is Play; right traverses the secondaries; down progresses to description and recommendations. The first viewport emphasizes the film and viewing action, with deeper content revealed by scrolling.

**Why:** Strongest continuity with mobile’s image-first sequence and the largest cinematic moment.

**Tradeoff:** The panoramic crop can lose subjects, needs per-image focal alignment, and moves the recommendations farther down. Scrim coverage must hold for bright artwork and two-line titles.

## Poster + story

**Composition:** A portrait artwork column anchors the left. Title, metadata, controls, Overview, description, and Cast form one coherent reading column to its right. Recommendations extend across the page underneath the complete summary.

**Tablet:** A 192×288px poster, 24px gutter, and approximately 744px reading column. Keep description measure around 60 characters rather than stretching it to the entire column. Play and secondary actions share the identity area. The summary height follows the text, not the poster bottom.

**TV:** A 180×270dp poster, 32dp gap, and 684dp text area. Use a readable two- or three-line synopsis and one concise Cast line initially. Maintain 36sp title and 18sp body. Right-column controls are a single ordered group; down exits it into the description and recommendations. There is no focus stop on decorative artwork.

**Why:** The strongest title-recognition and reading option; the artwork stays intact and details feel compact without tiny type. It changes the role of the artwork rather than merely moving the same blocks.

**Tradeoff:** Less cinematic landscape atmosphere. Best for rich poster art and viewers who read before playing; the long text column can still make this taller than the split direction.

## Interaction and continuity

Tablet targets are at least 48dp. TV reserves the project’s focus clearance around controls and at rail edges; focus outline and content shape stay aligned. The orange Play pill remains legible when focused; secondary focus uses an explicit outline without turning every inactive control orange. Disabled actions do not enter the D-pad sequence. Like/My List preserve label and toggle-state feedback.

Returning from a recommendation restores that poster’s focus and horizontal position. Moving back up restores the previous action without diagonal focus jumps. Any motion communicates focus, toggle state, or navigation continuity; no decorative entrance choreography is required. Artwork and title retain the existing Home-to-Details shared identity.

## Local references actually reviewed

- [Tubi series details](https://mobbin.com/screens/e3179f62-76c-4dc7-b496-c5c81e5f5cc2): compact primary action next to supporting actions; clear identity and description hierarchy.
- [Disney+ details and extras](https://mobbin.com/screens/1a7e330d-3afd-4997-9cd6-88f4d5bed219): icon-over-label secondary actions and separation of narrative from deeper media. Its tabs are not needed for this proposal.
- [Hulu Home shelves](https://mobbin.com/screens/34cfe858-cba4-4d27-848a-4a6132372403): portrait rail rhythm and strong image hierarchy.

These archived mobile screens inform hierarchy only. Tablet geometry and TV focus behavior above are design adaptations, not claims of native TV reference evidence.
