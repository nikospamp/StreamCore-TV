# Playback sources, progress and player events

All headless playback operations are available through `sdk.playback: PlaybackService`. The backend-agnostic SDK resolves media and manages progress; your application supplies the player, its events, rendering and navigation.

## Choose the operation by what you have

| You have | Operation | What it does |
| --- | --- | --- |
| A catalogue playback request | `sdk.playback.resolveSource(request)` | Checks authorization/content policy and resolves media information. It does not start a player. |
| A running player emitting events | `sdk.playback.createProgressRecorder(request, initialPositionMillis)` then `recorder.reportEvent(...)` | Processes events for that playback, applies checkpoint cadence and supplies timestamps. |
| A complete progress entry with your own timestamp | `sdk.playback.updateProgress(entry)` | Immediately applies the resume policy to that snapshot; saves or removes the entry. No event/cadence handling. |
| A request to read or delete saved progress | `getProgress`, `observeProgress`, `removeProgress` | Reads/observes/removes progress for the authorized profile. |

The recorder is the normal choice for player callbacks. It already performs the necessary persistence operation: do not also call `updateProgress` for the same event. Here, “recorder” means progress-event recording, not recording audio or video.

## What reportEvent actually does

After resolving the request and determining the resume position, create one recorder for that playback:

```kotlin
val recorder = sdk.playback.createProgressRecorder(
    request = request,
    initialPositionMillis = resumePositionMillis,
)
```

Creation captures the current profile activation and initializes a position bucket. It does not create a player, save data, launch a timer or subscribe to player events. The application must send those events explicitly.

| Event | With a known positive duration | With unknown/nonpositive duration |
| --- | --- | --- |
| `Periodic` | Attempts an update when the position enters a higher ten-second bucket than previously attempted. Other samples are ignored. | Ignores the sample and preserves saved progress. |
| `Checkpoint` | Attempts an update immediately, bypassing the periodic gate. Use for pause, seek or exit. | Ignores the sample and preserves saved progress. |
| `Completed` | Removes the saved progress entry. | Still removes the entry. |

An attempted update saves a resumable entry only when the position is **at least 30 seconds** and **strictly below 95%** of a positive duration. Otherwise it removes the existing entry. The recorder supplies `updatedAtMillis` from its clock.

For example, resuming at 45 seconds initializes bucket 4. Periodic samples at 45 and 49 seconds do nothing; 50 seconds attempts an update. A pause at 49 seconds still attempts an update because it is a `Checkpoint`. These are position buckets, not a wall-clock timer.

```kotlin
val result = recorder.reportEvent(
    event = StreamCorePlaybackProgressEvent.Checkpoint,
    positionMillis = playerPositionMillis,
    durationMillis = playerDurationMillis,
)
when (result) {
    is StreamCoreResult.Success -> Unit // Saved, removed or deliberately ignored.
    is StreamCoreResult.Failure -> {
        // Apply the application's non-blocking error handling.
        // A storage failure must not prevent leaving the player.
    }
}
```

Success is not a guarantee that a write occurred. Storage/context errors return `StreamCoreResult.Failure`; coroutine cancellation remains cancellation. The current StreamCore player continues its exit flow after a returned checkpoint failure.

The established cadence is unchanged: a periodic bucket advances when the attempt begins, even if persistence fails; no automatic retry is scheduled. A later `Checkpoint` can explicitly retry without entering a higher bucket. Checkpoints do not reset the periodic bucket, so seeking backwards does not restart periodic updates until that previously reached bucket is surpassed. Pause/seek/exit checkpoints remain available throughout.

## How updateProgress differs

`sdk.playback.updateProgress(entry)` receives an already-built `StreamCorePlaybackProgressEntry`, including content/profile IDs, snapshot, position, duration and timestamp. It applies the same resume eligibility rules immediately, without the recorder's bucket or event handling.

One deliberate existing distinction matters: **direct update with unknown/nonpositive duration removes prior progress**, while a recorder ignores an unknown-duration periodic/checkpoint event and preserves prior progress. Player metadata can be temporarily unknown during preparation, so normal player code should use the recorder.

Both paths enforce the same account/profile authorization and provider content rules. Neither API permits a caller to write into a different profile's partition. A recorder is bound to its original activation; after a profile change or re-entry, create a new recorder. The recorder has no rendering resources to close; cancel the application's player-event coroutine when that playback ends.

## Names before and after this migration

| Previous call | Current call |
| --- | --- |
| `sdk.playbackSources.resolve(request)` | `sdk.playback.resolveSource(request)` |
| `sdk.playbackProgress.openSession(request, position)` | `sdk.playback.createProgressRecorder(request, position)` |
| `recorder.record(event, position, duration)` | `recorder.reportEvent(event, position, duration)` |
| `sdk.playbackProgress.record(entry)` | `sdk.playback.updateProgress(entry)` |
| `sdk.playbackProgress.get / observe / remove` | `sdk.playback.getProgress / observeProgress / removeProgress` |

The two former `record` methods had different inputs and responsibilities. Their new names expose that distinction. This migration changes the public Kotlin surface and application wiring, not progress thresholds, persisted keys/formats, playback sources or user data.
