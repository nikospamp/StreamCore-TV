"""Build the offline SDK course from reviewed lessons and a safe source inventory.

Run from any directory with Python 3: python tools/sdk/build-guide.py
Only repository source is embedded. Build outputs, local properties and credentials
are never read. The HTML is a snapshot, not a live browser onto the filesystem.
"""
from pathlib import Path
from html.parser import HTMLParser
import json
import re
from datetime import datetime
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError
from source_inventory import git, source_inventory

ROOT = Path(__file__).resolve().parents[2]
CONTENT = ROOT / 'docs/sdk/guide-content'
ORDER = ['overview', 'modules', 'contracts', 'lifetime', 'profiles', 'session',
         'catalog', 'playback', 'persistence', 'providers', 'applications',
         'verification', 'changes', 'reference']



def main():
    chapters = []
    for file in sorted(CONTENT.glob('*.json')):
        if file.name == 'interactions.json':
            continue
        chapters.extend(json.loads(file.read_text(encoding='utf-8-sig')))
    by_id = {c['id']: c for c in chapters}
    if set(by_id) != set(ORDER) or len(chapters) != len(ORDER):
        raise ValueError(f'Chapter IDs incomplete/duplicated: {list(by_id)}')
    chapters = [by_id[i] for i in ORDER]

    files, fingerprint = source_inventory()

    # Fail on stale or ambiguous lesson links rather than silently shipping them.
    class References(HTMLParser):
        def handle_starttag(self, tag, attrs):
            attrs = dict(attrs)
            if 'data-source' not in attrs:
                return
            value = attrs['data-source']
            matches = [f for f in files if f['path'] == value] or [f for f in files if f['name'] == value]
            if len(matches) != 1:
                raise ValueError(f'Ambiguous/missing source reference: {value} ({len(matches)})')
            if attrs.get('data-symbol') and attrs['data-symbol'] not in matches[0]['source']:
                raise ValueError(f'Missing symbol {attrs["data-symbol"]} in {value}')
    parser = References()
    for chapter in chapters:
        parser.feed(chapter['body'])
        quiz = chapter.get('quiz')
        if quiz and not 0 <= quiz['correct'] < len(quiz['options']):
            raise ValueError(f'Invalid quiz in {chapter["id"]}')
    interactions = json.loads((CONTENT / 'interactions.json').read_text(encoding='utf-8'))
    for journey in interactions['journeys']:
        for step in journey['steps']:
            parser.feed(f'<button data-source="{step["file"]}" data-symbol="{step.get("symbol", "")}">')
    try:
        date = datetime.now(ZoneInfo('Europe/Athens')).strftime('%Y-%m-%d')
    except ZoneInfoNotFoundError:
        date = datetime.now().strftime('%Y-%m-%d')
    data = {'chapters': chapters, 'files': files, 'interactions': interactions,
            'snapshot': {'date': date, 'commit': git('rev-parse', '--short', 'HEAD'),
                         'fingerprint': fingerprint[:16],
                         'dirty': bool(git('status', '--porcelain', '--', 'sdk'))}}
    serialized = json.dumps(data, ensure_ascii=False).replace('<', '\\u003c').replace('>', '\\u003e').replace('&', '\\u0026')
    result = (CONTENT / 'shell.html').read_text(encoding='utf-8')
    result = result.replace('/* GUIDE_CSS */', (CONTENT / 'guide.css').read_text(encoding='utf-8'))
    result = result.replace('/* GUIDE_JS */', (CONTENT / 'guide.js').read_text(encoding='utf-8'))
    result = result.replace('<!-- GUIDE_DATA -->', serialized)
    out = ROOT / 'docs/sdk/streamcore-sdk-guide.html'
    out.write_text(result, encoding='utf-8', newline='\n')
    word_count = sum(len(re.sub('<[^>]+>', ' ', c['body']).split()) for c in chapters)
    print(f'Built {out.relative_to(ROOT)}: {len(chapters)} chapters, {word_count:,} lesson words, {len(files)} source/resource entries, {len(result.encode()):,} bytes')


if __name__ == '__main__':
    main()
