package com.door43.translationstudio.core;

import com.door43.translationstudio.App;
import com.door43.util.FileUtilities;
import com.door43.util.Zip;


import org.unfoldingword.tools.http.GetRequest;
import org.unfoldingword.tools.http.Request;
import org.unfoldingword.tools.logger.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by blm on 12/28/16.  Revived from pre-resource container code.
 * This is a temporary solution to downloading images until a resource container solution
 * is ready.
 */
public class DownloadImages {
    public static final String TAG = DownloadImages.class.getName();
    private static final String IMAGES_URL = "https://cdn.unfoldingword.org/obs/jpg/obs-images-360px.zip";
    public static final int IMAGES_CATALOG_SIZE = 37620940;
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

        GetRequest r = new GetRequest(url);
        r.setTimeout(5000);
        r.setProgressListener(new Request.OnProgressListener() {
            @Override
            public void onProgress(long max, long progress) {
                if(listener != null) {
                    if(max == 0) max = expectedSize;
                    listener.onProgress((int)progress, (int)max);
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

        boolean success = requestToFile(url, fullPath, (int) (IMAGES_CATALOG_SIZE * 1.05f), listener);
        if (success) {
            try {
                File tempDir = new File(mImagesDir, "temp");
                tempDir.mkdirs();
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
        boolean onProgress(int progress, int max);

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
