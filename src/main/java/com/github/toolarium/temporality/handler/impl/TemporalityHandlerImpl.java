/*
 * TemporalityHandlerImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler.impl;

import com.github.toolarium.temporality.handler.IDAOService;
import com.github.toolarium.temporality.handler.ITemporalityHandler;
import com.github.toolarium.temporality.handler.ITemporalityRecord;
import com.github.toolarium.temporality.handler.TemporalityActionType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Implements {@link ITemporalityHandler}.
 *
 * <code>
 * {@code
 * Case A: 1) <--(A)-->
 *         2) <--(A)-->
 *
 * Case B: 1) <--(A)-->
 *         2) <--(A)--> <--(B)-->
 *
 * Case C: 1)           <--(A)-->
 *         2) <--(B)--> <--(A)-->
 *
 * Case D: 1) <--(A)----->
 *         2) <--(A)--><--(B)-->
 *
 * Case E: 1)       <------(A)-->
 *         2) <--(B)--><---(A)-->
 *
 * Case F: 1) <------(A)-------->
 *         2) <-(A)-><-(B)-><(A)>
 *
 * Case G: 1) <-(A)-><-(B)-><-C->
 *         2) <-------(D)------->
 *
 * Case H: 1) <---(A)--->
 *         2) <---(A)-->
 *
 * Case E1: 1)      <------(A)-->
 *          2)     <---(B)------>
 * }</code>
 */
public final class TemporalityHandlerImpl implements ITemporalityHandler {
    private static final Logger log = LoggerFactory.getLogger(TemporalityHandlerImpl.class);
    private volatile Instant maxValidTill = LocalDateTime.of(9999, Month.DECEMBER, 31, 0, 0, 0).toInstant(ZoneOffset.UTC);


    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityHandler#writeTemporlityRecord(com.github.toolarium.temporality.handler.ITemporalityRecord, com.github.toolarium.temporality.handler.IDAOService)
     */
    @Override
    public <R extends ITemporalityRecord<R, P, D>, P, D> int writeTemporlityRecord(R record, IDAOService<R> daoService) {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        if (daoService == null) {
            throw new IllegalArgumentException("daoService must not be null");
        }
        if (record.getValidFrom() == null) {
            throw new IllegalArgumentException("validFrom must not be null");
        }
        if (record.getValidTill() == null) {
            throw new IllegalArgumentException("validTill must not be null");
        }
        if (!record.getValidTill().equals(Instant.MAX) && !record.getValidFrom().isBefore(record.getValidTill())) {
            throw new IllegalArgumentException("validFrom must be before validTill");
        }
        final Instant localMax = maxValidTill;
        final R effectiveRecord;
        if (record.getValidTill().isAfter(localMax)) {
            R cloned = record.clone();
            cloned.setValidTill(localMax);
            effectiveRecord = cloned;
        } else {
            effectiveRecord = record;
        }
        if (log.isDebugEnabled()) {
            log.debug("Write temporality record: {}", toString(effectiveRecord));
        }
        int result = 0;

        List<R> resultList = new ArrayList<>(readTemporalityRecordList(daoService, effectiveRecord));
        for (int i = resultList.size() - 1; i >= 0; i--) {
            R entry = resultList.get(i);
            if (entry.getValidFrom() != null && !entry.getValidFrom().isBefore(localMax)) {
                result += deleteTemporalRecord(daoService, entry, () -> "Delete out-of-range entry (validFrom >= maxValidTill): " + toString(entry));
                resultList.remove(i);
            } else if (entry.getValidTill() == null || entry.getValidTill().isAfter(localMax)) {
                R normalized = entry.clone();
                normalized.setValidTill(localMax);
                result += writeTemporalRecord(daoService, TemporalityActionType.TERMINATE, normalized, () -> "Normalize validTill to maxValidTill: " + toString(normalized));
                resultList.set(i, normalized);
            }
        }
        if (!resultList.isEmpty()) {
            // terminate entries
            boolean ignore = false;
            for (R existingEntry : resultList) {
                if (log.isDebugEnabled()) {
                    log.debug("Check record {} ({}) for update...", effectiveRecord.getDataKey(), effectiveRecord.getPrimaryKey());
                }

                KeyValueHolder<Integer, Boolean> k = updateExistingRecords(daoService, effectiveRecord, existingEntry);
                result += k.getKey();
                if (Boolean.TRUE.equals(k.getValue())) {
                    ignore = true;
                }
            }

            // write the temporal record
            if (!ignore) {
                result += writeTemporalRecord(daoService, TemporalityActionType.UPDATE, effectiveRecord, () -> "Update entry: " + toString(effectiveRecord));
            }

        } else {
            result += writeTemporalRecord(daoService, TemporalityActionType.CREATE, effectiveRecord, () -> "Create entry: " + toString(effectiveRecord));
        }

        return result;
    }

    /**
     * Get the canonical maximum validTill value.
     *
     * @return the maximum validTill instant
     */
    public Instant getMaxValidTill() {
        return maxValidTill;
    }


    /**
     * Set the canonical maximum validTill value. Any record whose validTill is strictly after
     * this instant is capped to this value before processing. Defaults to 9999-12-31T00:00:00Z.
     *
     * @param maxValidTill the maximum validTill instant; must not be null and must be after the epoch (1970-01-01T00:00:00Z)
     * @throws IllegalArgumentException if maxValidTill is null or not after the epoch
     */
    public void setMaxValidTill(Instant maxValidTill) {
        if (maxValidTill == null) {
            throw new IllegalArgumentException("maxValidTill must not be null");
        }
        if (!maxValidTill.isAfter(Instant.EPOCH)) {
            throw new IllegalArgumentException("maxValidTill must be after the epoch (1970-01-01T00:00:00Z)");
        }
        this.maxValidTill = maxValidTill;
    }


    /**
     * Update existing records
     *
     * @param <R> the record type.
     * @param <P> the generic primary key type.
     * @param <D> the generic data key type.
     * @param daoService the dao service
     * @param record the record
     * @param existingEntry the existing record
     * @return the number of updated records
     */
    protected <R extends ITemporalityRecord<R, P, D>, P, D> KeyValueHolder<Integer, Boolean> updateExistingRecords(IDAOService<R> daoService, R record, R existingEntry) {
        int result = 0;
        Boolean ignoreRecord = Boolean.FALSE;

        if (isNotEmpty(existingEntry.getValidFrom()) && isNotEmpty(record.getValidFrom())
            && existingEntry.getValidFrom().isBefore(record.getValidFrom())) { // <
            // existing entries starting earlier
            if (existingEntry.getValidTill().isBefore(record.getValidFrom())) { // <
                // Case B: Add
                if (log.isDebugEnabled()) {
                    log.debug("Keep original entry, because it is before (Case B): {}", toString(existingEntry));
                }
            } else if (existingEntry.getValidTill().isAfter(record.getValidTill())) { // >
                // Case F: Insert
                if (log.isDebugEnabled()) {
                    log.debug("Insert entry, because it is before and after (Case F): {}", toString(existingEntry));
                }
                R entry1 = existingEntry.clone();
                entry1.setValidTill(record.getValidFrom());
                result += writeTemporalRecord(daoService, TemporalityActionType.TERMINATE, entry1, () -> "Terminate entry (Case F): " + toString(entry1));

                R entry2 = existingEntry.clone();
                entry2.setPrimaryKey(null); // get new primary key for this entry
                entry2.setValidFrom(record.getValidTill());
                result += writeTemporalRecord(daoService, TemporalityActionType.CREATE, entry2, () -> "Add new entry at the end (Case F): " + toString(entry2));
            } else {
                // Case D: Terminate
                R entry = existingEntry.clone();
                entry.setValidTill(record.getValidFrom());
                result += writeTemporalRecord(daoService, TemporalityActionType.TERMINATE, entry, () -> "Terminate entry (Case D): " + toString(entry));
            }
        } else {
            // existing entries which starting now or in future
            if (existingEntry.getValidTill().isBefore(record.getValidTill()) // <
                || (isNotEmpty(existingEntry.getValidFrom()) && existingEntry.getValidFrom().isAfter(record.getValidFrom()) && !existingEntry.getValidTill().isAfter(record.getValidTill()))) {
                // Case G: Reduce
                // delete entries which are part of the current entry (including entries that start later but end at the same time)
                result += deleteTemporalRecord(daoService, existingEntry, () -> "Delete entry, because new entry valid till has changed (Case G): " + toString(existingEntry));
            } else if (isNotEmpty(existingEntry.getValidFrom()) && !existingEntry.getValidFrom().isBefore(record.getValidTill())) { // >=
                // Case C: Add
                // ignore entries which starting in future or exactly at the new entry's end
                if (log.isDebugEnabled()) {
                    log.debug("Keep original entry, because it is in future (Case C): {}", toString(existingEntry));
                }
            } else {
                // check if it is the same
                R compareEntry = record.clone();
                compareEntry.setPrimaryKey(existingEntry.getPrimaryKey());

                if (existingEntry.equals(compareEntry)) {
                    // Case A: same record
                    // ignore already existing entry!
                    if (log.isDebugEnabled()) {
                        log.debug("Identical entry found on database, ignore writing (Case A): [{}] == [{}].", toString(record), toString(existingEntry));
                    }
                    ignoreRecord = Boolean.TRUE;
                } else if (isNotEmpty(existingEntry.getValidFrom()) && isNotEmpty(record.getValidFrom())
                          && isNotEmpty(existingEntry.getDataKey()) && existingEntry.getDataKey().equals(record.getDataKey())
                          && existingEntry.getValidFrom().equals(record.getValidFrom())
                          && (record.getPrimaryKey() == null || existingEntry.getPrimaryKey().equals(record.getPrimaryKey()))) {
                    // Case H: terminate -> delete
                    R entry = record.clone();
                    entry.setPrimaryKey(existingEntry.getPrimaryKey());
                    result += writeTemporalRecord(daoService, TemporalityActionType.TERMINATE, entry, () -> "Terminate entry (Case H): " + toString(existingEntry) + " -> " + toString(record));
                    if (existingEntry.getValidTill().isAfter(record.getValidTill())) {
                        R remainder = existingEntry.clone();
                        remainder.setPrimaryKey(null);
                        remainder.setValidFrom(record.getValidTill());
                        result += writeTemporalRecord(daoService, TemporalityActionType.CREATE, remainder, () -> "Add remainder entry (Case H): " + toString(remainder));
                    }
                    ignoreRecord = Boolean.TRUE;
                } else {
                    // Case E: terminate
                    if (existingEntry.getValidTill().equals(record.getValidTill())) { // ==
                        R entry = record.clone();
                        entry.setPrimaryKey(existingEntry.getPrimaryKey());
                        result += writeTemporalRecord(daoService, TemporalityActionType.UPDATE, entry, () -> "Terminate entry (Case E1): " + toString(existingEntry) + " -> " + toString(record));
                        ignoreRecord = Boolean.TRUE;
                    } else {
                        R entry = existingEntry.clone();
                        entry.setValidFrom(record.getValidTill());

                        // write the temporal record
                        result += writeTemporalRecord(daoService, TemporalityActionType.TERMINATE, entry, () -> "Terminate entry (Case E): " + toString(existingEntry) + " -> " + toString(entry));
                    }
                }
            }
        }

        return new KeyValueHolder<Integer, Boolean>(result, ignoreRecord);
    }


    /**
     * Write a temporal record
     *
     * @param <R> the generic record type.
     * @param daoService the dao service
     * @param temporalityActionType the temporality action type
     * @param record the record
     * @param logComment the log comment
     * @return the number of written entries
     */
    protected <R> int writeTemporalRecord(IDAOService<R> daoService, TemporalityActionType temporalityActionType, R record, Supplier<String> logComment) {
        try {
            if (log.isDebugEnabled() && logComment != null) {
                log.debug(logComment.get());
            }

            daoService.write(temporalityActionType, record);
            return 1;
        } catch (RuntimeException e) {
            if (log.isDebugEnabled()) {
                log.debug("Could not write: {}\n-> {}", record, e.getMessage(), e);
            }
            throw e;
        }
    }


    /**
     * Delete a temporal record
     *
     * @param <T> the generic type
     * @param daoService the dao service
     * @param record the record
     * @param logComment the log comment
     * @return the number of written entries
     */
    protected <T> int deleteTemporalRecord(IDAOService<T> daoService, T record, Supplier<String> logComment) {
        try {
            if (log.isDebugEnabled() && logComment != null) {
                log.debug(logComment.get());
            }

            daoService.delete(record);
            return 1;
        } catch (RuntimeException e) {
            if (log.isDebugEnabled()) {
                log.debug("Could not delete: {}\n-> {}", record, e.getMessage(), e);
            }
            throw e;
        }
    }


    /**
     * Read the temporality records
     *
     * @param <T> the generic type
     * @param recordFilter the record
     * @param daoService the service
     * @return the result
     * @throws RuntimeException if the DAO search fails
     */
    protected <T> List<T> readTemporalityRecordList(IDAOService<T> daoService, T recordFilter) {
        try {
            List<T> result = daoService.search(recordFilter);
            if (result == null) {
                return Collections.emptyList();
            }
            return result;
        } catch (RuntimeException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Could not read current configuration: {}", ex.getMessage(), ex);
            }
            throw ex;
        }
    }


    /**
     * Convert a temporality record into a string
     *
     * @param record the record
     * @return the srting representation
     */
    protected String toString(ITemporalityRecord<?, ?, ?> record) {
        String dataKey = "(n/a)";
        if (record.getDataKey() != null) {
            dataKey = record.getDataKey().toString();
        }

        String primaryKey = "(n/a)";
        if (record.getPrimaryKey() != null) {
            primaryKey = "(" + record.getPrimaryKey().toString() + ")";
        }

        String from = "(n/a)";
        if (record.getValidFrom() != null)  {
            from = DateTimeFormatter.ISO_INSTANT.format(record.getValidFrom());
        }

        String to = "(n/a)";
        if (record.getValidTill() != null) {
            to = DateTimeFormatter.ISO_INSTANT.format(record.getValidTill());
        }

        return "" + dataKey + " " + primaryKey + ", " + from + " - " + to;

    }


    /**
     * Check if given object is not empty
     *
     * @param obj the object
     * @return true if it is not empty
     */
    protected boolean isNotEmpty(Object obj) {
        return obj != null;
    }
}
