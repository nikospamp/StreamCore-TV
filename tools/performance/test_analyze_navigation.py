import unittest
import json
import tempfile
from pathlib import Path
from unittest.mock import patch
from analyze_navigation import identity, percentile, sample_runs, summarize, render, JOURNEYS, ENTRIES
from analyze_trace import analyze, match_frames


class AnalyzerTest(unittest.TestCase):
    def test_percentile_uses_linear_interpolation(self):
        self.assertEqual(5.5, percentile(list(range(1, 11)), 50))
        self.assertAlmostEqual(9.91, percentile(list(range(1, 11)), 99))

    def test_missing_samples_are_not_reported_as_zero(self):
        self.assertIsNone(percentile([], 95))
        self.assertEqual([], sample_runs({"runs": [1, 2, 3]}))

    def test_parameterized_names_keep_journeys_separate(self):
        self.assertEqual(("homeToDetails", "repeated", "Partial"), identity({
            "name": "navigation[homeToDetails_repeated_Partial]",
        }))

    def test_unknown_identity_fails_instead_of_merging(self):
        with self.assertRaises(ValueError):
            identity({"name": "navigation"})

    def test_api30_summary_selects_cpu_trace_and_keeps_diagnostic_mode(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'run.json').write_text(json.dumps(dict(
                runId='oneplus', variant='benchmark', sdk=30, model='ONEPLUS A6013',
                fingerprint='oneplus-build', deviceId='oneplus-device',
            )))
            (root / 'native.json').write_text(json.dumps(dict(benchmarks=[dict(
                name='navigation[homeToDetails_first_Ignore]',
                sampledMetrics=dict(frameDurationCpuMs=dict(runs=[[1, 2], [4, 8]])),
            )])))
            (root / 'homeToDetails_first_Ignore_journey.json').write_text(
                json.dumps(dict(readinessWallMs=[20, 30])))
            trace = root / 'homeToDetails_first_Ignore_iter001_sample.perfetto-trace'
            trace.touch()
            rows, warnings = summarize(root)
            self.assertEqual([], warnings)
            self.assertEqual('Ignore', rows[0]['compilation'])
            self.assertEqual('frameDurationCpuMs', rows[0]['representativeMetric'])
            self.assertEqual(str(trace.resolve()), rows[0]['representativeTrace'])
            self.assertNotIn('frameOverrunMs_p95', rows[0])
            mixed = [rows[0], dict(rows[0], deviceId='samsung-device')]
            errors = render(mixed, [], root / 'report', expected_iterations=2)
            self.assertTrue(any('Mixed device/OS' in error for error in errors))

    def test_frame_matching_handles_synthetic_id_shift_and_late_render_end(self):
        rows = [
            dict(name='expected 99', ts='100000000', dur='16000000'),
            dict(name='Choreographer#doFrame 10', ts='100000000', dur='20000000'),
            dict(name='actual 99', ts='100002000', dur='24000000'),
            dict(name='Choreographer#doFrame - resynced to 11 in 1ms', ts='101000000', dur='19000000'),
            dict(name='DrawFrames 10', ts='120000000', dur='10000000'),
            dict(name='Choreographer#doFrame 11', ts='140000000', dur='10000000'),
            dict(name='DrawFrames 11', ts='150000000', dur='5000000'),
        ]
        frames = match_frames(rows, 90000000)
        self.assertEqual(1, len(frames))
        self.assertEqual(10, frames[0]['frame_id'])
        self.assertEqual(99, frames[0]['actual_frame_id'])
        self.assertEqual(30, frames[0]['cpu_duration_ms'])
        self.assertEqual(14, frames[0]['overrun_ms'])
        self.assertEqual(10, frames[0]['offset_ms'])

    def test_diagnostic_coverage_does_not_satisfy_controlled_matrix(self):
        rows = [dict(variant='benchmarkR8', journey=journey, entry=entry, compilation='Ignore',
                     iterations=10, source='fixture', appSha256='app', driverSha256='driver',
                     contentTag='content', deviceId='phone', fingerprint='build')
                for journey in JOURNEYS for entry in ENTRIES]
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.assertEqual(32, len(render(rows, [], root, 10)))
            self.assertEqual([], render(rows, [], root, 10, diagnostic_variant='benchmarkR8'))
            mixed = rows + [dict(rows[0], compilation='None')]
            self.assertTrue(any('unexpected build or compilation' in error
                                for error in render(mixed, [], root, 10, diagnostic_variant='benchmarkR8')))

    def test_api30_matches_render_start_inside_ui_without_frame_ids(self):
        rows = [
            dict(name='Choreographer#doFrame', ts='100000000', dur='20000000'),
            dict(name='DrawFrame', ts='115000000', dur='15000000'),
            dict(name='Choreographer#doFrame', ts='140000000', dur='10000000'),
            dict(name='DrawFrame', ts='155000000', dur='5000000'),
            dict(name='Choreographer#doFrame', ts='170000000', dur='-1'),
            dict(name='DrawFrame', ts='175000000', dur='5000000'),
        ]
        frames = match_frames(rows, 90000000, capture_api_level=30)
        self.assertEqual(1, len(frames))
        self.assertEqual(30, frames[0]['cpu_duration_ms'])
        self.assertEqual(20, frames[0]['ui_duration_ms'])
        self.assertEqual(10, frames[0]['offset_ms'])
        self.assertIsNone(frames[0]['overrun_ms'])
        self.assertIsNone(frames[0]['actual_frame_id'])

    def test_api30_ignores_ids_and_uses_first_containing_ui_slice(self):
        rows = [
            dict(name='Choreographer#doFrame 10', ts='100000000', dur='20000000'),
            dict(name='Choreographer#doFrame 20', ts='110000000', dur='20000000'),
            dict(name='DrawFrames 20', ts='120000000', dur='10000000'),
        ]
        frames = match_frames(rows, 90000000, capture_api_level=30)
        self.assertEqual(1, len(frames))
        self.assertEqual(30, frames[0]['cpu_duration_ms'])
        self.assertEqual(100000000, frames[0]['ts'])

    @patch('analyze_trace.query')
    def test_api30_analysis_ranks_by_cpu_and_marks_overrun_unavailable(self, query):
        def query_result(processor, trace, sql):
            if sql == 'SELECT start_ts FROM trace_bounds':
                return [dict(start_ts='90000000')]
            if 'Choreographer#doFrame%' in sql:
                return [
                    dict(name='Choreographer#doFrame', ts='100000000', dur='20000000'),
                    dict(name='DrawFrame', ts='115000000', dur='5000000'),
                    dict(name='Choreographer#doFrame', ts='140000000', dur='20000000'),
                    dict(name='DrawFrame', ts='150000000', dur='20000000'),
                ]
            return []

        query.side_effect = query_result
        evidence = analyze(Path('processor'), Path('capture.perfetto-trace'), capture_api_level=30)
        self.assertEqual('cpu_duration_ms', evidence['candidate_ranking_metric'])
        self.assertEqual(30, evidence['frames'][0]['cpu_duration_ms'])
        self.assertEqual(2, evidence['matched_frame_count'])
        self.assertFalse(evidence['frame_overrun_available'])
        self.assertEqual('FrameTimeline requires API 31+', evidence['frame_overrun_unavailable_reason'])
        self.assertTrue(all('frame_timeline_slice' not in call.args[2] for call in query.call_args_list))


if __name__ == "__main__":
    unittest.main()
