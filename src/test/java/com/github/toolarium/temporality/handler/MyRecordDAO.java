/*
 * MyRecordDAO.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.temporality.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements the {@link MyRecord} dao.
 */
public class MyRecordDAO implements IDAOService<MyRecord> {
    private static final Logger log = LoggerFactory.getLogger(MyRecordDAO.class);
    private Map<String, List<MyRecord>> data;
    private List<MyRecord> updated;
    private List<MyRecord> terminated;
    private List<MyRecord> created;


    /**
     * Constructor
     */
    public MyRecordDAO() {
        data = new ConcurrentHashMap<String, List<MyRecord>>();
        updated = new ArrayList<MyRecord>();
        terminated = new ArrayList<MyRecord>();
        created = new ArrayList<MyRecord>();
    }


    /**
     * @see com.github.toolarium.temporality.handler.IDAOService#write(com.github.toolarium.temporality.handler.TemporalityActionType, java.lang.Object)
     */
    @Override
    public synchronized void write(TemporalityActionType temporalityActionType, MyRecord inputRecord) {
        String key = inputRecord.getDataKey();
        List<MyRecord> recordList = data.get(key);

        if (recordList == null) {
            recordList = new CopyOnWriteArrayList<MyRecord>();
        }

        boolean replace = false;
        MyRecord record = inputRecord;
        if (record.getPrimaryKey() == null) {
            record = inputRecord.clone();
            record.setPrimaryKey(new RandomGenerator().getRandomNumber(100000, false));
        } else {
            for (int i = 0; i < recordList.size() && !replace; i++) {
                if (recordList.get(i).getPrimaryKey() != null && record.getPrimaryKey() != null && recordList.get(i).getPrimaryKey() == record.getPrimaryKey()) {
                    replace = true;
                    log.debug("Replace record: " + record);
                    recordList.set(i, record);
                    break;
                }
            }
        }

        if (!replace) {
            log.debug("Write record: " + record);
            recordList.add(record);
        }

        data.put(key, recordList);

        if (TemporalityActionType.CREATE.equals(temporalityActionType)) {
            created.add(inputRecord);
        } else if (TemporalityActionType.TERMINATE.equals(temporalityActionType)) {
            terminated.add(inputRecord);
        } else {
            updated.add(inputRecord);
        }
    }


    /**
     * @see com.github.toolarium.temporality.handler.IDAOService#delete(java.lang.Object)
     */
    @Override
    public synchronized void delete(MyRecord record) {
        log.debug("Delete record: " + record.getDataKey() + " (" + record.getPrimaryKey() + ")");
        String key = record.getDataKey();
        List<MyRecord> recordList = data.get(key);
        List<MyRecord> newRecordList = new ArrayList<MyRecord>();
        if (recordList != null) {
            for (Iterator<MyRecord> it = recordList.iterator(); it.hasNext();) {
                MyRecord r = it.next();
                if (r.getPrimaryKey() != record.getPrimaryKey()) {
                    newRecordList.add(r);
                }
            }
        }

        data.put(key, newRecordList);
        if (newRecordList == null || newRecordList.isEmpty()) {
            data.remove(record.getDataKey());
        }
    }


    /**
     * @see com.github.toolarium.temporality.handler.IDAOService#search(java.lang.Object)
     */
    @Override
    public synchronized List<MyRecord> search(MyRecord recordFilter) {
        String key = recordFilter.getDataKey();
        List<MyRecord> recordList = data.get(key);

        if (recordList == null) {
            log.debug("Search " + key + " (#0)");
        } else {
            log.debug("Search " + key + " (#" + recordList.size() + ")");
        }

        return recordList;
    }


    /**
     * Get the records written with UPDATE.
     *
     * @return the updated records
     */
    public List<MyRecord> getUpdated() {
        return updated;
    }


    /**
     * Get the records written with TERMINATE.
     *
     * @return the terminated records
     */
    public List<MyRecord> getTerminated() {
        return terminated;
    }


    /**
     * Get the records written with CREATE.
     *
     * @return the created records
     */
    public List<MyRecord> getCreated() {
        return created;
    }


    /**
     * Get the data
     *
     * @return the data
     */
    public Map<String, List<MyRecord>> getData() {
        return data;
    }


    /**
     * Get the data
     *
     * @return the data
     */
    public synchronized long getNumberOfRecords() {
        long result = 0;
        for (String key : data.keySet()) {
            List<MyRecord> recordList = data.get(key);
            if (recordList != null) {
                result += recordList.size();
            }
        }

        return result;
    }
}
