/*
 * ITemporalityRecord.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;


import java.time.Instant;


/**
 * Defines a temporality record.
 *
 * @param <R> the generic record type.
 * @param <P> the generic primary type.
 * @param <D> the generic data key type.
 */
public interface ITemporalityRecord<R, P, D> extends Cloneable {

    /**
     * Get the primary key of the record
     *
     * @return the primary key
     */
    P getPrimaryKey();


    /**
     * Set the primary key of the record
     *
     * @param key the primary key to set
     */
    void setPrimaryKey(P key);


    /**
     * Get the data key
     *
     * @return the data key
     */
    D getDataKey();


    /**
     * Get the valid from
     *
     * @return the vaild from
     */
    Instant getValidFrom();


    /**
     * Set the valid from
     *
     * @param validFrom the valid from
     */
    void setValidFrom(Instant validFrom);


    /**
     * Get the valid till
     *
     * @return the valid till
     */
    Instant getValidTill();


    /**
     * Set the valid till
     *
     * @param validTill the valid till
     */
    void setValidTill(Instant validTill);


    /**
     * Clone
     *
     * @return the cloned object
     */
    R clone();
}
