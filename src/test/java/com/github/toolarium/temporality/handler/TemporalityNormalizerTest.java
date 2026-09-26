/*
 * TemporalityNormalizerTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.temporality.handler.util.TemporalityPriority;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link ITemporalityNormalizer} via the factory.
 */
public class TemporalityNormalizerTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2024-02-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2024-03-01T00:00:00Z");
    private static final String KEY = "cfg";
    private static final String VAL_A = "a";
    private static final String VAL_B = "b";


    // ---------------------------------------------------------------------------
    // Argument validation
    // ---------------------------------------------------------------------------

    /**
     * Null records must throw.
     */
    @Test
    public void normalizeNullRecordsThrows() {
        ITemporalityNormalizer normalizer = TemporalityHandlerFactory.getInstance().getTemporalityNormalizer();
        MyRecordDAO store = new MyRecordDAO();
        assertThrows(IllegalArgumentException.class,
            () -> normalizer.normalize(null, store));
    }


    /**
     * Null daoService must throw.
     */
    @Test
    public void normalizeNullDaoServiceThrows() {
        ITemporalityNormalizer normalizer = TemporalityHandlerFactory.getInstance().getTemporalityNormalizer();
        List<MyRecord> records = new ArrayList<>();
        assertThrows(IllegalArgumentException.class,
            () -> normalizer.normalize(records, null));
    }


    /**
     * Null priority must throw.
     */
    @Test
    public void normalizeNullPriorityThrows() {
        ITemporalityNormalizer normalizer = TemporalityHandlerFactory.getInstance().getTemporalityNormalizer();
        MyRecordDAO store = new MyRecordDAO();
        List<MyRecord> records = new ArrayList<>();
        assertThrows(IllegalArgumentException.class,
            () -> normalizer.normalize(records, store, null));
    }


    // ---------------------------------------------------------------------------
    // Empty input
    // ---------------------------------------------------------------------------

    /**
     * Empty record list produces no DAO calls.
     */
    @Test
    public void normalizeEmptyRecordsDoesNothing() {
        ITemporalityNormalizer normalizer = TemporalityHandlerFactory.getInstance().getTemporalityNormalizer();
        MyRecordDAO store = new MyRecordDAO();
        normalizer.normalize(new ArrayList<>(), store);
        assertTrue(store.getUpdated().isEmpty());
        assertTrue(store.getTerminated().isEmpty());
        assertTrue(store.getCreated().isEmpty());
    }


    // ---------------------------------------------------------------------------
    // Single record
    // ---------------------------------------------------------------------------

    /**
     * A single record with no overlap is written with UPDATE and left unchanged.
     */
    @Test
    public void normalizeSingleRecordUnchanged() {
        ITemporalityNormalizer normalizer = TemporalityHandlerFactory.getInstance().getTemporalityNormalizer();
        MyRecord rec = new MyRecord(1L, KEY, "v", T0, T2);
        MyRecordDAO store = new MyRecordDAO();
        normalizer.normalize(list(rec), store);

        assertEquals(1, store.getUpdated().size());
        assertEquals(rec, store.getUpdated().get(0));
        assertTrue(store.getTerminated().isEmpty());
        assertTrue(store.getCreated().isEmpty());
        assertEquals(T0, rec.getValidFrom());
        assertEquals(T2, rec.getValidTill());
    }


    // ---------------------------------------------------------------------------
    // Two non-overlapping records
    // ---------------------------------------------------------------------------

    /**
     * Two adjacent non-overlapping records are both written with UPDATE.
     */
    @Test
    public void normalizeTwoNonOverlappingRecordsUnchanged() {
        ITemporalityNormalizer normalizer = TemporalityHandlerFactory.getInstance().getTemporalityNormalizer();
        MyRecord a = new MyRecord(1L, KEY, VAL_A, T0, T1);
        MyRecord b = new MyRecord(2L, KEY, VAL_B, T1, T2);
        MyRecordDAO store = new MyRecordDAO();
        normalizer.normalize(list(a, b), store);

        assertEquals(2, store.getUpdated().size());
        assertTrue(store.getUpdated().contains(a));
        assertTrue(store.getUpdated().contains(b));
        assertTrue(store.getTerminated().isEmpty());
        assertTrue(store.getCreated().isEmpty());
    }


    // ---------------------------------------------------------------------------
    // Overlapping records — higher-priority wins
    // ---------------------------------------------------------------------------

    /**
     * When A and B overlap and B starts later, B is updated unchanged, A is terminated,
     * and a trimmed [T0,T1) segment is created.
     */
    @Test
    public void normalizeOverlappingRecordsHigherPriorityWins() {
        ITemporalityNormalizer normalizer = TemporalityHandlerFactory.getInstance().getTemporalityNormalizer();
        MyRecord a = new MyRecord(1L, KEY, VAL_A, T0, T2);
        MyRecord b = new MyRecord(2L, KEY, VAL_B, T1, T2);
        MyRecordDAO store = new MyRecordDAO();
        normalizer.normalize(list(a, b), store);

        assertEquals(1, store.getUpdated().size());
        assertEquals(Long.valueOf(2L), store.getUpdated().get(0).getPrimaryKey());

        assertEquals(1, store.getTerminated().size());
        MyRecord terminated = store.getTerminated().get(0);
        assertEquals(Long.valueOf(1L), terminated.getPrimaryKey());
        assertEquals(terminated.getValidFrom(), terminated.getValidTill());

        assertEquals(1, store.getCreated().size());
        MyRecord created = store.getCreated().get(0);
        assertEquals(T0, created.getValidFrom());
        assertEquals(T1, created.getValidTill());
        assertEquals(VAL_A, created.getValue());
    }


    // ---------------------------------------------------------------------------
    // Zero-length input record
    // ---------------------------------------------------------------------------

    /**
     * A zero-length record is invisible to the timeline and gets terminated.
     */
    @Test
    public void normalizeZeroLengthRecordIsTerminated() {
        ITemporalityNormalizer normalizer = TemporalityHandlerFactory.getInstance().getTemporalityNormalizer();
        MyRecord rec = new MyRecord(1L, KEY, "v", T0, T0);
        MyRecordDAO store = new MyRecordDAO();
        normalizer.normalize(list(rec), store);

        assertTrue(store.getUpdated().isEmpty());
        assertEquals(1, store.getTerminated().size());
        assertEquals(Long.valueOf(1L), store.getTerminated().get(0).getPrimaryKey());
        assertTrue(store.getCreated().isEmpty());
    }


    // ---------------------------------------------------------------------------
    // Custom priority
    // ---------------------------------------------------------------------------

    /**
     * With earliest-start-wins priority, A [T0] beats B [T1]: A is updated unchanged, B terminated, no creates.
     */
    @Test
    public void normalizeWithCustomPriority() {
        ITemporalityNormalizer normalizer = TemporalityHandlerFactory.getInstance().getTemporalityNormalizer();
        MyRecord a = new MyRecord(1L, KEY, VAL_A, T0, T2);
        MyRecord b = new MyRecord(2L, KEY, VAL_B, T1, T2);
        MyRecordDAO store = new MyRecordDAO();
        normalizer.normalize(list(a, b), store, TemporalityPriority.<MyRecord>latestStart().reversed());

        assertEquals(1, store.getUpdated().size());
        assertEquals(Long.valueOf(1L), store.getUpdated().get(0).getPrimaryKey());

        assertEquals(1, store.getTerminated().size());
        assertEquals(Long.valueOf(2L), store.getTerminated().get(0).getPrimaryKey());

        assertTrue(store.getCreated().isEmpty());
    }


    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Create a mutable list from the given items.
     *
     * @param items the items
     * @param <R> the item type
     * @return the list
     */
    @SafeVarargs
    private static <R> List<R> list(R... items) {
        return new ArrayList<>(Arrays.asList(items));
    }


}
