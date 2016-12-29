package com.door43.translationstudio.core;

import com.door43.translationstudio.App;
import com.door43.util.FileUtilities;
import com.door43.util.Zip;


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
 */

public class DownloadImages {
    public static final String TAG = DownloadImages.class.getName();
    private static final String IMAGES_URL = "https://cdn.unfoldingword.org/obs/jpg/obs-images-360px.zip";
    public static final int IMAGES_CATALOG_SIZE = 37620940;
    private File mImagesDir;

    /**
     * Downloads content from a url and returns it as a string
     * @param apiUrl the url from which the content will be downloaded
     * @return
     */
    private String request(String apiUrl) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(apiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(5000);
            urlConnection.setReadTimeout(5000);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String response = FileUtilities.readStreamToString(in);
            urlConnection.disconnect();
            return response;
        } catch (IOException e) {
            Logger.e(TAG, "Failed to download file " + apiUrl, e);
            return null;
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private boolean requestToFile(String apiUrl, File outputFile, long expectedSize, OnProgressListener listener) {
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

        try {
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);

            FileOutputStream fos = new FileOutputStream(outputFile);

            int updateInterval = 1048 * 50; // send an update each time some bytes have been downloaded
            int updateQueue = 0;
            int bytesRead = 0;

            InputStream is = new BufferedInputStream(conn.getInputStream());
            byte[] buffer = new byte[4096];
            int n = 0;
            while((n = is.read(buffer)) != -1) {
                bytesRead += n;
                updateQueue += n;
                fos.write(buffer, 0, n);

                // send updates
                if(updateQueue >= updateInterval) {
                    updateQueue = 0;
                    listener.onProgress(bytesRead, (int) expectedSize);
                }
            }

            listener.onProgress(bytesRead, (int) expectedSize);

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
