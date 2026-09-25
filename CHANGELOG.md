# toolarium-temporality-handler

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [ 1.0.3 ] - 2026-09-25

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
