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
    private static TemporalityHandlerFactory instance;
    private static final ThreadLocal<ITemporalityHandler> threadLocal = new ThreadLocal<ITemporalityHandler>();


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
        if (instance == null) {
            instance = new TemporalityHandlerFactory();
        }

        return instance;
    }


    /**
     * Get the temporality handler
     *
     * @return the temporality handler
     */
    public ITemporalityHandler getTemporalityHandler() {
        ITemporalityHandler temporalityHandler = threadLocal.get();
        if (temporalityHandler == null) {
            temporalityHandler = new TemporalityHandlerImpl();
            threadLocal.set(temporalityHandler);
        }

        return temporalityHandler;
    }
}
