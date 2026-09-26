/*
 * TemporalityHandlerFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;

import com.github.toolarium.temporality.handler.impl.TemporalityHandlerImpl;
import com.github.toolarium.temporality.handler.impl.normalizer.TemporalityNormalizerImpl;

/**
 * Defines the temporality handler factory.
 */
public final class TemporalityHandlerFactory {
    private final TemporalityHandlerImpl handler = new TemporalityHandlerImpl();
    private final ITemporalityNormalizer normalizer = new TemporalityNormalizerImpl();

    private static final class Holder {
        static final TemporalityHandlerFactory INSTANCE = new TemporalityHandlerFactory();
    }


    /**
     * Constructor
     */
    private TemporalityHandlerFactory() {
        // NOP
    }


    /**
     * Get the factory instance
     *
     * @return the instance
     */
    public static TemporalityHandlerFactory getInstance() {
        return Holder.INSTANCE;
    }


    /**
     * Get the temporality handler
     *
     * @return the temporality handler
     */
    public ITemporalityHandler getTemporalityHandler() {
        return handler;
    }


    /**
     * Get the temporality normalizer.
     *
     * @return the temporality normalizer
     */
    public ITemporalityNormalizer getTemporalityNormalizer() {
        return normalizer;
    }


    /**
     * Get the canonical maximum validTill value.
     *
     * @return the maximum validTill instant
     */
    public java.time.Instant getMaxValidTill() {
        return handler.getMaxValidTill();
    }


    /**
     * Set the canonical maximum validTill value. Any record whose validTill is strictly after
     * this instant is capped to this value before processing. Defaults to 9999-12-31T00:00:00Z.
     *
     * @param maxValidTill the maximum validTill instant; must not be null
     * @return this factory, for chaining
     */
    public TemporalityHandlerFactory setMaxValidTill(java.time.Instant maxValidTill) {
        handler.setMaxValidTill(maxValidTill);
        return this;
    }
}
