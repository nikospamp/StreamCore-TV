import { expect, test, type Locator, type Page, type Response } from "@playwright/test";

const validConfig = {
  tmdbBaseUrl: "https://api.example.test/",
  tmdbReadAccessToken: "browser-visible-deployment-value",
  tmdbAccountId: "42",
};
const syntheticSessionId = `fixture-session-${"s".repeat(64)}`;
const syntheticAccountUsername = `fixture-account-${"u".repeat(48)}`;
const punctuationIdentifier = "fixture.user+tv@example.test";
const punctuationPassword = "P@ss!#$%&()*+,-./:;<=>?@[\\]^_`{|}~";
const loginActionLabels = [
  "Show password",
  "Continue",
  "Forgot password?",
  "Create account",
  "Need help?",
] as const;
const browseMovie = {
  id: 603,
  title: "Orbit Fall",
  overview: "A deterministic browse fixture.",
  adult: false,
  poster_path: null,
  backdrop_path: null,
  genre_ids: [878],
  release_date: "2024-04-01",
  vote_average: 8.7,
};
const browseRecommendation = {
  ...browseMovie,
  id: 604,
  title: "Northern Signal",
};

function movieList(results = [browseMovie]) {
  return {
    page: 1,
    results,
    total_pages: 1,
    total_results: results.length,
  };
}

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
      await route.fulfill({ headers, json: { success: true, session_id: syntheticSessionId } });
      return;
    }
    if (url.pathname.endsWith("/account/42")) {
      await route.fulfill({
        headers,
        json: { id: 42, username: syntheticAccountUsername, name: null },
      });
      return;
    }
    if (
      route.request().method() === "DELETE" &&
      url.pathname.endsWith("/authentication/session")
    ) {
      await route.fulfill({ headers, json: { success: true } });
      return;
    }
    if (url.pathname.endsWith("/configuration")) {
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
    if (url.pathname.endsWith("/genre/movie/list")) {
      await route.fulfill({ headers, json: { genres: [{ id: 878, name: "Science Fiction" }] } });
      return;
    }
    if (
      url.pathname.endsWith("/trending/movie/week") ||
      url.pathname.endsWith("/trending/movie/day") ||
      url.pathname.endsWith("/movie/popular") ||
      url.pathname.endsWith("/movie/now_playing") ||
      url.pathname.endsWith("/search/movie")
    ) {
      await route.fulfill({ headers, json: movieList() });
      return;
    }
    const accountStatesMatch = url.pathname.match(/\/movie\/(\d+)\/account_states$/);
    if (accountStatesMatch !== null) {
      await route.fulfill({
        headers,
        json: {
          id: Number(accountStatesMatch[1]),
          favorite: false,
          rated: false,
          watchlist: false,
        },
      });
      return;
    }
    const recommendationsMatch = url.pathname.match(/\/movie\/(\d+)\/recommendations$/);
    if (recommendationsMatch !== null) {
      await route.fulfill({ headers, json: movieList([browseRecommendation]) });
      return;
    }
    const detailsMatch = url.pathname.match(/\/movie\/(\d+)$/);
    if (detailsMatch !== null) {
      const id = Number(detailsMatch[1]);
      const movie = id === browseRecommendation.id ? browseRecommendation : browseMovie;
      await route.fulfill({
        headers,
        json: {
          ...movie,
          id,
          genres: [{ id: 878, name: "Science Fiction" }],
        },
      });
      return;
    }
    await route.fulfill({ status: 404, headers, json: { status_message: "Missing fixture" } });
  });
});

test("login, profile selection, persistence, history, keyboard and pointer contract", async ({ page }, testInfo) => {
  test.setTimeout(60_000);
  const pageErrors: PageErrorEvidence[] = [];
  let hardReloadPhase: HardReloadPhase | null = null;
  const normalizedCoroutineErrors: HardReloadErrorCounts = { restoration: 0, expiry: 0 };
  const responseClassCastErrors: HardReloadErrorCounts = { restoration: 0, expiry: 0 };
  let avatarVisualGatePassed = false;
  const avatarResourceEvidence: Promise<AvatarResourceEvidence>[] = [];
  const credentialInput = punctuationPassword;
  const assertCredentialPayload = await installCredentialPayloadAssertion(
    page,
    punctuationIdentifier,
    credentialInput,
  );
  page.on("pageerror", (error) => {
    if (
      hardReloadPhase === null ||
      !recordExpectedWebKitHardReloadError(
        testInfo.project.name,
        hardReloadPhase,
        error.message,
        normalizedCoroutineErrors,
        responseClassCastErrors,
      )
    ) {
      pageErrors.push({
        phase: hardReloadPhase,
        name: error.name,
        message: error.message,
      });
    }
  });
  const captureAvatarResponse = (response: Response): void => {
    if (!response.url().endsWith(TMDB_AVATAR_RESOURCE_PATH)) {
      return;
    }
    avatarResourceEvidence.push((async () => {
      const body = await response.body();
      const contentLength = Number(response.headers()["content-length"] ?? "0");
      const text = body.toString("utf8").trim();
      return {
        isSameOrigin: new URL(response.url()).origin === new URL(page.url()).origin,
        status: response.status(),
        contentType: response.headers()["content-type"] ?? "",
        bodyLength: body.byteLength,
        contentLength,
        isCompleteVector: text.startsWith("<vector") && text.endsWith("</vector>"),
      };
    })());
  };
  page.on("response", captureAvatarResponse);

  await page.goto("/login");
  await expect(page.locator("body")).toHaveAttribute("data-runtime-state", "ready", { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/login", { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
  for (const label of loginActionLabels) {
    await expect(page.getByRole("button", { name: label, exact: true })).toHaveCount(1);
  }
  await refreshLoginActionPaint(page);
  const loginFrame = await page.screenshot({
    path: `screenshots/${testInfo.project.name}-login.png`,
    animations: "disabled",
  });
  expect(loginFrame.byteLength).toBeGreaterThan(20_000);

  await typeCredentials(page, punctuationIdentifier, credentialInput);

  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/profiles");
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
  assertCredentialPayload();
  expect(page.url()).not.toContain(punctuationIdentifier);
  expect(page.url()).not.toContain(credentialInput);
  const firstProfile = page.getByRole("button", { name: "Select Nikos profile", exact: true });
  const firstProfileBounds = await semanticBounds(firstProfile);
  await expect.poll(
    async () => {
      await page.mouse.move(1, 1);
      await page.mouse.move(
        firstProfileBounds.x + firstProfileBounds.width / 2,
        firstProfileBounds.y + firstProfileBounds.height / 2,
      );
      const avatarFrame = await page.screenshot({
        clip: firstProfileBounds,
        animations: "disabled",
      });
      return avatarFrame.byteLength;
    },
    { timeout: 30_000, intervals: [500] },
  ).toBeGreaterThan(10_000);
  avatarVisualGatePassed = true;
  await page.mouse.move(1, 1);
  await page.waitForTimeout(500);
  const profilesFrame = await page.screenshot({
    path: `screenshots/${testInfo.project.name}-profiles.png`,
    animations: "disabled",
  });
  expect(profilesFrame.byteLength).toBeGreaterThan(30_000);
  page.off("response", captureAvatarResponse);
  expect(avatarResourceEvidence.length).toBeGreaterThan(0);
  await Promise.all(avatarResourceEvidence);

  await page.mouse.move(
    firstProfileBounds.x + firstProfileBounds.width / 2,
    firstProfileBounds.y + firstProfileBounds.height / 2,
  );
  await page.mouse.click(
    firstProfileBounds.x + firstProfileBounds.width / 2,
    firstProfileBounds.y + firstProfileBounds.height / 2,
  );
  await expect(page).toHaveURL(/\/home$/, { timeout: 30_000 });
  hardReloadPhase = "restoration";
  await page.reload();
  await expect(page).toHaveURL(/\/home$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/home");
  hardReloadPhase = null;

  await page.goBack();
  await expect(page).toHaveURL(/\/profiles$/);
  await page.goForward();
  await expect(page).toHaveURL(/\/home$/);

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
  hardReloadPhase = "expiry";
  await page.reload();
  await expect(page).toHaveURL(/\/login$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/login");
  hardReloadPhase = null;
  await page.goBack();
  await expect(page).toHaveURL(/\/login$/);
  expect(page.url()).not.toContain(credentialInput);
  for (const phase of HARD_RELOAD_PHASES) {
    expect(normalizedCoroutineErrors[phase], `${phase} emitted repeated coroutine errors.`)
      .toBeLessThanOrEqual(1);
    expect(responseClassCastErrors[phase], `${phase} emitted repeated Response cast errors.`)
      .toBeLessThanOrEqual(1);
  }
  expect(await strictPageErrors(
    testInfo.project.name,
    pageErrors,
    avatarVisualGatePassed,
    avatarResourceEvidence,
  )).toEqual([]);
});

test("TMDB code 30 always shows deterministic sign-in failure copy", async ({ page }) => {
  const credentialInput = ["fixture", "rejected", "credential"].join("-");
  await page.route("**/authentication/token/validate_with_login", async (route) => {
    await route.fulfill({
      status: 401,
      headers: {
        "access-control-allow-origin": "*",
        "content-type": "application/json",
      },
      json: {
        success: false,
        status_code: 30,
        status_message: "fixture rejection",
      },
    });
  });

  const assertCredentialPayload = await installCredentialPayloadAssertion(
    page,
    "fixture-user",
    credentialInput,
  );
  await page.goto("/login");
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
  await typeCredentials(page, "fixture-user", credentialInput);

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.locator("body")).toHaveAttribute("data-product-error-kind", "authentication");
  await expect(page.locator("body")).toHaveAttribute("data-product-error-title", "Sign-in failed");
  await expect(page.locator("body")).toHaveAttribute(
    "data-product-error-message",
    "Check your TMDB username and password, then try again.",
  );
  assertCredentialPayload();
  expect(page.url()).not.toContain("fixture-user");
  expect(page.url()).not.toContain(credentialInput);
});

test("native credential form re-enables controls after auth failure and retries once", async ({ page }) => {
  const identifierValue = "retry-user";
  const passwordValue = "retry-password";
  let validationAttempts = 0;
  let successfulRetries = 0;
  let sessionCreationRequests = 0;
  await page.route("**/authentication/token/validate_with_login", async (route) => {
    const payload = route.request().postDataJSON() as {
      username?: unknown;
      password?: unknown;
      request_token?: unknown;
    };
    expect(payload.username).toBe(identifierValue);
    expect(payload.password).toBe(passwordValue);
    expect(payload.request_token).toBe("fixture-request");
    validationAttempts += 1;
    if (validationAttempts === 1) {
      await route.fulfill({
        status: 401,
        headers: {
          "access-control-allow-origin": "*",
          "content-type": "application/json",
        },
        json: {
          success: false,
          status_code: 30,
          status_message: "fixture rejection",
        },
      });
      return;
    }
    successfulRetries += 1;
    await route.fallback();
  });
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.endsWith("/authentication/session/new")) {
      sessionCreationRequests += 1;
    }
  });

  await page.goto("/login");
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });

  const form = page.getByTestId("login:credentials-form");
  const identifier = page.getByTestId("login:identifier");
  const password = page.getByTestId("login:password");
  const visibility = page.getByTestId("login:password-visibility");
  const submit = page.getByTestId("login:submit");
  await identifier.fill(identifierValue);
  await password.fill(passwordValue);
  await submit.click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/login");
  await expect(page.locator("body")).toHaveAttribute("data-product-error-kind", "authentication");
  await expect(form).toHaveAttribute("aria-busy", "false");
  await expect(identifier).toBeEnabled();
  await expect(password).toBeEnabled();
  await expect(visibility).toBeEnabled();
  await expect(submit).toBeEnabled();
  expect(validationAttempts).toBe(1);
  expect(successfulRetries).toBe(0);
  expect(sessionCreationRequests).toBe(0);

  await page.keyboard.press("Escape");
  await expect(page.locator("body")).not.toHaveAttribute("data-product-error-kind");
  await submit.click();

  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/profiles");
  expect(validationAttempts).toBe(2);
  expect(successfulRetries).toBe(1);
  expect(sessionCreationRequests).toBe(1);
});

test("native credential form owns initial focus and rejects invalid submissions accessibly", async ({ page }) => {
  let authenticationRequests = 0;
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.includes("/authentication/")) {
      authenticationRequests += 1;
    }
  });

  await page.goto("/login");
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });

  const identifier = page.getByTestId("login:identifier");
  const password = page.getByTestId("login:password");
  const submit = page.getByTestId("login:submit");
  const identifierError = page.locator("#streamcore-login-identifier-error");
  const passwordError = page.locator("#streamcore-login-password-error");

  await expect(identifier).toBeFocused();
  await expect(submit).toBeEnabled();
  await submit.click();
  await expect(identifierError).toBeVisible();
  await expect(identifierError).toHaveAttribute("role", "alert");
  await expect(identifierError).toHaveText("Username or email is required");
  await expect(passwordError).toBeVisible();
  await expect(passwordError).toHaveAttribute("role", "alert");
  await expect(passwordError).toHaveText("Password is required");
  await expect(identifier).toHaveAttribute("aria-invalid", "true");
  await expect(identifier).toHaveAttribute(
    "aria-describedby",
    "streamcore-login-identifier-error",
  );
  await expect(identifier).toHaveAttribute(
    "aria-errormessage",
    "streamcore-login-identifier-error",
  );
  await expect(password).toHaveAttribute("aria-invalid", "true");
  await expect(password).toHaveAttribute(
    "aria-describedby",
    "streamcore-login-password-error",
  );
  await expect(password).toHaveAttribute(
    "aria-errormessage",
    "streamcore-login-password-error",
  );
  expect(authenticationRequests).toBe(0);

  await identifier.fill("partial-user");
  await submit.click();
  await expect(identifierError).toBeHidden();
  await expect(identifier).not.toHaveAttribute("aria-invalid");
  await expect(identifier).not.toHaveAttribute("aria-describedby");
  await expect(identifier).not.toHaveAttribute("aria-errormessage");
  await expect(passwordError).toBeVisible();
  await expect(passwordError).toHaveText("Password is required");
  await expect(password).toHaveAttribute("aria-invalid", "true");
  await expect(password).toHaveAttribute(
    "aria-errormessage",
    "streamcore-login-password-error",
  );
  expect(authenticationRequests).toBe(0);
});

test("native credential form keeps punctuation-heavy fields separated and submits once", async ({ page }) => {
  let releaseValidation: () => void = () => {};
  const validationGate = new Promise<void>((resolve) => {
    releaseValidation = resolve;
  });
  const assertCredentialPayload = await installCredentialPayloadAssertion(
    page,
    punctuationIdentifier,
    punctuationPassword,
    async () => validationGate,
  );
  await page.goto("/login");
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });

  const identifier = page.getByTestId("login:identifier");
  const password = page.getByTestId("login:password");
  const visibility = page.getByTestId("login:password-visibility");
  await expect(identifier).toHaveAttribute("type", "email");
  await expect(identifier).toHaveAttribute("autocomplete", "username");
  await expect(password).toHaveAttribute("type", "password");
  await expect(password).toHaveAttribute("autocomplete", "current-password");

  await identifier.focus();
  await identifier.pressSequentially(punctuationIdentifier, { delay: 10 });
  await page.keyboard.press("Tab");
  await expect(password).toBeFocused();
  await password.pressSequentially(punctuationPassword, { delay: 10 });
  await expect(identifier).toHaveValue(punctuationIdentifier);
  await expect(password).toHaveValue(punctuationPassword);

  await visibility.focus();
  await page.keyboard.press("Space");
  await expect(visibility).toHaveAttribute("aria-pressed", "true");
  await expect(password).toHaveAttribute("type", "text");
  await visibility.focus();
  await page.keyboard.press("Space");
  await expect(visibility).toHaveAttribute("aria-pressed", "false");
  await expect(password).toHaveAttribute("type", "password");

  await password.focus();
  await page.keyboard.press("Enter");
  const form = page.getByTestId("login:credentials-form");
  const submit = page.getByTestId("login:submit");
  await expect(submit).toBeDisabled();
  await expect(form).toHaveAttribute("aria-busy", "true");
  await form.dispatchEvent("submit");
  releaseValidation();
  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  assertCredentialPayload();
});

test("blank account name persists through official session-storage fallback", async ({ page }) => {
  await page.addInitScript(() => {
    const originalSetItem = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key: string, value: string): void {
      if (this === window.localStorage && key.includes("preferences_pb")) {
        throw new DOMException("DataStore quota exceeded", "QuotaExceededError");
      }
      originalSetItem.call(this, key, value);
    };
  });
  await page.route("**/account/42**", async (route) => {
    await route.fulfill({
      headers: { "access-control-allow-origin": "*", "content-type": "application/json" },
      json: { id: 42, username: syntheticAccountUsername, name: "" },
    });
  });

  await loginToProfiles(page, "session-store-user");
  await expect(page.locator("body")).toHaveAttribute("data-storage-mode", "session");
  await page.reload();
  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  await expireSession(page);
  await page.reload();
  await expect(page).toHaveURL(/\/login$/, { timeout: 30_000 });
});

test("account verification 503 keeps login protected and reports a mapped failure", async ({ page }) => {
  let remoteSessionCleanupRequests = 0;
  page.on("request", (request) => {
    const url = new URL(request.url());
    if (
      request.method() === "DELETE" &&
      url.pathname.endsWith("/authentication/session")
    ) {
      remoteSessionCleanupRequests += 1;
    }
  });
  await page.route("**/account/42**", async (route) => {
    await route.fulfill({
      status: 503,
      headers: { "access-control-allow-origin": "*", "content-type": "application/json" },
      json: { status_message: "fixture account unavailable" },
    });
  });

  await page.goto("/login");
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
  await typeCredentials(page, "unverified-account-user", "fixture-password");

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/login");
  await expect(page.locator("body")).toHaveAttribute("data-product-error-kind", "server");
  await expect(page.locator("body")).toHaveAttribute("data-product-error-title", /\S+/);
  await expect(page.locator("body")).toHaveAttribute("data-product-error-message", /\S+/);
  await expect.poll(() => remoteSessionCleanupRequests).toBe(1);
  await page.reload();
  await expect(page).toHaveURL(/\/login$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/login");
});

test("hover produces visible profile-card feedback", async ({ page }) => {
  await loginToProfiles(page, "hover-user");
  await activateSemanticButton(
    page,
    page.getByRole("button", { name: "Manage profiles", exact: true }),
  );
  const profile = page.getByRole("button", { name: "Edit Nikos profile", exact: true });
  const bounds = await semanticBounds(profile);
  const cardClip = { x: bounds.x, y: bounds.y, width: bounds.width, height: bounds.height };
  await page.mouse.move(10, 10);
  await page.waitForTimeout(500);
  const restingFrame = await page.screenshot({ clip: cardClip, animations: "disabled" });
  await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  await page.waitForTimeout(500);
  const hoveredFrame = await page.screenshot({ clip: cardClip, animations: "disabled" });
  expect(hoveredFrame.equals(restingFrame)).toBe(false);
});

test("profile create edit delete, editor arrows, modal trap and focused scrolling", async ({ page }) => {
  test.setTimeout(90_000);
  await loginToProfiles(page, "crud-user");
  await expect(page.locator("body")).toHaveAttribute("data-profile-count", "2");

  await activateSemanticButton(page, page.getByRole("button", { name: "Add profile", exact: true }));
  await expect(page).toHaveURL(/\/profiles\/new$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
  const avatarRowBefore = await page.screenshot({ animations: "disabled" });
  await page.keyboard.press("ArrowRight");
  await page.keyboard.press("Space");
  for (let index = 0; index < 18; index += 1) {
    await page.keyboard.press("ArrowRight");
  }
  const avatarRowAfter = await page.screenshot({ animations: "disabled" });
  expect(avatarRowAfter.equals(avatarRowBefore)).toBe(false);
  const createNameField = page.locator('[data-testid="profile-display-name"]');
  const createSaveButton = page.locator('[data-testid="profile-editor-save"]');
  await createSaveButton.focus();
  await createSaveButton.press("Space");
  await expect(page).toHaveURL(/\/profiles\/new$/);
  const displayNameError = page.locator('[data-testid="profile-display-name-error"]');
  await expect(displayNameError).toBeVisible();
  await expect(displayNameError).toHaveText("Required");
  await expect(createNameField).toHaveAttribute("aria-invalid", "true");
  await expect(createNameField).toHaveAttribute(
    "aria-errormessage",
    "streamcore-profile-display-name-error",
  );
  await createNameField.fill("Browser profile");
  await expect(createNameField).not.toHaveAttribute("aria-invalid");
  await expect(createNameField).not.toHaveAttribute("aria-errormessage");
  await expect(displayNameError).toBeHidden();
  await createSaveButton.focus();
  await createSaveButton.press("Space");
  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-profile-editor-action", "save");
  await expect(page.locator("body")).toHaveAttribute("data-profile-count", "3", { timeout: 30_000 });

  await openCreatedProfileEditor(page, "Browser profile");
  await page.locator('[data-testid="profile-editor-cancel"]').click();
  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-profile-editor-action", "cancel");
  await expect(page.locator("body")).toHaveAttribute("data-profile-count", "3");

  await openCreatedProfileEditor(page, "Browser profile");
  const editNameField = page.locator('[data-testid="profile-display-name"]');
  await editNameField.fill("Browser profile edited");
  await editNameField.press("Enter");
  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-profile-editor-action", "save");
  await expect(page.locator("body")).toHaveAttribute("data-profile-count", "3", { timeout: 30_000 });

  await page.reload();
  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute("data-profile-count", "3");
  await openCreatedProfileEditor(page, "Browser profile edited");
  const deleteButton = page.locator('[data-testid="profile-editor-delete"]');
  await deleteButton.click();
  await expect(page.locator("body")).toHaveAttribute("data-profile-editor-action", "delete");
  const deleteDialog = page.locator('[data-testid="profile-editor-delete-dialog"]');
  const cancelDelete = page.locator('[data-testid="profile-editor-delete-cancel"]');
  await expect(deleteDialog).toBeVisible();
  await expect(cancelDelete).toBeFocused();
  await page.keyboard.press("ArrowRight");
  await page.keyboard.press("ArrowLeft");
  await page.keyboard.press("Escape");
  await expect(deleteDialog).toHaveCount(0);
  await expect(deleteButton).toBeFocused();

  for (let index = 0; index < 20; index += 1) {
    await deleteButton.press("Enter");
    await expect(deleteDialog).toBeVisible();
    await expect(cancelDelete).toBeFocused();
    await page.keyboard.press("Escape");
    await expect(deleteDialog).toHaveCount(0);
    await expect(deleteButton).toBeFocused();
  }

  await deleteButton.press("Enter");
  await expect(cancelDelete).toBeFocused();
  await page.keyboard.press("ArrowRight");
  await page.keyboard.press("Enter");
  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-profile-count", "2", { timeout: 30_000 });

  await page.reload();
  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute("data-profile-count", "2");
  await expect(
    page.getByRole("button", { name: "Select Nikos profile", exact: true }),
  ).toHaveCount(1);
  await expect(
    page.getByRole("button", { name: "Select Browser profile edited profile", exact: true }),
  ).toHaveCount(0);
});

test("browse routes preserve focus, library mutations, reload and logout", async ({ page }, testInfo) => {
  test.setTimeout(120_000);
  const errorMonitor = installBrowsePageErrorMonitor(page, testInfo.project.name);
  let submittedSearches = 0;
  let logoutRequests = 0;
  await page.route("**/search/movie**", async (route) => {
    const query = new URL(route.request().url()).searchParams.get("query");
    if (query === "Orbit") {
      submittedSearches += 1;
    }
    await route.fallback();
  });
  await page.route("**/authentication/session", async (route) => {
    if (route.request().method() === "DELETE") {
      logoutRequests += 1;
    }
    await route.fallback();
  });
  await page.route("**/*", async (route) => {
    const pathname = new URL(route.request().url()).pathname.toLowerCase();
    if (/\.(?:mpd|m3u8|mp4|m4s|webm)$/.test(pathname)) {
      await route.fulfill({
        status: 503,
        headers: {
          "access-control-allow-origin": "*",
          "content-type": "application/octet-stream",
        },
        body: "",
      });
      return;
    }
    await route.fallback();
  });

  await loginToProfiles(page, "browse-user");
  await activateSemanticButton(
    page,
    page.getByRole("button", { name: "Select Nikos profile", exact: true }),
  );
  await expectProductRoute(page, "/home");
  await expect(homeHeroDetails(page)).toHaveCount(1);

  await activateSemanticButton(
    page,
    page.getByRole("button", { name: "Search", exact: true }),
  );
  await expectProductRoute(page, "/search");
  const searchField = page.getByTestId("search:field");
  await searchField.fill("Orbit");
  await searchField.press("Enter");
  const orbitResult = page.getByRole(
    "button",
    { name: "Open details for Orbit Fall", exact: true },
  );
  await expect(orbitResult).toHaveCount(1, { timeout: 30_000 });
  await activateSemanticButton(page, orbitResult);
  await expectProductRoute(page, "/details/603");

  await page.goBack();
  await expectProductRoute(page, "/search");
  await page.keyboard.press("Space");
  await expectProductRoute(page, "/details/603");
  await page.goBack();
  await expectProductRoute(page, "/search");
  await page.goForward();
  await expectProductRoute(page, "/details/603");
  await page.keyboard.press("Escape");
  await expectProductRoute(page, "/search");
  await activateSemanticButton(page, orbitResult);
  await expectProductRoute(page, "/details/603");
  expect(submittedSearches).toBe(1);

  await expect(page.getByRole("button", { name: "Play", exact: true })).toHaveCount(1);
  await activateSemanticButton(
    page,
    page.getByRole("button", { name: "My List", exact: true }),
  );
  await expect(page.getByRole("button", { name: "In My List", exact: true })).toHaveCount(1);

  await activateSemanticButton(
    page,
    page.getByRole("button", { name: "Library", exact: true }),
  );
  await expectProductRoute(page, "/library");
  await page.keyboard.press("Space");
  await expectProductRoute(page, "/details/603");

  await activateSemanticButton(
    page,
    page.getByRole("button", { name: "Play", exact: true }),
  );
  await expectProductRoute(page, "/player/603");
  await activateSemanticButton(
    page,
    page.getByRole("button", { name: "Back", exact: true }),
  );
  await expectProductRoute(page, "/details/603");
  await settleCanvasPaint(page, 1);
  await page.keyboard.press("Space");
  await expectProductRoute(page, "/player/603");
  await expect(page.getByRole("button", { name: "Back", exact: true })).toHaveCount(1);
  await page.keyboard.press("Escape");
  await expectProductRoute(page, "/details/603");
  await settleCanvasPaint(page, 1);
  await page.keyboard.press("Space");
  await expectProductRoute(page, "/player/603");
  await page.keyboard.press("Escape");
  await expectProductRoute(page, "/details/603");

  errorMonitor.setReloadPhase("restoration");
  await page.reload();
  await expectProductRoute(page, "/details/603");
  errorMonitor.setReloadPhase(null);
  await expect(page.getByRole("button", { name: "Play", exact: true })).toHaveCount(1);
  await activateSemanticButton(
    page,
    page.getByRole("button", { name: "Sign out", exact: true }),
  );
  await expectProductRoute(page, "/login");
  expect(logoutRequests).toBe(1);
  await page.goBack();
  await expectProductRoute(page, "/login");
  errorMonitor.assertNoUnexpectedErrors();
});

test("browse visual states are deterministic and capturable", async ({ page }, testInfo) => {
  test.setTimeout(150_000);
  const errorMonitor = installBrowsePageErrorMonitor(page, testInfo.project.name);
  const slowSearch = { started: false, release: undefined as (() => void) | undefined };
  let failCachedQuery = false;
  const longTitle = "Η τελευταία αποστολή πέρα από τον ορατό ορίζοντα";
  const longMovie = {
    ...browseMovie,
    id: 605,
    title: longTitle,
    overview:
      "Ένα πλήρωμα διασχίζει ένα ασταθές πεδίο συντριμμιών και ανακαλύπτει ότι η επιστροφή απαιτεί περισσότερα από θάρρος.",
  };
  const cachedMovie = { ...browseMovie, title: "Cached Orbit" };
  const bridgeMovie = { ...browseMovie, title: "Bridge Orbit" };
  await page.route("**/search/movie**", async (route) => {
    const query = new URL(route.request().url()).searchParams.get("query");
    const headers = {
      "access-control-allow-origin": "*",
      "content-type": "application/json",
    };
    if (query === "Slow") {
      slowSearch.started = true;
      await new Promise<void>((resolve) => {
        slowSearch.release = resolve;
      });
      await route.fulfill({ headers, json: movieList() });
      return;
    }
    if (query === "Empty") {
      await route.fulfill({ headers, json: movieList([]) });
      return;
    }
    if (query === "Cached" && failCachedQuery) {
      await route.fulfill({
        status: 408,
        headers,
        json: { status_message: "Deterministic fixture timeout" },
      });
      return;
    }
    if (query === "Error") {
      await route.fulfill({
        status: 503,
        headers,
        json: { status_message: "Deterministic fixture failure" },
      });
      return;
    }
    if (query === "Long") {
      await route.fulfill({ headers, json: movieList([longMovie]) });
      return;
    }
    if (query === "Cached") {
      await route.fulfill({ headers, json: movieList([cachedMovie]) });
      return;
    }
    if (query === "Bridge") {
      await route.fulfill({ headers, json: movieList([bridgeMovie]) });
      return;
    }
    await route.fulfill({ headers, json: movieList() });
  });
  await page.route("**/movie/605/recommendations**", async (route) => {
    await route.fulfill({
      headers: {
        "access-control-allow-origin": "*",
        "content-type": "application/json",
      },
      json: movieList([]),
    });
  });
  await page.route(/\/movie\/605(?:\?.*)?$/, async (route) => {
    await route.fulfill({
      headers: {
        "access-control-allow-origin": "*",
        "content-type": "application/json",
      },
      json: {
        ...longMovie,
        genres: [{ id: 878, name: "Επιστημονική φαντασία" }],
      },
    });
  });

  await loginToProfiles(page, "visual-user");
  await activateSemanticButton(
    page,
    page.getByRole("button", { name: "Select Nikos profile", exact: true }),
  );
  await expectProductRoute(page, "/home");
  await expect(homeHeroDetails(page)).toHaveCount(1, { timeout: 30_000 });
  await captureBrowseState(page, testInfo.project.name, "content");

  await activateSemanticButton(
    page,
    page.getByRole("button", { name: "Search", exact: true }),
  );
  await expectProductRoute(page, "/search");
  const searchField = page.getByTestId("search:field");
  await searchField.fill("Slow");
  await searchField.press("Enter");
  await expect.poll(() => slowSearch.started, { timeout: 30_000 }).toBe(true);
  await page.waitForTimeout(300);
  await captureBrowseState(page, testInfo.project.name, "loading");
  if (slowSearch.release === undefined) {
    throw new Error("Slow search release was not registered");
  }
  slowSearch.release();
  await expect(
    page.getByRole("button", { name: "Open details for Orbit Fall", exact: true }),
  ).toHaveCount(1, { timeout: 30_000 });

  await searchField.fill("Empty");
  await searchField.press("Enter");
  await expect(page.getByRole("button", { name: "Clear search", exact: true })).toHaveCount(1);
  await captureBrowseState(page, testInfo.project.name, "empty");

  await searchField.fill("Error");
  await searchField.press("Enter");
  await expect(page.getByRole("button", { name: "Try again", exact: true })).toHaveCount(1);
  await captureBrowseState(page, testInfo.project.name, "error");

  await searchField.fill("Cached");
  await searchField.press("Enter");
  await expect(
    page.getByRole("button", { name: "Open details for Cached Orbit", exact: true }),
  ).toHaveCount(1, { timeout: 30_000 });
  await searchField.fill("Bridge");
  await searchField.press("Enter");
  await expect(
    page.getByRole("button", { name: "Open details for Bridge Orbit", exact: true }),
  ).toHaveCount(1, { timeout: 30_000 });
  failCachedQuery = true;
  const offlineResponse = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return url.pathname.endsWith("/search/movie") &&
      url.searchParams.get("query") === "Cached" &&
      response.status() === 408;
  });
  await searchField.fill("Cached");
  await searchField.press("Enter");
  await offlineResponse;
  await expect(
    page.getByRole("button", { name: "Open details for Cached Orbit", exact: true }),
  ).toHaveCount(1, { timeout: 30_000 });
  await expect(
    page.getByText("You’re offline. Showing saved results.", { exact: true }),
  ).toHaveCount(1, { timeout: 30_000 });
  await captureBrowseState(page, testInfo.project.name, "offline");

  await searchField.fill("Long");
  await searchField.press("Enter");
  const longResult = page.getByRole(
    "button",
    { name: `Open details for ${longTitle}`, exact: true },
  );
  await expect(longResult).toHaveCount(1, { timeout: 30_000 });
  await activateSemanticButton(page, longResult);
  await expectProductRoute(page, "/details/605");
  await expect(page.getByRole("button", { name: "Play", exact: true })).toHaveCount(1);
  await captureBrowseState(page, testInfo.project.name, "long-text");
  errorMonitor.assertNoUnexpectedErrors();
});

async function loginToProfiles(page: Page, identifier: string): Promise<void> {
  const credentialInput = [identifier, "credential"].join("-");
  const assertCredentialPayload = await installCredentialPayloadAssertion(
    page,
    identifier,
    credentialInput,
  );
  await page.goto("/login");
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
  await typeCredentials(page, identifier, credentialInput);
  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
  assertCredentialPayload();
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
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

function homeHeroDetails(page: Page): Locator {
  return page
    .getByRole("button", { name: "Open details for Orbit Fall", exact: true })
    .filter({ hasText: "More details" });
}

async function captureBrowseState(
  page: Page,
  projectName: string,
  state: "loading" | "content" | "empty" | "offline" | "error" | "long-text",
): Promise<void> {
  const actionName = state === "empty" ? "Clear search" : state === "error" ? "Try again" : null;
  if (actionName !== null) {
    const actionBounds = await semanticBounds(
      page.getByRole("button", { name: actionName, exact: true }),
    );
    await page.mouse.move(
      actionBounds.x + actionBounds.width / 2,
      actionBounds.y + actionBounds.height / 2,
    );
  }
  await settleCanvasPaint(page, 1);
  const frame = await page.screenshot({
    path: `screenshots/${projectName}-browse-${state}.png`,
    animations: "disabled",
  });
  expect(frame.byteLength).toBeGreaterThan(20_000);
}

type BrowsePageErrorMonitor = {
  setReloadPhase: (phase: HardReloadPhase | null) => void;
  assertNoUnexpectedErrors: () => void;
};

function installBrowsePageErrorMonitor(page: Page, projectName: string): BrowsePageErrorMonitor {
  const unexpectedErrors: string[] = [];
  const normalizedCoroutineErrors: HardReloadErrorCounts = { restoration: 0, expiry: 0 };
  const responseClassCastErrors: HardReloadErrorCounts = { restoration: 0, expiry: 0 };
  let reloadPhase: HardReloadPhase | null = null;
  page.on("pageerror", (error) => {
    if (projectName.startsWith("webkit-") && error.message === WEBKIT_AVATAR_ACCESS_ERROR) {
      return;
    }
    if (
      reloadPhase !== null &&
      recordExpectedWebKitHardReloadError(
        projectName,
        reloadPhase,
        error.message,
        normalizedCoroutineErrors,
        responseClassCastErrors,
      )
    ) {
      return;
    }
    unexpectedErrors.push(error.message);
  });
  return {
    setReloadPhase: (phase) => {
      reloadPhase = phase;
    },
    assertNoUnexpectedErrors: () => {
      for (const phase of HARD_RELOAD_PHASES) {
        expect(normalizedCoroutineErrors[phase]).toBeLessThanOrEqual(1);
        expect(responseClassCastErrors[phase]).toBeLessThanOrEqual(1);
      }
      expect(unexpectedErrors).toEqual([]);
    },
  };
}

async function typeCredentials(page: Page, identifier: string, password: string): Promise<void> {
  const identifierInput = page.getByTestId("login:identifier");
  const passwordInput = page.getByTestId("login:password");
  await identifierInput.focus();
  await identifierInput.pressSequentially(identifier, { delay: 35 });
  await page.keyboard.press("Tab");
  await expect(passwordInput).toBeFocused();
  await passwordInput.pressSequentially(password, { delay: 35 });
  await page.keyboard.press("Enter");
}

async function installCredentialPayloadAssertion(
  page: Page,
  identifier: string,
  password: string,
  beforeFallback: () => Promise<void> = async () => {},
): Promise<() => void> {
  let matchingRequests = 0;
  await page.route("**/authentication/token/validate_with_login", async (route) => {
    const payload = route.request().postDataJSON() as {
      username?: unknown;
      password?: unknown;
      request_token?: unknown;
    };
    expect(payload.username).toBe(identifier);
    expect(payload.password).toBe(password);
    expect(payload.request_token).toBe("fixture-request");
    matchingRequests += 1;
    await beforeFallback();
    await route.fallback();
  });
  return () => expect(matchingRequests).toBe(1);
}

async function expireSession(page: Page): Promise<void> {
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
}

async function openCreatedProfileEditor(page: Page, displayName: string): Promise<void> {
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
  await expect(page.locator("body")).toHaveAttribute("data-profile-count", "3");
  const manageProfiles = page.getByRole("button", { name: "Manage profiles", exact: true });
  if (await manageProfiles.count() > 0) {
    await activateSemanticButton(page, manageProfiles);
  }
  await activateSemanticButton(
    page,
    page.getByRole("button", { name: `Edit ${displayName} profile`, exact: true }),
  );
  await expect(page).toHaveURL(/\/profiles\/tmdb-profile-created-1\/edit$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-visual-state", "ready", {
    timeout: 30_000,
  });
}

async function activateSemanticButton(page: Page, button: Locator): Promise<void> {
  await settleCanvasPaint(page, 1);
  const bounds = await semanticBounds(button);
  await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  await waitForAnimationFrames(page, 2);
  await page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  await waitForAnimationFrames(page, 4);
}

async function semanticBounds(
  button: Locator,
): Promise<{ x: number; y: number; width: number; height: number }> {
  let resolvedBounds: { x: number; y: number; width: number; height: number } | null = null;
  await expect.poll(
    async () => {
      resolvedBounds = await button.boundingBox({ timeout: 1_000 }).catch(() => null);
      return resolvedBounds !== null;
    },
    { timeout: 30_000, intervals: [250] },
  ).toBe(true);
  if (resolvedBounds === null) {
    throw new Error("Semantic button does not expose viewport bounds");
  }
  return resolvedBounds;
}

async function settleCanvasPaint(page: Page, warmupCount: number): Promise<void> {
  for (let warmupIndex = 0; warmupIndex < warmupCount; warmupIndex += 1) {
    await waitForAnimationFrames(page, 4);
    await page.screenshot({ animations: "disabled" });
  }
  await waitForAnimationFrames(page, 4);
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

async function refreshLoginActionPaint(page: Page): Promise<void> {
  for (const label of loginActionLabels) {
    const bounds = await semanticBounds(page.getByRole("button", { name: label, exact: true }));
    await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    await page.waitForTimeout(50);
  }
  await page.mouse.move(1, 1);
  await page.waitForTimeout(500);
}

type HardReloadPhase = "restoration" | "expiry";
type HardReloadErrorCounts = Record<HardReloadPhase, number>;

function recordExpectedWebKitHardReloadError(
  projectName: string,
  phase: HardReloadPhase,
  message: string,
  normalizedCoroutineErrors: HardReloadErrorCounts,
  responseClassCastErrors: HardReloadErrorCounts,
): boolean {
  if (!projectName.startsWith("webkit-")) {
    return false;
  }
  if (WEBKIT_HARD_RELOAD_COROUTINE_ERROR.test(message)) {
    normalizedCoroutineErrors[phase] += 1;
    return normalizedCoroutineErrors[phase] <= 1;
  }
  if (message === WEBKIT_HARD_RELOAD_RESPONSE_CLASS_CAST_ERROR) {
    responseClassCastErrors[phase] += 1;
    return responseClassCastErrors[phase] <= 1;
  }
  return false;
}

type AvatarResourceEvidence = {
  isSameOrigin: boolean;
  status: number;
  contentType: string;
  bodyLength: number;
  contentLength: number;
  isCompleteVector: boolean;
};

type PageErrorEvidence = {
  phase: HardReloadPhase | null;
  name: string;
  message: string;
};

async function strictPageErrors(
  projectName: string,
  errors: PageErrorEvidence[],
  avatarVisualGatePassed: boolean,
  avatarResourceEvidence: Promise<AvatarResourceEvidence>[],
): Promise<PageErrorEvidence[]> {
  if (!projectName.startsWith("webkit-")) {
    return errors;
  }
  const evidence = await Promise.all(avatarResourceEvidence);
  const avatarResponseIsComplete = evidence.some((item) => {
    return item.isSameOrigin &&
      item.status === 200 &&
      item.contentType.startsWith("application/xml") &&
      item.bodyLength > 0 &&
      item.bodyLength === item.contentLength &&
      item.isCompleteVector;
  });
  if (!avatarVisualGatePassed || !avatarResponseIsComplete) {
    return errors;
  }
  const filteredErrors = errors.filter((error) => error.message !== WEBKIT_AVATAR_ACCESS_ERROR);
  const retainedErrors: PageErrorEvidence[] = [];
  let decoderTeardownPairCount = 0;
  const composeResourceAbortCounts: HardReloadErrorCounts = { restoration: 0, expiry: 0 };
  for (let index = 0; index < filteredErrors.length; index += 1) {
    const current = filteredErrors[index];
    const next = filteredErrors[index + 1];
    if (
      current.phase !== null &&
      current.name === "Fetch API cannot load http" &&
      WEBKIT_COMPOSE_RESOURCE_ACCESS_ERROR.test(current.message)
    ) {
      const phase = current.phase;
      if (composeResourceAbortCounts[phase] === 0) {
        composeResourceAbortCounts[phase] += 1;
        continue;
      }
    }
    const isDecoderTeardownPair = current.phase === "restoration" &&
      current.name === "Cannot load blob" &&
      WEBKIT_AVATAR_BLOB_ACCESS_ERROR.test(current.message) &&
      next?.phase === "restoration" &&
      next.name === "JsException" &&
      next.message === WEBKIT_AVATAR_IO_READ_ERROR;
    if (isDecoderTeardownPair && decoderTeardownPairCount === 0) {
      decoderTeardownPairCount += 1;
      index += 1;
      continue;
    }
    retainedErrors.push(current);
  }
  return retainedErrors;
}

const TMDB_AVATAR_RESOURCE_PATH =
  "/composeResources/streamcoretv.client.tmdb.ui.generated.resources/drawable/tmdb_profile_avatar_01.xml";
const WEBKIT_AVATAR_ACCESS_ERROR =
  `${TMDB_AVATAR_RESOURCE_PATH.replace("/composeResources", "/127.0.0.1:4173/composeResources")} due to access control checks.`;
const WEBKIT_AVATAR_BLOB_ACCESS_ERROR =
  /^ttp:\/\/127\.0\.0\.1:4173\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12} due to access control checks\.$/;
const WEBKIT_AVATAR_IO_READ_ERROR = "The I/O read operation failed.";
const WEBKIT_COMPOSE_RESOURCE_ACCESS_ERROR =
  /^\/127\.0\.0\.1:4173\/composeResources\/[A-Za-z0-9._\/-]+ due to access control checks\.$/;
const HARD_RELOAD_PHASES: readonly HardReloadPhase[] = ["restoration", "expiry"];
const WEBKIT_HARD_RELOAD_COROUTINE_ERROR =
  /^Fatal exception in coroutines machinery for AwaitContinuation\(DispatchedContinuation\[FlushCoroutineDispatcher@\d+, kotlinx\.coroutines\.DeferredCoroutine\.\$awaitCOROUTINE\$@\d+\]\)\{Completed\}@\d+\. Please read KDoc to 'handleFatalException' method and report this incident to maintainers$/;
const WEBKIT_HARD_RELOAD_RESPONSE_CLASS_CAST_ERROR =
  "ClassCastException: Cannot cast instance of Response to Response: incompatible types";
