// KGP appends this fragment to the generated Karma config. Serve only this consumer's
// processed resources, extracted by Compose from resolved Maven dependencies.
const streamcoreUiPath = require("path");
const streamcoreUiFs = require("fs");
const streamcoreUiResourceInput = process.env.STREAMCORE_PUBLISHED_UI_RESOURCES;
if (!streamcoreUiResourceInput) {
  throw new Error("Published UI resource directory was not supplied by the consumer test task.");
}
const streamcoreUiResourceRoot = streamcoreUiFs.realpathSync(streamcoreUiResourceInput).replace(/\\/g, "/");
if (streamcoreUiPath.basename(streamcoreUiResourceRoot) !== "composeResources") {
  throw new Error("Published UI resource directory must be the processed composeResources output.");
}
config.files.push({
  pattern: streamcoreUiResourceRoot + "/**/*",
  included: false,
  served: true,
  watched: false,
});
config.proxies = config.proxies || {};
// Karma's source-files middleware maps /absolute<path> to the exact listed file path,
// including drive letters on Windows; no checkout source resource directory is served.
config.proxies["/composeResources/"] = "/absolute" + streamcoreUiResourceRoot + "/";
