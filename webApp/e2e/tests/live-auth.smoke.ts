import { expect, test } from "@playwright/test";

type LiveTmdbConfig = {
  baseUrl: string;
  readAccessToken: string;
  accountId: string;
  username: string;
  password: string;
};

const liveConfig = readLiveConfig();

test("valid TMDB login reaches profiles and restores after reload", async ({ page }) => {
  test.setTimeout(60_000);
  test.skip(liveConfig === null, "Live TMDB configuration and credentials are absent; smoke was not run.");
  if (liveConfig === null) {
    return;
  }

  let sessionId: string | null = null;
  let credentialPayloadMatched = false;
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
    await page.keyboard.type(liveConfig.username, { delay: 35 });
    await page.keyboard.press("Tab");
    await expect.poll(async () => {
      return page.evaluate(() => {
        return document.activeElement instanceof HTMLInputElement &&
          document.activeElement.value.length === 0;
      });
    }).toBe(true);
    await page.waitForTimeout(500);
    await page.keyboard.type(liveConfig.password, { delay: 35 });
    await page.keyboard.press("Enter");

    await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
    expect(credentialPayloadMatched).toBe(true);
    await expect(page.locator("body")).toHaveAttribute("data-product-route", "/profiles");
    await page.reload();
    await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
    await expect(page.locator("body")).toHaveAttribute("data-product-route", "/profiles");
    expect(sessionId).not.toBeNull();
  } finally {
    if (sessionId !== null) {
      const cleanupUrl = new URL("3/authentication/session", ensureTrailingSlash(liveConfig.baseUrl));
      const cleanup = await page.request.delete(cleanupUrl.toString(), {
        headers: {
          accept: "application/json",
          authorization: `Bearer ${liveConfig.readAccessToken}`,
          "content-type": "application/json",
        },
        data: { session_id: sessionId },
      });
      expect(cleanup.ok()).toBe(true);
    }
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
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
