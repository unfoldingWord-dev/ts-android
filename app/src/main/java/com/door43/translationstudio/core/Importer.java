package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the importing of tstudio archives.
 * The importing is placed here to keep the Translator clean and organized.
 */
public class Importer {
    private Importer() {

    }

    /**
     * Prepares an archive for import with backwards compatable support.
     * @param expandedArchiveDir
     * @return an array of target translation directories that are ready and valid for import
     * @throws Exception
     */
    public static File[] importArchive(File expandedArchiveDir) throws Exception {
        File manifestFile = new File(expandedArchiveDir, "manifest.json");
        if(manifestFile.exists()) {

            JSONObject manifestJson = new JSONObject(FileUtils.readFileToString(manifestFile));
            if(manifestJson.has("package_version")) {
                int packageVersion = manifestJson.getInt("package_version");
                switch (packageVersion) {
                    case 1:
                        return v1(expandedArchiveDir); // just to keep the switch pretty
                    case 2:
                        return v2(manifestJson, expandedArchiveDir);
                    default:
                        return new File[0];
                }
            } else {
                // version 1 manifest change significantly
                return v1(expandedArchiveDir);
            }
        }
        return legacy();
    }

    /**
     * translation dirs in the archive are named after their id
     * so we only need to return the path.
     * @param manifest
     * @param dir
     * @return
     * @throws JSONException
     */
    private static File[] v2(JSONObject manifest, File dir) throws JSONException {
        List<File> files = new ArrayList<>();
        JSONArray translationsJson = manifest.getJSONArray("target_translations");
        for(int i = 0; i < translationsJson.length(); i ++) {
            JSONObject translation = translationsJson.getJSONObject(i);
            files.add(new File(dir, translation.getString("path")));
        }
        return files.toArray(new File[files.size()]);
    }

    /**
     * targetTranslations are in directories labled by id
     * @param dir
     * @return
     */
    private static File[] v1(File dir) {
        // TODO: 10/5/2015 need to provide support for v1 archives
        return new File[0];
    }

    /**
     * todo: provide support for legacy archives
     * @return
     */
    private static File[] legacy() {
        return new File[0];
    }
}
