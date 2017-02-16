package com.door43.translationstudio.core;

import android.util.Log;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.util.FileUtilities;
import com.door43.util.Zip;


import org.unfoldingword.tools.http.GetRequest;
import org.unfoldingword.tools.http.Request;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by blm on 12/28/16.  Revived from pre-resource container code.
 * This is a temporary solution to downloading images until a resource container solution
 * is ready.
 */
public class DownloadImages {
    public static final String TAG = DownloadImages.class.getName();
    private static final String IMAGES_URL = "https://cdn.unfoldingword.org/obs/jpg/obs-images-360px.zip";
    public static final int IMAGES_CATALOG_SIZE = 37620940; // this value doesn't seem to matter since GetRequest knows the size of the download
    public static final int TOTAL_FILE_COUNT = 598;
    private File mImagesDir;


    private boolean requestToFile(String apiUrl, File outputFile, final long expectedSize, final OnProgressListener listener) {
        if(apiUrl.trim().isEmpty()) {
            return false;
        }
        URL url;
        try {
            url = new URL(apiUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }

        final String outOf = App.context().getResources().getString(R.string.out_of);
        final String mbDownloaded = App.context().getResources().getString(R.string.mb_downloaded);

        GetRequest r = new GetRequest(url);
        r.setTimeout(5000);
        r.setProgressListener(new Request.OnProgressListener() {
            @Override
            public void onProgress(long max, long progress) {
                if(listener != null) {
                    if(max == 0) {
                        max = expectedSize;
                    }
                    String message = String.format("%2.2f %s %2.2f %s",
                            progress / (1024f * 1024f),
                            outOf,
                            max / (1024f * 1024f),
                            mbDownloaded);
                    listener.onProgress((int)progress, (int)max, message);
//                    Log.i(TAG,  "Download progress - " + progress + "out of " + max);
                }
            }

            @Override
            public void onIndeterminate() {
                if(listener != null) {
                    listener.onIndeterminate();
                }
            }
        });

        try {
            r.download(outputFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *
     * @param listener
     * @return
     */
    public boolean download(OnProgressListener listener) {
        // TODO: 1/21/2016 we need to be sure to download images for the correct project. right now only obs has images
        // eventually the api will be updated so we can easily download the correct images.

        String url = IMAGES_URL;
        String filename = url.replaceAll(".*/", "");
        File basePath = App.publicDir();
        mImagesDir = new File(basePath, "assets/images");
        File fullPath = new File(String.format("%s/%s", mImagesDir, filename));
        if (mImagesDir == null) {
            return false;
        }

        if(!mImagesDir.isDirectory()) { // make sure folder exists
            mImagesDir.mkdirs();
        }

        if (!mImagesDir.isDirectory()) {
            return false;
        }

        boolean success = requestToFile(url, fullPath, IMAGES_CATALOG_SIZE, listener);
        if (success) {
            int fileCount = 0;
            try {
                File tempDir = new File(mImagesDir, "temp");
                tempDir.mkdirs();

                String outOf = App.context().getResources().getString(R.string.out_of);
                String unpacking = App.context().getResources().getString(R.string.unpacking);
                listener.onProgress((int)0, (int)100, unpacking);
                Log.i(TAG,  "unpacking: ");

                Zip.unzip(fullPath, tempDir);
                success = true;
                fullPath.delete();
                // move files out of dir
                File[] extractedFiles = tempDir.listFiles();
                if(extractedFiles != null) {
                    for (File dir:extractedFiles) {
                        if(dir.isDirectory()) {
                            for(File f:dir.listFiles()) {
                                FileUtilities.moveOrCopyQuietly(f, new File(mImagesDir, f.getName()));

                                String message = String.format("%s: %d %s %d",
                                        unpacking,
                                        ++fileCount,
                                        outOf,
                                        TOTAL_FILE_COUNT);
                                listener.onProgress(fileCount, TOTAL_FILE_COUNT, message);
//                                Log.i(TAG,  "Download progress - " + fileCount + " out of " + TOTAL_FILE_COUNT);
                            }
                        }
                    }
                }
                FileUtilities.deleteQuietly(tempDir);
            } catch (IOException e) {
                success = false;
            }
        }
        return success;
    }

    public interface OnProgressListener {
        /**
         * Progress the progress on an operation between 0 and max
         * @param progress
         * @param max
         * @return the process should stop if returns false
         */
        boolean onProgress(int progress, int max, String message);

        /**
         * Identifes the current task as not quantifiable
         * @return the process should stop if returns false
         */
        boolean onIndeterminate();
    }

    public File getImagesDir() {
        return mImagesDir;
    }
}
