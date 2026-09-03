import { expect, test, type Locator, type Page, type TestInfo } from "@playwright/test";

const fixturesEnabled = process.env.STREAMCORE_WEB_PLAYER_FIXTURES === "enabled";
const fixtureContentId = "603";
const fixtureRoute = `/diagnostic/player/${fixtureContentId}`;
const fixtureSkipReason =
  "WEB-04 player fixtures are structural until WEB-04D integrates the A/B engine and UI.";

test.describe("WEB-04 deterministic player acceptance", () => {
  test.skip(!fixturesEnabled, fixtureSkipReason);

  test("successful fake playback reaches ready and plays after activation", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "success");

    await expect(playerVideo(page)).toHaveCount(1);
    await expectPlayerState(page, "ready");
    await activateProjectedButton(page, "Play");
    await expect(page.locator("body")).toHaveAttribute("data-player-playing", "true");
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
    await activateProjectedButton(page, "Speed");
    await activateProjectedButton(page, "1.5x");
    await expect(page.locator("body")).toHaveAttribute("data-player-speed", "1.5");

    await activateProjectedButton(page, "Playback settings");
    await activateProjectedButton(page, "Quality");
    await activateProjectedButton(page, "1080p");
    await expect(page.locator("body")).toHaveAttribute("data-player-video-track", "video-1080");

    await activateProjectedButton(page, "Playback settings");
    await activateProjectedButton(page, "Audio");
    await activateProjectedButton(page, "Greek");
    await expect(page.locator("body")).toHaveAttribute("data-player-audio-track", "audio-el");

    await activateProjectedButton(page, "Playback settings");
    await activateProjectedButton(page, "Subtitles");
    await activateProjectedButton(page, "English");
    await expect(page.locator("body")).toHaveAttribute("data-player-text-track", "text-en");
    await diagnostics.assertClean();
  });

  test("profile-scoped resume survives close and hard reload", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "resume", { profile: "profile-a" });

    await expect(page.locator("body")).toHaveAttribute("data-player-profile-id", "profile-a");
    await expectNumericBodyAttribute(page, "data-player-position-ms", 0, 1_000);
    await scrubTimeline(page, 0.375);
    await expectNumericBodyAttribute(page, "data-player-position-ms", 44_000, 46_000);
    await page.keyboard.press("Escape");
    await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);

    await openPlayerFixture(page, "resume", { profile: "profile-a" });
    await expect(page.locator("body")).toHaveAttribute("data-player-profile-id", "profile-a");
    await expectNumericBodyAttribute(page, "data-player-position-ms", 44_000, 46_000);
    await page.reload();
    await waitForFixtureReadiness(page, "resume");
    await expect(page.locator("body")).toHaveAttribute("data-player-profile-id", "profile-a");
    await expectNumericBodyAttribute(page, "data-player-position-ms", 44_000, 46_000);

    await page.keyboard.press("Escape");
    await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);
    await openPlayerFixture(page, "resume", { profile: "profile-b" });
    await expect(page.locator("body")).toHaveAttribute("data-player-profile-id", "profile-b");
    await expectNumericBodyAttribute(page, "data-player-position-ms", 0, 1_000);
    await diagnostics.assertClean();
  });

  test("fullscreen exit restores focus to the invoking control", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "success");

    await activateProjectedButton(page, "Fullscreen");
    await expect(page.locator("body")).toHaveAttribute("data-player-fullscreen", "true");
    await page.keyboard.press("Escape");
    await expect(page.locator("body")).toHaveAttribute("data-player-fullscreen", "false");
    await expect(page.locator("body")).toHaveAttribute("data-player-focused-action", "fullscreen");
    await diagnostics.assertClean();
  });

  test("Escape closes one layer and browser Back returns to details", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await openPlayerFixture(page, "success");

    await activateProjectedButton(page, "Playback settings");
    await expect(page.locator("body")).toHaveAttribute("data-player-layer", "settings-root");
    await page.keyboard.press("Escape");
    await expect(page.locator("body")).toHaveAttribute("data-player-layer", "player");
    await expect(page).toHaveURL(new RegExp(`${fixtureRoute.replaceAll("/", "\\/")}\\?`));
    await page.keyboard.press("Escape");
    await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);

    await page.goto(`${fixtureRoute}?fixture=success`);
    await waitForFixtureReadiness(page, "success");
    await page.goBack();
    await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);
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
    await expect(page.getByRole("slider", { name: "Playback position", exact: true })).toHaveCount(1);
    await scrubTimeline(page, 0.25);
    await expectNumericBodyAttribute(page, "data-player-position-ms", 29_000, 31_000);
    await expect(page.locator("body")).toHaveAttribute("data-player-canvas-capture-count", "0");
    await diagnostics.assertClean();
  });

  test("repeated enter, play and close leaves no session, node, listener or timer", async ({ page }, testInfo) => {
    const diagnostics = installSanitizedDiagnostics(page, testInfo);
    await installRuntimeConfig(page);
    await page.goto(`/diagnostic/details/${fixtureContentId}`);

    for (let iteration = 1; iteration <= 5; iteration += 1) {
      await page.goto(`${fixtureRoute}?fixture=success&iteration=${iteration}`);
      await waitForFixtureReadiness(page, "success");
      await activateProjectedButton(page, "Play");
      await page.keyboard.press("Escape");
      await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);
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
  const query = new URLSearchParams({ fixture: scenario, ...parameters });
  await page.goto(`${fixtureRoute}?${query.toString()}`);
  await waitForFixtureReadiness(page, scenario);
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

async function scrubTimeline(page: Page, fraction: number): Promise<void> {
  const slider = page.getByRole("slider", { name: "Playback position", exact: true });
  await expect(slider).toHaveCount(1);
  const bounds = await stableSemanticBounds(slider);
  const x = bounds.x + bounds.width * fraction;
  const y = bounds.y + bounds.height / 2;
  await page.mouse.move(bounds.x + bounds.width * 0.1, y);
  await page.mouse.down();
  await page.mouse.move(x, y, { steps: 8 });
  await page.mouse.up();
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

function installSanitizedDiagnostics(page: Page, testInfo: TestInfo) {
  const messages: string[] = [];
  page.on("pageerror", (error) => messages.push(redactDiagnostic(error.message)));
  page.on("console", (message) => {
    if (message.type() === "error") messages.push(redactDiagnostic(message.text()));
  });
  return {
    async assertClean(): Promise<void> {
      if (messages.length > 0) {
        await testInfo.attach("sanitized-browser-errors", {
          body: Buffer.from(JSON.stringify(messages, null, 2)),
          contentType: "application/json",
        });
      }
      expect(messages).toEqual([]);
    },
  };
}

function redactDiagnostic(message: string): string {
  return message
    .replace(/https?:\/\/\S+/gi, "[redacted-url]")
    .replace(/bearer\s+\S+/gi, "Bearer [redacted]")
    .replace(/session_id=[^&\s]+/gi, "session_id=[redacted]")
    .slice(0, 500);
}
