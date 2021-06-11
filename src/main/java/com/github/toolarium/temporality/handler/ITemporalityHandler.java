/*
 * ITemporalityHandler.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;


/**
 * Defines the temporality handler interface.
 */
public interface ITemporalityHandler {

    /**
     * Writes a temporality record and handling behavior.
     *
     * @param <R> the generic record type.
     * @param <K> the generic primary type.
     * @param <D> the generic data key type.
     * @param record the record to write.
     * @param daoService the data access service.
     * @return the number of updated records.
     */
    <R extends ITemporalityRecord<R, K, D>, K, D> int writeTemporlityRecord(R record, IDAOService<R> daoService);
}
