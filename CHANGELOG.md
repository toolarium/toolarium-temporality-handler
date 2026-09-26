# toolarium-temporality-handler

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [ 1.1.1 ] - 2026-09-26

## [ 1.1.0 ] - 2026-09-26
### Added
- Added `ITemporalityNormalizer` to reconcile already-persisted overlapping records for one data key into a consistent, gap-free timeline: records that survived unchanged receive `UPDATE`, replaced records receive `TERMINATE` (with `validTill` set to `validFrom`), and new trimmed segments are inserted with `CREATE`.
- Added `TemporalityTimeline` (sweep-line timeline computation) and `TemporalityOverlapCheck` (pairwise overlap detection).
- Added `TemporalityPriority` utility class in `com.github.toolarium.temporality.handler.util` with priority-comparator factories: `latestStart()`, `byGroup()`, and `groupsByCurrentStart()`.
- Added `TemporalityHandlerFactory.getTemporalityNormalizer()` as the factory entry point for the normalizer.

## [ 1.0.3 ] - 2026-09-26
### Fixed
- Fixed Case G: a future record `[futureDate, MAX]` was not deleted when a new open-ended record `[now, MAX]` was written, causing both to be valid simultaneously from `futureDate` onward. The new record now correctly supersedes the future record, restoring the one-value-at-any-time invariant.
- Fixed Case C boundary: existing record starting exactly at the new record's `validTill` (adjacent, zero gap) fell through to Case E and triggered an unnecessary DAO write. Case C now uses `!isBefore` (`>=`) instead of `isAfter` (`>`).

### Changed
- Canonical maximum `validTill` is now `9999-12-31T00:00:00Z`. Both `Instant.MAX` and any near-MAX value (year ≥ 9999, December 31) supplied as input are accepted as open-ended sentinels and normalised to this canonical value before any logic runs. The DAO therefore never receives `Instant.MAX` as a `validTill`.
- Existing DB entries returned by `IDAOService.search()` whose `validTill` exceeds the configured maximum are automatically capped and written back before case logic runs. Entries whose `validFrom` is at or beyond the maximum are deleted on the same pass.
- The canonical maximum `validTill` can be overridden at runtime via `TemporalityHandlerFactory.getInstance().setMaxValidTill(Instant)`. Default remains `9999-12-31T00:00:00Z`.

## [ 1.0.2 ] - 2026-09-25
### Changed
- Input validation added to `writeTemporlityRecord`: `record`, `daoService`, `validFrom`, `validTill`, and a valid (non-reversed) interval are now enforced at the entry point.
- Removed `Serializable` from `TemporalityHandlerImpl`; the class is stateless and serialization is not a library requirement.

### Fixed
- Fixed `writeTemporlityRecord` dropping the remainder of an existing record when the new record has the same `validFrom` but an earlier `validTill` (Case H). The part of the existing record after the new record's `validTill` is now preserved as a separate entry, consistent with the split behaviour applied in Case F.
- Fixed `isMaxInstant` using `getDayOfYear()` (returns 1–366, never reached 9999 — dead code) instead of `getYear()`; also replaced `ZoneId.systemDefault()` with `ZoneOffset.UTC` to make the check timezone-independent.
- Fixed `TemporalityHandlerFactory.getInstance()` race condition: non-volatile static field could allow two threads to each create a separate factory instance on first access. Replaced with an initialization-on-demand holder.
- Fixed `ThreadLocal` memory leak in `TemporalityHandlerFactory`: handler instances were never removed in thread-pool environments. Replaced with a single shared handler instance (`TemporalityHandlerImpl` is stateless).
- Fixed `readTemporalityRecordList` silently treating a DAO `search()` failure as "no records found", which caused a ghost `CREATE` instead of propagating the error. Exception is now re-thrown after logging.
- Fixed `writeTemporalRecord` and `deleteTemporalRecord` swallowing DAO exceptions, leaving temporal data in an inconsistent state with no caller feedback. Exceptions are now re-thrown after logging.
- Fixed potential `ConcurrentModificationException` when iterating the list returned by `IDAOService.search()` while the loop body calls `write()`/`delete()`. The result is now defensively copied into a new `ArrayList` before iteration.

## [ 1.0.1 ] - 2024-06-28
### Changed
- Update build dependencies.

## [ 1.0.0 ] - 2022-02-17
### Fixed
- Update build dependencies.

## [ 0.9.2 ] - 2021-06-17
### Fixed
- Stability fix.

## [ 0.9.1 ] - 2021-06-17
### Fixed
- First CREATE entry.

## [ 0.9.0 ] - 2021-06-09
### Changed
- Setup initial version.
