package com.door43.util;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * This class handles the management of a manifest file.
 *
 */
public class Manifest {
    private final File mManifestFile;
    private JSONObject mManifest = new JSONObject();
    public static final String MANIFEST_JSON = "manifest.json";
    public static final String FINISHED_FRAMES = "finished_frames";
    public static final String FINISHED_TITLES = "finished_titles";
    public static final String FINISHED_REFERENCES = "finished_references";
    public static final String TRANSLATORS = "translators";

    /**
     * Creates a new manifest object representing a file on the disk
     * @param file
     */
    private Manifest(File file) {
        mManifestFile = file;
    }

    /**
     * Generates a new manifest object.
     * If a manifest file already exists it will be loaded otherwise it will be created.
     * @param directory the directory in which the manifest file exists
     * @return the manifest object or null if the manifest could not be created
     */
    public static Manifest generate(File directory) {
        File file = new File(directory, MANIFEST_JSON);
        if(!file.exists()) {
            file.getParentFile().mkdirs();
        }
        if(!file.isFile()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        Manifest m = new Manifest(file);
        m.load();
        return m;
    }

    /**
     *
     * @param key
     * @return an empty string if the key is invalid
     */
    public String getString(String key) {
        try {
            return mManifest.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Checks if the key exists in the manifest
     * @param key
     * @return
     */
    public Boolean has(String key) {
        return mManifest.has(key);
    }

    /**
     *
     * @param key
     * @return
     * @throws JSONException
     */
    public int getInt(String key) throws JSONException {
        return mManifest.getInt(key);
    }

    /**
     *
     * @param key
     * @return an empty json object if the key is invalid
     */
    public JSONObject getJSONObject(String key) {
        try {
            return mManifest.getJSONObject(key);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    /**
     *
     * @param key
     * @return an empty json array if the key is invalid
     */
    public JSONArray getJSONArray(String key) {
        try {
            return mManifest.getJSONArray(key);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    /**
     * Adds an element to the manifest
     * @param key
     * @param json
     */
    public void put(String key, JSONObject json) {
        try {
            mManifest.put(key, json);
            save();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds an element to the manifest
     * @param key
     * @param json
     */
    public void put(String key, JSONArray json) {
        try {
            mManifest.put(key, json);
            save();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds an element to the manifest
     * @param key
     * @param value
     */
    public void put(String key, int value) {
        try {
            mManifest.put(key, value);
            save();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds an element to the manifest
     * @param key
     * @param value
     */
    public void put(String key, String value) {
        try {
            mManifest.put(key, value);
            save();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes an element from the manifest
     * @param key
     */
    public void remove(String key) {
        mManifest.remove(key);
        save();
    }

    /**
     * Saves the manifest to the disk
     */
    private void save() {
        try {
            FileUtils.writeStringToFile(mManifestFile, mManifest.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Deletes the manifest file
     */
    private void delete() {
        mManifestFile.delete();
        mManifest = new JSONObject();
    }

    /**
     * Reads the manifest file from the disk
     */
    private void load() {
        String contents = "";
        try {
            contents = FileUtils.readFileToString(mManifestFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(contents.isEmpty()) {
            mManifest = new JSONObject();
        } else {
            try {
                mManifest = new JSONObject(contents);
            } catch (JSONException e) {
                e.printStackTrace();
                mManifest = new JSONObject();
            }
        }
    }

    /**
     * Uniquely merges the values of the array into the manifest
     * @param newArray
     * @param key
     */
    public void join(JSONArray newArray, String key) {
        if(newArray != null && key != null) {
            try {
                if (!mManifest.has(key)) {
                    mManifest.put(key, newArray);
                } else {
                    JSONArray array = mManifest.getJSONArray(key);
                    for (int i = 0; i < newArray.length(); i++) {
                        Object obj = newArray.get(i);
                        if (!hasValueInArray(array, obj)) {
                            array.put(obj);
                        }
                    }
                    mManifest.put(key, array);
                }
                save();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Uniquely merges the keys of the object into the manifest
     * @param newObj
     * @param key
     */
    public void join(JSONObject newObj, String key) {
        if(newObj != null && key != null) {
            try {
                if (!mManifest.has(key)) {
                    mManifest.put(key, newObj);
                } else {
                    JSONObject obj = mManifest.getJSONObject(key);
                    Iterator<String> newKeys = newObj.keys();
                    while(newKeys.hasNext()) {
                        String newObjKey = newKeys.next();
                        if(!obj.has(newObjKey)) {
                            obj.put(newObjKey, newObj.get(newObjKey));
                        }
                    }
                    mManifest.put(key, obj);
                }
                save();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if a value exists in the array
     * @param array
     * @param value
     * @return
     */
    private static boolean hasValueInArray(JSONArray array, Object value) {
        if(value != null  && array != null) {
            try {
                for (int i = 0; i < array.length(); i++) {
                    if (value.equals(array.get(i))) {
                        return true;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
