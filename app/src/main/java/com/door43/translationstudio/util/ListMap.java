package com.door43.translationstudio.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class makes it easier to look up items in a list by key and by index at the same time
 * @param <E>
 */
public class ListMap<E> {
    private Map<String, E> mObjectMap = new HashMap<String, E>();
    private List<E> mObjects = new ArrayList<E>();

    /**
     * Returns the object by it's key
     * @param key the key of the object
     * @return the object or null
     */
    public E get(String key) {
        if(key != null && !key.isEmpty() && mObjectMap.containsKey(key)) {
            return mObjectMap.get(key);
        } else {
            return null;
        }
    }

    /**
     * Returns the object by it's index
     * @param index the index of the object to return
     * @return the object or null
     */
    public E get(int index) {
        if(index < mObjects.size() && index >= 0) {
            return mObjects.get(index);
        } else {
            return null;
        }
    }

    /**
     * Adds an object to the list by key value
     * @param key the key of the object
     * @param object the object
     * @return true if the object did not already exist
     */
    public boolean add(String key, E object) {
        if(key != null && !key.isEmpty() && !mObjectMap.containsKey(key)) {
            mObjectMap.put(key, object);
            mObjects.add(object);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds or replaces an object ot the list by key value.
     * @param key the key of the object
     * @param object the object
     * @return true if the object was added
     */
    public boolean replace(String key, E object) {
        if(key != null && !key.isEmpty()) {
            // remove old value
            if(mObjectMap.containsKey(key)) {
                mObjects.remove(mObjectMap.get(key));
                mObjectMap.remove(key);
            }
            mObjectMap.put(key, object);
            mObjects.add(object);
            return true;
        } else {
            return false;
        }
    }
}