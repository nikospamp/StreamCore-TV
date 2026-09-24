import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { existsSync, lstatSync, readdirSync, readFileSync, realpathSync } from 'node:fs';
import { join, relative, resolve, sep } from 'node:path';
import { homedir } from 'node:os';
import { fileURLToPath } from 'node:url';

export const checkout = realpathSync(fileURLToPath(new URL('../..', import.meta.url)));
// Changing fingerprint coverage requires a new schema; old records cannot certify current inputs.
export const provenanceSchemaVersion = 2;
export const defaultArtifacts = {
  android: 'app/build/outputs/apk/tmdb/debug/app-tmdb-debug.apk',
  web: 'webApp/build/dist/wasmJs/productionExecutable',
};

export function digest(value) {
  return createHash('sha256').update(value).digest('hex');
}

function isProductionInput(name, target) {
  const source = name.match(/^(.*)\/src\/([^/]+)\//);
  if (source) {
    const [, module, sourceSet] = source;
    // These source sets/modules are not packaged by either supported review build.
    if (/(?:^test|test(?:fixtures)?$)/i.test(sourceSet)
      || /^(benchmark|baselineprofile|kmp-convention-fixtures)(\/|$)/.test(module)
      || /^(benchmark|benchmarkR8|profile)$/.test(sourceSet)) return false;
    // Convention-plugin source is shared build logic, not app platform source.
    if (module === 'build-logic') return true;
    if (target === 'web') {
      if (/^(app|playback\/media3|core\/tracing)(\/|$)/.test(module)
        || /(^|\/)ui-(mobile|tablet|tv)(\/|$)/.test(module)
        || /^android/.test(sourceSet)) return false;
    } else if (/^(webApp|playback\/web)(\/|$)/.test(module)
      || /(^|\/)ui-web(\/|$)/.test(module)
      || /^(wasmJs|js)/.test(sourceSet)) return false;
    // Keep commonMain/resources and unknown source sets conservatively. This is a platform filter,
    // not a reimplementation of Gradle's transitive dependency graph.
    return true;
  }
  // Shared build configuration can affect either target even when it belongs to another module.
  if (/^(build-logic|gradle)\//.test(name)
    || /(^|\/)(build|settings)\.gradle(?:\.kts)?$/.test(name)
    || /(^|\/)(gradle\.properties|gradlew|gradlew\.bat)$/.test(name)) return true;
  if (target === 'web') {
    return /^kotlin-js-store\//.test(name) || /(^|\/)webpack\.config\.d\//.test(name)
      || (!/^webApp\/e2e\//.test(name) && /(^|\/)(package(?:-lock)?\.json|yarn\.lock)$/.test(name));
  }
  return /(^|\/)[^/]+\.pro$/.test(name);
}

export function sourceFingerprint(target, root = checkout) {
  if (!Object.hasOwn(defaultArtifacts, target)) throw new Error('UNSUPPORTED_TARGET');
  // Include new/modified/deleted production inputs, not just HEAD. Ignore docs and review tooling.
  const names = execFileSync('git', ['ls-files', '-c', '-o', '--exclude-standard', '-z'], {
    cwd: root, encoding: 'utf8', timeout: 15000, maxBuffer: 16 * 1024 * 1024, windowsHide: true,
  }).split('\0').filter(Boolean);
  const inputs = [...new Set(names)].filter((name) => isProductionInput(name, target)).sort();
  const hash = createHash('sha256');
  hash.update(`schema:${provenanceSchemaVersion}\0target:${target}\0`);
  for (const name of inputs) {
    const path = join(root, name);
    hash.update(name).update('\0');
    if (!existsSync(path)) { hash.update('deleted\0'); continue; }
    if (!lstatSync(path).isFile()) throw new Error('Unsupported source entry');
    hash.update(digest(readFileSync(path))).update('\0');
  }
  return hash.digest('hex');
}

export function artifactFingerprint(path) {
  const hash = createHash('sha256');
  function visit(current) {
    const stat = lstatSync(current);
    if (stat.isSymbolicLink()) throw new Error('Artifact symlinks are not supported');
    if (stat.isDirectory()) {
      for (const name of readdirSync(current).sort()) visit(join(current, name));
    } else if (stat.isFile()) {
      hash.update(relative(path, current).split(sep).join('/')).update('\0');
      hash.update(digest(readFileSync(current))).update('\0');
    } else {
      throw new Error('Unsupported artifact entry');
    }
  }
  visit(path);
  return hash.digest('hex');
}

export function manifestPath(target, root = checkout) {
  return join(root, 'build', 'review', 'provenance', `${target}.json`);
}

export function configurationState(root = checkout, selection, environment = process.env) {
  const selected = resolve(root, selection ?? environment.STREAMCORE_LOCAL_PROPERTIES ?? 'local.properties');
  const gradleHome = resolve(environment.GRADLE_USER_HOME || join(homedir(), '.gradle'));
  const paths = [...new Set([join(root, 'local.properties'), selected,
    join(root, 'gradle.properties'), join(gradleHome, 'gradle.properties')])].sort();
  const hash = createHash('sha256');
  hash.update(selected).update('\0');
  for (const path of paths) {
    hash.update(path).update('\0');
    hash.update(existsSync(path) ? digest(readFileSync(path)) : 'missing').update('\0');
  }
  // These can override Gradle properties or JVM/toolchain selection. Hash privately, never return values.
  for (const name of Object.keys(environment).filter((key) => /^ORG_GRADLE_PROJECT_/i.test(key)
    || ['GRADLE_OPTS', 'JAVA_OPTS', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', 'JAVA_HOME'].includes(key)).sort()) {
    hash.update(name).update('\0').update(environment[name] ?? '').update('\0');
  }
  // An arbitrary init script can read inputs outside this manifest's coverage. Do not certify it.
  const initScripts = ['init.gradle', 'init.gradle.kts', 'init.d'].some((name) => {
    const path = join(gradleHome, name);
    if (!existsSync(path)) return false;
    return lstatSync(path).isDirectory() ? readdirSync(path).some((file) => /\.gradle(?:\.kts)?$/.test(file)) : true;
  });
  return { selected, fingerprint: hash.digest('hex'), customInitScripts: initScripts };
}

export function reviewStatus(target, options = {}) {
  const root = options.root ?? checkout;
  const artifact = resolve(root, options.artifact ?? defaultArtifacts[target] ?? '');
  const result = { status: 'unknown', target, artifact };
  if (!Object.hasOwn(defaultArtifacts, target)) return { ...result, reason: 'UNSUPPORTED_TARGET' };
  if (!existsSync(artifact)) return { ...result, reason: 'ARTIFACT_MISSING' };
  try {
    const path = manifestPath(target, root);
    if (!existsSync(path)) return { ...result, reason: 'BUILD_NOT_RECORDED' };
    const manifest = JSON.parse(readFileSync(path, 'utf8'));
    if (manifest.schemaVersion !== provenanceSchemaVersion || manifest.checkout !== realpathSync(root)
      || resolve(root, manifest.artifact) !== artifact || manifest.target !== target) {
      return { ...result, reason: 'MANIFEST_MISMATCH' };
    }
    const stale = (reason) => ({ ...result, status: 'stale', reason, builtAt: manifest.builtAt });
    if (sourceFingerprint(target, root) !== manifest.sourceFingerprint) return stale('SOURCE_CHANGED');
    const configuration = configurationState(root, options.localProperties);
    if (configuration.customInitScripts) return { ...result, reason: 'CUSTOM_GRADLE_INIT_SCRIPTS' };
    if (configuration.selected !== manifest.localPropertiesPath
      || configuration.fingerprint !== manifest.configurationFingerprint) {
      return stale('LOCAL_CONFIGURATION_CHANGED');
    }
    if (artifactFingerprint(artifact) !== manifest.artifactFingerprint) return stale('ARTIFACT_CHANGED');
    if (target === 'web') {
      const runtime = resolve(root, options.runtimeConfig ?? manifest.runtimeConfigPath ?? '');
      if (!existsSync(runtime) || digest(readFileSync(runtime)) !== manifest.runtimeFingerprint) {
        return stale('RUNTIME_CONFIGURATION_CHANGED');
      }
    }
    return { ...result, status: 'fresh', builtAt: manifest.builtAt, sourceFingerprint: manifest.sourceFingerprint,
      artifactFingerprint: manifest.artifactFingerprint, variant: manifest.variant };
  } catch (error) {
    // Paths, secrets, arbitrary filesystem errors and Git output stay out of readiness responses.
    return { ...result, reason: error.code === 'EACCES' || error.code === 'EPERM' ? 'ACCESS_DENIED' : 'VERIFICATION_UNAVAILABLE' };
  }
}
