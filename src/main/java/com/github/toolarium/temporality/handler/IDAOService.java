/*
 * IDAOService.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;

import java.util.List;


/**
 * Defines an abstraction of a DAO service
 *
 * @param <R> the generic record type.
 */
public interface IDAOService<R> {
    /**
     * Write the record
     *
     * @param temporalityActionType the temporality action type
     * @param record the record
     */
    void write(TemporalityActionType temporalityActionType, R record);


    /**
     * Delete record
     *
     * @param record the record
     */
    void delete(R record);


    /**
     * Search
     *
     * @param recordFilter the record for filtering
     * @return the record list
     */
    List<R> search(R recordFilter);
}
