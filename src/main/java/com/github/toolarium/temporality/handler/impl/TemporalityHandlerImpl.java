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
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
public final class TemporalityHandlerImpl implements ITemporalityHandler, Serializable {
    private static final long serialVersionUID = -1597927371967741727L;
    private static final Logger log = LoggerFactory.getLogger(TemporalityHandlerImpl.class);
    

    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityHandler#writeTemporlityRecord(com.github.toolarium.temporality.handler.ITemporalityRecord, com.github.toolarium.temporality.handler.IDAOService)
     */
    @Override
    public <R extends ITemporalityRecord<R, P, D>, P, D> int writeTemporlityRecord(R record, IDAOService<R> daoService) {
        log.debug("Write temporality record: " + toString(record));
        int result = 0;

        boolean ignore = false;

        List<R> resultList = readTemporalityRecordList(daoService, record);
        if (resultList != null && resultList.size() > 0) {
            // terminate entries
            for (R existingEntry : resultList) {
                log.info("Check record " + record.getDataKey() + " (" + record.getPrimaryKey() + ") for update...");

                KeyValueHolder<Integer, Boolean> k = updateExistingRecords(daoService, record, existingEntry);
                result += k.getKey();
                if (Boolean.TRUE.equals(k.getValue())) {
                    ignore = true;
                }
            }
        }

        // write the temporal record
        if (!ignore) {
            result += writeTemporalRecord(daoService, TemporalityActionType.UPDATE, record, "Update entry: " + toString(record));
        }

        return result;
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
                log.debug("Keep original entry, because it is before (Case B): " + toString(existingEntry));
            } else if (existingEntry.getValidTill().isAfter(record.getValidTill())) { // >
                // Case F: Insert
                log.debug("Insert entry, because it is before and after (Case F): " + toString(existingEntry));
                R entry1 = existingEntry.clone();
                entry1.setValidTill(record.getValidFrom());
                result += writeTemporalRecord(daoService, TemporalityActionType.TERMINATE, entry1, "Terminate entry (Case F): " + toString(entry1));

                R entry2 = existingEntry.clone();
                entry2.setPrimaryKey(null); // get new primary key for this entry
                entry2.setValidFrom(record.getValidTill());
                result += writeTemporalRecord(daoService, TemporalityActionType.CREATE, entry2, "Add new entry at the end (Case F): " + toString(entry2));
            } else {
                // Case D: Terminate
                R entry = existingEntry.clone();
                entry.setValidTill(record.getValidFrom());
                result += writeTemporalRecord(daoService, TemporalityActionType.TERMINATE, entry, "Terminate entry (Case D): " + toString(entry));
            }
        } else {
            // existing entries which starting now or in future
            if (existingEntry.getValidTill().isBefore(record.getValidTill())) { // <
                // Case G: Reduce
                // delete entries which are part of the current entry
                result += deleteTemporalRecord(daoService, existingEntry, "Delete entry, because new entry valid till has changed (Case G): " + toString(existingEntry));
            } else if (existingEntry.getValidFrom().isAfter(record.getValidTill())) { // >
                // Case C: Add
                // ignore entries which starting in future
                log.debug("Keep original entry, because it is in future (Case C): " + toString(existingEntry));
            } else {
                // check if it is the same
                R compareEntry = record.clone();
                compareEntry.setPrimaryKey(existingEntry.getPrimaryKey());

                if (existingEntry.equals(compareEntry)) {
                    // Case A: same record
                    // ignore already existing entry!
                    log.debug("Identical entry found on database, ignore writing (Case A): [" + toString(record) + "] == [" + toString(existingEntry) + "].");
                    ignoreRecord = Boolean.TRUE;
                } else if (isNotEmpty(existingEntry.getValidFrom()) && isNotEmpty(record.getValidFrom())
                          && existingEntry.getDataKey().equals(record.getDataKey())
                          && existingEntry.getValidFrom().equals(record.getValidFrom())
                          && (record.getPrimaryKey() == null || existingEntry.getPrimaryKey().equals(record.getPrimaryKey()))) {
                    // Case H: terminate -> delete
                    //deleteTemporalRecord(daoService, record, "Terminate entry (Case H): " + toString(existingEntry) + " -> " + toString(record));
                    R entry = record.clone();
                    entry.setPrimaryKey(existingEntry.getPrimaryKey());
                    result += writeTemporalRecord(daoService, TemporalityActionType.TERMINATE, entry, "Terminate entry (Case H): " + toString(existingEntry) + " -> " + toString(record));
                    ignoreRecord = Boolean.TRUE;
                } else {
                    // Case E: terminate

                    // if from = till
                    if (isNotEmpty(record.getValidTill()) && isMaxInstant(record.getValidTill())) {
                        // Case E: empty record
                        log.debug("Ignore record, nothing to terminate because max timestamp: [" + toString(existingEntry) + "], [" + toString(record) + "].");
                    } else if (existingEntry.getValidTill().equals(record.getValidTill())) { // ==
                        R entry = record.clone();
                        entry.setPrimaryKey(existingEntry.getPrimaryKey());
                        result += writeTemporalRecord(daoService, TemporalityActionType.UPDATE, entry, "Terminate entry (Case E1): " + toString(existingEntry) + " -> " + toString(record));
                        ignoreRecord = Boolean.TRUE;
                    } else {
                        R entry = existingEntry.clone();
                        entry.setValidFrom(record.getValidTill());

                        // write the temporal record
                        result += writeTemporalRecord(daoService, TemporalityActionType.TERMINATE, entry, "Terminate entry (Case E): " + toString(existingEntry) + " -> " + toString(entry));
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
    protected <R> int writeTemporalRecord(IDAOService<R> daoService, TemporalityActionType temporalityActionType, R record, String logComment) {
        int result = 0;

        try {
            if (logComment != null) {
                log.debug(logComment);
            }

            daoService.write(temporalityActionType, record);
            result++;
        } catch (RuntimeException e) {
            log.debug("Could not write: " + record + "\n->" + e.getMessage(), e);
        }

        return result;
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
    protected <T> int deleteTemporalRecord(IDAOService<T> daoService, T record, String logComment) {
        int result = 0;

        try {
            if (logComment != null) {
                log.debug(logComment);
            }

            daoService.delete(record);
            result++;
        } catch (RuntimeException e) {
            log.debug("Could not delete: " + record + "\n->" + e.getMessage(), e);
        }

        return result;
    }


    /**
     * Read the temporality records
     *
     * @param <T> the generic type
     * @param recordFilter the record
     * @param daoService the service
     * @return the result
     */
    protected <T> List<T> readTemporalityRecordList(IDAOService<T> daoService, T recordFilter) {
        try {
            return daoService.search(recordFilter);
        } catch (Exception ex) {
            log.debug("Could not read current configuration: " + ex.getMessage(), ex);
        }

        return null;
    }


    /**
     * Convert a temporality record into a string
     *
     * @param record the record
     * @return the srting representation
     */
    protected String toString(ITemporalityRecord<?, ?, ?> record) {
        String dataKey = "(n/a)";
        String primaryKey = "(n/a)";
        String from = "(n/a)";
        String to = "(n/a)";

        if (record.getDataKey() != null) {
            dataKey = record.getDataKey().toString();
        }

        if (record.getPrimaryKey() != null) {
            primaryKey = "(" + record.getPrimaryKey().toString() + ")";
        }

        if (record.getValidFrom() != null)  {
            from = DateTimeFormatter.ISO_INSTANT.format(record.getValidFrom());
        }

        if (record.getValidTill() != null) {
            to = DateTimeFormatter.ISO_INSTANT.format(record.getValidTill());
        }

        return "" + dataKey + " " + primaryKey + ", " + from + " - " + to;

    }


    /**
     * Check if max date is reached
     *
     * @param instat the instant
     * @return true if the max date is reached
     */
    protected boolean isMaxInstant(Instant instat) {
        if (Instant.MAX.equals(instat)) {
            return true;
        }
        
        LocalDateTime d = LocalDateTime.ofInstant(instat, ZoneId.systemDefault());
        if (d.getDayOfMonth() == 31 && Month.DECEMBER.equals(d.getMonth()) && d.getDayOfYear() >= 9999) {
            return true;
        }
        
        return false;
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
