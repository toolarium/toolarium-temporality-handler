/*
 * KeyValueHolder.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler.impl;

import java.io.Serializable;
import java.util.Objects;


/**
 * Defines a key-value holder.
 * 
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class KeyValueHolder<K, V> implements Serializable {
    private static final long serialVersionUID = -5636271967645388056L;
    private K key;
    private V value;


    /**
     * Default constructor
     * 
     * @param key the key.
     * @param value the value.
     */
    public KeyValueHolder(K key, V value) {
        this.key = key;
        this.value = value;
    }

    
    /**
     * Returns the key.
     * 
     * @return the key.
     */
    public K getKey() {
        return key;
    }


    /**
     * Sets the key.
     * 
     * @param key the key to set.
     */
    public void setKey(K key) {
        this.key = key;
    }


    /**
     * Returns the value.
     * 
     * @return the value.
     */
    public V getValue() {
        return value;
    }


    /**
     * Sets the value.
     * 
     * @param value the value to set.
     * @return the old object.
     */
    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;        
        return oldValue;
    }


    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }


    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        @SuppressWarnings("rawtypes")
        KeyValueHolder other = (KeyValueHolder) obj;
        return Objects.equals(key, other.key) && Objects.equals(value, other.value);
    }


    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "KeyValueHolder [key=" + key + ", value=" + value + "]";
    }
}
