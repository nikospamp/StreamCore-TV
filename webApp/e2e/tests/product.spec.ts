import { expect, test, type Locator, type Page } from "@playwright/test";

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
    if (url.pathname.includes("/movie/550/account_states")) {
      await route.fulfill({ headers, json: { id: 550, favorite: false, rated: false, watchlist: false } });
      return;
    }
    await route.fulfill({ headers, json: {} });
  });
});

test("login, profile selection, persistence, history, keyboard and pointer contract", async ({ page }, testInfo) => {
  test.setTimeout(60_000);
  const pageErrors: string[] = [];
  let isHardReloadTransition = false;
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
      !isHardReloadTransition ||
      !isExpectedWebKitHardReloadError(testInfo.project.name, error.message)
    ) {
      pageErrors.push(error.message);
    }
  });
  page.on("response", (response) => {
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
  });

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

  const profileX = firstProfileX(page);
  await page.mouse.move(profileX, 220);
  await page.mouse.click(profileX, 220);
  await expect(page).toHaveURL(/\/authenticated$/, { timeout: 30_000 });
  isHardReloadTransition = true;
  await page.reload();
  await expect(page).toHaveURL(/\/authenticated$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/authenticated");
  isHardReloadTransition = false;

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
  isHardReloadTransition = true;
  await page.reload();
  await expect(page).toHaveURL(/\/login$/, { timeout: 30_000 });
  await expect(page.locator("body")).toHaveAttribute("data-product-route", "/login");
  isHardReloadTransition = false;
  await page.goBack();
  await expect(page).toHaveURL(/\/login$/);
  expect(page.url()).not.toContain(credentialInput);
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

test("native credential form keeps punctuation-heavy fields separated", async ({ page }) => {
  const assertCredentialPayload = await installCredentialPayloadAssertion(
    page,
    punctuationIdentifier,
    punctuationPassword,
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
  await page.route("**/account/42", async (route) => {
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

test("missing optional account still persists session in WebLocalStorage", async ({ page }) => {
  await page.route("**/account/42", async (route) => {
    await route.fulfill({
      status: 503,
      headers: { "access-control-allow-origin": "*", "content-type": "application/json" },
      json: { status_message: "fixture account unavailable" },
    });
  });

  await loginToProfiles(page, "missing-account-user");
  await expect(page.locator("body")).toHaveAttribute("data-storage-mode", "persistent");
  await page.reload();
  await expect(page).toHaveURL(/\/profiles$/, { timeout: 30_000 });
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
  const bounds = await semanticBounds(button);
  await page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
}

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

async function refreshLoginActionPaint(page: Page): Promise<void> {
  for (const label of loginActionLabels) {
    const bounds = await semanticBounds(page.getByRole("button", { name: label, exact: true }));
    await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    await page.waitForTimeout(50);
  }
  await page.mouse.move(1, 1);
  await page.waitForTimeout(500);
}

function firstProfileX(page: Page): number {
  return (page.viewportSize()?.width ?? 1280) >= 1600 ? 430 : 150;
}

function isExpectedWebKitHardReloadError(projectName: string, message: string): boolean {
  if (!projectName.startsWith("webkit-")) {
    return false;
  }
  return message.startsWith(
    "Fatal exception in coroutines machinery for AwaitContinuation(DispatchedContinuation[FlushCoroutineDispatcher@",
  ) || message === "ClassCastException: Cannot cast instance of Response to Response: incompatible types";
}

type AvatarResourceEvidence = {
  isSameOrigin: boolean;
  status: number;
  contentType: string;
  bodyLength: number;
  contentLength: number;
  isCompleteVector: boolean;
};

async function strictPageErrors(
  projectName: string,
  errors: string[],
  avatarVisualGatePassed: boolean,
  avatarResourceEvidence: Promise<AvatarResourceEvidence>[],
): Promise<string[]> {
  if (!projectName.startsWith("webkit-") || !errors.includes(WEBKIT_AVATAR_ACCESS_ERROR)) {
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
  return errors.filter((message) => message !== WEBKIT_AVATAR_ACCESS_ERROR);
}

const TMDB_AVATAR_RESOURCE_PATH =
  "/composeResources/streamcoretv.client.tmdb.ui.generated.resources/drawable/tmdb_profile_avatar_01.xml";
const WEBKIT_AVATAR_ACCESS_ERROR =
  `${TMDB_AVATAR_RESOURCE_PATH.replace("/composeResources", "/127.0.0.1:4173/composeResources")} due to access control checks.`;
