import { expect, test, type Locator } from "@playwright/test";

const validConfig = {
  tmdbBaseUrl: "https://api.example.test/",
  tmdbReadAccessToken: "browser-visible-token",
  tmdbAccountId: "42",
};

test.beforeEach(async ({ page }) => {
  await page.route("**/config.json", async (route) => {
    await route.fulfill({ json: validConfig });
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
    const path = new URL(route.request().url()).pathname;
    const headers = {
      "access-control-allow-origin": "*",
      "content-type": "application/json",
    };
    if (path.endsWith("/configuration")) {
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
    if (path.endsWith("/genre/movie/list")) {
      await route.fulfill({ headers, json: { genres: [] } });
      return;
    }
    await route.fulfill({
      headers,
      json: { page: 1, results: [], total_pages: 1, total_results: 0 },
    });
  });
});

test("records the actual Compose DOM/accessibility selector projection", async ({ page }, testInfo) => {
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (request) => {
    if (request.url().includes("web-probe")) {
      console.log(`${testInfo.project.name} image request failed: ${request.failure()?.errorText}`);
    }
  });
  page.on("response", (response) => {
    if (response.url().includes("web-probe")) {
      console.log(`${testInfo.project.name} image response: ${response.status()}`);
    }
  });

  await page.goto("/diagnostic");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.getByRole("button", { name: "Details ID", exact: true })).toHaveCount(1);

  const evidence = await page.evaluate(() => ({
    bodyChildren: Array.from(document.body.children).map((element) => element.tagName),
    roles: Array.from(document.querySelectorAll("[role]")).map((element) => ({
      role: element.getAttribute("role"),
      label: element.getAttribute("aria-label"),
      testId: element.getAttribute("data-testid"),
      text: element.textContent,
    })),
    testIds: Array.from(document.querySelectorAll("[data-testid]")).map((element) =>
      element.getAttribute("data-testid"),
    ),
  }));

  console.log(`${testInfo.project.name} selector evidence: ${JSON.stringify(evidence)}`);
  expect(evidence.roles).toEqual([]);
  expect(evidence.testIds).toEqual([]);
  await expect(page.locator("body")).toHaveAttribute("data-image-probe", "loaded", {
    timeout: 20_000,
  });
  expect(pageErrors).toEqual([]);
});

test("missing runtime config never starts the product graph", async ({ page }) => {
  await page.unroute("**/config.json");
  await page.route("**/config.json", async (route) => {
    await route.fulfill({ status: 404, body: "missing" });
  });

  await page.goto("/diagnostic");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "blocking-error");
  await expect(page.getByRole("button", { name: "Details ID", exact: true })).toHaveCount(0);
});

test("diagnostic direct ID route and reload preserve route identity", async ({ page }) => {
  await page.goto("/diagnostic/details/603");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute(
    "data-product-route",
    "/diagnostic/details/603",
  );
  expect(new URL(page.url()).pathname).toBe("/diagnostic/details/603");

  await page.reload();
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute(
    "data-product-route",
    "/diagnostic/details/603",
  );
  expect(new URL(page.url()).pathname).toBe("/diagnostic/details/603");
});

test("diagnostic pointer navigation binds browser Back and Forward history", async ({ page }) => {
  await page.goto("/diagnostic");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });

  const detailsBounds = await semanticBounds(
    page.getByRole("button", { name: "Details ID", exact: true }),
  );
  await page.mouse.click(
    detailsBounds.x + detailsBounds.width / 2,
    detailsBounds.y + detailsBounds.height / 2,
  );
  await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);
  const playerBounds = await semanticBounds(
    page.getByRole("button", { name: "Player ID", exact: true }),
  );
  await page.mouse.click(
    playerBounds.x + playerBounds.width / 2,
    playerBounds.y + playerBounds.height / 2,
  );
  await expect(page).toHaveURL(/\/diagnostic\/player\/603$/);

  await page.goBack();
  await expect(page).toHaveURL(/\/diagnostic\/details\/603$/);
  await page.goForward();
  await expect(page).toHaveURL(/\/diagnostic\/player\/603$/);
});

test("four distinct official DataStore names retain values across reload", async ({ page }) => {
  await page.goto("/diagnostic");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  const beforeReload = await page.evaluate(() => ({ ...localStorage }));

  await page.reload();
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  const afterReload = await page.evaluate(() => ({ ...localStorage }));

  expect(Object.keys(beforeReload).sort()).toEqual([
    "datastore_LOCAL_library.preferences_pb_version",
    "datastore_LOCAL_playback_progress.preferences_pb_version",
    "datastore_LOCAL_search_history.preferences_pb_version",
    "datastore_LOCAL_tmdb_auth.preferences_pb_version",
    "library.preferences_pb",
    "playback_progress.preferences_pb",
    "search_history.preferences_pb",
    "tmdb_auth.preferences_pb",
  ]);
  expect(afterReload).toEqual(beforeReload);
});

test("corrupt persistent protobuf is replaced and remains in persistent mode", async ({ page }) => {
  await page.goto("/diagnostic");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  await page.evaluate(() => {
    localStorage.setItem("tmdb_auth.preferences_pb", "not-a-valid-preferences-protobuf");
  });

  await page.reload();
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute("data-storage-mode", "persistent");
  expect(await page.evaluate(() => localStorage.getItem("tmdb_auth.preferences_pb"))).not.toBe(
    "not-a-valid-preferences-protobuf",
  );
});

test("persistent DataStore quota failure retries official session storage", async ({ page }) => {
  await page.addInitScript(() => {
    const originalSetItem = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key: string, value: string): void {
      if (this === window.localStorage && key.includes("preferences_pb")) {
        throw new DOMException("DataStore quota exceeded", "QuotaExceededError");
      }
      originalSetItem.call(this, key, value);
    };
  });

  await page.goto("/diagnostic");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute("data-storage-mode", "session");
  const sessionKeys = await page.evaluate(() => Object.keys(sessionStorage).sort());
  expect(sessionKeys).toEqual([
    "datastore_SESSION_library.preferences_pb_version",
    "datastore_SESSION_playback_progress.preferences_pb_version",
    "datastore_SESSION_search_history.preferences_pb_version",
    "datastore_SESSION_tmdb_auth.preferences_pb_version",
    "library.preferences_pb",
    "playback_progress.preferences_pb",
    "search_history.preferences_pb",
    "tmdb_auth.preferences_pb",
  ]);
});

test("Ktor Js Fetch preserves TMDB URL and headers", async ({ page }) => {
  const requests: Array<{ url: string; authorization?: string; accept?: string }> = [];
  page.on("request", (request) => {
    if (request.url().startsWith("https://api.example.test/")) {
      requests.push({
        url: request.url(),
        authorization: request.headers()["authorization"],
        accept: request.headers()["accept"],
      });
    }
  });

  await page.goto("/diagnostic");
  await expect(page.locator("body")).toHaveAttribute("data-network-probe", "success", {
    timeout: 30_000,
  });

  expect(requests).toHaveLength(3);
  expect(requests.map((request) => new URL(request.url).pathname).sort()).toEqual([
    "/3/configuration",
    "/3/genre/movie/list",
    "/3/search/movie",
  ]);
  for (const request of requests) {
    expect(request.authorization).toBe("Bearer browser-visible-token");
    expect(request.accept).toBe("application/json");
  }
});

test("Ktor Fetch server failure maps through the repository AppError boundary", async ({ page }) => {
  await page.unroute("https://api.example.test/**");
  await page.route("https://api.example.test/**", async (route) => {
    await route.fulfill({
      status: 503,
      headers: {
        "access-control-allow-origin": "*",
        "content-type": "application/json",
      },
      json: { status_message: "unavailable" },
    });
  });

  await page.goto("/diagnostic");
  await expect(page.locator("body")).toHaveAttribute("data-network-probe", "server-error", {
    timeout: 30_000,
  });
});

test("external HTTPS links cannot retain window.opener", async ({ page, context }) => {
  await context.route("https://example.com/**", async (route) => {
    await route.fulfill({ contentType: "text/html", body: "<!doctype html><title>probe</title>" });
  });
  await page.goto("/diagnostic");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });

  const externalLink = page.getByRole("button", { name: "External link", exact: true });
  const bounds = await semanticBounds(externalLink);
  await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  await page.waitForTimeout(300);
  const popupPromise = page.waitForEvent("popup");
  await page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  const popup = await popupPromise;
  await popup.waitForLoadState();

  expect(await popup.evaluate(() => window.opener === null)).toBe(true);
  await popup.close();
});

async function semanticBounds(
  button: Locator,
): Promise<{ x: number; y: number; width: number; height: number }> {
  let resolvedBounds = await button.boundingBox();
  await expect.poll(
    async () => {
      resolvedBounds = await button.boundingBox();
      return resolvedBounds !== null;
    },
    { timeout: 30_000, intervals: [250] },
  ).toBe(true);
  if (resolvedBounds === null) {
    throw new Error("Semantic button does not expose viewport bounds");
  }
  return resolvedBounds;
}
