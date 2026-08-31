"""Extract representative traces from completed batches and verify against native samples."""
import argparse
import hashlib
import json
from pathlib import Path

from analyze_navigation import identity, summarize
from analyze_trace import analyze


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('campaign', type=Path)
    parser.add_argument('--processor', type=Path, default=Path(__file__).parent / '.tools/trace_processor')
    args = parser.parse_args()
    campaign = args.campaign.resolve()
    journal = json.loads((campaign / 'campaign.json').read_text(encoding='utf-8-sig'))
    directory = campaign / 'trace-evidence'
    directory.mkdir(exist_ok=True)
    extracted = 0
    for batch in journal['batches']:
        if batch['status'] != 'complete':
            continue
        roots = batch.get('outputs') or [campaign.parent / batch['runId'] / batch['variant']]
        for root_value in roots:
            root = Path(root_value) if batch.get('outputs') else root_value
            rows, warnings = summarize(root)
            if warnings:
                raise ValueError(warnings)
            for row in rows:
                trace = Path(row['representativeTrace'])
                digest = hashlib.sha256(trace.read_bytes()).hexdigest()
                label = '_'.join(row[key] for key in ('variant', 'journey', 'entry', 'compilation'))
                output = directory / f'{label}.json'
                if output.exists() and json.loads(output.read_text()).get('traceSha256') == digest:
                    continue
                evidence = analyze(args.processor, trace, capture_api_level=int(row.get('sdk') or 31))
                native = json.loads(Path(row['source']).read_text())
                item = next(item for item in native['benchmarks']
                            if identity(item) == (row['journey'], row['entry'], row['compilation']))
                iteration = row['representativeSlowIteration']
                for metric, key in (('frameDurationCpuMs', 'cpu_duration_ms'), ('frameOverrunMs', 'overrun_ms')):
                    if metric not in item['sampledMetrics']:
                        if metric == 'frameOverrunMs' and int(row.get('sdk') or 31) < 31:
                            continue
                        raise ValueError(f'Missing native metric: {label}/{metric}')
                    expected = sorted(item['sampledMetrics'][metric]['runs'][iteration])
                    actual = sorted(frame[key] for frame in evidence['matched_frames'])
                    if len(expected) != len(actual) or any(abs(a-b) > 0.00001 for a, b in zip(expected, actual)):
                        raise ValueError(f'Native frame reconstruction mismatch: {label}/{metric}')
                evidence.update(traceSha256=digest, nativeSamplesVerified=True, measurement=row)
                output.write_text(json.dumps(evidence, indent=2), encoding='utf-8')
                extracted += 1
                print(f'Verified {label}: iteration {iteration}, {evidence["matched_frame_count"]} frames', flush=True)
    print(f'{extracted} new representative trace summaries written to {directory}')


if __name__ == '__main__':
    main()
