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
     * Clone this record.
     *
     * <p>Implementors must return a copy that is independent from this instance: mutations of
     * {@code validFrom}, {@code validTill}, and {@code primaryKey} on the clone must not affect
     * the original, and vice versa. Because {@link java.time.Instant} is immutable these fields
     * are inherently safe; all other mutable payload fields must be copied deeply enough to
     * preserve this invariant.</p>
     *
     * @return a copy of this record
     */
    R clone();
}
