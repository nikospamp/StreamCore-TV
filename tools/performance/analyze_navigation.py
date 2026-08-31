"""Summarize Macrobenchmark samples without pooling different journeys or compilation modes."""
import argparse
import csv
import json
import math
from pathlib import Path

JOURNEYS = ("profilesToHome", "homeToDetails", "playerToDetails", "initialSearch")
MODES = ("None", "Partial")
DIAGNOSTIC_MODES = ("Ignore",)
ENTRIES = ("first", "repeated")
METRICS = ("frameDurationCpuMs", "frameOverrunMs")


def percentile(values, percent):
    values = sorted(float(v) for v in values if isinstance(v, (float, int)) and math.isfinite(v))
    if not values:
        return None
    position = (len(values) - 1) * percent / 100
    low, high = math.floor(position), math.ceil(position)
    return values[low] + (values[high] - values[low]) * (position - low)


def identity(benchmark):
    name = str(benchmark.get("name", "")) + json.dumps(benchmark.get("params", {}))
    matches = [[item for item in choices if item in name] for choices in (JOURNEYS, ENTRIES, MODES + DIAGNOSTIC_MODES)]
    if any(len(items) != 1 for items in matches):
        raise ValueError(f"Cannot identify a unique journey/entry/compilation: {name}")
    return tuple(items[0] for items in matches)


def sample_runs(metric):
    runs = metric.get("runs", [])
    if not isinstance(runs, list) or any(not isinstance(run, list) for run in runs):
        return []
    return runs


def run_directory(path, root):
    for directory in path.parents:
        if (directory / "run.json").is_file():
            return directory
        if directory == root.parent:
            break
    raise ValueError(f"Missing provenance run.json for {path}")


def summarize(root):
    rows = []
    warnings = []
    for path in sorted(root.rglob("*.json")):
        try:
            payload = json.loads(path.read_text(encoding="utf-8-sig"))
        except (ValueError, OSError):
            continue
        if not isinstance(payload, dict) or not isinstance(payload.get("benchmarks"), list):
            continue
        directory = run_directory(path, root)
        provenance = json.loads((directory / "run.json").read_text(encoding="utf-8-sig"))
        for benchmark in payload["benchmarks"]:
            journey, entry, mode = identity(benchmark)
            row = dict(runId=provenance["runId"], variant=provenance["variant"],
                       journey=journey, entry=entry, compilation=mode,
                       source=str(path.resolve()), appSha256=provenance.get("appSha256", ""),
                       driverSha256=provenance.get("driverSha256", ""), contentTag=provenance.get("contentTag", ""))
            row.update(model=provenance.get("model", ""), sdk=provenance.get("sdk", ""),
                       fingerprint=provenance.get("fingerprint", ""),
                       deviceId=provenance.get("deviceId") or provenance.get("serial", ""))
            reported_iterations = benchmark.get("repeatIterations", benchmark.get("iterations"))
            per_iteration_overruns = []
            for metric_name in METRICS:
                metric = benchmark.get("sampledMetrics", {}).get(metric_name)
                if metric is None:
                    if not (metric_name == "frameOverrunMs" and provenance.get("sdk", 31) < 31):
                        warnings.append(f"{journey}/{entry}/{mode}: {metric_name} unavailable")
                    continue
                runs = sample_runs(metric)
                values = [value for run in runs for value in run]
                if runs:
                    row["iterations"] = len(runs)
                    row[metric_name + "_samples"] = len(values)
                else:
                    row.setdefault("iterations", reported_iterations)
                    warnings.append(f"{journey}/{entry}/{mode}: no raw {metric_name}; using SDK percentiles")
                for p in (50, 95, 99):
                    row[f"{metric_name}_p{p}"] = percentile(values, p) if values else metric.get(f"P{p}", metric.get(f"percentile{p}"))
                if runs and (metric_name == "frameOverrunMs" or not per_iteration_overruns):
                    per_iteration_overruns = [percentile(run, 95) for run in runs]
                    row["representativeMetric"] = metric_name
                if metric_name == "frameOverrunMs" and runs:
                    row["missedDeadlineFrames"] = sum(value > 0 for value in values)
            wall_files = list(directory.rglob(f"{journey}_{entry}_{mode}_journey.json"))
            if len(wall_files) == 1:
                wall = json.loads(wall_files[0].read_text())
                for p in (50, 95, 99):
                    row[f"readinessWallMs_p{p}"] = percentile(wall["readinessWallMs"], p)
                row["wallIterations"] = len(wall["readinessWallMs"])
            else:
                warnings.append(f"{journey}/{entry}/{mode}: missing or ambiguous readiness-wall samples")
            if per_iteration_overruns:
                worst = max(range(len(per_iteration_overruns)), key=lambda i:
                            per_iteration_overruns[i] if per_iteration_overruns[i] is not None else float('-inf'))
                row["representativeSlowIteration"] = worst
                traces = [trace for trace in directory.rglob("*.perfetto-trace")
                          if all(part in trace.name for part in (journey, entry, mode))]
                row["traceCandidates"] = " | ".join(str(trace.resolve()) for trace in sorted(traces))
                representative = [trace for trace in traces if f"_iter{worst:03d}_" in trace.name]
                row["representativeTrace"] = str(representative[0].resolve()) if len(representative) == 1 else ""
            rows.append(row)
    return rows, warnings


def render(rows, warnings, output, expected_iterations, diagnostic_variant=None):
    output.mkdir(parents=True, exist_ok=True)
    fields = list(dict.fromkeys(key for row in rows for key in row))
    with (output / "navigation.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)
    def values(row, metric):
        return " / ".join("n/a" if row.get(f"{metric}_p{p}") is None else f"{row[f'{metric}_p{p}']:.2f}"
                          for p in (50, 95, 99))
    lines = ["# Mobile navigation measurements", "",
             "All durations are milliseconds. Each cell is **P50 / P95 / P99**. Frame percentiles pool frames only within the named journey, entry kind, build, and compilation mode.", "",
             "Readiness wall time includes real data/IME waits and UIAutomator polling; it is not CPU work or a frame-stall metric. The fixed 500 ms capture tail is excluded from wall time. P95/P99 of ten wall samples are exploratory, not robust tail estimates.", "",
             "Ignore runs are diagnostic only: installed compilation is uncontrolled and these do not satisfy the None/Partial comparison. Android <12 has no deadline-overrun metric; representative traces use frame CPU P95 instead.", "",
             "| Build | Journey | Entry | Compilation | Iterations | Frame CPU | Frame deadline overrun | Readiness wall |",
             "|---|---|---|---|---:|---|---|---|"]
    for row in rows:
        lines.append(f"| {row['variant']} | {row['journey']} | {row['entry']} | {row['compilation']} | {row.get('iterations', 'n/a')} | {values(row, 'frameDurationCpuMs')} | {values(row, 'frameOverrunMs')} | {values(row, 'readinessWallMs')} |")
    lines += ["", "## Coverage and evidence", ""]
    missing = []
    variants = (diagnostic_variant,) if diagnostic_variant else ("benchmark", "benchmarkR8")
    modes = DIAGNOSTIC_MODES if diagnostic_variant else MODES
    for variant in variants:
        for journey in JOURNEYS:
            for entry in ENTRIES:
                for mode in modes:
                    matches = [r for r in rows if (r['variant'], r['journey'], r['entry'], r['compilation']) == (variant, journey, entry, mode)]
                    if len(matches) != 1 or matches[0].get('iterations') != expected_iterations:
                        missing.append(f"{variant}/{journey}/{entry}/{mode}")
    expected_cells = len(variants) * len(JOURNEYS) * len(ENTRIES) * len(modes)
    scope = 'Diagnostic' if diagnostic_variant else 'Controlled'
    lines.append(f"{scope} complete cells: {expected_cells - len(missing)}/{expected_cells}, requiring {expected_iterations} measured iterations each.")
    provenance_errors = []
    if diagnostic_variant and any(row['variant'] != diagnostic_variant or row['compilation'] != 'Ignore' for row in rows):
        provenance_errors.append("Diagnostic report contains an unexpected build or compilation mode.")
    for variant in ("benchmark", "benchmarkR8"):
        hashes = {row['appSha256'] for row in rows if row['variant'] == variant}
        if len(hashes) > 1:
            provenance_errors.append(f"Mixed APK hashes for {variant}; do not compare these as one build.")
    if len({row['driverSha256'] for row in rows}) > 1:
        provenance_errors.append("Mixed benchmark-driver APKs in the comparison.")
    if len({row['contentTag'] for row in rows}) > 1:
        provenance_errors.append("Mixed content selection in the comparison.")
    if len({(row.get('deviceId'), row.get('fingerprint')) for row in rows}) > 1:
        provenance_errors.append("Mixed device/OS identities; keep each phone in a separate report.")
    if missing:
        lines += ["", "Missing/incomplete/duplicated cells:", ""] + [f"- {cell}" for cell in missing]
    if warnings:
        lines += ["", "Warnings:", ""] + [f"- {warning}" for warning in warnings]
    if provenance_errors:
        lines += ["", "Provenance errors:", ""] + [f"- {error}" for error in provenance_errors]
    for row in rows:
        lines += ["", f"### {row['variant']} / {row['journey']} / {row['entry']} / {row['compilation']}", "",
                  f"- Native report: `{row['source']}`",
                  f"- APK SHA-256: `{row['appSha256']}`",
                  f"- Device: {row.get('model', 'unknown')}; {row.get('fingerprint', 'unknown')}",
                  f"- Representative selection metric: {row.get('representativeMetric', 'unavailable')} (iteration P95)",
                  f"- Representative slow iteration (zero based): {row.get('representativeSlowIteration', 'unavailable')}"]
        for trace in [row.get("representativeTrace", "")]:
            if trace:
                lines.append(f"- Trace: [{Path(trace).name}]({Path(trace).as_posix()})")
    (output / "navigation.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return missing + provenance_errors


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("results", type=Path, nargs='+')
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--expected-iterations", type=int, default=10)
    parser.add_argument("--require-complete", action="store_true")
    parser.add_argument("--diagnostic-variant", choices=("benchmark", "benchmarkR8"))
    args = parser.parse_args()
    rows, warnings = [], []
    for root in args.results:
        root_rows, root_warnings = summarize(root.resolve())
        rows.extend(root_rows)
        warnings.extend(root_warnings)
    missing = render(rows, warnings, args.output, args.expected_iterations, args.diagnostic_variant)
    print(f"Wrote {len(rows)} measurement cells to {args.output}; {len(missing)} incomplete cells.")
    return 2 if args.require_complete and missing else 0


if __name__ == "__main__":
    raise SystemExit(main())
