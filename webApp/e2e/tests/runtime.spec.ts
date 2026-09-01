import { expect, test } from "@playwright/test";

const validConfig = {
  tmdbBaseUrl: "https://api.example.test/3/",
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

  await page.goto("/");
  const videoProbe = page.locator('[data-testid="html-video-probe"]');
  await expect(videoProbe).toHaveAttribute("data-shaka-probe", "linked", { timeout: 30_000 });

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
  expect(evidence.testIds).toEqual(["html-video-probe"]);
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

  await page.goto("/");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "blocking-error");
  await expect(page.locator('[data-testid="html-video-probe"]')).toHaveCount(0);
});

test("direct ID route and reload preserve route identity", async ({ page }) => {
  await page.goto("/details/603");
  await expect(page.locator('[data-testid="html-video-probe"]')).toHaveAttribute(
    "data-shaka-probe",
    "linked",
    { timeout: 30_000 },
  );
  expect(new URL(page.url()).pathname).toBe("/details/603");

  await page.reload();
  await expect(page.locator('[data-testid="html-video-probe"]')).toHaveAttribute(
    "data-shaka-probe",
    "linked",
    { timeout: 30_000 },
  );
  expect(new URL(page.url()).pathname).toBe("/details/603");
});

test("pointer navigation binds browser Back and Forward history", async ({ page }) => {
  await page.goto("/");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });

  await page.mouse.click(200, 165);
  await expect(page).toHaveURL(/\/details\/603$/);
  await page.mouse.click(315, 165);
  await expect(page).toHaveURL(/\/player\/603$/);

  await page.goBack();
  await expect(page).toHaveURL(/\/details\/603$/);
  await page.goForward();
  await expect(page).toHaveURL(/\/player\/603$/);
});

test("four distinct official DataStore names retain values across reload", async ({ page }) => {
  await page.goto("/");
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

test("external HTTPS links cannot retain window.opener", async ({ page, context }) => {
  await context.route("https://example.com/**", async (route) => {
    await route.fulfill({ contentType: "text/html", body: "<!doctype html><title>probe</title>" });
  });
  await page.goto("/");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", {
    timeout: 30_000,
  });

  const popupPromise = page.waitForEvent("popup");
  await page.mouse.click(440, 165);
  const popup = await popupPromise;
  await popup.waitForLoadState();

  expect(await popup.evaluate(() => window.opener === null)).toBe(true);
  await popup.close();
});
