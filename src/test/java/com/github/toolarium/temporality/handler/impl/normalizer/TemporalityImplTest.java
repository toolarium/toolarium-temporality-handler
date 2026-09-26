/*
 * TemporalityImplTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler.impl.normalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.temporality.handler.MyRecord;
import com.github.toolarium.temporality.handler.util.TemporalityPriority;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link TemporalityTimeline}, {@link TemporalityOverlapCheck} and {@link TemporalityPriority}.
 */
public class TemporalityImplTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2024-02-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2024-03-01T00:00:00Z");
    private static final Instant T3 = Instant.parse("2024-04-01T00:00:00Z");
    private static final String KEY = "cfg";
    private static final String VAL_A = "a";
    private static final String VAL_B = "b";


    // ---------------------------------------------------------------------------
    // TemporalityOverlapCheck — argument validation
    // ---------------------------------------------------------------------------

    /**
     * Null records argument must throw.
     */
    @Test
    public void findOverlapsNullRecordsThrows() {
        assertThrows(IllegalArgumentException.class, () -> TemporalityOverlapCheck.findOverlaps(null));
    }


    // ---------------------------------------------------------------------------
    // TemporalityOverlapCheck — no overlap
    // ---------------------------------------------------------------------------

    /**
     * Adjacent (touching) records do not overlap.
     */
    @Test
    public void findOverlapsNoOverlap() {
        MyRecord a = new MyRecord(1L, KEY, VAL_A, T0, T1);
        MyRecord b = new MyRecord(2L, KEY, VAL_B, T1, T2);
        assertTrue(TemporalityOverlapCheck.findOverlaps(list(a, b)).isEmpty());
    }


    /**
     * Two records sharing only a boundary instant do not overlap.
     */
    @Test
    public void findOverlapsTouchingBoundaryIsNotOverlap() {
        MyRecord a = new MyRecord(1L, KEY, VAL_A, T0, T1);
        MyRecord b = new MyRecord(2L, KEY, VAL_B, T1, T2);
        assertTrue(TemporalityOverlapCheck.findOverlaps(list(a, b)).isEmpty());
    }


    // ---------------------------------------------------------------------------
    // TemporalityOverlapCheck — with overlap
    // ---------------------------------------------------------------------------

    /**
     * Two overlapping records are detected.
     */
    @Test
    public void findOverlapsDetectsOverlap() {
        MyRecord a = new MyRecord(1L, KEY, VAL_A, T0, T2);
        MyRecord b = new MyRecord(2L, KEY, VAL_B, T1, T3);
        List<Map.Entry<MyRecord, MyRecord>> overlaps = TemporalityOverlapCheck.findOverlaps(list(a, b));
        assertEquals(1, overlaps.size());
    }


    /**
     * A zero-length record never overlaps with another record.
     */
    @Test
    public void findOverlapsZeroLengthRecordNeverOverlaps() {
        MyRecord a = new MyRecord(1L, KEY, VAL_A, T0, T2);
        MyRecord b = new MyRecord(2L, KEY, VAL_B, T1, T1);
        assertTrue(TemporalityOverlapCheck.findOverlaps(list(a, b)).isEmpty());
    }


    /**
     * Null arguments to overlap() return false.
     */
    @Test
    public void overlapReturnsFalseForNullArguments() {
        MyRecord a = new MyRecord(1L, KEY, VAL_A, T0, T2);
        assertTrue(!TemporalityOverlapCheck.overlap(a, null));
        assertTrue(!TemporalityOverlapCheck.overlap(null, a));
        assertTrue(!TemporalityOverlapCheck.overlap(null, null));
    }


    // ---------------------------------------------------------------------------
    // TemporalityTimeline — argument validation
    // ---------------------------------------------------------------------------

    /**
     * Null records argument must throw.
     */
    @Test
    public void computeNullRecordsThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> TemporalityTimeline.<MyRecord>compute(null, TemporalityPriority.latestStart()));
    }


    /**
     * Null priority argument must throw.
     */
    @Test
    public void computeNullPriorityThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> TemporalityTimeline.compute(new ArrayList<MyRecord>(), null));
    }


    // ---------------------------------------------------------------------------
    // TemporalityTimeline — basic scenarios
    // ---------------------------------------------------------------------------

    /**
     * Empty records produce empty timeline.
     */
    @Test
    public void computeEmptyReturnsEmpty() {
        List<MyRecord> result = TemporalityTimeline.compute(new ArrayList<MyRecord>(), TemporalityPriority.latestStart());
        assertTrue(result.isEmpty());
    }


    /**
     * A single record produces a single unchanged timeline segment.
     */
    @Test
    public void computeSingleRecordReturnsItself() {
        MyRecord rec = new MyRecord(1L, KEY, "v", T0, T2);
        List<MyRecord> result = TemporalityTimeline.compute(list(rec), TemporalityPriority.latestStart());
        assertEquals(1, result.size());
        assertEquals(T0, result.get(0).getValidFrom());
        assertEquals(T2, result.get(0).getValidTill());
        assertEquals(Long.valueOf(1L), result.get(0).getPrimaryKey());
    }


    /**
     * Two non-overlapping records produce two unchanged segments.
     */
    @Test
    public void computeNonOverlappingRecordsPreservedAsTwoSegments() {
        MyRecord a = new MyRecord(1L, KEY, VAL_A, T0, T1);
        MyRecord b = new MyRecord(2L, KEY, VAL_B, T1, T2);
        List<MyRecord> result = TemporalityTimeline.compute(list(a, b), TemporalityPriority.latestStart());
        assertEquals(2, result.size());
    }


    /**
     * Overlapping records are merged: [T0,T1) from A (trimmed), [T1,T2) from B (unchanged).
     */
    @Test
    public void computeOverlappingRecordsMergedToConsistentTimeline() {
        MyRecord a = new MyRecord(1L, KEY, VAL_A, T0, T2);
        MyRecord b = new MyRecord(2L, KEY, VAL_B, T1, T2);
        List<MyRecord> result = TemporalityTimeline.compute(list(a, b), TemporalityPriority.latestStart());

        assertEquals(2, result.size());

        MyRecord first = result.get(0);
        assertEquals(T0, first.getValidFrom());
        assertEquals(T1, first.getValidTill());
        assertEquals(VAL_A, first.getValue());

        MyRecord second = result.get(1);
        assertEquals(T1, second.getValidFrom());
        assertEquals(T2, second.getValidTill());
        assertEquals(VAL_B, second.getValue());
        assertEquals(Long.valueOf(2L), second.getPrimaryKey());
    }


    /**
     * A zero-length record is filtered out and the remaining record spans the full timeline.
     */
    @Test
    public void computeZeroLengthRecordIgnored() {
        MyRecord valid = new MyRecord(1L, KEY, "v", T0, T2);
        MyRecord zero = new MyRecord(2L, KEY, "z", T1, T1);
        List<MyRecord> result = TemporalityTimeline.compute(list(valid, zero), TemporalityPriority.latestStart());
        assertEquals(1, result.size());
        assertEquals(T0, result.get(0).getValidFrom());
        assertEquals(T2, result.get(0).getValidTill());
    }


    // ---------------------------------------------------------------------------
    // TemporalityPriority — argument validation
    // ---------------------------------------------------------------------------

    /**
     * Null records argument must throw.
     */
    @Test
    public void groupsByCurrentStartNullRecordsThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> TemporalityPriority.groupsByCurrentStart(null, MyRecord::getDataKey, T0, Comparator.naturalOrder()));
    }


    /**
     * Null group function must throw.
     */
    @Test
    public void groupsByCurrentStartNullGroupThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> TemporalityPriority.<MyRecord, String>groupsByCurrentStart(new ArrayList<>(), null, T0, Comparator.naturalOrder()));
    }


    /**
     * Null now instant must throw.
     */
    @Test
    public void groupsByCurrentStartNullNowThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> TemporalityPriority.<MyRecord, String>groupsByCurrentStart(new ArrayList<>(), MyRecord::getDataKey, null, Comparator.naturalOrder()));
    }


    /**
     * Null tieBreaker must throw.
     */
    @Test
    public void groupsByCurrentStartNullTieBreakerThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> TemporalityPriority.<MyRecord, String>groupsByCurrentStart(new ArrayList<>(), MyRecord::getDataKey, T0, null));
    }


    /**
     * Null group function in byGroup must throw.
     */
    @Test
    public void byGroupNullGroupThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> TemporalityPriority.<MyRecord, String>byGroup(null, Comparator.naturalOrder()));
    }


    /**
     * Null groupOrder in byGroup must throw.
     */
    @Test
    public void byGroupNullGroupOrderThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> TemporalityPriority.<MyRecord, String>byGroup(MyRecord::getDataKey, null));
    }


    // ---------------------------------------------------------------------------
    // TemporalityPriority — latestStart
    // ---------------------------------------------------------------------------

    /**
     * latestStart orders the record with the newer validFrom before the older one.
     */
    @Test
    public void latestStartOrdersNewerStartFirst() {
        MyRecord older = new MyRecord(1L, KEY, VAL_A, T0, T2);
        MyRecord newer = new MyRecord(2L, KEY, VAL_B, T1, T2);
        Comparator<MyRecord> cmp = TemporalityPriority.latestStart();
        assertTrue(cmp.compare(newer, older) < 0);
        assertTrue(cmp.compare(older, newer) > 0);
        assertEquals(0, cmp.compare(newer, newer));
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
