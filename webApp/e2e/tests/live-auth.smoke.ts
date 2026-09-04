import { expect, test, type Locator, type Page } from "@playwright/test";

type LiveTmdbConfig = {
  baseUrl: string;
  readAccessToken: string;
  accountId: string;
  username: string;
  password: string;
};

type LiveEndpoint =
  | "request-token"
  | "validate-login"
  | "create-session"
  | "account-details"
  | "configuration"
  | "genres"
  | "trending-week"
  | "trending-day"
  | "popular"
  | "now-playing"
  | "search"
  | "movie-details"
  | "recommendations"
  | "account-states"
  | "delete-session";

const liveConfig = readLiveConfig();

test("valid TMDB session completes the browse journey and cleans up", async ({ page }) => {
  test.setTimeout(180_000);
  test.skip(liveConfig === null, "Live TMDB configuration and credentials are absent; smoke was not run.");
  if (liveConfig === null) {
    return;
  }

  let sessionId: string | null = null;
  let credentialPayloadMatched = false;
  let applicationCleanupConfirmed = false;
  const cleanupChecks: Promise<void>[] = [];
  const responseStatuses = new Map<LiveEndpoint, number[]>();
  page.on("response", (response) => {
    const endpoint = identifyLiveEndpoint(
      response.url(),
      response.request().method(),
      liveConfig,
    );
    if (endpoint === null) {
      return;
    }
    const statuses = responseStatuses.get(endpoint) ?? [];
    statuses.push(response.status());
    responseStatuses.set(endpoint, statuses);
    if (endpoint === "delete-session") {
      cleanupChecks.push((async () => {
        try {
          const payload = await response.json() as { success?: unknown };
          applicationCleanupConfirmed = response.ok() && payload.success === true;
        } catch {
          applicationCleanupConfirmed = false;
        }
      })());
    }
  });
  await page.route("**/authentication/token/validate_with_login", async (route) => {
    const payload = route.request().postDataJSON() as {
      username?: unknown;
      password?: unknown;
      request_token?: unknown;
    };
    if (
      payload.username !== liveConfig.username ||
      payload.password !== liveConfig.password ||
      typeof payload.request_token !== "string" ||
      payload.request_token.length === 0
    ) {
      throw new Error("Credential field-separation preflight failed.");
    }
    credentialPayloadMatched = true;
    await route.fallback();
  });
  await page.route("**/authentication/session/new", async (route) => {
    const response = await route.fetch();
    if (response.ok()) {
      const payload = await response.json() as { session_id?: unknown };
      if (typeof payload.session_id === "string" && payload.session_id.length > 0) {
        sessionId = payload.session_id;
      }
    }
    await route.fulfill({ response });
  });

  await page.route("**/config.json", async (route) => {
    await route.fulfill({
      json: {
        tmdbBaseUrl: liveConfig.baseUrl,
        tmdbReadAccessToken: liveConfig.readAccessToken,
        tmdbAccountId: liveConfig.accountId,
      },
    });
  });

  try {
    await page.goto("/login");
    await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
      timeout: 30_000,
    });
    const identifierInput = page.getByTestId("login:identifier");
    const passwordInput = page.getByTestId("login:password");
    await identifierInput.focus();
    await identifierInput.pressSequentially(liveConfig.username, { delay: 35 });
    await page.keyboard.press("Tab");
    await expect(passwordInput).toBeFocused();
    await passwordInput.pressSequentially(liveConfig.password, { delay: 35 });
    await page.keyboard.press("Enter");

    await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
    expect(credentialPayloadMatched).toBe(true);
    await expect(page.locator("body")).toHaveAttribute("data-product-route", "/profiles");
    await activateSemanticButton(
      page,
      page.getByRole("button", { name: "Select Nikos profile", exact: true }),
    );
    await expectProductRoute(page, "/home");

    await activateSemanticButton(
      page,
      page.getByRole("button", { name: "Search", exact: true }),
    );
    await expectProductRoute(page, "/search");
    const searchField = page.getByTestId("search:field");
    await searchField.fill("Fight Club");
    await searchField.press("Enter");
    const fightClubCandidates = page.getByRole(
      "button",
      { name: "Open details for Fight Club", exact: true },
    );
    const fightClub = fightClubCandidates.first();
    await expect(fightClub).toHaveCount(1, { timeout: 30_000 });
    await activateSemanticButton(page, fightClub);
    await expectProductRoute(page, "/details/550");

    const myList = page.getByRole("button", { name: "My List", exact: true });
    await expect(myList).toBeEnabled({ timeout: 30_000 });
    await activateSemanticButton(page, myList);
    await expect(page.getByRole("button", { name: "In My List", exact: true })).toHaveCount(1);
    await activateSemanticButton(
      page,
      page.getByRole("button", { name: "Library", exact: true }),
    );
    await expectProductRoute(page, "/library");
    await page.keyboard.press("Space");
    await expectProductRoute(page, "/details/550");

    await activateSemanticButton(
      page,
      page.getByRole("button", { name: "Play", exact: true }),
    );
    await expectProductRoute(page, "/player/550");
    const productionVideo = page.getByTestId("playback-video");
    await expect(productionVideo).toHaveCount(1, { timeout: 30_000 });
    await ensureUserActivatedPlayback(page, productionVideo);
    const resumePositionSeconds = await seekPastResumeThreshold(page, productionVideo);

    await activateSemanticButton(
      page,
      page.getByRole("button", { name: "Enter fullscreen", exact: true }),
    );
    await expect.poll(async () => {
      return page.evaluate(() => document.fullscreenElement !== null);
    }, { timeout: 30_000, intervals: [100] }).toBe(true);
    await page.keyboard.press("Escape");
    await expect.poll(async () => {
      return page.evaluate(() => document.fullscreenElement === null);
    }, { timeout: 30_000, intervals: [100] }).toBe(true);
    await page.keyboard.press("Space");
    await expect.poll(async () => {
      return page.evaluate(() => document.fullscreenElement !== null);
    }, { timeout: 30_000, intervals: [100] }).toBe(true);
    await page.keyboard.press("Escape");
    await expect.poll(async () => {
      return page.evaluate(() => document.fullscreenElement === null);
    }, { timeout: 30_000, intervals: [100] }).toBe(true);

    await page.keyboard.press("Escape");
    await expectProductRoute(page, "/details/550");
    await expect(page.getByTestId("playback-video")).toHaveCount(0);

    await page.reload();
    await expectProductRoute(page, "/details/550");
    await activateSemanticButton(
      page,
      page.getByRole("button", { name: "Play", exact: true }),
    );
    await expectProductRoute(page, "/player/550");
    const resumedVideo = page.getByTestId("playback-video");
    await expect(resumedVideo).toHaveCount(1, { timeout: 30_000 });
    await expect.poll(async () => {
      return readVideoPositionSeconds(resumedVideo);
    }, { timeout: 30_000, intervals: [250] }).toBeGreaterThanOrEqual(resumePositionSeconds - 2);
    await page.keyboard.press("Escape");
    await expectProductRoute(page, "/details/550");
    await expect(page.getByTestId("playback-video")).toHaveCount(0);
    await activateSemanticButton(
      page,
      page.getByRole("button", { name: "Sign out", exact: true }),
    );
    await expectProductRoute(page, "/login");
    await page.goBack();
    await expectProductRoute(page, "/login");
    await Promise.all(cleanupChecks);
    expect(applicationCleanupConfirmed).toBe(true);
    assertSuccessfulLiveResponses(responseStatuses);
    expect(sessionId).not.toBeNull();
  } finally {
    try {
      if (sessionId !== null && !applicationCleanupConfirmed) {
        const cleanupUrl = new URL("3/authentication/session", ensureTrailingSlash(liveConfig.baseUrl));
        let cleanupConfirmed = false;
        try {
          const cleanup = await page.request.delete(cleanupUrl.toString(), {
            headers: {
              accept: "application/json",
              authorization: `Bearer ${liveConfig.readAccessToken}`,
              "content-type": "application/json",
            },
            data: { session_id: sessionId },
          });
          const payload = await cleanup.json() as { success?: unknown };
          cleanupConfirmed = cleanup.ok() && payload.success === true;
        } catch {
          cleanupConfirmed = false;
        }
        if (!cleanupConfirmed) {
          throw new Error("Temporary-session cleanup was not confirmed.");
        }
      }
    } finally {
      await page.evaluate(() => {
        localStorage.clear();
        sessionStorage.clear();
      });
    }
  }
});

function readLiveConfig(): LiveTmdbConfig | null {
  const values = {
    baseUrl: process.env.STREAMCORE_LIVE_TMDB_BASE_URL,
    readAccessToken: process.env.STREAMCORE_LIVE_TMDB_READ_ACCESS_TOKEN,
    accountId: process.env.STREAMCORE_LIVE_TMDB_ACCOUNT_ID,
    username: process.env.STREAMCORE_LIVE_TMDB_USERNAME,
    password: process.env.STREAMCORE_LIVE_TMDB_PASSWORD,
  };
  if (Object.values(values).some((value) => value === undefined || value.trim().length === 0)) {
    return null;
  }
  return values as LiveTmdbConfig;
}

function ensureTrailingSlash(value: string): string {
  return value.endsWith("/") ? value : `${value}/`;
}

function identifyLiveEndpoint(
  urlValue: string,
  method: string,
  config: LiveTmdbConfig,
): LiveEndpoint | null {
  const actualUrl = new URL(urlValue);
  const apiBaseUrl = new URL("3/", ensureTrailingSlash(config.baseUrl));
  if (actualUrl.origin !== apiBaseUrl.origin) {
    return null;
  }
  const expectedPaths = new Map<LiveEndpoint, string>([
    ["request-token", new URL("authentication/token/new", apiBaseUrl).pathname],
    ["validate-login", new URL("authentication/token/validate_with_login", apiBaseUrl).pathname],
    ["create-session", new URL("authentication/session/new", apiBaseUrl).pathname],
    ["account-details", new URL(`account/${config.accountId}`, apiBaseUrl).pathname],
    ["configuration", new URL("configuration", apiBaseUrl).pathname],
    ["genres", new URL("genre/movie/list", apiBaseUrl).pathname],
    ["trending-week", new URL("trending/movie/week", apiBaseUrl).pathname],
    ["trending-day", new URL("trending/movie/day", apiBaseUrl).pathname],
    ["popular", new URL("movie/popular", apiBaseUrl).pathname],
    ["now-playing", new URL("movie/now_playing", apiBaseUrl).pathname],
    ["search", new URL("search/movie", apiBaseUrl).pathname],
    ["delete-session", new URL("authentication/session", apiBaseUrl).pathname],
  ]);
  for (const [endpoint, pathname] of expectedPaths) {
    if (
      actualUrl.pathname === pathname &&
      (endpoint !== "delete-session" || method === "DELETE")
    ) {
      return endpoint;
    }
  }
  if (/\/movie\/\d+\/recommendations$/.test(actualUrl.pathname)) {
    return "recommendations";
  }
  if (/\/movie\/\d+\/account_states$/.test(actualUrl.pathname)) {
    return "account-states";
  }
  if (/\/movie\/\d+$/.test(actualUrl.pathname)) {
    return "movie-details";
  }
  return null;
}

function assertSuccessfulLiveResponses(statuses: Map<LiveEndpoint, number[]>): void {
  const endpoints: LiveEndpoint[] = [
    "request-token",
    "validate-login",
    "create-session",
    "account-details",
    "configuration",
    "genres",
    "trending-week",
    "trending-day",
    "popular",
    "now-playing",
    "search",
    "movie-details",
    "recommendations",
    "account-states",
    "delete-session",
  ];
  for (const endpoint of endpoints) {
    const observedStatuses = statuses.get(endpoint) ?? [];
    expect(observedStatuses.length, `${endpoint} response was not observed.`).toBeGreaterThan(0);
    expect(
      observedStatuses.every((status) => status >= 200 && status < 300),
      `${endpoint} returned a non-2xx status.`,
    ).toBe(true);
  }
}

async function expectProductRoute(page: Page, path: string): Promise<void> {
  await expect(page).toHaveURL(new RegExp(`${path.replaceAll("/", "\\/")}$`), {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", path, {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
}

async function activateSemanticButton(page: Page, button: Locator): Promise<void> {
  await waitForAnimationFrames(page, 4);
  let bounds = await stableSemanticBounds(button);
  await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  await waitForAnimationFrames(page, 2);
  bounds = await stableSemanticBounds(button);
  await page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  await waitForAnimationFrames(page, 4);
}

async function ensureUserActivatedPlayback(page: Page, video: Locator): Promise<void> {
  const play = page.getByRole("button", { name: "Play", exact: true });
  const pause = page.getByRole("button", { name: "Pause", exact: true });
  await expect.poll(async () => {
    const playEnabled = await play.count() === 1 && await play.isEnabled();
    const pauseEnabled = await pause.count() === 1 && await pause.isEnabled();
    return playEnabled || pauseEnabled;
  }, { timeout: 30_000, intervals: [250] }).toBe(true);
  if (await pause.count() === 1 && await pause.isEnabled()) {
    await activateSemanticButton(page, pause);
    await expect.poll(async () => {
      return video.evaluate((element) => (element as HTMLVideoElement).paused);
    }, { timeout: 30_000, intervals: [250] }).toBe(true);
    await expect(play).toBeEnabled({ timeout: 30_000 });
  }
  await activateSemanticButton(page, play);
  await expect.poll(async () => {
    return video.evaluate((element) => !(element as HTMLVideoElement).paused);
  }, { timeout: 30_000, intervals: [250] }).toBe(true);
  await expect(pause).toBeEnabled({ timeout: 30_000 });
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
  if (current === null) {
    throw new Error("Semantic control does not expose stable viewport bounds");
  }
  return current;
}

async function seekPastResumeThreshold(page: Page, video: Locator): Promise<number> {
  const forward = page.getByRole("button", { name: "Forward 10 seconds", exact: true });
  let positionSeconds = await readVideoPositionSeconds(video);
  for (let seekIndex = 0; seekIndex < 4; seekIndex += 1) {
    const previousPositionSeconds = positionSeconds;
    await activateSemanticButton(page, forward);
    await expect.poll(async () => {
      return readVideoPositionSeconds(video);
    }, { timeout: 30_000, intervals: [250] }).toBeGreaterThan(previousPositionSeconds + 5);
    positionSeconds = await readVideoPositionSeconds(video);
  }
  expect(positionSeconds).toBeGreaterThan(30);
  await expect(page.getByLabel(/^Playback position (?!0:[012]\d)\d+:\d{2} of /)).toHaveCount(1, {
    timeout: 30_000,
  });
  return positionSeconds;
}

async function readVideoPositionSeconds(video: Locator): Promise<number> {
  return video.evaluate((element) => (element as HTMLVideoElement).currentTime);
}

async function waitForAnimationFrames(page: Page, frameCount: number): Promise<void> {
  await page.evaluate(async (count) => {
    for (let frameIndex = 0; frameIndex < count; frameIndex += 1) {
      await new Promise<void>((resolve) => {
        let completed = false;
        const complete = (): void => {
          if (completed) {
            return;
          }
          completed = true;
          clearTimeout(fallback);
          resolve();
        };
        const fallback = setTimeout(complete, 100);
        requestAnimationFrame(complete);
      });
    }
  }, frameCount);
}
