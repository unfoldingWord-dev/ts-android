package com.door43.translationstudio.core;

import android.util.Log;

import com.door43.util.Zip;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by joel on 8/29/2015.
 */
public class Library {
    private final Downloader mDownloader;
    private final Indexer mServerIndex;
    private final Indexer mAppIndex;
    private LibraryUpdates mLibraryUpdates = new LibraryUpdates();

    public Library(Downloader downloader, Indexer serverIndex, Indexer appIndex) {
        mDownloader = downloader;
        mServerIndex = serverIndex;
        mAppIndex = appIndex;
    }

    private void downloadResourceList(String projectId, String sourceLanguageId) {
        if(mDownloader.downloadResourceList(projectId, sourceLanguageId)) {
            for(String resourceId:mDownloader.getIndex().getResources(projectId, sourceLanguageId)) {
                SourceTranslation sourceTranslation = new SourceTranslation(projectId, sourceLanguageId, resourceId);
                try {
                    int latestResourceModified = mDownloader.getIndex().getResource(sourceTranslation).getInt("date_modified");
                    JSONObject localResource = mAppIndex.getResource(sourceTranslation);
                    int localResourceModified = -1;
                    if(localResource != null) {
                        localResourceModified = localResource.getInt("date_modified");
                    }
                    if(localResourceModified == -1 || localResourceModified < latestResourceModified) {
                        // build update list
                        mLibraryUpdates.addUpdate(new SourceTranslation(projectId, sourceLanguageId, resourceId));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void downloadSourceLanguageList(String projectId) {
        if(mDownloader.downloadSourceLanguageList(projectId)) {
            for(String sourceLanguageId:mDownloader.getIndex().getSourceLanguages(projectId)) {
                try {
                    int latestSourceLanguageModified = mDownloader.getIndex().getSourceLanguage(projectId, sourceLanguageId).getInt("date_modified");
                    JSONObject lastSourceLanguage = mServerIndex.getSourceLanguage(projectId, sourceLanguageId);
                    int lastSourceLanguageModified = -1;
                    if(lastSourceLanguage != null) {
                        lastSourceLanguageModified = lastSourceLanguage.getInt("date_modified");
                    }
                    if(lastSourceLanguageModified == -1 || lastSourceLanguageModified < latestSourceLanguageModified) {
                        downloadResourceList(projectId, sourceLanguageId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns a list of update that are available on the server
     * @return
     */
    public LibraryUpdates getAvailableLibraryUpdates() {
        mLibraryUpdates = new LibraryUpdates();
        if(mDownloader.downloadProjectList()) {
            for (String projectId : mDownloader.getIndex().getProjects()) {
                try {
                    int latestProjectModified = mDownloader.getIndex().getProject(projectId).getInt("date_modified");
                    JSONObject lastProject = mServerIndex.getProject(projectId);
                    int lastProjectModified = -1;
                    if(lastProject != null) {
                        lastProjectModified = lastProject.getInt("date_modified");
                    }
                    if(lastProjectModified == -1 || lastProjectModified < latestProjectModified) {
                        downloadSourceLanguageList(projectId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            mServerIndex.mergeIndex(mDownloader.getIndex(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mLibraryUpdates;
    }

    /**
     * Downloads updates from the server
     * @param updates
     */
    public Boolean downloadUpdates(LibraryUpdates updates) throws Exception {
        boolean success = true;
        for(String projectId:updates.getUpdatedProjects()) {
            boolean projectDownloadSuccess = true;
            for(String sourceLanguageId:updates.getUpdatedSourceLanguages(projectId)) {
                for(String resourceId:updates.getUpdatedResources(projectId, sourceLanguageId)) {
                    projectDownloadSuccess = downloadSourceTranslationWithoutMerging(new SourceTranslation(projectId, sourceLanguageId, resourceId)) ? projectDownloadSuccess : false;
                    if(!projectDownloadSuccess) {
                        throw new Exception("Failed to download " + projectId + " " + sourceLanguageId + " " + resourceId);
                    }
                }
            }
            success = projectDownloadSuccess ? success : false;
            if(projectDownloadSuccess) {
                try {
                    mAppIndex.mergeProject(projectId, mDownloader.getIndex());
                } catch (IOException e) {
                    e.printStackTrace();
                    success = false;
                }
            }
        }
        return success;
    }

    public Boolean downloadSourceTranslation(SourceTranslation translation) {
        if(downloadSourceTranslationWithoutMerging(translation)) {
            try {
                mAppIndex.mergeSourceTranslation(translation, mDownloader.getIndex());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private Boolean downloadSourceTranslationWithoutMerging(SourceTranslation translation) {
        boolean success = true;
        success = mDownloader.downloadSource(translation) ? success : false;
        mDownloader.downloadTerms(translation); // optional to success of download
        mDownloader.downloadNotes(translation); // optional to success of download
        mDownloader.downloadCheckingQuestions(translation); // optional to success of download
        return success;
    }

    /**
     * Exports the library
     * @param destDir the directory where the library will be exported
     * @return the path to the exported file
     */
    public File export(File destDir) {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String date = s.format(new Date());
        File destFile = new File(destDir, "library_" + date + ".zip");
        try {
            Zip.zip(mAppIndex.getIndexDir().getPath(), destFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return destFile;
    }
}
