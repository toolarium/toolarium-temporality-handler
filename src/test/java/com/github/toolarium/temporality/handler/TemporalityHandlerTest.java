/*
 * TemporalityHandlerTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Test the temporality handler:
 * 
 * <code>
 * Case A: 1) <--(A)-->
 *         2) <--(A)-->
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
 * </code>
 */
public class TemporalityHandlerTest {
    private static final String KEY2 = "key2";
    private static final String VALUE = "value";
    private static final String KEY = "key";
    private static final String NEW = "new";
    private static final String SEP = " - ";
    private Instant referenceTimestamp;


    /**
     * Constructor
     */
    public TemporalityHandlerTest() {
        referenceTimestamp = DateTimeFormatter.ISO_DATE_TIME.parse("2014-05-26T13:11:10Z", Instant::from);
    }


    /**
     * Write records test
     */
    @Test
    public void writeRecords() {
        int size = 100;
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, size, Instant.now(), Instant.MAX);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);
    }


    /**
     * Write records test
     */
    @Test
    public void writeTwoRecords() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, 1, Instant.now(), Instant.MAX);
        createDataEntries(daoService, 1, Instant.now().plus(100, ChronoUnit.SECONDS), Instant.MAX);
        createDataEntries(daoService, 1, Instant.now().plus(100, ChronoUnit.SECONDS), Instant.MAX);
        
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), 3);
    }


    /**
     * Write identical record, which means do nothing
     * <pre>
     * Case A: 1) <--(A)-->
     *         2) <--(A)-->
     * </pre>
     */
    @Test
    public void writeIdenticalRecord() {
        int size = 100;
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, size, referenceTimestamp, Instant.MAX);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        int i = 2;
        TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(new MyRecord(KEY + i, VALUE + i, referenceTimestamp, Instant.MAX), daoService);

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals(recordList.size(), 1);
        assertEquals("key2 / value2 / 2014-05-26T13:11:10Z - +1000000000-12-31T23:59:59.999999999Z", recordList.get(0).toString());
    }


    /**
     * Write record in later time range
     * <pre>
     * Case B: 1) <--(A)-->
     *         2) <--(A)--> <--(B)-->
     * </pre>
     */
    @Test
    public void writeRecordLaterTimeRange() {
        int size = 4;
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, size, referenceTimestamp, DateTimeFormatter.ISO_DATE_TIME.parse("2014-05-28T13:11:10Z", Instant::from));
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        int i = 3;
        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i,
                                                    DateTimeFormatter.ISO_DATE_TIME.parse("2014-05-30T13:11:10Z", Instant::from),
                                                    DateTimeFormatter.ISO_DATE_TIME.parse("2014-05-31T13:11:10Z", Instant::from)),
                                       daoService);

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size + 1);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals("key2 / value2 / 2014-05-26T13:11:10Z - 2014-05-28T13:11:10Z", recordList.get(0).toString());

        recordList = ((MyRecordDAO)daoService).getData().get("key3");
        assertEquals(recordList.size(), 2);
        assertEquals("key3 / value3 / 2014-05-26T13:11:10Z - 2014-05-28T13:11:10Z", recordList.get(0).toString());
        assertEquals("key3 / value3 / 2014-05-30T13:11:10Z - 2014-05-31T13:11:10Z", recordList.get(1).toString());
    }


    /**
     * Write record in earlier time range
     * <pre>
     * Case C: 1)           <--(A)-->
     *         2) <--(B)--> <--(A)-->
     * </pre>
     */
    @Test
    public void writeRecordEarlierTimeRange() {
        int size = 100;
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, size, referenceTimestamp, Instant.MAX);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        int i = 3;
        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i,
                                                    DateTimeFormatter.ISO_DATE_TIME.parse("2014-05-01T13:11:10Z", Instant::from),
                                                    DateTimeFormatter.ISO_DATE_TIME.parse("2014-05-16T13:11:10Z", Instant::from)),
                                       daoService);

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size + 1);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals("key2 / value2 / 2014-05-26T13:11:10Z - +1000000000-12-31T23:59:59.999999999Z", recordList.get(0).toString());

        recordList = ((MyRecordDAO)daoService).getData().get("key3");
        assertEquals(recordList.size(), 2);
        assertEquals("key3 / value3 / 2014-05-26T13:11:10Z - +1000000000-12-31T23:59:59.999999999Z", recordList.get(0).toString());
        assertEquals("key3 / value3 / 2014-05-01T13:11:10Z - 2014-05-16T13:11:10Z", recordList.get(1).toString());
    }


    /**
     * Write record in later time range
     * <pre>
     * Case D: 1) <--(A)----->
     *         2) <--(A)--><--(B)-->
     * </pre>
     */
    @Test
    public void writeRecordWithSameDataKeyAndOtherValidTill() {
        int size = 100;
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, size, referenceTimestamp, referenceTimestamp.plus(5, ChronoUnit.DAYS));
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        int i = 2;
        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i,
                                                    referenceTimestamp.plus(4, ChronoUnit.DAYS),
                                                    Instant.MAX),
                                       daoService);

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size + 1);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals(recordList.size(), 2);
        assertEquals("key2 / value2 / 2014-05-26T13:11:10Z - 2014-05-30T13:11:10Z", recordList.get(0).toString());
        assertEquals("key2 / value2 / 2014-05-30T13:11:10Z - +1000000000-12-31T23:59:59.999999999Z", recordList.get(1).toString());
    }


    /**
     * Write record in earlier time range
     * <pre>
     * Case E: 1)       <------(A)-->
     *         2) <--(B)--><---(A)-->
     * </pre>
     */
    @Test
    public void writeRecordWithEarlierValidFrom() {
        int size = 100;
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, size, referenceTimestamp, Instant.MAX);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        int i = 2;
        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i,
                                                    referenceTimestamp.minus(1, ChronoUnit.DAYS),
                                                    referenceTimestamp.plus(1, ChronoUnit.DAYS)),
                                       daoService);

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size + 1);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals(recordList.size(), 2);
        assertEquals("key2 / value2 / 2014-05-27T13:11:10Z - +1000000000-12-31T23:59:59.999999999Z", recordList.get(0).toString());
        assertEquals("key2 / value2 / 2014-05-25T13:11:10Z - 2014-05-27T13:11:10Z", recordList.get(1).toString());
    }

    /**
     * Write record in earlier time range
     * <pre>
     * Case E: 1)       <------(A)-->
     *         2) <--(B)--><---(A)-->
     *         Input:
     *              Existing record: validFrom: 2014-05-26T13:11:10  - validTill: 2014-05-27T13:11:10
     *              Editing record: validFrom: 2014-05-25T13:11:10  - validTill: 2014-05-27T13:11:10
     *         Output:
     *              Updated existing record: validFrom: 2014-05-25T13:11:10  - validTill: 2014-05-27T13:11:10
     *              BUT current implementation the result like this:
     *                  Update Existing record: validFrom: 2014-05-27T13:11:10  - validTill: 2014-05-27T13:11:10 (not make sense with this case validFrom the same with validTill)
     *                  Create Editing record: validFrom: 2014-05-25T13:11:10  - validTill: 2014-05-27T13:11:10
     * </pre>
     */
    @Test
    public void writeRecordWithEarlierValidFromAndSameValidTill() {
        int size = 100;
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, size, referenceTimestamp, referenceTimestamp.plus(1, ChronoUnit.DAYS));
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        int i = 2;
        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i,
                                                    referenceTimestamp.minus(1, ChronoUnit.DAYS),
                                                    referenceTimestamp.plus(1, ChronoUnit.DAYS)),
                                       daoService);

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals(recordList.size(), 1);
        assertEquals("key2 / value2 / 2014-05-25T13:11:10Z - 2014-05-27T13:11:10Z", recordList.get(0).toString());
    }


    /**
     * Write record test
     * -> terminate current entry and add new entry
     * <pre>
     * Case F: 1) <------(A)-------->
     *         2) <-(A)-><-(B)-><(A)>
     * </pre>
     */
    @Test
    public void writeRecordInsertBetween() {
        int size = 100;
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, size, referenceTimestamp, Instant.MAX);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        int i = 2;
        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i + NEW,
                                                    referenceTimestamp.plus(3, ChronoUnit.DAYS),
                                                    referenceTimestamp.plus(5, ChronoUnit.DAYS)),
                                       daoService);

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size + 2);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals(recordList.size(), 3);

        assertEquals("key2 / value2 / 2014-05-26T13:11:10Z - 2014-05-29T13:11:10Z", recordList.get(0).toString());
        assertEquals("key2 / value2new / 2014-05-29T13:11:10Z - 2014-05-31T13:11:10Z", recordList.get(2).toString());
        assertEquals("key2 / value2 / 2014-05-31T13:11:10Z - +1000000000-12-31T23:59:59.999999999Z", recordList.get(1).toString());
    }


    /**
     * Write record test
     * -> terminate current entry and add new entry
     * <pre>
     * Case G: 1) <-(A)-><-(B)-><-C->
     *         2) <-------(D)------->
     * </pre>
     */
    @Test
    public void combineRecord() {
        int size = 100;
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, size, referenceTimestamp, referenceTimestamp.plus(1, ChronoUnit.DAYS));
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        int i = 2;
        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i + "new1",
                                                    referenceTimestamp.plus(2, ChronoUnit.DAYS),
                                                    referenceTimestamp.plus(3, ChronoUnit.DAYS)),
                                       daoService);

        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i + "new2",
                                                    referenceTimestamp.plus(4, ChronoUnit.DAYS),
                                                    referenceTimestamp.plus(5, ChronoUnit.DAYS)),
                                       daoService);

        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i + NEW,
                                                    referenceTimestamp,
                                                    referenceTimestamp.plus(6, ChronoUnit.DAYS)),
                                       daoService);

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals(recordList.size(), 1);

        assertEquals("key2 / value2new / 2014-05-26T13:11:10Z - 2014-06-01T13:11:10Z", recordList.get(0).toString());
    }


    /**
     * Terminate record test (delete)
     * <pre>
     * Case H: 1) <---(A)--->
     *         2) <---(A)-->
     * </pre>
     */
    @Test
    public void deleteRecord() {
        int size = 100;
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        createDataEntries(daoService, size, referenceTimestamp, referenceTimestamp.plus(5, ChronoUnit.DAYS));
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals(recordList.size(), 1);

        int i = 2;

        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i + "new2",
                                                    referenceTimestamp,
                                                    referenceTimestamp.plus(3, ChronoUnit.DAYS)),
                                       daoService);

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size + 1);

        recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals(recordList.size(), 2);

        assertEquals("key2 / value2new2 / 2014-05-26T13:11:10Z - 2014-05-29T13:11:10Z", recordList.get(0).toString());
        assertEquals("key2 / value2 / 2014-05-29T13:11:10Z - 2014-05-31T13:11:10Z", recordList.get(1).toString());
    }


    /**
     * Write record with same validFrom but earlier validTill — the remainder of the existing record must be preserved.
     * <pre>
     * Case H (remainder): 1) <---------(A)---------->
     *                     2) <--(B)--><-----(A)----->
     * </pre>
     */
    @Test
    public void writeRecordWithSameValidFromAndEarlierValidTill() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        Instant start = DateTimeFormatter.ISO_DATE_TIME.parse("2025-01-01T00:00:00Z", Instant::from);
        Instant mid   = DateTimeFormatter.ISO_DATE_TIME.parse("2026-01-01T00:00:00Z", Instant::from);

        // record a: [2025-01-01, MAX)
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord("smtpHost", "mail.example.com", start, Instant.MAX), daoService);

        // record b: [2025-01-01, 2026-01-01) — same start, earlier end
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord("smtpHost", "new-mail.example.com", start, mid), daoService);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get("smtpHost");
        assertEquals(2, recordList.size());
        assertEquals("smtpHost / new-mail.example.com / 2025-01-01T00:00:00Z - 2026-01-01T00:00:00Z", recordList.get(0).toString());
        assertEquals("smtpHost / mail.example.com / 2026-01-01T00:00:00Z - +1000000000-12-31T23:59:59.999999999Z", recordList.get(1).toString());
    }


    /**
     * Null record throws IllegalArgumentException
     */
    @Test
    public void writeNullRecordThrows() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        assertThrows(IllegalArgumentException.class, () ->
            TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(null, daoService));
    }


    /**
     * Null dao service throws IllegalArgumentException
     */
    @Test
    public void writeNullDaoServiceThrows() {
        MyRecord record = new MyRecord(KEY, VALUE, referenceTimestamp, Instant.MAX);
        assertThrows(IllegalArgumentException.class, () ->
            TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(record, null));
    }


    /**
     * Null validFrom throws IllegalArgumentException
     */
    @Test
    public void writeRecordWithNullValidFromThrows() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        MyRecord record = new MyRecord(KEY, VALUE, null, Instant.MAX);
        assertThrows(IllegalArgumentException.class, () ->
            TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(record, daoService));
    }


    /**
     * Null validTill throws IllegalArgumentException
     */
    @Test
    public void writeRecordWithNullValidTillThrows() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        MyRecord record = new MyRecord(KEY, VALUE, referenceTimestamp, null);
        assertThrows(IllegalArgumentException.class, () ->
            TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(record, daoService));
    }


    /**
     * Reversed interval (validFrom >= validTill) throws IllegalArgumentException
     */
    @Test
    public void writeRecordWithReversedIntervalThrows() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        MyRecord reversed = new MyRecord(KEY, VALUE, referenceTimestamp.plus(1, ChronoUnit.DAYS), referenceTimestamp);
        assertThrows(IllegalArgumentException.class, () ->
            TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(reversed, daoService));
    }


    /**
     * Zero-length interval (validFrom == validTill) throws IllegalArgumentException
     */
    @Test
    public void writeRecordWithZeroLengthIntervalThrows() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        MyRecord zero = new MyRecord(KEY, VALUE, referenceTimestamp, referenceTimestamp);
        assertThrows(IllegalArgumentException.class, () ->
            TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(zero, daoService));
    }


    /**
     * Exception thrown by search() propagates to the caller
     */
    @Test
    public void searchExceptionPropagates() {
        IDAOService<MyRecord> daoService = new MyRecordDAO() {
            @Override
            public synchronized List<MyRecord> search(MyRecord recordFilter) {
                throw new RuntimeException("search failed");
            }
        };
        MyRecord record = new MyRecord(KEY, VALUE, referenceTimestamp, Instant.MAX);
        assertThrows(RuntimeException.class, () ->
            TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(record, daoService));
    }


    /**
     * Null result from search() is treated as empty — a single CREATE is issued
     */
    @Test
    public void searchNullResultTreatedAsEmpty() {
        IDAOService<MyRecord> daoService = new MyRecordDAO() {
            @Override
            public synchronized List<MyRecord> search(MyRecord recordFilter) {
                return null;
            }
        };
        MyRecord record = new MyRecord(KEY, VALUE, referenceTimestamp, Instant.MAX);
        int result = TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(record, daoService);
        assertEquals(1, result);
    }


    /**
     * Exception thrown by write() propagates to the caller
     */
    @Test
    public void writeExceptionPropagates() {
        IDAOService<MyRecord> daoService = new MyRecordDAO() {
            @Override
            public synchronized void write(TemporalityActionType temporalityActionType, MyRecord record) {
                throw new RuntimeException("write failed");
            }
        };
        MyRecord record = new MyRecord(KEY, VALUE, referenceTimestamp, Instant.MAX);
        assertThrows(RuntimeException.class, () ->
            TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(record, daoService));
    }


    /**
     * Exception thrown by delete() propagates to the caller (Case G)
     */
    @Test
    public void deleteExceptionPropagates() {
        IDAOService<MyRecord> daoService = new MyRecordDAO() {
            @Override
            public synchronized void delete(MyRecord record) {
                throw new RuntimeException("delete failed");
            }
        };

        // Setup: write an initial record — write() is not overridden, so storage works
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE,
                                               referenceTimestamp,
                                               referenceTimestamp.plus(1, ChronoUnit.DAYS)), daoService);

        // Spanning record triggers Case G → delete() throws
        assertThrows(RuntimeException.class, () ->
            TemporalityHandlerFactory.getInstance().getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY, VALUE + NEW,
                                                   referenceTimestamp,
                                                   referenceTimestamp.plus(2, ChronoUnit.DAYS)), daoService));
    }


    /**
     * Same interval, different data — record is updated in-place without creating a remainder (Case H, same validTill)
     */
    @Test
    public void writeRecordWithSameIntervalDifferentData() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();

        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE, referenceTimestamp, Instant.MAX), daoService);

        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE + NEW, referenceTimestamp, Instant.MAX), daoService);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY);
        assertEquals(1, recordList.size());
        assertEquals("key / valuenew / 2014-05-26T13:11:10Z - +1000000000-12-31T23:59:59.999999999Z", recordList.get(0).toString());
    }


    /**
     * Case D with finite new validTill — existing record is terminated, new record appended
     * <pre>
     * Case D: 1) <------(A)-------->
     *         2) <-(A)-><----(B)---->
     * </pre>
     */
    @Test
    public void writeRecordCaseDWithFiniteValidTill() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        Instant t1 = referenceTimestamp;
        Instant t2 = t1.plus(3, ChronoUnit.DAYS);
        Instant t3 = t1.plus(5, ChronoUnit.DAYS);
        Instant t4 = t1.plus(7, ChronoUnit.DAYS);

        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE, t1, t3), daoService);

        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE + NEW, t2, t4), daoService);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY);
        assertEquals(2, recordList.size());
        assertEquals("key / value / " + t1 + SEP + t2, recordList.get(0).toString());
        assertEquals("key / valuenew / " + t2 + SEP + t4, recordList.get(1).toString());
    }


    /**
     * Case E with finite existing validTill — existing record's start is shifted forward
     * <pre>
     * Case E: 1)       <------(A)-->
     *         2) <--(B)--><---(A)-->
     * </pre>
     */
    @Test
    public void writeRecordCaseEWithFiniteExisting() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        Instant t1 = referenceTimestamp;
        Instant t2 = t1.plus(2, ChronoUnit.DAYS);
        Instant t3 = t1.plus(3, ChronoUnit.DAYS);
        Instant t4 = t1.plus(5, ChronoUnit.DAYS);

        // Existing: [t2, t4)
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE, t2, t4), daoService);

        // New: [t1, t3) — starts before existing, ends within existing
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE + NEW, t1, t3), daoService);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY);
        assertEquals(2, recordList.size());
        assertEquals("key / value / " + t3 + SEP + t4, recordList.get(0).toString());
        assertEquals("key / valuenew / " + t1 + SEP + t3, recordList.get(1).toString());
    }


    /**
     * Case F with finite existing validTill — existing record is split around the new record
     * <pre>
     * Case F: 1) <-----------(A)----------->
     *         2) <-(A)-><-(B)-><-----(A)--->
     * </pre>
     */
    @Test
    public void writeRecordCaseFWithFiniteBounds() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        Instant t1 = referenceTimestamp;
        Instant t2 = t1.plus(2, ChronoUnit.DAYS);
        Instant t3 = t1.plus(4, ChronoUnit.DAYS);
        Instant t4 = t1.plus(6, ChronoUnit.DAYS);

        // Existing: [t1, t4) — finite both ends
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE, t1, t4), daoService);

        // New: [t2, t3) — fully within existing
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE + NEW, t2, t3), daoService);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY);
        assertEquals(3, recordList.size());
        assertEquals("key / value / " + t1 + SEP + t2, recordList.get(0).toString());
        assertEquals("key / value / " + t3 + SEP + t4, recordList.get(1).toString());
        assertEquals("key / valuenew / " + t2 + SEP + t3, recordList.get(2).toString());
    }


    /**
     * Case G with exactly two existing records — both are deleted and replaced by the spanning record
     * <pre>
     * Case G: 1) <-(A)-><-(B)->
     *         2) <-----(C)---->
     * </pre>
     */
    @Test
    public void combineRecordTwoExisting() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        Instant t1 = referenceTimestamp;
        Instant t2 = t1.plus(2, ChronoUnit.DAYS);
        Instant t3 = t1.plus(4, ChronoUnit.DAYS);
        Instant t4 = t1.plus(6, ChronoUnit.DAYS);

        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE + "1", t1, t2), daoService);
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE + "2", t2, t3), daoService);

        // Spanning record covers both existing records
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE + NEW, t1, t4), daoService);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY);
        assertEquals(1, recordList.size());
        assertEquals("key / valuenew / " + t1 + SEP + t4, recordList.get(0).toString());
    }


    /**
     * Dec-31-9999 is treated as a max instant: in Case E, when the new record's validTill is near-max,
     * the existing record must not be shifted (it would produce a nonsensical reversed interval if
     * isMaxInstant failed to recognise the near-max value).
     */
    @Test
    public void nearMaxInstantTreatedAsMax() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();
        Instant t1 = referenceTimestamp;
        Instant t2 = t1.plus(2, ChronoUnit.DAYS);
        Instant nearMax = LocalDateTime.of(9999, Month.DECEMBER, 31, 0, 0, 0).toInstant(ZoneOffset.UTC);

        // Existing: [t2, MAX) — validTill must be >= nearMax so Case E is entered, not Case G
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE, t2, Instant.MAX), daoService);

        // New record ends at near-max: isMaxInstant must recognise it so the existing record is NOT shifted
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE + NEW, t1, nearMax), daoService);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY);
        assertEquals(2, recordList.size());
        assertEquals("key / value / " + t2 + " - +1000000000-12-31T23:59:59.999999999Z", recordList.get(0).toString());
        assertEquals("key / valuenew / " + t1 + SEP + nearMax, recordList.get(1).toString());
    }


    /**
     * Verify return values: CREATE returns 1, Case A (no-op) returns 0, Case F returns 3
     */
    @Test
    public void writeReturnValue() {
        IDAOService<MyRecord> daoService = new MyRecordDAO();

        // First write: CREATE → 1
        int result = TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE, referenceTimestamp, Instant.MAX), daoService);
        assertEquals(1, result);

        // Identical record: Case A → 0
        result = TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE, referenceTimestamp, Instant.MAX), daoService);
        assertEquals(0, result);

        // Case F: insert within existing → 3 (terminate left + create right + update new)
        IDAOService<MyRecord> daoService2 = new MyRecordDAO();
        TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE, referenceTimestamp, Instant.MAX), daoService2);
        result = TemporalityHandlerFactory.getInstance().getTemporalityHandler()
            .writeTemporlityRecord(new MyRecord(KEY, VALUE + NEW,
                                               referenceTimestamp.plus(1, ChronoUnit.DAYS),
                                               referenceTimestamp.plus(3, ChronoUnit.DAYS)), daoService2);
        assertEquals(3, result);
    }


    /**
     * Factory always returns the same singleton instance
     */
    @Test
    public void factoryReturnsSameInstance() {
        TemporalityHandlerFactory instance1 = TemporalityHandlerFactory.getInstance();
        TemporalityHandlerFactory instance2 = TemporalityHandlerFactory.getInstance();
        assertSame(instance1, instance2);
    }


    /**
     * Create data entries
     *
     * @param daoService the dao service
     * @param size the size
     * @param from the from date
     * @param to the to date
     */
    private void createDataEntries(IDAOService<MyRecord> daoService, int size, Instant from, Instant to) {
        ITemporalityHandler h = TemporalityHandlerFactory.getInstance().getTemporalityHandler();

        for (int i = 1; i <= size; i++) {
            h.writeTemporlityRecord(new MyRecord(KEY + i, VALUE + i, from, to), daoService);
        }

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
    }


}
