import { expect, test } from "@playwright/test";

const validConfig = {
  tmdbBaseUrl: "https://api.example.test/",
  tmdbReadAccessToken: "browser-visible-deployment-value",
  tmdbAccountId: "42",
};

test.beforeEach(async ({ page }) => {
  await page.route("**/config.json", async (route) => {
    await route.fulfill({ json: validConfig });
  });
  await page.route("https://api.example.test/**", async (route) => {
    const url = new URL(route.request().url());
    const headers = {
      "access-control-allow-origin": "*",
      "content-type": "application/json",
    };
    if (url.pathname.endsWith("/authentication/token/new")) {
      await route.fulfill({ headers, json: { success: true, request_token: "fixture-request" } });
      return;
    }
    if (url.pathname.endsWith("/authentication/token/validate_with_login")) {
      await route.fulfill({ headers, json: { success: true, request_token: "fixture-validated" } });
      return;
    }
    if (url.pathname.endsWith("/authentication/session/new")) {
      await route.fulfill({ headers, json: { success: true, session_id: "fixture-session" } });
      return;
    }
    if (url.pathname.endsWith("/account/42")) {
      await route.fulfill({ headers, json: { id: 42, username: "web-user", name: "Web User" } });
      return;
    }
    if (url.pathname.includes("/movie/550/account_states")) {
      await route.fulfill({ headers, json: { id: 550, favorite: false, rated: false, watchlist: false } });
      return;
    }
    await route.fulfill({ headers, json: {} });
  });
});

test("login, profile selection, persistence, history, keyboard and pointer contract", async ({ page }, testInfo) => {
  const pageErrors: string[] = [];
  const credentialInput = ["fixture", "credential"].join("-");
  page.on("pageerror", (error) => pageErrors.push(error.message));

  await page.goto("/login");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/login", { timeout: 30_000 });
  await page.waitForTimeout(500);
  await page.screenshot({
    path: `screenshots/${testInfo.project.name}-login.png`,
    animations: "disabled",
  });

  await page.mouse.move(220, 260);
  await page.mouse.click(220, 260);
  await page.keyboard.type("web-user");
  await page.keyboard.press("Tab");
  await page.keyboard.type(credentialInput);
  await page.keyboard.press("Enter");

  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/profiles");
  expect(page.url()).not.toContain("web-user");
  expect(page.url()).not.toContain(credentialInput);
  await page.waitForTimeout(500);
  await page.screenshot({
    path: `screenshots/${testInfo.project.name}-profiles.png`,
    animations: "disabled",
  });

  await page.mouse.move(150, 220);
  await page.mouse.click(150, 220);
  await expect(page).toHaveURL(/\/authenticated$/, { timeout: 30_000 });
  await page.reload();
  await expect(page).toHaveURL(/\/authenticated$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/authenticated");

  await page.goBack();
  await expect(page).toHaveURL(/\/profiles$/);
  await page.goForward();
  await expect(page).toHaveURL(/\/authenticated$/);

  await page.route("**/movie/550/account_states**", async (route) => {
    await route.fulfill({
      status: 401,
      headers: {
        "access-control-allow-origin": "*",
        "content-type": "application/json",
      },
      json: { status_code: 3, status_message: "Session expired" },
    });
  });
  await page.reload();
  await expect(page).toHaveURL(/\/login$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/login");
  await page.goBack();
  await expect(page).toHaveURL(/\/login$/);
  expect(page.url()).not.toContain(credentialInput);
  expect(pageErrors).toEqual([]);
});
