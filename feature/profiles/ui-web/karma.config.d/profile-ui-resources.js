// The real ComposeViewport tests fetch resources through the same URL as the browser app.
// Serve only processed dependency resources belonging to this feature test build.
const profileUiFs = require("fs");
const profileUiPath = require("path");
const profileUiInput = process.env.STREAMCORE_PROFILE_UI_RESOURCES;
if (!profileUiInput) {
  throw new Error("Profile UI test task did not supply its processed Compose resource directory.");
}
const profileUiRoot = profileUiFs.realpathSync(profileUiInput).replace(/\\/g, "/");
if (profileUiPath.basename(profileUiRoot) !== "composeResources") {
  throw new Error("Profile UI resources must come from the processed composeResources output.");
}
config.files.push({
  pattern: profileUiRoot + "/**/*",
  included: false,
  served: true,
  watched: false,
});
config.proxies = config.proxies || {};
config.proxies["/composeResources/"] = "/absolute" + profileUiRoot + "/";
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 10000 });
