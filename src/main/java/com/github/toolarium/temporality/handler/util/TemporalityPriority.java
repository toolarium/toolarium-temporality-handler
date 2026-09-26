/*
 * TemporalityPriority.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler.util;

import com.github.toolarium.temporality.handler.ITemporalityRecord;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


/**
 * Priority rules for the normalizer timeline, as comparators with the highest priority first.
 */
public final class TemporalityPriority {

    /**
     * Constructor
     */
    private TemporalityPriority() {
        // utility class
    }


    /**
     * Orders records by their start, the most recently started first: where records overlap, the later one wins, like
     * a later write overrides an earlier one. Records with the same start keep their list order (the first one wins).
     *
     * @param <R> the record type
     * @return the record order, the highest priority first
     */
    public static <R extends ITemporalityRecord<R, ?, ?>> Comparator<R> latestStart() {
        return Comparator.comparing(ITemporalityRecord::getValidFrom, Comparator.reverseOrder());
    }


    /**
     * Orders groups (e.g. duplicate configurations) by the start of their currently valid record, the most recent
     * first; a group without currently valid record comes after all groups with one. Future records do not count.
     *
     * @param records the records of all groups
     * @param group the group of a record
     * @param now the time which decides the currently valid records
     * @param tieBreaker the order of groups with the same start, the higher priority first
     * @param <R> the record type
     * @param <G> the group type
     * @return the group order, the highest priority first
     * @throws IllegalArgumentException if records, group, now or tieBreaker is null
     */
    public static <R extends ITemporalityRecord<R, ?, ?>, G> Comparator<G> groupsByCurrentStart(Collection<R> records, Function<R, G> group, Instant now, Comparator<G> tieBreaker) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }
        if (group == null) {
            throw new IllegalArgumentException("group must not be null");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (tieBreaker == null) {
            throw new IllegalArgumentException("tieBreaker must not be null");
        }

        Map<G, Instant> currentStart = new HashMap<>();
        for (R record : records) {
            boolean currentlyValid = record.getValidFrom() != null && record.getValidTill() != null
                && record.getValidFrom().isBefore(record.getValidTill())
                && !record.getValidFrom().isAfter(now) && record.getValidTill().isAfter(now);
            if (currentlyValid) {
                currentStart.merge(group.apply(record), record.getValidFrom(), (a, b) -> {
                    if (a.isAfter(b)) {
                        return a;
                    }
                    return b;
                });
            }
        }
        Comparator<G> byStart = Comparator.comparing((G g) -> Optional.ofNullable(currentStart.get(g)),
            (a, b) -> {
                if (a.isPresent() && b.isPresent()) {
                    return b.get().compareTo(a.get());
                }
                if (a.isPresent() == b.isPresent()) {
                    return 0;
                }
                if (a.isPresent()) {
                    return -1;
                }
                return 1;
            });
        return byStart.thenComparing(tieBreaker);
    }


    /**
     * Orders records by the priority of their group; records of the same group by their start, the most recent first.
     *
     * @param group the group of a record
     * @param groupOrder the group order, the highest priority first
     * @param <R> the record type
     * @param <G> the group type
     * @return the record order, the highest priority first
     * @throws IllegalArgumentException if group or groupOrder is null
     */
    public static <R extends ITemporalityRecord<R, ?, ?>, G> Comparator<R> byGroup(Function<R, G> group, Comparator<G> groupOrder) {
        if (group == null) {
            throw new IllegalArgumentException("group must not be null");
        }
        if (groupOrder == null) {
            throw new IllegalArgumentException("groupOrder must not be null");
        }

        Comparator<R> byGroupComparator = Comparator.comparing(group, groupOrder);
        return byGroupComparator.thenComparing(ITemporalityRecord::getValidFrom, Comparator.reverseOrder());
    }
}
