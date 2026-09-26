[![License](https://img.shields.io/github/license/toolarium/toolarium-temporality-handler)](https://github.com/toolarium/toolarium-temporality-handler/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.toolarium/toolarium-temporality-handler/1.0.3)](https://search.maven.org/artifact/com.github.toolarium/toolarium-temporality-handler/1.0.3/jar)
[![javadoc](https://javadoc.io/badge2/com.github.toolarium/toolarium-temporality-handler/javadoc.svg)](https://javadoc.io/doc/com.github.toolarium/toolarium-temporality-handler)

# toolarium-temporality-handler

A Java library that manages the complete lifecycle of time-bounded (temporal) records. Instead of
overwriting data in place, it stores each version of a record with a validity interval (`validFrom` /
`validTill`) and automatically splits, terminates, or extends those intervals whenever a new version
is written.

The library handles all the edge cases of temporal data management (eight canonical cases, see below).
Your application only needs to supply a thin DAO adapter — the library drives all the insert, update,
terminate, and delete decisions.

---

## Table of Contents

1. [Core concepts](#core-concepts)
2. [Quick start](#quick-start)
3. [API reference](#api-reference)
   - [ITemporalityRecord](#itemporalityrecord)
   - [IDAOService](#idaoservice)
   - [TemporalityActionType](#temporalityactiontype)
   - [TemporalityHandlerFactory](#temporalityhandlerfactory)
4. [Temporal cases](#temporal-cases)
5. [Step-by-step example](#step-by-step-example)
6. [What the library covers](#what-the-library-covers)
7. [What the library does NOT cover](#what-the-library-does-not-cover)
8. [Thread safety](#thread-safety)
9. [Dependency setup](#dependency-setup)
10. [Built with](#built-with)
11. [Versioning](#versioning)

---

## Core concepts

| Concept | Description |
|---|---|
| **Data key** | The logical identity of a piece of data (e.g. a configuration key, an entity ID). Multiple versions of the same data key can coexist, each covering a distinct time interval. |
| **Primary key** | The physical storage key of a single record row. Each version has its own primary key. `null` means the storage layer should assign a new one. |
| **validFrom** | Inclusive start of the interval during which this version is in effect. |
| **validTill** | Exclusive end of the interval. Use `Instant.MAX` (or `9999-12-31T00:00:00Z`) for an open-ended ("forever") record. Both are accepted and normalised to the canonical maximum before storage — `Instant.MAX` is never written to the DAO. The canonical maximum defaults to `9999-12-31T00:00:00Z` and can be changed via `TemporalityHandlerFactory.setMaxValidTill(Instant)`. |

---

## Quick start

### 1 — Implement `ITemporalityRecord`

Your domain object must implement `ITemporalityRecord` and `Cloneable`:

```java
public class ConfigRecord implements ITemporalityRecord<ConfigRecord, Long, String>, Cloneable {

    private Long   primaryKey;
    private String key;        // data key
    private String value;
    private Instant validFrom;
    private Instant validTill;

    // constructor, getters, setters ...

    @Override public Long    getPrimaryKey()            { return primaryKey; }
    @Override public void    setPrimaryKey(Long key)    { this.primaryKey = key; }
    @Override public String  getDataKey()               { return key; }
    @Override public Instant getValidFrom()             { return validFrom; }
    @Override public void    setValidFrom(Instant v)    { this.validFrom = v; }
    @Override public Instant getValidTill()             { return validTill; }
    @Override public void    setValidTill(Instant v)    { this.validTill = v; }

    @Override
    public ConfigRecord clone() {
        try { return (ConfigRecord) super.clone(); }
        catch (CloneNotSupportedException e) { throw new InternalError(e); }
    }

    // equals() and hashCode() must compare ALL fields including primaryKey
}
```

> **Important:** `equals()` must include `primaryKey` in its comparison. The handler uses
> `equals()` to detect whether a record is truly identical to an existing one (Case A).

### 2 — Implement `IDAOService`

Bridge between the library and your persistence layer:

```java
public class ConfigRecordDAO implements IDAOService<ConfigRecord> {

    @Override
    public void write(TemporalityActionType actionType, ConfigRecord record) {
        // actionType tells you whether this is a CREATE, UPDATE, or TERMINATE —
        // use it for audit logs or optimistic-lock decisions.
        // record.getPrimaryKey() == null  →  INSERT (assign a new PK)
        // record.getPrimaryKey() != null  →  UPDATE existing row
        myRepository.save(record);
    }

    @Override
    public void delete(ConfigRecord record) {
        myRepository.deleteById(record.getPrimaryKey());
    }

    @Override
    public List<ConfigRecord> search(ConfigRecord filter) {
        // Return all stored records for filter.getDataKey(), regardless of time range.
        return myRepository.findAllByKey(filter.getDataKey());
    }
}
```

### 3 — Write records

```java
IDAOService<ConfigRecord> dao = new ConfigRecordDAO();
ITemporalityHandler handler = TemporalityHandlerFactory.getInstance().getTemporalityHandler();

// First version: valid from 2024-01-01 until forever
handler.writeTemporlityRecord(
    new ConfigRecord("smtpHost", "mail.example.com",
                     Instant.parse("2024-01-01T00:00:00Z"), Instant.MAX),
    dao);

// New version: valid from 2025-06-01 until forever
// → existing record is automatically terminated at 2025-06-01
handler.writeTemporlityRecord(
    new ConfigRecord("smtpHost", "relay.example.com",
                     Instant.parse("2025-06-01T00:00:00Z"), Instant.MAX),
    dao);
```

After these two calls the storage contains:

```
smtpHost = "mail.example.com"   [2024-01-01, 2025-06-01)
smtpHost = "relay.example.com"  [2025-06-01, MAX)
```

---

## API reference

### ITemporalityRecord

```
ITemporalityRecord<R, P, D>
```

| Type param | Role |
|---|---|
| `R` | The concrete record type (self-referential for `clone()`) |
| `P` | Primary key type (e.g. `Long`, `UUID`) |
| `D` | Data key type (e.g. `String`, custom enum) |

| Method | Description |
|---|---|
| `P getPrimaryKey()` | Physical storage key; `null` when the record has not been persisted yet |
| `void setPrimaryKey(P key)` | Called by the handler to assign or clear the primary key |
| `D getDataKey()` | Logical identity used to locate existing versions |
| `Instant getValidFrom()` | Inclusive start of validity |
| `void setValidFrom(Instant)` | Used when splitting or shifting a record's interval |
| `Instant getValidTill()` | Exclusive end of validity; use `Instant.MAX` or the configured canonical maximum (`9999-12-31T00:00:00Z` by default) for open-ended |
| `void setValidTill(Instant)` | Used when truncating a record's interval |
| `R clone()` | Deep copy; called before every mutation so originals are never changed in-place |

### IDAOService

```
IDAOService<R>
```

| Method | Description |
|---|---|
| `void write(TemporalityActionType, R)` | Persist a record. If `getPrimaryKey()` is null the storage layer must assign a new key. |
| `void delete(R)` | Remove the record identified by its primary key. |
| `List<R> search(R filter)` | Return **all** stored records for `filter.getDataKey()`. The handler evaluates time ranges itself; do not pre-filter by date. |

### TemporalityActionType

Passed to `IDAOService.write()` as a hint for audit/logging purposes. It does not change what the
handler writes — it describes the semantic intent of the write:

| Value | Meaning |
|---|---|
| `CREATE` | A brand-new record with no existing predecessor for this time slot |
| `UPDATE` | Replaces an existing record in-place (same primary key, same interval) |
| `TERMINATE` | Shortens or replaces an existing record (same primary key, new data or different interval) |

### TemporalityHandlerFactory

```java
ITemporalityHandler handler = TemporalityHandlerFactory.getInstance().getTemporalityHandler();
```

- Singleton factory, thread-safe (initialization-on-demand holder).
- Returns a single shared `TemporalityHandlerImpl` — safe to call concurrently from any thread.

**Entry point:**

```java
int written = handler.writeTemporlityRecord(record, daoService);
```

Throws `IllegalArgumentException` if `record`, `daoService`, `validFrom`, or `validTill` is `null`,
or if `validFrom >= validTill` (reversed or zero-length interval; the only exception is when
`validTill` is `Instant.MAX` or the configured canonical maximum — see `setMaxValidTill`).

Returns the number of DAO operations performed (writes + deletes). A return value of `0` means the
record was identical to what is already stored (Case A — no-op).

**Configuring the canonical maximum date (optional):**

```java
// set a custom maximum (call once at startup, before concurrent writes)
TemporalityHandlerFactory.getInstance()
    .setMaxValidTill(Instant.parse("2099-12-31T00:00:00Z"));

// read the current maximum
Instant max = TemporalityHandlerFactory.getInstance().getMaxValidTill();
```

Any `validTill` strictly greater than the configured maximum is capped to it automatically — both
for incoming records and for records returned by `IDAOService.search()`. The default maximum is
`9999-12-31T00:00:00Z`. Call `setMaxValidTill` once at application startup, before concurrent
writes begin.

---

## Temporal cases

The diagram notation is: time flows left → right. Each row shows the **before** state (1) and the
**after** state (2) once the new record `B` is written.

---

### Case A — Identical record (no-op)

```
1)  <-------(A)------->
2)  <-------(A)------->   (unchanged)
```

New record is equal to the stored record (same data key, same primary key, same interval, same
data). Nothing is written. `writeTemporlityRecord` returns `0`.

---

### Case B — New record starts after existing ends

```
1)  <--(A)-->
2)  <--(A)-->  <--(B)-->
```

The new record's `validFrom` is after the existing record's `validTill`. There is no overlap.
The existing record is not touched; the new record is inserted.

---

### Case C — New record ends before or exactly when existing starts

```
1)           <--(A)-->
2)  <--(B)-->  <--(A)-->
```

The new record's `validTill` is less than or equal to the existing record's `validFrom`. There is
no overlap. The existing record is not touched; the new record is inserted.

---

### Case D — New record starts inside existing, ends later

```
1)  <------(A)------>
2)  <-(A)-><----(B)---->
```

The existing record is truncated: its `validTill` is set to the new record's `validFrom`.
The new record is then inserted.

---

### Case E — New record starts earlier, ends inside existing

```
1)        <------(A)-->
2)  <-(B)--><----(A)-->
```

The existing record's `validFrom` is shifted forward to the new record's `validTill`.
The new record is inserted.

Special sub-case **E1**: if the new record's `validTill` equals the existing record's `validTill`,
the existing record is updated in-place with the new data and the earlier `validFrom`.

---

### Case F — New record is fully contained within existing

```
1)  <-----------(A)----------->
2)  <-(A)-><-(B)-><-----(A)--->
```

The existing record is split into two parts:

- **Left part**: original record, `validTill` truncated to new record's `validFrom`.
- **New record**: inserted as-is.
- **Right part**: clone of original record, `validFrom` advanced to new record's `validTill`, new
  primary key assigned by storage.

---

### Case G — New record spans multiple existing records

```
1)  <-(A)-><-(B)-><-(C)->
2)  <----------(D)------->
```

All existing records that are fully contained within the new record's interval are **deleted**.
This includes records that start after the new record's `validFrom` and end at or before the new
record's `validTill` (equal `validTill` is covered). The new record is inserted covering the
entire span.

---

### Case H — Same `validFrom`, new record ends earlier

```
1)  <-----------(A)----------->
2)  <-----(B)----><-----(A)--->
```

The existing record is updated in-place (primary key preserved, data replaced with new record's
data, `validTill` updated). If the existing record extended beyond the new record's `validTill`, a
**remainder** clone of the old record is inserted with `validFrom = newRecord.validTill` and a new
primary key.

If the two intervals are identical (same `validFrom` and same `validTill`), the record is updated
in-place (functionally equivalent to Case A with changed data).

---

## Step-by-step example

Below is a complete, self-contained walkthrough that demonstrates Cases D, F, and H together.

```java
IDAOService<ConfigRecord> dao    = new ConfigRecordDAO();
ITemporalityHandler       handler = TemporalityHandlerFactory.getInstance().getTemporalityHandler();

Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
Instant t2 = Instant.parse("2025-01-01T00:00:00Z");
Instant t3 = Instant.parse("2026-01-01T00:00:00Z");

// Step 1: create initial record [t1, MAX)
handler.writeTemporlityRecord(new ConfigRecord("host", "alpha", t1, Instant.MAX), dao);
// Storage: alpha [t1, MAX)

// Step 2: write a record that starts inside → Case D
// alpha is truncated; beta is appended
handler.writeTemporlityRecord(new ConfigRecord("host", "beta", t2, Instant.MAX), dao);
// Storage: alpha [t1, t2)
//          beta  [t2, MAX)

// Step 3: insert a short record inside beta's range → Case F
// beta is split around gamma
handler.writeTemporlityRecord(new ConfigRecord("host", "gamma", t2, t3), dao);
// Storage: alpha [t1, t2)     (unchanged)
//          gamma [t2, t3)     (new)
//          beta  [t3, MAX)    (remainder)

// Step 4: replace gamma with the same validFrom but an earlier validTill → Case H
Instant t25 = Instant.parse("2025-07-01T00:00:00Z");
handler.writeTemporlityRecord(new ConfigRecord("host", "delta", t2, t25), dao);
// Storage: alpha [t1,  t2)    (unchanged)
//          delta [t2,  t25)   (replaced gamma)
//          gamma [t25, t3)    (remainder of gamma)
//          beta  [t3,  MAX)   (unchanged)
```

---

## What the library covers

- **All temporal record mutations** via a single method: `writeTemporlityRecord`.
- **Eight canonical overlap cases** (A–H) including remainder preservation when an existing record
  extends beyond a newly written record that shares the same `validFrom`.
- **Any record type** via generics — primary key, data key, and record type are all type parameters.
- **Any persistence backend** via the `IDAOService` interface (relational DB, NoSQL, in-memory map, etc.).
- **Audit hints** via `TemporalityActionType` passed to every `write()` call.
- **Open-ended intervals** using `Instant.MAX` or `9999-12-31T00:00:00Z` as a sentinel for "valid forever"; both are normalised to the canonical maximum before storage.
- **Automatic max-date enforcement**: any `validTill` exceeding the configured maximum is capped; DB entries beyond the maximum are corrected or deleted on the next write touching that key.
- **Configurable canonical maximum date** via `TemporalityHandlerFactory.setMaxValidTill(Instant)` (default `9999-12-31T00:00:00Z`).

---

## What the library does NOT cover

| Concern | Notes |
|---|---|
| **Persistence** | You must implement `IDAOService`. The library has no knowledge of databases, ORMs, or repositories. |
| **Transactions** | Temporal mutations may result in multiple DAO calls. Wrapping them in a transaction is the caller's responsibility. |
| **Concurrency / optimistic locking** | Concurrent writes to the same data key are not coordinated by the library. Use database-level locking or application-level synchronization. |
| **Bi-temporal modelling** | The library manages one time axis (valid time). It does not track transaction time / system time (the second axis of full bi-temporal modelling). |
| **Querying by point in time** | Reading "which record was valid at time T?" is not part of the library. Implement range queries in your `IDAOService.search()` or in a separate query layer. |
| **Cascade / referential integrity** | The library operates on a single data key at a time. Cross-key constraints are the caller's responsibility. |
| **Schema migration** | No DDL or schema tooling is provided. |

---

## Thread safety

`TemporalityHandlerFactory.getInstance()` uses an initialization-on-demand holder, so the singleton
is created lazily and safely without any synchronization cost after the first access.

`getTemporalityHandler()` returns a single shared `TemporalityHandlerImpl` instance.
`writeTemporlityRecord` itself carries no per-call mutable state and can be called concurrently
from any number of threads without additional synchronization.

The one mutable field, `maxValidTill`, is declared `volatile`. `setMaxValidTill` should be called
once at application startup, before concurrent writes begin. Changing it while writes are in
flight is safe at the JVM level but may cause some in-flight calls to use the old value and others
the new one.

Concurrent writes to the **same data key** still require external coordination (see
[What the library does NOT cover](#what-the-library-does-not-cover)).

---

## Dependency setup

### Gradle

```groovy
dependencies {
    implementation "com.github.toolarium:toolarium-temporality-handler:1.0.3"
}
```

### Maven

```xml
<dependency>
    <groupId>com.github.toolarium</groupId>
    <artifactId>toolarium-temporality-handler</artifactId>
    <version>1.0.3</version>
</dependency>
```

---

## Built with

* [cb](https://github.com/toolarium/common-build) — The toolarium common build

---

## Versioning

We use [SemVer](http://semver.org/) for versioning. For available versions, see the
[tags on this repository](https://github.com/toolarium/toolarium-temporality-handler/tags).
