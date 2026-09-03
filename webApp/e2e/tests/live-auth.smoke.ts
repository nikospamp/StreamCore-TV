import { expect, test } from "@playwright/test";

type LiveTmdbConfig = {
  baseUrl: string;
  readAccessToken: string;
  accountId: string;
  username: string;
  password: string;
};

type AuthEndpoint =
  | "request-token"
  | "validate-login"
  | "create-session"
  | "account-details";

const liveConfig = readLiveConfig();

test("valid TMDB login reaches profiles and restores after reload", async ({ page }) => {
  test.setTimeout(60_000);
  test.skip(liveConfig === null, "Live TMDB configuration and credentials are absent; smoke was not run.");
  if (liveConfig === null) {
    return;
  }

  let sessionId: string | null = null;
  let credentialPayloadMatched = false;
  const authResponseStatuses = new Map<AuthEndpoint, number[]>();
  page.on("response", (response) => {
    const endpoint = identifyAuthEndpoint(response.url(), liveConfig);
    if (endpoint === null) {
      return;
    }
    const statuses = authResponseStatuses.get(endpoint) ?? [];
    statuses.push(response.status());
    authResponseStatuses.set(endpoint, statuses);
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
    assertSuccessfulAuthResponses(authResponseStatuses);
    await page.reload();
    await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
    await expect(page.locator("body")).toHaveAttribute("data-product-route", "/profiles");
    expect(sessionId).not.toBeNull();
  } finally {
    try {
      if (sessionId !== null) {
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

function identifyAuthEndpoint(urlValue: string, config: LiveTmdbConfig): AuthEndpoint | null {
  const actualUrl = new URL(urlValue);
  const apiBaseUrl = new URL("3/", ensureTrailingSlash(config.baseUrl));
  if (actualUrl.origin !== apiBaseUrl.origin) {
    return null;
  }
  const expectedPaths = new Map<AuthEndpoint, string>([
    ["request-token", new URL("authentication/token/new", apiBaseUrl).pathname],
    ["validate-login", new URL("authentication/token/validate_with_login", apiBaseUrl).pathname],
    ["create-session", new URL("authentication/session/new", apiBaseUrl).pathname],
    ["account-details", new URL(`account/${config.accountId}`, apiBaseUrl).pathname],
  ]);
  for (const [endpoint, pathname] of expectedPaths) {
    if (actualUrl.pathname === pathname) {
      return endpoint;
    }
  }
  return null;
}

function assertSuccessfulAuthResponses(statuses: Map<AuthEndpoint, number[]>): void {
  const endpoints: AuthEndpoint[] = [
    "request-token",
    "validate-login",
    "create-session",
    "account-details",
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
