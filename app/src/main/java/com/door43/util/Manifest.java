package com.door43.util;

import com.door43.util.reporting.Logger;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * This class handles the managment of a manifest file.
 *
 */
public class Manifest {
    private final String mDirectory;
    private JSONObject mManifest = new JSONObject();
    private static String FILE_NAME = "manifest.json";

    public Manifest(String directory) {
        mDirectory = directory;
        load();
    }

    /**
     * Deletes the manifest file
     */
    private void destroy() {
        File manifestFile = new File(mDirectory, FILE_NAME);
        manifestFile.delete();
    }

    /**
     * Reads the manifest file from the disk
     */
    private void load() {
        File manifestFile = new File(mDirectory, FILE_NAME);
        if (manifestFile.exists()) {
            try {
                mManifest = new JSONObject(FileUtils.readFileToString(manifestFile));
            } catch (Exception e) {
                Logger.e(this.getClass().getName(), "failed to load the manifest", e);
            }
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
        File manifestFile = new File(mDirectory, FILE_NAME);
        try {
            FileUtils.writeStringToFile(manifestFile, mManifest.toString());
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to write the manifest", e);
        }
    }
}
