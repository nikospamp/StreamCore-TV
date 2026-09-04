import { expect, test, type Locator, type Page, type TestInfo } from "@playwright/test";

const fixtureContentId = "603";
const fixtureRoute = `/diagnostic/player/${fixtureContentId}`;

test.describe("WEB-04 deterministic player acceptance", () => {
  test("successful fake playback reaches ready and plays after activation", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "success");

    const video = playerVideo(page);
    await expect(video).toHaveCount(1);
    await expectVideoPointerPassthrough(video);
    await expectPlayerState(page, "ready");
    await expectPlayerVideoComposition(page, video, testInfo);
    await activateProjectedButton(page, "Play");
    await expect(page.locator("body")).toHaveAttribute("data-player-playing", "true");
    await activateProjectedButton(page, "Pause");
    await expect(page.locator("body")).toHaveAttribute("data-player-playing", "false");
    await expect(page.getByRole("button", { name: "Play", exact: true })).toHaveCount(1);
    await diagnostics.assertClean();
  });

  test("hidden controls reveal through physical pointer input", async ({ page }, testInfo) => {
    test.setTimeout(45_000);
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "success");

    const body = page.locator("body");
    await activateProjectedButton(page, "Play");
    await expect(body).toHaveAttribute("data-player-playing", "true");
    await expect(body).toHaveAttribute("data-player-active-timers", "1");
    const videoBounds = await playerVideo(page).boundingBox({ timeout: 1_000 });
    if (videoBounds === null) {
      throw new Error("Player video surface does not expose viewport bounds");
    }
    const pointerY = videoBounds.y + videoBounds.height / 2;
    await page.mouse.move(videoBounds.x + videoBounds.width * 0.25, pointerY);
    await waitForAnimationFrames(page, 2);
    await expect(body).toHaveAttribute("data-player-controls-visible", "false", {
      timeout: 15_000,
    });

    await page.mouse.move(videoBounds.x + videoBounds.width * 0.75, pointerY);
    await waitForAnimationFrames(page, 2);
    await expect(body).toHaveAttribute("data-player-controls-visible", "true");
    await expect(page.getByRole("button", { name: "Pause", exact: true })).toBeVisible();
    await expect(body).toHaveAttribute("data-player-playing", "true");
    await expect(body).toHaveAttribute("data-player-active-timers", "1");

    await page.mouse.move(videoBounds.x + videoBounds.width / 2, pointerY);
    await waitForAnimationFrames(page, 2);
    await expect(body).toHaveAttribute("data-player-controls-visible", "false", {
      timeout: 15_000,
    });
    await clickPlayerSurfaceCenter(page);
    await expect(body).toHaveAttribute("data-player-controls-visible", "true");
    await expect(page.getByRole("button", { name: "Pause", exact: true })).toBeVisible();
    await expect(body).toHaveAttribute("data-player-playing", "true");
    await expect(body).toHaveAttribute("data-player-active-timers", "1");

    await diagnostics.assertClean();
  });

  test("hidden controls reveal through keyboard input and Space toggles only after release", async ({ page }, testInfo) => {
    test.setTimeout(45_000);
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "success");

    const body = page.locator("body");
    await activateProjectedButton(page, "Play");
    await expect(body).toHaveAttribute("data-player-playing", "true");
    await expect(body).toHaveAttribute("data-player-active-timers", "1");
    await expect(body).toHaveAttribute("data-player-controls-visible", "false", {
      timeout: 15_000,
    });

    await page.keyboard.press("ArrowUp");
    await expect(body).toHaveAttribute("data-player-controls-visible", "true");
    await expect(page.getByRole("button", { name: "Pause", exact: true })).toBeVisible();
    await expect(body).toHaveAttribute("data-player-playing", "true");
    await expect(body).toHaveAttribute("data-player-active-timers", "1");

    await expect(body).toHaveAttribute("data-player-controls-visible", "false", {
      timeout: 15_000,
    });
    await page.keyboard.down(" ");
    await expect(body).toHaveAttribute("data-player-controls-visible", "true");
    await expect(page.getByRole("button", { name: "Pause", exact: true })).toBeVisible();
    await expect(body).toHaveAttribute("data-player-playing", "true");
    await page.keyboard.up(" ");
    await expect(body).toHaveAttribute("data-player-playing", "true");
    await expect(body).toHaveAttribute("data-player-active-timers", "1");

    await page.keyboard.press("Space");
    await expect(body).toHaveAttribute("data-player-playing", "false");
    await expect(body).toHaveAttribute("data-player-active-timers", "0");
    await diagnostics.assertClean();
  });

  test("autoplay rejection remains ready and explicit activation starts playback", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "autoplay-blocked");

    await expectPlayerState(page, "ready");
    await expect(page.locator("body")).toHaveAttribute("data-player-activation-required", "true");
    await activateProjectedButton(page, "Play");
    await expect(page.locator("body")).toHaveAttribute("data-player-activation-required", "false");
    await expect(page.locator("body")).toHaveAttribute("data-player-playing", "true");
    await diagnostics.assertClean();
  });

  test("sanitized network error retries the same request once", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "recoverable-error");

    await expectPlayerState(page, "error");
    await expect(page.locator("body")).toHaveAttribute("data-player-error-code", "PLAYBACK_FAILED");
    const message = await page.locator("body").getAttribute("data-player-error-message");
    expect(message).toBe("Playback failed.");
    expect(message).not.toMatch(/https?:|token|session|manifest/i);
    await activateProjectedButton(page, "Retry");
    await expectPlayerState(page, "ready");
    await expect(page.locator("body")).toHaveAttribute("data-player-prepare-count", "2");
    await diagnostics.assertClean();
  });

  test("seek, scrub, speed, quality, audio and subtitles update the fixture state", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "tracks");

    await scrubTimeline(page, 0.5);
    await expectNumericBodyAttribute(page, "data-player-position-ms", 59_000, 61_000);

    await activateProjectedButton(page, "Playback settings");
    await activateProjectedButton(page, "Speed · 1×");
    await activateProjectedButton(page, "1.5×");
    await expect(page.locator("body")).toHaveAttribute("data-player-speed", "1.5");
    await activateProjectedButton(page, "Back to playback settings");

    await activateProjectedButton(page, "Quality · 1080p · 5.8 Mbps");
    await activateProjectedButton(page, "1080p · 5.8 Mbps");
    await expect(page.locator("body")).toHaveAttribute("data-player-video-track", "video-1080");
    await activateProjectedButton(page, "Back to playback settings");

    await activateProjectedButton(page, "Audio · English · Original 5.1");
    await activateProjectedButton(page, "Ελληνικά · Stereo");
    await expect(page.locator("body")).toHaveAttribute("data-player-audio-track", "audio-el");
    await activateProjectedButton(page, "Back to playback settings");

    await activateProjectedButton(page, "Subtitles · Off");
    await activateProjectedButton(page, "English (CC)");
    await expect(page.locator("body")).toHaveAttribute("data-player-text-track", "text-en");
    await diagnostics.assertClean();
  });

  test("profile-scoped resume survives close and hard reload", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo, {
      allowWebKitHardReloadCoroutinePair: true,
    });
    await openPlayerFixture(page, "resume", { profile: "profile-a" });

    await expect(page.locator("body")).toHaveAttribute("data-player-profile-id", "profile-a");
    await expectNumericBodyAttribute(page, "data-player-position-ms", 0, 1_000);
    await scrubTimeline(page, 0.375);
    await expectNumericBodyAttribute(page, "data-player-position-ms", 44_000, 46_000);
    diagnostics.setPhase("player-exit");
    await page.keyboard.press("Escape");
    await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);
    await settleNavigationDiagnostics(page, diagnostics);

    await openPlayerFixture(page, "resume", { profile: "profile-a" });
    await expect(page.locator("body")).toHaveAttribute("data-player-profile-id", "profile-a");
    await expectNumericBodyAttribute(page, "data-player-position-ms", 44_000, 46_000);
    diagnostics.setPhase("hard-reload");
    await page.reload();
    await waitForFixtureReadiness(page, "resume");
    await settleNavigationDiagnostics(page, diagnostics, fixtureRoute);
    await expect(page.locator("body")).toHaveAttribute("data-player-profile-id", "profile-a");
    await expectNumericBodyAttribute(page, "data-player-position-ms", 44_000, 46_000);

    diagnostics.setPhase("player-exit");
    await page.keyboard.press("Escape");
    await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);
    await settleNavigationDiagnostics(page, diagnostics);
    await openPlayerFixture(page, "resume", { profile: "profile-b" });
    await expect(page.locator("body")).toHaveAttribute("data-player-profile-id", "profile-b");
    await expectNumericBodyAttribute(page, "data-player-position-ms", 0, 1_000);
    await diagnostics.assertClean();
  });

  test("fullscreen exit restores focus to the invoking control", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "success");

    await activateProjectedButton(page, "Enter fullscreen");
    await expect(page.locator("body")).toHaveAttribute("data-player-fullscreen", "true");
    await page.keyboard.press("Escape");
    await expect(page.locator("body")).toHaveAttribute("data-player-fullscreen", "false");
    await page.keyboard.press("Space");
    await expect(page.locator("body")).toHaveAttribute("data-player-fullscreen", "true");
    await page.keyboard.press("Escape");
    await expect(page.locator("body")).toHaveAttribute("data-player-fullscreen", "false");
    await diagnostics.assertClean();
  });

  test("Escape closes one layer and browser Back returns to details", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openSuccessfulPlayerFixtureFromDetails(page);

    await activateProjectedButton(page, "Playback settings");
    await expect(page.locator("body")).toHaveAttribute("data-player-layer", "settings-root");
    await page.keyboard.press("Escape");
    await expect(page.locator("body")).toHaveAttribute("data-player-layer", "player");
    await expect(page).toHaveURL(new RegExp(`${fixtureRoute.replaceAll("/", "\\/")}$`));
    diagnostics.setPhase("player-exit");
    await page.keyboard.press("Escape");
    await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);
    await settleNavigationDiagnostics(page, diagnostics);
    await expect(page.locator("body")).toHaveAttribute("data-player-close-count", "1");

    await page.goForward();
    await expect(page).toHaveURL(new RegExp(`${fixtureRoute.replaceAll("/", "\\/")}$`));
    await waitForFixtureReadiness(page, "success");
    await expect(page.locator("body")).toHaveAttribute("data-player-active-sessions", "1");
    await expect(playerVideo(page)).toHaveCount(1);
    diagnostics.setPhase("player-exit");
    await page.goBack();
    await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);
    await settleNavigationDiagnostics(page, diagnostics);
    await expect(page.locator("body")).toHaveAttribute("data-player-active-sessions", "0");
    await expect(page.locator("body")).toHaveAttribute("data-player-active-listeners", "0");
    await expect(page.locator("body")).toHaveAttribute("data-player-active-timers", "0");
    await expect(playerVideo(page)).toHaveCount(0);
    await expect(page.locator("body")).toHaveAttribute("data-player-close-count", "2");
    await diagnostics.assertClean();
  });

  test("diagnostic invalid direct ID creates no playback session or prepare", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await installRuntimeConfig(page);
    const invalidFixtureRoute = "/diagnostic/player/invalid";
    await page.goto(`${invalidFixtureRoute}?fixture=invalid-direct-id&requestedId=%24invalid`);
    await waitForFixtureReadiness(page, "invalid-direct-id", invalidFixtureRoute);

    await expect(page.locator("body")).toHaveAttribute("data-player-invalid-request", "true");
    await expect(page.locator("body")).toHaveAttribute("data-player-active-sessions", "0");
    await expect(page.locator("body")).toHaveAttribute("data-player-prepare-count", "0");
    await expect(playerVideo(page)).toHaveCount(0);
    await diagnostics.assertClean();
  });

  test("no-filmstrip fixture preserves seek without cross-origin frame capture", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "no-filmstrip");

    await expect(page.locator("body")).toHaveAttribute("data-player-filmstrip-count", "0");
    await expect(playerTimeline(page)).toHaveCount(1);
    await scrubTimeline(page, 0.25, async () => {
      await expect(page.getByText("Preview unavailable", { exact: true })).toBeVisible();
    });
    await expectNumericBodyAttribute(page, "data-player-position-ms", 29_000, 31_000);
    await expect(page.locator("body")).toHaveAttribute("data-player-canvas-capture-count", "0");
    await diagnostics.assertClean();
  });

  test("repeated enter, play and close leaves no session, node, listener or timer", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await installRuntimeConfig(page);
    await page.goto(`/diagnostic/details/${fixtureContentId}?fixture=success`);

    for (let iteration = 1; iteration <= 5; iteration += 1) {
      await activateProjectedButton(page, "Player ID");
      await expect(page).toHaveURL(new RegExp(`${fixtureRoute.replaceAll("/", "\\/")}$`));
      await waitForFixtureReadiness(page, "success");
      await expect(page.locator("body")).toHaveAttribute("data-player-active-sessions", "1");
      await expectNumericBodyAttribute(page, "data-player-active-listeners", 1, 20);
      await expect(playerVideo(page)).toHaveCount(1);
      await activateProjectedButton(page, "Play");
      await expect(page.locator("body")).toHaveAttribute("data-player-active-timers", "1");
      await page.keyboard.press("Escape");
      await expect(page).toHaveURL(/\/diagnostic\/details\/603\?fixture=success$/);
      await expect(page.locator("body")).toHaveAttribute("data-player-active-sessions", "0");
      await expect(page.locator("body")).toHaveAttribute("data-player-active-listeners", "0");
      await expect(page.locator("body")).toHaveAttribute("data-player-active-timers", "0");
      await expect(playerVideo(page)).toHaveCount(0);
    }
    await expect(page.locator("body")).toHaveAttribute("data-player-close-count", "5");
    await diagnostics.assertClean();
  });
});

async function openPlayerFixture(
  page: Page,
  scenario: string,
  parameters: Readonly<Record<string, string>> = {},
): Promise<void> {
  await installRuntimeConfig(page);
  await page.goto(`/diagnostic/details/${fixtureContentId}`);
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute(
    "data-product-route",
    `/diagnostic/details/${fixtureContentId}`,
    { timeout: 30_000 },
  );
  await expect(page.locator("body")).toHaveAttribute("data-image-probe", "loaded", {
    timeout: 30_000,
  });
  const query = new URLSearchParams({ fixture: scenario, ...parameters });
  await page.goto(`${fixtureRoute}?${query.toString()}`);
  await waitForFixtureReadiness(page, scenario);
}

async function openSuccessfulPlayerFixtureFromDetails(page: Page): Promise<void> {
  await installRuntimeConfig(page);
  await page.goto(`/diagnostic/details/${fixtureContentId}`);
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute(
    "data-product-route",
    `/diagnostic/details/${fixtureContentId}`,
    { timeout: 30_000 },
  );
  await expect(page.locator("body")).toHaveAttribute("data-image-probe", "loaded", {
    timeout: 30_000,
  });
  await activateProjectedButton(page, "Player ID");
  await waitForFixtureReadiness(page, "success");
}

async function installRuntimeConfig(page: Page): Promise<void> {
  await page.route("**/config.json", async (route) => {
    await route.fulfill({
      json: {
        tmdbBaseUrl: "https://api.example.test/",
        tmdbReadAccessToken: "fixture-browser-value",
        tmdbAccountId: "42",
      },
    });
  });
  await page.route("**/web-probe.png", async (route) => {
    await route.fulfill({
      contentType: "image/png",
      body: Buffer.from(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
        "base64",
      ),
    });
  });
  await page.route("https://api.example.test/**", async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    const headers = {
      "access-control-allow-origin": "*",
      "content-type": "application/json",
    };
    if (pathname.endsWith("/configuration")) {
      await route.fulfill({
        headers,
        json: {
          images: {
            secure_base_url: "https://images.example.test/",
            poster_sizes: ["w500"],
            backdrop_sizes: ["w780"],
            profile_sizes: ["w185"],
          },
        },
      });
      return;
    }
    if (pathname.endsWith("/genre/movie/list")) {
      await route.fulfill({ headers, json: { genres: [] } });
      return;
    }
    await route.fulfill({
      headers,
      json: { page: 1, results: [], total_pages: 1, total_results: 0 },
    });
  });
}

async function waitForFixtureReadiness(
  page: Page,
  scenario: string,
  expectedRoute: string = fixtureRoute,
): Promise<void> {
  const body = page.locator("body");
  await expect(body).toHaveAttribute("data-runtime-state", "ready", { timeout: 30_000 });
  await expect(body).toHaveAttribute("data-player-fixture", scenario, { timeout: 30_000 });
  await expect(body).toHaveAttribute("data-player-fixture-ready", "true", { timeout: 30_000 });
  await expect(body).toHaveAttribute("data-product-route", expectedRoute);
}

function playerVideo(page: Page): Locator {
  return page.locator('[data-testid="player:video"]');
}

async function clickPlayerSurfaceCenter(page: Page): Promise<void> {
  const bounds = await playerVideo(page).boundingBox({ timeout: 1_000 });
  if (bounds === null) {
    throw new Error("Player video surface does not expose viewport bounds");
  }
  await page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  await waitForAnimationFrames(page, 2);
}

async function expectVideoPointerPassthrough(video: Locator): Promise<void> {
  await expect.poll(async () => {
    return video.evaluate((element) => {
      const host = element.parentElement;
      if (host === null) {
        return "missing-host";
      }
      const videoPointerEvents = window.getComputedStyle(element).pointerEvents;
      const hostPointerEvents = window.getComputedStyle(host).pointerEvents;
      return `${videoPointerEvents}|${hostPointerEvents}`;
    });
  }, { timeout: 30_000, intervals: [100] }).toBe("none|none");
}

async function expectPlayerVideoComposition(
  page: Page,
  video: Locator,
  testInfo: TestInfo,
): Promise<void> {
  const structure = await video.evaluate((element) => {
    const videoLayer = document.getElementById("streamcore-playback-video-layer");
    const composeRoot = document.getElementById("streamcore-compose-root");
    if (videoLayer === null || composeRoot === null) {
      return null;
    }
    const videoStyle = window.getComputedStyle(element);
    const videoLayerStyle = window.getComputedStyle(videoLayer);
    const composeRootStyle = window.getComputedStyle(composeRoot);
    const viewportCoveredBy = (candidate: Element): boolean => {
      const bounds = candidate.getBoundingClientRect();
      return Math.abs(bounds.left) < 0.5 &&
        Math.abs(bounds.top) < 0.5 &&
        Math.abs(bounds.width - window.innerWidth) < 0.5 &&
        Math.abs(bounds.height - window.innerHeight) < 0.5;
    };
    return {
      rootsAreOrderedBodySiblings: videoLayer.parentElement === document.body &&
        composeRoot.parentElement === document.body &&
        videoLayer.nextElementSibling === composeRoot,
      videoParentId: element.parentElement?.id ?? null,
      videoLayerPosition: videoLayerStyle.position,
      videoLayerZIndex: videoLayerStyle.zIndex,
      videoLayerPointerEvents: videoLayerStyle.pointerEvents,
      videoLayerVisible: videoLayerStyle.display !== "none" &&
        videoLayerStyle.visibility !== "hidden",
      composeRootPosition: composeRootStyle.position,
      composeRootZIndex: composeRootStyle.zIndex,
      videoPointerEvents: videoStyle.pointerEvents,
      videoBackgroundColor: videoStyle.backgroundColor,
      videoLayerCoversViewport: viewportCoveredBy(videoLayer),
      composeRootCoversViewport: viewportCoveredBy(composeRoot),
      videoCoversViewport: viewportCoveredBy(element),
    };
  });
  expect(structure).toEqual({
    rootsAreOrderedBodySiblings: true,
    videoParentId: "streamcore-playback-video-layer",
    videoLayerPosition: "absolute",
    videoLayerZIndex: "0",
    videoLayerPointerEvents: "none",
    videoLayerVisible: true,
    composeRootPosition: "absolute",
    composeRootZIndex: "1",
    videoPointerEvents: "none",
    videoBackgroundColor: "rgb(17, 197, 113)",
    videoLayerCoversViewport: true,
    composeRootCoversViewport: true,
    videoCoversViewport: true,
  });

  const bounds = await video.boundingBox({ timeout: 1_000 });
  if (bounds === null) {
    throw new Error("Diagnostic player video does not expose viewport bounds");
  }
  const clipSize = 32;
  const clip = {
    x: Math.floor(bounds.x + bounds.width / 2 - clipSize / 2),
    y: Math.floor(bounds.y + bounds.height / 2 - clipSize / 2),
    width: clipSize,
    height: clipSize,
  };
  await waitForAnimationFrames(page, 2);
  const visibleFrame = await page.screenshot({ clip, animations: "disabled" });
  const previousVisibility = await video.evaluate((element) => element.style.visibility);
  let hiddenFrame: Buffer;
  try {
    await video.evaluate((element) => {
      element.style.visibility = "hidden";
    });
    await waitForAnimationFrames(page, 2);
    hiddenFrame = await page.screenshot({ clip, animations: "disabled" });
  } finally {
    await video.evaluate((element, visibility) => {
      if (visibility.length === 0) {
        element.style.removeProperty("visibility");
      } else {
        element.style.visibility = visibility;
      }
    }, previousVisibility);
    await waitForAnimationFrames(page, 2);
  }
  const videoAffectedFinalComposition = !visibleFrame.equals(hiddenFrame);
  if (!videoAffectedFinalComposition) {
    await testInfo.attach("diagnostic-video-visible", {
      body: visibleFrame,
      contentType: "image/png",
    });
    await testInfo.attach("diagnostic-video-hidden", {
      body: hiddenFrame,
      contentType: "image/png",
    });
  }
  expect(videoAffectedFinalComposition).toBe(true);
}

async function expectPlayerState(page: Page, state: string): Promise<void> {
  await expect(page.locator("body")).toHaveAttribute("data-player-phase", state, {
    timeout: 30_000,
  });
}

async function activateProjectedButton(page: Page, name: string): Promise<void> {
  const action = page.getByRole("button", { name, exact: true });
  await expect(action).toHaveCount(1);
  const bounds = await stableSemanticBounds(action);
  await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  await waitForAnimationFrames(page, 2);
  await page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  await waitForAnimationFrames(page, 2);
}

function playerTimeline(page: Page): Locator {
  return page.getByLabel(/^Playback position \d+:\d{2} of 2:00$/);
}

async function scrubTimeline(
  page: Page,
  fraction: number,
  whileScrubbing: (() => Promise<void>) | undefined = undefined,
): Promise<void> {
  const slider = playerTimeline(page);
  await expect(slider).toHaveCount(1);
  const bounds = await stableSemanticBounds(slider);
  const x = bounds.x + bounds.width * fraction;
  const y = bounds.y + bounds.height / 2;
  await page.mouse.move(bounds.x + bounds.width * 0.1, y);
  await page.mouse.down();
  try {
    await page.mouse.move(x, y, { steps: 8 });
    await whileScrubbing?.();
  } finally {
    await page.mouse.up();
  }
}

async function stableSemanticBounds(
  locator: Locator,
): Promise<{ x: number; y: number; width: number; height: number }> {
  let previous = await locator.boundingBox({ timeout: 1_000 }).catch(() => null);
  let current = previous;
  await expect.poll(async () => {
    await waitForAnimationFrames(locator.page(), 2);
    current = await locator.boundingBox({ timeout: 1_000 }).catch(() => null);
    const stable = previous !== null && current !== null &&
      Math.abs(previous.x - current.x) < 0.5 &&
      Math.abs(previous.y - current.y) < 0.5 &&
      Math.abs(previous.width - current.width) < 0.5 &&
      Math.abs(previous.height - current.height) < 0.5;
    previous = current;
    return stable;
  }, { timeout: 30_000, intervals: [100] }).toBe(true);
  if (current === null) throw new Error("Projected semantic action has no stable viewport bounds");
  return current;
}

async function expectNumericBodyAttribute(
  page: Page,
  name: string,
  minimum: number,
  maximum: number,
): Promise<void> {
  await expect.poll(async () => {
    const value = Number(await page.locator("body").getAttribute(name));
    return Number.isFinite(value) && value >= minimum && value <= maximum;
  }, { timeout: 30_000, intervals: [100] }).toBe(true);
}

async function waitForAnimationFrames(page: Page, count: number): Promise<void> {
  await page.evaluate(async (frameCount) => {
    for (let frame = 0; frame < frameCount; frame += 1) {
      await new Promise<void>((resolve) => {
        let completed = false;
        const complete = (): void => {
          if (completed) return;
          completed = true;
          clearTimeout(fallback);
          resolve();
        };
        const fallback = setTimeout(complete, 100);
        requestAnimationFrame(complete);
      });
    }
  }, count);
}

function installSanitizedDiagnostics(
  page: Page,
  testInfo: TestInfo,
  options: DiagnosticOptions = {},
) {
  const messages: DiagnosticMessage[] = [];
  let phase: DiagnosticPhase | null = null;
  let phaseEpoch = 0;
  page.on("pageerror", (error) => messages.push({
    source: "pageerror",
    text: redactDiagnostic(error.message),
    phase,
    phaseEpoch,
  }));
  page.on("console", (message) => {
    if (message.type() === "error") {
      messages.push({
        source: "console",
        text: redactDiagnostic(message.text()),
        phase,
        phaseEpoch,
      });
    }
  });
  return {
    setPhase(nextPhase: DiagnosticPhase | null): void {
      phase = nextPhase;
      if (nextPhase !== null) {
        phaseEpoch += 1;
      }
    },
    async assertClean(): Promise<void> {
      const unexpected = unexpectedDiagnostics(messages, testInfo.project.name, options);
      if (unexpected.length > 0) {
        await testInfo.attach("sanitized-browser-errors", {
          body: Buffer.from(JSON.stringify(unexpected.map((message) => message.text), null, 2)),
          contentType: "application/json",
        });
      }
      expect(unexpected).toEqual([]);
    },
  };
}

function unexpectedDiagnostics(
  messages: readonly DiagnosticMessage[],
  projectName: string,
  options: DiagnosticOptions,
): DiagnosticMessage[] {
  const unexpected: DiagnosticMessage[] = [];
  const webKitHardReloadKnownCounts = new Map<number, number>();
  const webKitHardReloadCoroutineEpochs = new Set<number>();
  const webKitHardReloadComposeResourceEpochs = new Set<number>();
  const webKitHardReloadClassCastEpochs = new Set<number>();
  const webKitHardReloadBlobEpochs = new Set<number>();
  const webKitHardReloadIoEpochs = new Set<number>();
  const webKitPlayerExitKnownCounts = new Map<number, number>();
  const webKitPlayerExitKnownSignatures = new Map<number, Set<string>>();
  for (let index = 0; index < messages.length; index += 1) {
    const current = messages[index];
    const normalized = current.text.trim();
    const next = messages[index + 1];
    if (
      projectName.startsWith("webkit-") &&
      current.source === "console" &&
      normalized === WEBKIT_RENDERER_INFO_WARNING
    ) {
      continue;
    }
    if (
      options.allowWebKitHardReloadCoroutinePair === true &&
      projectName.startsWith("webkit-") &&
      current.source === "pageerror" &&
      current.phase === "hard-reload" &&
      WEBKIT_COROUTINE_TEARDOWN_ERROR.test(normalized) &&
      next?.source === "pageerror" &&
      next.phase === current.phase &&
      next.phaseEpoch === current.phaseEpoch &&
      WEBKIT_COROUTINE_TEARDOWN_ERROR.test(next.text.trim()) &&
      (webKitHardReloadKnownCounts.get(current.phaseEpoch) ?? 0) + 2 <= MAX_WEBKIT_HARD_RELOAD_ERRORS &&
      !webKitHardReloadCoroutineEpochs.has(current.phaseEpoch)
    ) {
      webKitHardReloadCoroutineEpochs.add(current.phaseEpoch);
      incrementEpochCount(webKitHardReloadKnownCounts, current.phaseEpoch, 2);
      index += 1;
      continue;
    }
    if (
      projectName.startsWith("webkit-") &&
      current.source === "pageerror" &&
      current.phase === "hard-reload" &&
      WEBKIT_COROUTINE_TEARDOWN_ERROR.test(normalized) &&
      (webKitHardReloadKnownCounts.get(current.phaseEpoch) ?? 0) < MAX_WEBKIT_HARD_RELOAD_ERRORS &&
      !webKitHardReloadCoroutineEpochs.has(current.phaseEpoch)
    ) {
      webKitHardReloadCoroutineEpochs.add(current.phaseEpoch);
      incrementEpochCount(webKitHardReloadKnownCounts, current.phaseEpoch);
      continue;
    }
    if (
      projectName.startsWith("webkit-") &&
      current.source === "pageerror" &&
      current.phase === "hard-reload" &&
      WEBKIT_COMPOSE_RESOURCE_ACCESS_ERROR.test(normalized) &&
      (webKitHardReloadKnownCounts.get(current.phaseEpoch) ?? 0) < MAX_WEBKIT_HARD_RELOAD_ERRORS &&
      !webKitHardReloadComposeResourceEpochs.has(current.phaseEpoch)
    ) {
      webKitHardReloadComposeResourceEpochs.add(current.phaseEpoch);
      incrementEpochCount(webKitHardReloadKnownCounts, current.phaseEpoch);
      continue;
    }
    if (
      projectName.startsWith("webkit-") &&
      current.source === "pageerror" &&
      current.phase === "hard-reload" &&
      normalized === WEBKIT_RESPONSE_CLASS_CAST_ERROR &&
      (webKitHardReloadKnownCounts.get(current.phaseEpoch) ?? 0) < MAX_WEBKIT_HARD_RELOAD_ERRORS &&
      !webKitHardReloadClassCastEpochs.has(current.phaseEpoch)
    ) {
      webKitHardReloadClassCastEpochs.add(current.phaseEpoch);
      incrementEpochCount(webKitHardReloadKnownCounts, current.phaseEpoch);
      continue;
    }
    if (
      projectName.startsWith("webkit-") &&
      current.source === "pageerror" &&
      current.phase === "hard-reload" &&
      normalized === WEBKIT_IO_READ_ERROR &&
      (webKitHardReloadKnownCounts.get(current.phaseEpoch) ?? 0) < MAX_WEBKIT_HARD_RELOAD_ERRORS &&
      !webKitHardReloadIoEpochs.has(current.phaseEpoch)
    ) {
      webKitHardReloadIoEpochs.add(current.phaseEpoch);
      incrementEpochCount(webKitHardReloadKnownCounts, current.phaseEpoch);
      continue;
    }
    if (
      projectName.startsWith("webkit-") &&
      current.source === "pageerror" &&
      current.phase === "hard-reload" &&
      WEBKIT_BLOB_ACCESS_ERROR.test(normalized) &&
      next?.source === "pageerror" &&
      next.phase === current.phase &&
      next.phaseEpoch === current.phaseEpoch &&
      next.text.trim() === WEBKIT_IO_READ_ERROR &&
      (webKitHardReloadKnownCounts.get(current.phaseEpoch) ?? 0) + 2 <= MAX_WEBKIT_HARD_RELOAD_ERRORS &&
      !webKitHardReloadBlobEpochs.has(current.phaseEpoch)
    ) {
      webKitHardReloadBlobEpochs.add(current.phaseEpoch);
      incrementEpochCount(webKitHardReloadKnownCounts, current.phaseEpoch, 2);
      index += 1;
      continue;
    }
    if (
      projectName.startsWith("webkit-") &&
      current.source === "pageerror" &&
      current.phase === "player-exit" &&
      WEBKIT_COROUTINE_TEARDOWN_ERROR.test(normalized) &&
      next?.source === "pageerror" &&
      next.phase === current.phase &&
      next.phaseEpoch === current.phaseEpoch &&
      WEBKIT_COROUTINE_TEARDOWN_ERROR.test(next.text.trim()) &&
      consumePlayerExitSignatures(
        webKitPlayerExitKnownCounts,
        webKitPlayerExitKnownSignatures,
        current.phaseEpoch,
        [WEBKIT_PLAYER_EXIT_COROUTINE_SIGNATURE],
        2,
      )
    ) {
      index += 1;
      continue;
    }
    if (
      projectName.startsWith("webkit-") &&
      current.source === "pageerror" &&
      current.phase === "player-exit" &&
      WEBKIT_BLOB_ACCESS_ERROR.test(normalized) &&
      next?.source === "pageerror" &&
      next.phase === current.phase &&
      next.phaseEpoch === current.phaseEpoch &&
      next.text.trim() === WEBKIT_IO_READ_ERROR &&
      consumePlayerExitSignatures(
        webKitPlayerExitKnownCounts,
        webKitPlayerExitKnownSignatures,
        current.phaseEpoch,
        [WEBKIT_PLAYER_EXIT_BLOB_SIGNATURE, WEBKIT_PLAYER_EXIT_IO_SIGNATURE],
        2,
      )
    ) {
      index += 1;
      continue;
    }
    const playerExitSignature = WEBKIT_COMPOSE_RESOURCE_ACCESS_ERROR.test(normalized)
      ? WEBKIT_PLAYER_EXIT_COMPOSE_RESOURCE_SIGNATURE
      : WEBKIT_CONFIG_ACCESS_ERROR.test(normalized)
        ? WEBKIT_PLAYER_EXIT_CONFIG_SIGNATURE
        : WEBKIT_COROUTINE_TEARDOWN_ERROR.test(normalized)
          ? WEBKIT_PLAYER_EXIT_COROUTINE_SIGNATURE
        : normalized === WEBKIT_IO_READ_ERROR
          ? WEBKIT_PLAYER_EXIT_IO_SIGNATURE
          : normalized === WEBKIT_RESPONSE_CLASS_CAST_ERROR
            ? WEBKIT_PLAYER_EXIT_RESPONSE_CLASS_CAST_SIGNATURE
            : null;
    if (
      projectName.startsWith("webkit-") &&
      current.source === "pageerror" &&
      current.phase === "player-exit" &&
      playerExitSignature !== null
    ) {
      if (
        consumePlayerExitSignatures(
          webKitPlayerExitKnownCounts,
          webKitPlayerExitKnownSignatures,
          current.phaseEpoch,
          [playerExitSignature],
          1,
        )
      ) {
        continue;
      }
    }
    if (
      projectName.startsWith("firefox-") &&
      current.source === "console" &&
      FIREFOX_WASM_STREAMING_FALLBACK_START.test(normalized) &&
      next?.source === "console" &&
      next.phase === current.phase &&
      next.phaseEpoch === current.phaseEpoch &&
      next.text.trim() === FIREFOX_WASM_STREAMING_FALLBACK_END
    ) {
      index += 1;
      continue;
    }
    unexpected.push(current);
  }
  return unexpected;
}

function redactDiagnostic(message: string): string {
  return message
    .replace(/https?:\/\/\S+/gi, "[redacted-url]")
    .replace(/bearer\s+\S+/gi, "Bearer [redacted]")
    .replace(/session_id=[^&\s]+/gi, "session_id=[redacted]")
    .slice(0, 500);
}

type DiagnosticMessage = {
  source: "console" | "pageerror";
  text: string;
  phase: DiagnosticPhase | null;
  phaseEpoch: number;
};

type DiagnosticOptions = {
  allowWebKitHardReloadCoroutinePair?: boolean;
};

type DiagnosticPhase = "hard-reload" | "player-exit";

const WEBKIT_RENDERER_INFO_WARNING =
  "WebGL: INVALID_ENUM: getParameter: invalid parameter name, WEBGL_debug_renderer_info not enabled";
const FIREFOX_WASM_STREAMING_FALLBACK_START =
  /^wasm streaming compile failed: (?:AbortError: The operation was aborted\.|TypeError: NetworkError when attempting to fetch resource\.)$/;
const FIREFOX_WASM_STREAMING_FALLBACK_END = "falling back to ArrayBuffer instantiation";
const WEBKIT_RESPONSE_CLASS_CAST_ERROR =
  "ClassCastException: Cannot cast instance of Response to Response: incompatible types";
const WEBKIT_BLOB_ACCESS_ERROR =
  /^ttp:\/\/127\.0\.0\.1:4173\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12} due to access control checks\.$/;
const WEBKIT_IO_READ_ERROR = "The I/O read operation failed.";
const WEBKIT_COMPOSE_RESOURCE_ACCESS_ERROR =
  /^\/127\.0\.0\.1:4173\/composeResources\/[A-Za-z0-9._\/-]+ due to access control checks\.$/;
const WEBKIT_CONFIG_ACCESS_ERROR =
  /^\/127\.0\.0\.1:4173\/config\.json due to access control checks\.$/;
const WEBKIT_COROUTINE_TEARDOWN_ERROR =
  /^Fatal exception in coroutines machinery for AwaitContinuation\(DispatchedContinuation\[FlushCoroutineDispatcher@\d+, kotlinx\.coroutines\.DeferredCoroutine\.\$awaitCOROUTINE\$@\d+\]\)\{Completed\}@\d+\. Please read KDoc to 'handleFatalException' method and report this incident to maintainers$/;
const MAX_WEBKIT_HARD_RELOAD_ERRORS = 2;
const MAX_WEBKIT_PLAYER_EXIT_ERRORS = 3;
const WEBKIT_PLAYER_EXIT_BLOB_SIGNATURE = "blob";
const WEBKIT_PLAYER_EXIT_COMPOSE_RESOURCE_SIGNATURE = "compose-resource";
const WEBKIT_PLAYER_EXIT_CONFIG_SIGNATURE = "config";
const WEBKIT_PLAYER_EXIT_COROUTINE_SIGNATURE = "coroutine";
const WEBKIT_PLAYER_EXIT_IO_SIGNATURE = "io-read";
const WEBKIT_PLAYER_EXIT_RESPONSE_CLASS_CAST_SIGNATURE = "response-class-cast";

function incrementEpochCount(
  counts: Map<number, number>,
  epoch: number,
  increment: number = 1,
): void {
  counts.set(epoch, (counts.get(epoch) ?? 0) + increment);
}

function consumePlayerExitSignatures(
  counts: Map<number, number>,
  signatures: Map<number, Set<string>>,
  epoch: number,
  names: readonly string[],
  eventCount: number,
): boolean {
  const count = counts.get(epoch) ?? 0;
  const epochSignatures = signatures.get(epoch) ?? new Set<string>();
  if (
    count + eventCount > MAX_WEBKIT_PLAYER_EXIT_ERRORS ||
    names.some((name) => epochSignatures.has(name))
  ) {
    return false;
  }
  names.forEach((name) => epochSignatures.add(name));
  signatures.set(epoch, epochSignatures);
  counts.set(epoch, count + eventCount);
  return true;
}

async function settleNavigationDiagnostics(
  page: Page,
  diagnostics: { setPhase: (phase: DiagnosticPhase | null) => void },
  expectedRoute: string = `/diagnostic/details/${fixtureContentId}`,
): Promise<void> {
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute(
    "data-product-route",
    expectedRoute,
    { timeout: 30_000 },
  );
  await waitForAnimationFrames(page, 4);
  diagnostics.setPhase(null);
}
