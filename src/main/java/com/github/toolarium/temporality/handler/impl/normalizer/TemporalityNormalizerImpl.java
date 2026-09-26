/*
 * TemporalityNormalizerImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler.impl.normalizer;

import com.github.toolarium.temporality.handler.IDAOService;
import com.github.toolarium.temporality.handler.ITemporalityNormalizer;
import com.github.toolarium.temporality.handler.ITemporalityRecord;
import com.github.toolarium.temporality.handler.TemporalityActionType;
import com.github.toolarium.temporality.handler.util.TemporalityPriority;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Default implementation of {@link ITemporalityNormalizer}.
 */
public final class TemporalityNormalizerImpl implements ITemporalityNormalizer {

    /**
     * Constructor
     */
    public TemporalityNormalizerImpl() {
        // NOP
    }


    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityNormalizer#normalize(List, IDAOService)
     */
    @Override
    public <R extends ITemporalityRecord<R, K, D>, K, D> void normalize(List<R> records, IDAOService<R> daoService) {
        normalize(records, daoService, TemporalityPriority.latestStart());
    }


    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityNormalizer#normalize(List, IDAOService, Comparator)
     */
    @Override
    public <R extends ITemporalityRecord<R, K, D>, K, D> void normalize(List<R> records, IDAOService<R> daoService, Comparator<? super R> priority) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }
        if (daoService == null) {
            throw new IllegalArgumentException("daoService must not be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("priority must not be null");
        }

        List<R> timeline = TemporalityTimeline.compute(records, priority);

        // Collect the primary keys of records that survived unchanged in the timeline.
        Set<Object> keptKeys = new HashSet<>();
        for (R segment : timeline) {
            if (segment.getPrimaryKey() != null) {
                keptKeys.add(segment.getPrimaryKey());
            }
        }

        // Update unchanged records; terminate replaced records.
        for (R record : records) {
            if (record.getPrimaryKey() == null) {
                continue;
            }
            if (keptKeys.contains(record.getPrimaryKey())) {
                daoService.write(TemporalityActionType.UPDATE, record);
            } else {
                record.setValidTill(record.getValidFrom());
                daoService.write(TemporalityActionType.TERMINATE, record);
            }
        }

        // Insert new trimmed segments that have no primary key.
        for (R segment : timeline) {
            if (segment.getPrimaryKey() == null) {
                daoService.write(TemporalityActionType.CREATE, segment);
            }
        }

        // Post-condition: the timeline must be overlap-free.
        List<Map.Entry<R, R>> overlaps = TemporalityOverlapCheck.findOverlaps(timeline);
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Timeline has overlaps after normalization: " + overlaps);
        }
    }
}
