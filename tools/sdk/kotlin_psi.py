"""Offline Kotlin PSI extraction using the project's already-cached Kotlin compiler.

Only the Java exporter is compiled. No Gradle invocation, downloads, production
compilation, or arbitrary filesystem inventory are performed here.
"""
from __future__ import annotations

import base64
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
EXPORTER = Path(__file__).with_name('KotlinSourceIndex.java')


def _runtime(root: Path) -> tuple[str, Path, Path, list[Path]]:
    catalog = root / 'gradle/libs.versions.toml'
    if not catalog.is_file():
        catalog = ROOT / 'gradle/libs.versions.toml'
    match = re.search(r'^kotlin\s*=\s*"([^"]+)"', catalog.read_text(encoding='utf-8'), re.M)
    if not match:
        raise RuntimeError('Cannot identify the project Kotlin version in gradle/libs.versions.toml.')
    version = match.group(1)
    cache = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle'))) / 'caches/modules-2/files-2.1'

    def artifact(group: str, name: str, release: str, suffix: str = 'jar') -> Path:
        directory = cache / group / name / release
        matches = sorted(directory.glob(f'*/{name}-{release}.{suffix}'))
        if not matches:
            raise RuntimeError(f'Offline Kotlin PSI requires cached {group}:{name}:{release} ({suffix}). '
                               'Restore the project Gradle dependency cache; the indexer never downloads dependencies.')
        return matches[0]

    compiler = artifact('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', version)
    pom = artifact('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', version, 'pom')
    classpath = [compiler]
    namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
    for dependency in ET.parse(pom).findall('m:dependencies/m:dependency', namespace):
        def field(name: str) -> str:
            return dependency.findtext('m:' + name, default='', namespaces=namespace)
        if field('scope') in ('', 'compile', 'runtime') and field('optional') != 'true':
            classpath.append(artifact(field('groupId'), field('artifactId'), field('version')))
    java = shutil.which('java')
    if not java and os.environ.get('JAVA_HOME'):
        candidate = Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')
        if candidate.is_file():
            java = str(candidate)
    if not java:
        raise RuntimeError('Offline Kotlin PSI needs a JDK with java and javac on PATH (or JAVA_HOME).')
    java_path = Path(java)
    javac = java_path.with_name('javac.exe' if os.name == 'nt' else 'javac')
    if not javac.is_file():
        fallback = shutil.which('javac')
        if not fallback:
            raise RuntimeError('Offline Kotlin PSI needs javac from a JDK, not a Java-only runtime.')
        javac = Path(fallback)
    return version, java_path, javac, classpath


def _run(command: list[str], *, timeout: int, description: str) -> subprocess.CompletedProcess:
    try:
        result = subprocess.run(command, capture_output=True, text=True, encoding='utf-8',
                                errors='replace', timeout=timeout, check=False)
    except subprocess.TimeoutExpired as error:
        raise RuntimeError(f'{description} exceeded its {timeout}-second limit.') from error
    if result.returncode:
        detail = (result.stderr or result.stdout)[-8000:]
        raise RuntimeError(f'{description} failed (exit {result.returncode}):\n{detail}')
    return result


def parse_sources(files: list[dict], root: Path = ROOT) -> dict:
    """Parse supplied production Kotlin snapshots, preserving exact source bytes.

    Inputs are safe inventory dictionaries with path/source; test inputs are
    excluded. Returned coordinates use Unicode code-point offsets, normalized
    from Kotlin PSI's UTF-16 offsets for direct Python source slicing.
    Compiler and source caches remain under build/sdk-explorer/psi.
    """
    root = Path(root).resolve()
    selected = []
    for file in files:
        relative = str(file['path']).replace('\\', '/')
        path = PurePosixPath(relative)
        if path.is_absolute() or '..' in path.parts or ':' in relative:
            raise ValueError(f'Kotlin source path must be repository-relative: {relative}')
        source_set = file.get('sourceSet', '')
        if not relative.endswith('.kt') or file.get('test') or 'test' in source_set.lower():
            continue
        selected.append({**file, 'path': relative, 'source': file.get('source', '')})
    selected.sort(key=lambda file: file['path'])
    if len({file['path'] for file in selected}) != len(selected):
        raise ValueError('Duplicate source paths supplied to Kotlin PSI.')
    version, java, javac, classpath = _runtime(root)
    parser_key = hashlib.sha256(version.encode() + b'\0' + EXPORTER.read_bytes()).hexdigest()
    cache = root / 'build/sdk-explorer/psi'
    cache.mkdir(parents=True, exist_ok=True)
    classes = cache / ('classes-' + parser_key[:20])
    classpath_text = os.pathsep.join(map(str, classpath))
    if not (classes / 'KotlinSourceIndex.class').is_file():
        with tempfile.TemporaryDirectory(prefix='compile-', dir=cache) as temporary:
            _run([str(javac), '-encoding', 'UTF-8', '-cp', classpath_text, '-d', temporary, str(EXPORTER)],
                 timeout=60, description='Kotlin PSI exporter compilation')
            classes.mkdir(exist_ok=True)
            for compiled in Path(temporary).glob('*.class'):
                shutil.copy2(compiled, classes / compiled.name)
    fingerprint = hashlib.sha256(parser_key.encode())
    for file in selected:
        for value in (file['path'], file['source']):
            encoded = value.encode('utf-8')
            fingerprint.update(len(encoded).to_bytes(8, 'big'))
            fingerprint.update(encoded)
    result_path = cache / (fingerprint.hexdigest() + '.json')
    if result_path.is_file():
        result = json.loads(result_path.read_text(encoding='utf-8'))
    else:
        with tempfile.NamedTemporaryFile(mode='w', encoding='ascii', newline='\n', suffix='.sources', dir=cache,
                                         delete=False) as manifest:
            manifest_path = Path(manifest.name)
            for file in selected:
                manifest.write(base64.b64encode(file['path'].encode('utf-8')).decode('ascii') + '\t' +
                               base64.b64encode(file['source'].encode('utf-8')).decode('ascii') + '\n')
        try:
            process = _run([str(java), '-Djava.awt.headless=true', '-Xmx1024m', '-cp',
                            str(classes) + os.pathsep + classpath_text, 'KotlinSourceIndex', str(manifest_path)],
                           timeout=60, description='Kotlin PSI source parsing')
            result = json.loads(process.stdout)
            if result.get('schemaVersion') != 1:
                raise RuntimeError('Unexpected Kotlin PSI exporter schema.')
            # Complete output only; a killed parser cannot leave a successful cache entry.
            temporary_output = result_path.with_suffix('.tmp-' + str(os.getpid()))
            temporary_output.write_text(json.dumps(result, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
            temporary_output.replace(result_path)
        finally:
            manifest_path.unlink(missing_ok=True)
    inventory = {file['path']: file for file in selected}
    for parsed in result['files']:
        original = inventory[parsed['path']]
        for key in ('module', 'sourceSet', 'test'):
            if key in original:
                parsed[key] = original[key]
        parsed['sourceHash'] = hashlib.sha256(original['source'].encode('utf-8')).hexdigest()
    result['compilerVersion'] = version
    result['parserHash'] = parser_key
    return result
