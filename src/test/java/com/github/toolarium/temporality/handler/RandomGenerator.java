/*
 * RandomGenerator.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;

import java.security.SecureRandom;

public class RandomGenerator {
    private SecureRandom secureRandom;

    
    /**
     * Constructor for RandomGenerator
     */
    public RandomGenerator() {
        secureRandom = new SecureRandom();
    }

    
    /**
     * Gets a random long number
     * @param range the range of the long. As example the range 5 means a random
     *              number between 1 and 5.
     * @param allowZero true if zero should also included into the range. As
     *              example the range 5 means a random number between 0 and 4.
     * @return the random number
     */
    public long getRandomNumber(long range, boolean allowZero) {
        long s = range;

        if (s < 0) {
            s = s * -1;
        }

        if (range == 0) {
            if (!allowZero) {
                return 1;
            }
            
            return 0;
        }

        if (range == 1 && !allowZero) {
            return 1;
        }

        long r = -1;
        if (allowZero) {
            s++;
        }
        
        synchronized (secureRandom) {
            while (r <= 0) {
                r = Math.abs(secureRandom.nextLong() % s);
            }
        }

        if (allowZero) {
            r--;
        }
        
        return r;
    }
}
