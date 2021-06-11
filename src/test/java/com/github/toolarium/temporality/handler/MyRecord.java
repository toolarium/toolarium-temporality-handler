/*
 * MyRecord.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;

import java.time.Instant;
import java.util.Objects;


/**
 * Simple record.
 *
 * @author pmeier
 * @version $Revision: 1.1 $
 */
public class MyRecord implements ITemporalityRecord<MyRecord, Long, String>, Cloneable {
    private Long primaryKey;
    private String key;
    private String value;
    private Instant validFrom;
    private Instant validTill;


    /**
     * Constructor
     *
     * @param key the key
     * @param value the value
     * @param validFrom the valid from
     * @param validTill the valid till
     */
    MyRecord(String key, String value, Instant validFrom, Instant validTill) {
        this.primaryKey = null;
        this.key = key;
        this.value = value;
        this.validFrom = validFrom;
        this.validTill = validTill;
    }


    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityRecord#getPrimaryKey()
     */
    @Override
    public Long getPrimaryKey() {
        return primaryKey;
    }


    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityRecord#setPrimaryKey(java.lang.Object)
     */
    @Override
    public void setPrimaryKey(Long primaryKey) {
        this.primaryKey = primaryKey;
    }


    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityRecord#getDataKey()
     */
    @Override
    public String getDataKey() {
        return key;
    }


    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityRecord#getValidFrom()
     */
    @Override
    public Instant getValidFrom() {
        return validFrom;
    }


    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityRecord#setValidFrom(java.time.Instant)
     */
    @Override
    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }


    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityRecord#getValidTill()
     */
    @Override
    public Instant getValidTill() {
        return validTill;
    }


    /**
     * @see com.github.toolarium.temporality.handler.ITemporalityRecord#setValidTill(java.time.Instant)
     */
    @Override
    public void setValidTill(Instant validTill) {
        this.validTill = validTill;
    }

    
    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, primaryKey, validFrom, validTill, value);
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
        
        MyRecord other = (MyRecord) obj;
        return Objects.equals(key, other.key) && Objects.equals(primaryKey, other.primaryKey) && Objects.equals(validFrom, other.validFrom) && Objects.equals(validTill, other.validTill) && Objects.equals(value, other.value);
    }


    /**
     * @see jptools.pattern.vo.AbstractValueObject#clone()
     */
    @Override
    public MyRecord clone() {
        try {
            MyRecord result = (MyRecord)super.clone();
            result.key = key;
            result.value = value;
            result.primaryKey = primaryKey;
            result.validFrom = validFrom;
            result.validTill = validTill;
            return result;

        } catch (CloneNotSupportedException e) {
            InternalError ex = new InternalError("Could not clone object " + getClass().getName() + ": " + e.getMessage());
            ex.setStackTrace(e.getStackTrace());
            throw ex;
        }
    }


    /**
     * @see jptools.pattern.vo.AbstractValueObject#toString()
     */
    @Override
    public String toString() {
        return ""
               + key
               //+ " (" + primaryKey + ")"
               + " / "
               + value + " / "
               + validFrom + " - "
               + validTill;
    }
}
