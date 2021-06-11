/*
 * TemporalityHandlerTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
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
                                                    VALUE + i + "new",
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
                                                    VALUE + i + "new",
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

        int i = 2;

        TemporalityHandlerFactory.getInstance()
                .getTemporalityHandler()
                .writeTemporlityRecord(new MyRecord(KEY + i,
                                                    VALUE + i + "new2",
                                                    referenceTimestamp,
                                                    referenceTimestamp.plus(3, ChronoUnit.DAYS)),
                                       daoService);

        assertEquals(((MyRecordDAO)daoService).getData().size(), size);
        assertEquals(((MyRecordDAO)daoService).getNumberOfRecords(), size);

        List<MyRecord> recordList = ((MyRecordDAO)daoService).getData().get(KEY2);
        assertEquals(recordList.size(), 1);

        assertEquals("key2 / value2new2 / 2014-05-26T13:11:10Z - 2014-05-29T13:11:10Z", recordList.get(0).toString());
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
