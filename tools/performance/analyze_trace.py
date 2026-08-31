"""Extract auditable frame/slice evidence; this does not automatically diagnose causality."""
import argparse
import csv
import io
import json
import subprocess
import sys
from pathlib import Path

PACKAGE = "com.pampoukidis.streamcoretv.benchmark"

FRAMES = f"""
SELECT a.id, a.surface_frame_token, a.ts, a.dur,
       ROUND((a.ts-b.start_ts)/1e6, 3) AS offset_ms,
       ROUND(a.dur/1e6, 3) AS duration_ms,
       ROUND((a.ts+a.dur-e.ts-e.dur)/1e6, 3) AS overrun_ms,
       a.jank_type, a.layer_name
FROM actual_frame_timeline_slice a
JOIN expected_frame_timeline_slice e
  ON e.upid=a.upid AND e.surface_frame_token=a.surface_frame_token
JOIN process p ON p.upid=a.upid CROSS JOIN trace_bounds b
WHERE p.name='{PACKAGE}' AND a.dur>0
ORDER BY overrun_ms DESC LIMIT 12
"""

SLICES = f"""
SELECT s.name, COALESCE(t.name, 'async') AS thread, COUNT(*) AS count,
       ROUND(MAX(s.dur)/1e6, 3) AS max_ms, ROUND(SUM(s.dur)/1e6, 3) AS inclusive_total_ms
FROM slice s LEFT JOIN thread_track tt ON tt.id=s.track_id
LEFT JOIN thread t USING(utid) LEFT JOIN process_track pt ON pt.id=s.track_id
JOIN process p ON p.upid=COALESCE(t.upid, pt.upid)
WHERE p.name='{PACKAGE}' AND s.dur>=0
  AND (s.name GLOB 'SC.*' OR s.name LIKE '%streamcoretv%'
       OR lower(s.name) LIKE '%recompose%' OR lower(s.name) LIKE '%layout%'
       OR lower(s.name) LIKE '%measure%' OR lower(s.name) LIKE '%jit%'
       OR lower(s.name) LIKE '%gc%' OR lower(s.name) LIKE '%classlinker%')
GROUP BY s.name, thread ORDER BY max_ms DESC LIMIT 150
"""

COUNTERS = f"""
SELECT ct.name, MIN(c.value) AS min_value, MAX(c.value) AS max_value,
       (SELECT c2.value FROM counter c2 WHERE c2.track_id=c.track_id ORDER BY c2.ts DESC LIMIT 1) AS last_value
FROM counter c JOIN process_counter_track ct ON ct.id=c.track_id
JOIN process p ON p.upid=ct.upid
WHERE p.name='{PACKAGE}' AND ct.name GLOB 'SC.*'
GROUP BY c.track_id ORDER BY ct.name
"""

HEALTH = "SELECT name, severity, value FROM stats WHERE value > 0 AND severity IN ('error', 'data_loss') ORDER BY name"

# Matching follows AndroidX Benchmark 1.4.1 FrameTimingQuery (AOSP, Apache-2.0),
# including its b/279088460 timestamp-overlap workaround. Native reports remain
# authoritative; this reconstruction supplies the corresponding frame timestamps.
FRAME_THREAD_SLICES = f"""
SELECT s.name, s.ts, s.dur FROM slice s
JOIN thread_track tt ON tt.id=s.track_id JOIN thread t USING(utid)
JOIN process p ON p.upid=t.upid
WHERE p.name='{PACKAGE}' AND (
 (s.name LIKE 'Choreographer#doFrame%' AND s.name NOT LIKE '%resynced%' AND p.pid=t.tid)
 OR (s.name LIKE 'DrawFrame%' AND t.name='RenderThread'))
"""

FRAME_SLICES = FRAME_THREAD_SLICES + f"""
UNION
SELECT 'actual ' || a.name, a.ts, a.dur FROM actual_frame_timeline_slice a
JOIN process p USING(upid) WHERE p.name='{PACKAGE}'
UNION
SELECT 'expected ' || e.name, e.ts, e.dur FROM expected_frame_timeline_slice e
JOIN process p USING(upid) WHERE p.name='{PACKAGE}'
ORDER BY ts ASC
"""


def query(processor, trace, sql):
    command = [sys.executable, str(processor), '-Q', sql, str(trace)]
    result = subprocess.run(command, text=True, capture_output=True, check=True, encoding='utf-8')
    return list(csv.DictReader(io.StringIO(result.stdout)))


def find_frame(slices, frame_id):
    low, high = 0, len(slices) - 1
    while low <= high:
        middle = (low + high) // 2
        current = slices[middle]['frame_id']
        if current == frame_id:
            return slices[middle]
        if current < frame_id:
            low = middle + 1
        else:
            high = middle - 1
    return None


def match_frames(rows, trace_start, capture_api_level=31):
    groups = {name: [] for name in ('ui', 'rt', 'actual', 'expected')}
    for row in rows:
        name, ts, dur = row['name'], int(row['ts']), int(row['dur'])
        if dur <= 0 or 'resynced' in name:
            continue
        kind = ('ui' if name.startswith('Choreographer#doFrame') else
                'rt' if name.startswith('DrawFrame') else name.split(' ')[0])
        frame_id = int(name.split(' ')[-1]) if capture_api_level >= 31 else None
        groups[kind].append(dict(frame_id=frame_id, ts=ts, dur=dur, end=ts+dur))
    if capture_api_level < 31:
        # AndroidX ignores IDs before API 31, even on devices that emit them.
        # Match the first UI slice containing the RenderThread slice's start.
        matched = []
        for rt in groups['rt']:
            ui = next((item for item in groups['ui'] if item['ts'] <= rt['ts'] <= item['end']), None)
            if ui is None:
                continue
            matched.append(dict(frame_id=None, actual_frame_id=None,
                                ts=ui['ts'], dur=rt['end']-ui['ts'], offset_ms=(ui['ts']-trace_start)/1e6,
                                ui_duration_ms=ui['dur']/1e6, cpu_duration_ms=(rt['end']-ui['ts'])/1e6,
                                overrun_ms=None))
        return matched
    pool = list(groups['actual'])
    matched = []
    for rt in groups['rt']:
        ui = find_frame(groups['ui'], rt['frame_id'])
        if ui is None:
            continue
        middle = ui['ts'] + ui['dur'] // 2
        actual = next((item for item in reversed(pool)
                       if item['ts'] < ui['ts'] + 50_000 and item['ts'] <= middle <= item['end']), None)
        if actual is None:
            continue
        pool.remove(actual)
        expected = find_frame(groups['expected'], actual['frame_id'])
        if expected is None:
            continue
        end = max(actual['end'], rt['end'])
        matched.append(dict(frame_id=ui['frame_id'], actual_frame_id=actual['frame_id'],
                            ts=ui['ts'], dur=end-ui['ts'], offset_ms=(ui['ts']-trace_start)/1e6,
                            ui_duration_ms=ui['dur']/1e6, cpu_duration_ms=(rt['end']-ui['ts'])/1e6,
                            overrun_ms=(end-expected['end'])/1e6))
    return matched


def analyze(processor, trace, capture_api_level=31):
    has_frame_timeline = capture_api_level >= 31
    timeline_frames = query(processor, trace, FRAMES) if has_frame_timeline else []
    bounds = query(processor, trace, 'SELECT start_ts FROM trace_bounds')[0]
    frame_query = FRAME_SLICES if has_frame_timeline else FRAME_THREAD_SLICES + ' ORDER BY ts ASC'
    matched = match_frames(query(processor, trace, frame_query), int(bounds['start_ts']), capture_api_level)
    ranking_metric = 'overrun_ms' if has_frame_timeline else 'cpu_duration_ms'
    frames = sorted(matched, key=lambda frame: frame[ranking_metric], reverse=True)[:12]
    overlaps = []
    running = []
    if frames:
        first = frames[0]
        start, end = int(first['ts']), int(first['ts']) + int(first['dur'])
        sql = f"""
        SELECT s.name, t.name AS thread, ROUND((s.ts-b.start_ts)/1e6,3) AS offset_ms,
               ROUND(s.dur/1e6,3) AS duration_ms,
               ROUND((MIN(s.ts+s.dur,{end})-MAX(s.ts,{start}))/1e6,3) AS overlap_ms
        FROM slice s JOIN thread_track tt ON tt.id=s.track_id JOIN thread t USING(utid)
        JOIN process p ON p.upid=t.upid CROSS JOIN trace_bounds b
        WHERE p.name='{PACKAGE}' AND s.dur>0 AND s.ts<{end} AND s.ts+s.dur>{start}
        ORDER BY overlap_ms DESC, duration_ms DESC LIMIT 100
        """
        overlaps = query(processor, trace, sql)
        running = query(processor, trace, f"""
        SELECT t.name AS thread, t.tid,
               ROUND(SUM(MIN(sc.ts+sc.dur,{end})-MAX(sc.ts,{start}))/1e6,3) AS running_ms
        FROM sched sc JOIN thread t USING(utid) JOIN process p ON p.upid=t.upid
        WHERE p.name='{PACKAGE}' AND sc.dur>0 AND sc.ts<{end} AND sc.ts+sc.dur>{start}
        GROUP BY t.utid ORDER BY running_ms DESC
        """)
    sources = query(processor, trace, "SELECT name, COUNT(*) AS count FROM slice WHERE name GLOB 'SC.Image.source.*' GROUP BY name")
    return dict(trace=str(trace.resolve()), capture_api_level=capture_api_level,
                frame_overrun_available=has_frame_timeline,
                frame_overrun_unavailable_reason=None if has_frame_timeline else 'FrameTimeline requires API 31+',
                candidate_ranking_metric=ranking_metric, frames=frames, matched_frame_count=len(matched),
                matched_frames=matched, timeline_candidates=timeline_frames, image_sources=sources,
                slices=query(processor, trace, SLICES),
                counters=query(processor, trace, COUNTERS),
                health=query(processor, trace, HEALTH),
                worst_frame_overlaps=overlaps,
                worst_frame_cpu_running=running,
                caveat="Nested slice durations are inclusive and not additive; elapsed duration is not CPU running time.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('trace', type=Path)
    parser.add_argument('--processor', type=Path, default=Path(__file__).parent / '.tools/trace_processor')
    parser.add_argument('--api-level', type=int, default=31,
                        help='Capture device API level; use 30 for Android 11 CPU-only frame matching')
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    evidence = analyze(args.processor, args.trace, args.api_level)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(evidence, indent=2), encoding='utf-8')
    print(f"Wrote {len(evidence['frames'])} candidate frames and {len(evidence['slices'])} slice groups to {args.output}")


if __name__ == '__main__':
    main()
