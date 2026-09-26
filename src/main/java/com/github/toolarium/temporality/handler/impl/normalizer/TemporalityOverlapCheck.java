/*
 * TemporalityOverlapCheck.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler.impl.normalizer;

import com.github.toolarium.temporality.handler.ITemporalityRecord;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Finds records of the same data key whose validities overlap ({@code validFrom <= t < validTill}, records with zero
 * length never overlap). Used to find data which needs a cleanup and to verify a migration.
 */
public final class TemporalityOverlapCheck {

    /**
     * Constructor
     */
    private TemporalityOverlapCheck() {
        // utility class
    }


    /**
     * Finds the overlapping pairs of records of the same data key.
     *
     * @param records the records
     * @param <R> the record type
     * @return the overlapping pairs, empty if no two records overlap
     * @throws IllegalArgumentException if records is null
     */
    public static <R extends ITemporalityRecord<R, ?, ?>> List<Map.Entry<R, R>> findOverlaps(Collection<R> records) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }

        List<R> list = new ArrayList<>(records);
        List<Map.Entry<R, R>> overlaps = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                if (overlap(list.get(i), list.get(j))) {
                    overlaps.add(new AbstractMap.SimpleImmutableEntry<>(list.get(i), list.get(j)));
                }
            }
        }
        return overlaps;
    }


    /**
     * Checks if two records of the same data key overlap.
     *
     * @param a the first record
     * @param b the second record
     * @param <R> the record type
     * @return true if both have the same data key, a validity of non-zero length and a common point in time
     */
    public static <R extends ITemporalityRecord<R, ?, ?>> boolean overlap(R a, R b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getValidFrom() == null || a.getValidTill() == null) {
            return false;
        }
        if (b.getValidFrom() == null || b.getValidTill() == null) {
            return false;
        }
        return Objects.equals(a.getDataKey(), b.getDataKey())
            && a.getValidFrom().isBefore(a.getValidTill()) && b.getValidFrom().isBefore(b.getValidTill())
            && a.getValidFrom().isBefore(b.getValidTill()) && b.getValidFrom().isBefore(a.getValidTill());
    }
}
