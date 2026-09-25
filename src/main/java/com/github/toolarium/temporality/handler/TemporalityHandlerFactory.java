/*
 * TemporalityHandlerFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;

import com.github.toolarium.temporality.handler.impl.TemporalityHandlerImpl;

/**
 * Defines the temporality handler factory.
 */
public final class TemporalityHandlerFactory {
    private final ITemporalityHandler handler = new TemporalityHandlerImpl();

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
}
