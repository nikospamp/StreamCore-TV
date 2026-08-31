-- Run with Perfetto trace_processor_shell -q attribution.sql TRACE (CSV on stdout).
-- Durations of nested slices are NOT additive. Inspect overlap with the slow frames.
SELECT s.name, COALESCE(t.name, 'async') AS thread,
       COUNT(*) AS occurrences, ROUND(MAX(s.dur) / 1e6, 3) AS max_ms,
       ROUND(SUM(s.dur) / 1e6, 3) AS inclusive_total_ms
FROM slice s
LEFT JOIN thread_track tt ON tt.id = s.track_id
LEFT JOIN thread t USING (utid)
LEFT JOIN process_track pt ON pt.id = s.track_id
JOIN process p ON p.upid = COALESCE(t.upid, pt.upid)
WHERE p.name = 'com.pampoukidis.streamcoretv.benchmark'
  AND s.dur > 0
  AND (s.name GLOB 'SC.*' OR lower(s.name) LIKE '%gc%'
       OR lower(s.name) LIKE '%jit%' OR lower(s.name) LIKE '%classlinker%'
       OR s.name GLOB '*doFrame*' OR s.name GLOB '*DrawFrame*'
       OR s.name GLOB '*recompose*' OR s.name GLOB '*lookahead*')
GROUP BY s.name, thread
ORDER BY max_ms DESC;
