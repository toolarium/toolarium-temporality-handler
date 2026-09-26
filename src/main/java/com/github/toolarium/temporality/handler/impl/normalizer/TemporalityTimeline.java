/*
 * TemporalityTimeline.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler.impl.normalizer;

import com.github.toolarium.temporality.handler.ITemporalityRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;


/**
 * Computes the consistent timeline of the records of <b>one data key</b>, which may overlap: for every point in time
 * the record with the highest priority covering it wins, a record with lower priority stays where no higher one covers
 * it. The validity is {@code validFrom <= t < validTill}; records with zero length or null temporal fields are ignored.
 *
 * <p>The same semantics as writing the records with the temporality handler in ascending priority, but computed as
 * plain interval arithmetic: deterministic and independent of the case logic of the temporality handler.
 */
public final class TemporalityTimeline {

    /**
     * Constructor
     */
    private TemporalityTimeline() {
        // utility class
    }


    /**
     * Computes the timeline.
     *
     * @param records the records of one data key
     * @param priority the priority order, the highest priority first
     * @param <R> the record type
     * @return the segments of the timeline ordered by validity; each segment is a clone of the winning record with the
     *         validity of the segment and the primary key of the record only if the segment has exactly the validity of
     *         the record (the record stays unchanged), otherwise without primary key (a new record)
     * @throws IllegalArgumentException if records or priority is null
     */
    public static <R extends ITemporalityRecord<R, ?, ?>> List<R> compute(List<R> records, Comparator<? super R> priority) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("priority must not be null");
        }

        List<R> visible = new ArrayList<>();
        for (R record : records) {
            if (record.getValidFrom() != null && record.getValidTill() != null
                    && record.getValidFrom().isBefore(record.getValidTill())) {
                visible.add(record);
            }
        }

        TreeSet<Instant> boundaries = new TreeSet<>();
        for (R record : visible) {
            boundaries.add(record.getValidFrom());
            boundaries.add(record.getValidTill());
        }

        List<R> segments = new ArrayList<>();
        R currentWinner = null;
        Instant segmentStart = null;
        Instant previous = null;
        for (Instant boundary : boundaries) {
            if (previous != null) {
                R winner = winner(visible, priority, previous, boundary);
                if (winner != currentWinner) {
                    addSegment(segments, currentWinner, segmentStart, previous);
                    currentWinner = winner;
                    segmentStart = previous;
                }
            }
            previous = boundary;
        }
        addSegment(segments, currentWinner, segmentStart, previous);
        return segments;
    }


    /**
     * Returns the record with the highest priority covering the interval {@code [from, till)}.
     *
     * @param records the visible records
     * @param priority the priority order, the highest priority first
     * @param from the start of the interval
     * @param till the end of the interval
     * @param <R> the record type
     * @return the winning record, {@code null} if no record covers the interval
     */
    private static <R extends ITemporalityRecord<R, ?, ?>> R winner(List<R> records, Comparator<? super R> priority, Instant from, Instant till) {
        R winner = null;
        for (R record : records) {
            boolean covers = !record.getValidFrom().isAfter(from) && !record.getValidTill().isBefore(till);
            if (covers && (winner == null || priority.compare(record, winner) < 0)) {
                winner = record;
            }
        }
        return winner;
    }


    /**
     * Adds the segment of a winning record.
     *
     * @param segments the segments
     * @param winner the winning record, {@code null} for a gap
     * @param from the start of the segment
     * @param till the end of the segment
     * @param <R> the record type
     */
    private static <R extends ITemporalityRecord<R, ?, ?>> void addSegment(List<R> segments, R winner, Instant from, Instant till) {
        if (winner == null) {
            return;
        }
        R segment = winner.clone();
        boolean unchanged = winner.getValidFrom().equals(from) && winner.getValidTill().equals(till);
        if (!unchanged) {
            segment.setPrimaryKey(null);
        }
        segment.setValidFrom(from);
        segment.setValidTill(till);
        segments.add(segment);
    }
}
