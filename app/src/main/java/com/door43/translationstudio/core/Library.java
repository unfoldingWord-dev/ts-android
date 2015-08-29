package com.door43.translationstudio.core;

import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.transform.Source;

/**
 * Created by joel on 8/29/2015.
 */
public class Library {
    private final Downloader mDownloader;
    private final Indexer mDownloadIndex;
    private final Indexer mServerIndex;
    private final Indexer mAppIndex;

    public Library(Downloader downloader, Indexer downloadIndex, Indexer serverIndex, Indexer appIndex) {
        mDownloader = downloader;
        mDownloadIndex = downloadIndex;
        mServerIndex = serverIndex;
        mAppIndex = appIndex;
    }

    private void downloadResourceList(String projectId, String sourceLanguageId) {
        if(mDownloader.downloadResourceList(projectId, sourceLanguageId)) {
            for(String resourceId:mDownloadIndex.getResources(projectId, sourceLanguageId)) {
                SourceTranslation sourceTranslation = new SourceTranslation(projectId, sourceLanguageId, resourceId);
                try {
                    int latestResourceModified = mDownloadIndex.getResource(sourceTranslation).getInt("date_modified");
                    JSONObject localResource = mAppIndex.getResource(sourceTranslation);
                    int localResourceModified = -1;
                    if(localResource != null) {
                        localResourceModified = localResource.getInt("date_modified");
                    }
                    if(localResourceModified == -1 || localResourceModified < latestResourceModified) {
                        // build update list
                        // TODO: build the update list
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void downloadSourceLanguageList(String projectId) {
        if(mDownloader.downloadSourceLanguageList(projectId)) {
            for(String sourceLanguageId:mDownloadIndex.getSourceLanguages(projectId)) {
                try {
                    int latestSourceLanguageModified = mDownloadIndex.getSourceLanguage(projectId, sourceLanguageId).getInt("date_modified");
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

    public void getAvailableLibraryUpdates() {
        if(mDownloader.downloadProjectList()) {
            for (String projectId : mDownloadIndex.getProjects()) {
                try {
                    int latestProjectModified = mDownloadIndex.getProject(projectId).getInt("date_modified");
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
        // TODO: return update list
    }
}
