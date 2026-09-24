import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { artifactFingerprint, configurationState, digest, manifestPath, provenanceSchemaVersion, reviewStatus, sourceFingerprint } from './review-provenance.mjs';

function fixture(t, target = 'android') {
  const root = mkdtempSync(join(tmpdir(), 'streamcore-provenance-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  execFileSync('git', ['init', '-q', root], { windowsHide: true });
  mkdirSync(join(root, 'client/provider/src/commonMain'), { recursive: true });
  writeFileSync(join(root, 'client/provider/src/commonMain/Repository.kt'), 'original');
  writeFileSync(join(root, '.gitignore'), 'build/\nlocal.properties\n');
  writeFileSync(join(root, 'local.properties'), 'fixture-only');
  mkdirSync(join(root, 'build/review/provenance'), { recursive: true });
  const artifact = target === 'android' ? 'build/app.apk' : 'build/dist';
  if (target === 'web') { mkdirSync(join(root, artifact)); writeFileSync(join(root, artifact, 'index.html'), '<html>fixture</html>'); }
  else writeFileSync(join(root, artifact), 'apk-fixture');
  const manifest = { schemaVersion: provenanceSchemaVersion, checkout: root, target, artifact,
    sourceFingerprint: sourceFingerprint(target, root), artifactFingerprint: artifactFingerprint(join(root, artifact)),
    localPropertiesPath: join(root, 'local.properties'), configurationFingerprint: configurationState(root).fingerprint };
  if (target === 'web') {
    manifest.runtimeConfigPath = join(root, 'build/config.json');
    writeFileSync(manifest.runtimeConfigPath, '{"fixture":true}');
    manifest.runtimeFingerprint = digest(readFileSync(manifest.runtimeConfigPath));
  }
  writeFileSync(manifestPath(target, root), JSON.stringify(manifest));
  return { root, artifact: join(root, artifact), manifest, status: () => reviewStatus(target, { root, artifact }) };
}

test('existing artifact without a successful recorded build stays unknown', (t) => {
  const f = fixture(t);
  rmSync(manifestPath('android', f.root));
  assert.equal(f.status().reason, 'BUILD_NOT_RECORDED');
});
test('new/dirty provider sources invalidate provenance; unrelated docs do not', (t) => {
  const f = fixture(t);
  assert.equal(f.status().status, 'fresh');
  writeFileSync(join(f.root, 'README.md'), 'new docs');
  assert.equal(f.status().status, 'fresh');
  writeFileSync(join(f.root, 'client/provider/src/commonMain/Repository.kt'), 'changed');
  assert.equal(f.status().reason, 'SOURCE_CHANGED');
});
test('artifact replacement and local configuration edits invalidate provenance', (t) => {
  const f = fixture(t);
  writeFileSync(f.artifact, 'different-apk');
  assert.equal(f.status().reason, 'ARTIFACT_CHANGED');
  writeFileSync(f.artifact, 'apk-fixture');
  writeFileSync(join(f.root, 'local.properties'), 'different-fixture');
  assert.equal(f.status().reason, 'LOCAL_CONFIGURATION_CHANGED');
});
test('web checks runtime selection and changes within the distribution', (t) => {
  const f = fixture(t, 'web');
  assert.equal(f.status().status, 'fresh');
  const other = join(f.root, 'build/other-config.json');
  writeFileSync(other, '{"fixture":false}');
  assert.equal(reviewStatus('web', { root: f.root, artifact: f.artifact, runtimeConfig: other }).reason, 'RUNTIME_CONFIGURATION_CHANGED');
  writeFileSync(join(f.artifact, 'bundle.wasm'), 'changed-bundle');
  assert.equal(f.status().reason, 'ARTIFACT_CHANGED');
});
test('copied provenance from another checkout cannot certify local artifacts', (t) => {
  const f = fixture(t);
  f.manifest.checkout = `${f.root}-another-pc`;
  writeFileSync(manifestPath('android', f.root), JSON.stringify(f.manifest));
  assert.equal(f.status().reason, 'MANIFEST_MISMATCH');
});

test('previous fingerprint schemas require a new recorded build', (t) => {
  const f = fixture(t);
  f.manifest.schemaVersion = 1;
  writeFileSync(manifestPath('android', f.root), JSON.stringify(f.manifest));
  assert.equal(f.status().status, 'unknown');
  assert.equal(f.status().reason, 'MANIFEST_MISMATCH');
});

function writeSource(root, path, value = 'new input') {
  const destination = join(root, path);
  mkdirSync(join(destination, '..'), { recursive: true });
  writeFileSync(destination, value);
}

test('webpack fragments invalidate production web without invalidating Android', (t) => {
  for (const target of ['android', 'web']) {
    const f = fixture(t, target);
    writeSource(f.root, 'webApp/webpack.config.d/review.js', 'config.output.publicPath = "/";');
    f.manifest.sourceFingerprint = sourceFingerprint(target, f.root);
    writeFileSync(manifestPath(target, f.root), JSON.stringify(f.manifest));
    assert.equal(f.status().status, 'fresh');
    writeSource(f.root, 'webApp/webpack.config.d/review.js', 'config.output.publicPath = "/updated/";');
    assert.equal(f.status().status, target === 'web' ? 'stale' : 'fresh');
  }
});

test('test and benchmark sources do not invalidate production review artifacts', (t) => {
  const excluded = [
    'app/src/androidTest/FocusTest.kt', 'app/src/test/LoginTest.kt',
    'feature/profiles/ui-common/src/commonTest/ProfileTest.kt',
    'core/ui/src/androidHostTest/StateTest.kt', 'core/ui/src/androidDeviceTest/FocusTest.kt',
    'webApp/src/wasmJsTest/AppTest.kt', 'playback/web/src/wasmJsTest/PlaybackTest.kt',
    'app/src/benchmark/AndroidManifest.xml', 'benchmark/src/main/NavigationBenchmark.kt',
    'baselineprofile/src/main/BaselineProfile.kt', 'benchmark/ui-driver/src/main/UiDriver.kt',
    'build-logic/src/test/ConventionTest.kt',
  ];
  for (const target of ['android', 'web']) {
    const f = fixture(t, target);
    for (const path of excluded) writeSource(f.root, path);
    assert.equal(f.status().status, 'fresh', target);
  }
});

test('platform source sets and UI modules only invalidate their owning target', (t) => {
  const platformPaths = {
    android: ['app/src/main/AndroidManifest.xml', 'app/src/debug/Debug.kt',
      'core/ui/src/androidMain/Platform.kt', 'feature/profiles/ui-mobile/src/main/Screen.kt',
      'feature/profiles/ui-tablet/src/main/Screen.kt', 'feature/profiles/ui-tv/src/main/Screen.kt',
      'playback/media3/src/main/Player.kt'],
    web: ['webApp/src/wasmJsMain/App.kt', 'core/ui/src/wasmJsMain/Platform.kt',
      'core/ui-web/src/commonMain/Overlay.kt', 'feature/profiles/ui-web/src/commonMain/Screen.kt',
      'playback/web/src/wasmJsMain/Player.kt'],
  };
  for (const target of ['android', 'web']) {
    const f = fixture(t, target);
    for (const [owner, paths] of Object.entries(platformPaths)) {
      for (const path of paths) {
        writeSource(f.root, path);
        assert.equal(f.status().status, owner === target ? 'stale' : 'fresh', `${target}: ${path}`);
        rmSync(join(f.root, path));
      }
    }
  }
});

test('shared source, resources and build configuration invalidate both targets', (t) => {
  const shared = ['core/domain/src/commonMain/Model.kt',
    'core/ui/src/commonMain/composeResources/drawable/icon.xml',
    'feature/profiles/ui-common/src/commonMain/Screen.kt',
    'build-logic/src/main/kotlin/Convention.kt', 'gradle/libs.versions.toml',
    'feature/profiles/ui-common/build.gradle.kts', 'settings.gradle.kts'];
  for (const target of ['android', 'web']) {
    const f = fixture(t, target);
    for (const path of shared) {
      writeSource(f.root, path);
      assert.equal(f.status().reason, 'SOURCE_CHANGED', `${target}: ${path}`);
      rmSync(join(f.root, path));
    }
  }
});

test('deleting a tracked shared input invalidates both targets', (t) => {
  for (const target of ['android', 'web']) {
    const f = fixture(t, target);
    execFileSync('git', ['add', 'client/provider/src/commonMain/Repository.kt'], { cwd: f.root, windowsHide: true });
    assert.equal(f.status().status, 'fresh');
    rmSync(join(f.root, 'client/provider/src/commonMain/Repository.kt'));
    assert.equal(f.status().reason, 'SOURCE_CHANGED');
  }
});

test('configuration includes both root and linked files, property overrides, and selection', (t) => {
  const f = fixture(t);
  const linked = join(f.root, 'build/linked.properties');
  writeFileSync(linked, 'linked-fixture');
  const env = { ...process.env, STREAMCORE_LOCAL_PROPERTIES: linked };
  const initial = configurationState(f.root, undefined, env);
  writeFileSync(join(f.root, 'local.properties'), 'root-override');
  assert.notEqual(configurationState(f.root, undefined, env).fingerprint, initial.fingerprint);
  assert.notEqual(configurationState(f.root, undefined, { ...env, ORG_GRADLE_PROJECT_tmdbAccountId: 'fixture' }).fingerprint,
    configurationState(f.root, undefined, env).fingerprint);
  assert.notEqual(configurationState(f.root, join(f.root, 'local.properties'), env).selected, initial.selected);
  assert.equal(reviewStatus('android', { root: f.root, artifact: f.artifact, localProperties: linked }).reason, 'LOCAL_CONFIGURATION_CHANGED');
});
