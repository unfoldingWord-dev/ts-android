package com.door43.translationstudio.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by joel on 1/14/2015.
 */
public class KeyValueStore {
    private Map<String, Object> dataStore = new HashMap<String, Object>();

    /**
     * Add a vlaue to the store
     * @param key
     * @param value
     */
    public void add(String key, Object value) {
        dataStore.put(key, value);
    }

    /**
     * Retrieve a value as a string
     * @param key
     * @return
     */
    public String getString(String key) {
        if(dataStore.containsKey(key)) {
            Object value = dataStore.get(key);
            return value.toString();
        } else {
            return null;
        }
    }

    /**
     * Retrieve a value as an int
     * @param key
     */
    public int getInt(String key) {
        if(dataStore.containsKey(key)) {
            Object value = dataStore.get(key);
            return Integer.parseInt(value.toString());
        } else {
            return 0;
        }
    }

    /**
     * Retrieve a value
     * @param key
     */
    public Object get(String key) {
        if(dataStore.containsKey(key)) {
            return dataStore.get(key);
        } else {
            return null;
        }
    }

    public boolean getBool(String key) {
        if(dataStore.containsKey(key)) {
            Object value = dataStore.get(key);
            return Boolean.parseBoolean(value.toString());
        } else {
            return false;
        }
    }
}
