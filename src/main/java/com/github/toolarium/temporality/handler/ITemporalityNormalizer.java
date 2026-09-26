/*
 * ITemporalityNormalizer.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;

import java.util.Comparator;
import java.util.List;


/**
 * Reconciles the records of <b>one data key</b> with the consistent timeline: records whose
 * validity matches the timeline exactly are kept (updated), records replaced by a higher-priority
 * overlapping record are terminated ({@code validTill} set to {@code validFrom}), and new trimmed
 * segments produced by the timeline are inserted.
 *
 * <p><b>Mutation note:</b> normalize mutates terminated records in place by setting their
 * {@code validTill} to their {@code validFrom}. Clone the input list before calling if you need
 * to preserve the originals.</p>
 *
 * <p>All input records must carry a non-null primary key (i.e. they already exist in the store).
 * Records with a {@code null} primary key are skipped silently.</p>
 */
public interface ITemporalityNormalizer {

    /**
     * Normalizes the records of one data key against the consistent timeline using the default priority (latest start wins).
     *
     * <ol>
     *   <li>Computes the timeline.</li>
     *   <li>Updates every record that survived unchanged ({@link com.github.toolarium.temporality.handler.TemporalityActionType#UPDATE}).</li>
     *   <li>Terminates every record that was replaced ({@link com.github.toolarium.temporality.handler.TemporalityActionType#TERMINATE}, {@code validTill = validFrom}).</li>
     *   <li>Inserts every new trimmed segment ({@link com.github.toolarium.temporality.handler.TemporalityActionType#CREATE}).</li>
     *   <li>Asserts that the resulting timeline has no overlaps.</li>
     * </ol>
     *
     * @param records the records of one data key
     * @param daoService the DAO service to apply changes to
     * @param <R> the record type
     * @param <K> the primary key type
     * @param <D> the data key type
     * @throws IllegalArgumentException if records or daoService is null
     * @throws IllegalStateException if the computed timeline still has overlaps (indicates a bug)
     */
    <R extends ITemporalityRecord<R, K, D>, K, D> void normalize(List<R> records, IDAOService<R> daoService);


    /**
     * Normalizes the records of one data key against the consistent timeline.
     *
     * <ol>
     *   <li>Computes the timeline.</li>
     *   <li>Updates every record that survived unchanged ({@link com.github.toolarium.temporality.handler.TemporalityActionType#UPDATE}).</li>
     *   <li>Terminates every record that was replaced ({@link com.github.toolarium.temporality.handler.TemporalityActionType#TERMINATE}, {@code validTill = validFrom}).</li>
     *   <li>Inserts every new trimmed segment ({@link com.github.toolarium.temporality.handler.TemporalityActionType#CREATE}).</li>
     *   <li>Asserts that the resulting timeline has no overlaps.</li>
     * </ol>
     *
     * @param records the records of one data key
     * @param daoService the DAO service to apply changes to
     * @param priority the priority order, the highest priority first
     * @param <R> the record type
     * @param <K> the primary key type
     * @param <D> the data key type
     * @throws IllegalArgumentException if records, daoService or priority is null
     * @throws IllegalStateException if the computed timeline still has overlaps (indicates a bug)
     */
    <R extends ITemporalityRecord<R, K, D>, K, D> void normalize(List<R> records, IDAOService<R> daoService, Comparator<? super R> priority);
}
