package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

/**
 * Created by blm on 4/2/16.
 */
public class ImportUsfm {
    public static final String TAG = ImportUsfm.class.getSimpleName();
    public JSONObject chunkMap;

    public ImportUsfm() {
        chunkMap = null;
    }
    
    public boolean importStream(InputStream usfmStream) {

        boolean success = true;
        File tempLibraryDir = null;
        try {
            tempLibraryDir = new File(AppContext.context().getCacheDir(), System.currentTimeMillis() + "");
            tempLibraryDir.mkdirs();
            Zip.unzipFromStream(usfmStream, tempLibraryDir);
            File[] usfmFiles = tempLibraryDir.listFiles();

            for (File usfmFile : usfmFiles) {
                processFolder(usfmFile);
            }
            Logger.i(TAG, "found files: " + usfmFiles);

        } catch (Exception e) {
            Logger.e(TAG, "error reading stream ", e);
        }

        FileUtils.deleteQuietly(tempLibraryDir);
        return success;
    }

    public boolean processFolder(File usfmFile) {
        Logger.i(TAG, "processing folder: " + usfmFile.toString());

        if (usfmFile.isDirectory()) {
            File[] usfmSubFiles = usfmFile.listFiles();
            for (File usfmSubFile : usfmSubFiles) {
                processFolder(usfmSubFile);
            }
            Logger.i(TAG, "found files: " + usfmSubFiles.toString());
        } else {
            processFile(usfmFile);
        }
        return true;
    }

    public boolean processFile(File usfmFile) {
        Logger.i(TAG, "processing file: " + usfmFile.toString());
        return true;
    }
}