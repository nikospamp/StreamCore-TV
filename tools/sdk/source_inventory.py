"""Safe, deterministic repository source snapshots shared by offline SDK tools."""
from pathlib import Path
import hashlib
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOTS = ('sdk', 'app/src', 'webApp/src', 'feature', 'playback',
                'build-logic/src', 'samples/sdk-consumer', 'tools/sdk')
EXCLUDED = {'build', '.gradle', 'node_modules', '.kotlin', '__pycache__', '.git'}
TEXT_SUFFIXES = {'.kt', '.kts', '.xml', '.md', '.py', '.ps1', '.pro'}


def git(*args, root=ROOT):
    return subprocess.check_output(['git', *args], cwd=root).decode('utf-8').strip()


def module_for(path):
    parts = path.split('/')
    if parts[0] != 'sdk':
        return 'application / tooling'
    count = 4 if len(parts) > 3 and parts[1] == 'providers' and parts[3] == 'ui' else (3 if parts[1] == 'providers' else 2)
    return ':' + ':'.join(parts[:count])


def source_inventory(root=ROOT):
    """Read only source paths known to Git; never read ignored runtime config."""
    candidates = set(git('ls-files', '--cached', '--others', '--exclude-standard', '--',
                         *SOURCE_ROOTS, root=root).splitlines())
    files = []
    digest = hashlib.sha256()
    for relative in sorted(candidates):
        path = root / relative
        if not path.is_file() or any(part in EXCLUDED for part in Path(relative).parts):
            continue
        if not path.resolve().is_relative_to(root.resolve()):
            raise ValueError(f'Source escapes repository: {relative}')
        is_sdk = relative.startswith('sdk/')
        textual = path.suffix in TEXT_SUFFIXES
        if not is_sdk and not textual:
            continue
        if relative.startswith('tools/sdk/') and path.name in {'build-guide.py', 'check-guide.mjs'}:
            continue
        if relative.startswith('feature/') and not any(s in path.name for s in ('ViewModel', 'Module', 'Factory', 'Route')):
            continue
        raw = path.read_bytes()
        text = raw.decode('utf-8-sig').replace('\r\n', '\n') if textual else ''
        digest.update(relative.encode() + b'\0' + raw)
        parts = relative.split('/')
        source_set = parts[parts.index('src') + 1] if 'src' in parts else ''
        package = re.search(r'^package\s+([^\s;]+)', text, re.M)
        declarations = re.findall(r'^\s*(?:(?:public|internal|private|data|sealed|enum|abstract|open|expect|actual|value|fun)\s+)*(?:class|interface|object)\s+(\w+)', text, re.M)
        doc = re.search(r'/\*\*\s*([\s\S]*?)\*/\s*(?:@[^\n]+\n\s*)*(?:(?:internal|private|public|data|sealed|enum|expect|actual|value)\s+)*(?:class|interface|object)', text)
        summary = re.sub(r'\s+', ' ', re.sub(r'^\s*\*\s?', '', doc.group(1), flags=re.M)).strip() if doc else ''
        files.append({'path': relative, 'name': path.name, 'module': module_for(relative),
                      'sourceSet': source_set, 'test': 'test' in source_set.lower(), 'source': text,
                      'package': package.group(1) if package else '',
                      'declarations': declarations[:30], 'summary': summary,
                      'binary': not textual, 'lines': len(text.splitlines()),
                      'hash': hashlib.sha256(text.encode('utf-8') if textual else raw).hexdigest()})
    return files, digest.hexdigest()
