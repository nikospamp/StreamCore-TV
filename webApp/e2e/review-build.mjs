import { spawn } from 'node:child_process';
import { closeSync, existsSync, mkdirSync, openSync, readFileSync, renameSync, unlinkSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { artifactFingerprint, checkout, configurationState, defaultArtifacts, digest, manifestPath, provenanceSchemaVersion, reviewStatus, sourceFingerprint } from './review-provenance.mjs';

function options(args) {
  const [command, ...rest] = args;
  if (!['status', 'build'].includes(command)) throw new Error('Use status|build --target android|web|all');
  const values = { command, target: 'all', 'timeout-ms': '1800000' };
  for (let i = 0; i < rest.length; i += 2) {
    const key = rest[i]?.replace(/^--/, '');
    if (!['target', 'artifact', 'local-properties', 'timeout-ms', 'runtime-config'].includes(key)
      || rest[i] !== `--${key}` || !rest[i + 1] || rest[i + 1].startsWith('--')) throw new Error('Invalid arguments');
    values[key] = rest[i + 1];
  }
  if (!['android', 'web', 'all'].includes(values.target)) throw new Error('Invalid target');
  if (values.artifact && (command !== 'status' || values.target === 'all')) throw new Error('--artifact requires one status target');
  if (values['runtime-config'] && (command !== 'status' || values.target === 'android')) throw new Error('--runtime-config requires web status');
  if (!Number.isSafeInteger(Number(values['timeout-ms'])) || Number(values['timeout-ms']) < 1000) throw new Error('Invalid timeout');
  return values;
}

function run(executable, args, log, timeout, env = process.env) {
  return new Promise((resolveRun, reject) => {
    const fd = openSync(log, 'a');
    const child = spawn(executable, args, { cwd: checkout, env, windowsHide: true, stdio: ['ignore', fd, fd] });
    closeSync(fd);
    let timedOut = false;
    function stop() {
      if (process.platform === 'win32') {
        spawn('taskkill.exe', ['/PID', String(child.pid), '/T', '/F'], { windowsHide: true, stdio: 'ignore' });
      } else child.kill('SIGTERM');
    }
    const timer = setTimeout(() => { timedOut = true; if (child.pid) stop(); }, timeout);
    const cancel = () => { timedOut = true; if (child.pid) stop(); };
    process.once('SIGINT', cancel);
    process.once('SIGTERM', cancel);
    function finish() { clearTimeout(timer); process.removeListener('SIGINT', cancel); process.removeListener('SIGTERM', cancel); }
    child.once('error', (error) => { finish(); reject(new Error(['EACCES', 'EPERM'].includes(error.code) ? 'ACCESS_DENIED' : 'PROCESS_START_FAILED')); });
    child.once('exit', (code) => { finish(); if (timedOut) reject(new Error('PROCESS_TIMEOUT_OR_CANCELLED')); else if (code !== 0) reject(new Error('PROCESS_FAILED')); else resolveRun(); });
  });
}

const psLiteral = (value) => `'${value.replaceAll("'", "''")}'`;

export async function main(args = process.argv.slice(2)) {
  if (args.includes('--help')) {
    console.log('node review-build.mjs status|build --target android|web|all [--local-properties PATH] [--artifact PATH (status only)] [--runtime-config PATH (web status only)] [--timeout-ms 1800000]\nSupports TMDB review builds only: Android tmdbDebug and web production. Use normal Gradle tasks for clientB, benchmark, release, Android Studio, or development-server workflows.');
    return;
  }
  let lock;
  let log;
  try {
    const opts = options(args);
    const targets = opts.target === 'all' ? ['android', 'web'] : [opts.target];
    if (opts.command === 'status') {
      const statuses = targets.map((target) => reviewStatus(target, { artifact: opts.artifact, runtimeConfig: opts['runtime-config'], localProperties: opts['local-properties'] }));
      console.log(JSON.stringify(statuses.length === 1 ? statuses[0] : { targets: statuses }));
      return;
    }
    const output = resolve(checkout, 'build/review');
    mkdirSync(output, { recursive: true });
    lock = resolve(output, 'build.lock');
    try { writeFileSync(lock, JSON.stringify({ pid: process.pid, createdAt: new Date().toISOString() }), { flag: 'wx' }); }
    catch (error) { lock = undefined; throw new Error(error.code === 'EEXIST' ? 'BUILD_ALREADY_RUNNING_OR_STALE_LOCK' : 'BUILD_LOCK_FAILED'); }
    const localPropertiesPath = resolve(checkout, opts['local-properties'] ?? process.env.STREAMCORE_LOCAL_PROPERTIES ?? 'local.properties');
    if (!existsSync(localPropertiesPath)) throw new Error('LOCAL_PROPERTIES_MISSING');
    const configuration = configurationState(checkout, localPropertiesPath);
    if (configuration.customInitScripts) throw new Error('CUSTOM_GRADLE_INIT_SCRIPTS');
    const configurationFingerprint = configuration.fingerprint;
    const before = Object.fromEntries(targets.map((target) => [target, sourceFingerprint(target)]));
    log = resolve(output, `build-${Date.now()}.log`);
    // Fixed task allowlist; no shell string assembled from arbitrary task names.
    const tasks = [':app:verifyTmdbRuntimeConfig', '-PrequireTmdbRuntimeConfig=true', '--console=plain'];
    tasks.push(`-PstreamcoreLocalPropertiesPath=${localPropertiesPath}`);
    if (targets.includes('android')) tasks.push(':app:assembleTmdbDebug');
    if (targets.includes('web')) tasks.push(':webApp:generateWebDevelopmentConfig', ':webApp:wasmJsBrowserDistribution');
    const env = { ...process.env, STREAMCORE_LOCAL_PROPERTIES: localPropertiesPath };
    const timeout = Number(opts['timeout-ms']);
    if (process.platform === 'win32') {
      const command = `& ${psLiteral(resolve(checkout, 'gradlew.bat'))} ${tasks.map(psLiteral).join(' ')}; exit $LASTEXITCODE`;
      await run('pwsh.exe', ['-NoProfile', '-NonInteractive', '-Command', command], log, timeout, env);
    } else {
      await run(resolve(checkout, 'gradlew'), tasks, log, timeout, env);
    }
    if (targets.includes('web')) {
      await run(process.execPath, [resolve(checkout, 'webApp/e2e/validate-release-artifact.mjs')], log, timeout,
        { ...env, STREAMCORE_WEB_RELEASE_DIRECTORY: resolve(checkout, defaultArtifacts.web) });
    }
    if (targets.some((target) => sourceFingerprint(target) !== before[target])
      || configurationState(checkout, localPropertiesPath).fingerprint !== configurationFingerprint) {
      throw new Error('INPUTS_CHANGED_DURING_BUILD');
    }
    const results = [];
    for (const target of targets) {
      const artifact = defaultArtifacts[target];
      const manifest = { schemaVersion: provenanceSchemaVersion, checkout, target, artifact, variant: target === 'android' ? 'tmdbDebug' : 'production',
        builtAt: new Date().toISOString(), sourceFingerprint: before[target], localPropertiesPath, configurationFingerprint,
        artifactFingerprint: artifactFingerprint(resolve(checkout, artifact)) };
      if (target === 'web') {
        manifest.runtimeConfigPath = resolve(checkout, 'webApp/build/generated/webDevelopmentConfig/config.json');
        manifest.runtimeFingerprint = digest(readFileSync(manifest.runtimeConfigPath));
      }
      const path = manifestPath(target);
      mkdirSync(dirname(path), { recursive: true });
      const temp = `${path}.${process.pid}.tmp`;
      writeFileSync(temp, JSON.stringify(manifest, null, 2));
      renameSync(temp, path);
      results.push(reviewStatus(target, { localProperties: localPropertiesPath }));
    }
    console.log(JSON.stringify({ success: true, targets: results, log }));
  } catch (error) {
    console.log(JSON.stringify({ success: false, code: /^[A-Z_]+$/.test(error.message) ? error.message : 'REVIEW_BUILD_FAILED', log }));
    process.exitCode = 1;
  } finally {
    if (lock) unlinkSync(lock);
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) await main();
