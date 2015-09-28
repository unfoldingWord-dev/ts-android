package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by joel on 8/27/2015.
 */
public class Downloader {
    private final String mRootApiUrl;
    private final Indexer mIndex;

    public Downloader(Indexer downloadIndex, String apiUrl) {
        mIndex = downloadIndex;
        mRootApiUrl = apiUrl;
    }

    /**
     * Returns the downloader's index
     * @return
     */
    public Indexer getIndex() {
        return mIndex;
    }

    /**
     * Returns the url from an object
     * @param json
     * @param urlProperty
     * @return
     */
    private String getUrlFromObject(JSONObject json, String urlProperty) {
        if(json.has(urlProperty)) {
            try {
                return json.getString(urlProperty);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Downloads content from a url and returns it as a string
     * @param apiUrl the url from which the content will be downloaded
     * @return
     */
    private String request(String apiUrl) {
        URL url;
        try {
            url = new URL(apiUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        try {
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);

            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            ByteArrayBuffer bab = new ByteArrayBuffer(5000);
            int current;
            while ((current = bis.read()) != -1) {
                bab.append((byte) current);
            }
            return new String(bab.toByteArray(), "UTF-8");
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Downloads the project catalog from the server
     */
    public Boolean downloadProjectList() {
        String catalog = request(mRootApiUrl);
        if(catalog != null) {
            return mIndex.indexProjects(catalog);
        }
        return false;
    }

    /**
     * Downloads the source languages for a project from the server
     * @param projectId
     * @return
     */
    public Boolean downloadSourceLanguageList(String projectId) {
        String catalogApiUrl = getUrlFromObject(mIndex.getProject(projectId), "lang_catalog");
        if(catalogApiUrl != null) {
            String catalog = request(catalogApiUrl);
            if(catalog != null) {
                return mIndex.indexSourceLanguages(projectId, catalog);
            }
        }
        return false;
    }

    /**
     * Downloads the resources for a source language from the server
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    public Boolean downloadResourceList(String projectId, String sourceLanguageId) {
        String catalogApiUrl = getUrlFromObject(mIndex.getSourceLanguage(projectId, sourceLanguageId), "res_catalog");
        if(catalogApiUrl != null) {
            String catalog = request(catalogApiUrl);
            if(catalog != null) {
                return mIndex.indexResources(projectId, sourceLanguageId, catalog);
            }
        }
        return false;
    }

    /**
     * Downloads the source for a source translation from the server
     * @param translation
     * @return
     */
    public Boolean downloadSource(SourceTranslation translation) {
        String catalogApiUrl = getUrlFromObject(mIndex.getResource(translation), "source");
        if(catalogApiUrl != null) {
            String catalog = request(catalogApiUrl);
            if(catalog != null) {
                return mIndex.indexSource(translation, catalog);
            } else {
                Logger.w(this.getClass().getName(), "Failed to fetch the catalog from " + catalogApiUrl);
            }
        }
        return false;
    }

    /**
     * Downloads the translationWords for a source translation from the server
     * @param translation
     * @return
     */
    public Boolean downloadTerms(SourceTranslation translation) {
        String catalogApiUrl = getUrlFromObject(mIndex.getResource(translation), "terms");
        if(catalogApiUrl != null) {
            String catalog = request(catalogApiUrl);
            if(catalog != null) {
                return mIndex.indexTerms(translation, catalog);
            }
        }
        return false;
    }

    /**
     * Downloads the translationWord assignments for a source translation from the server
     * @param translation
     * @return
     */
    public Boolean downloadTermAssignments(SourceTranslation translation) {
        String catalogApiUrl = getUrlFromObject(mIndex.getResource(translation), "tw_cat");
        if(catalogApiUrl != null) {
            String catalog = request(catalogApiUrl);
            if(catalog != null) {
                return mIndex.indexTermAssignments(translation, catalog);
            }
        }
        return false;
    }

    /**
     * Downloads the translationNotes for a source translation from the server
     * @param translation
     * @return
     */
    public Boolean downloadNotes(SourceTranslation translation) {
        String catalogApiUrl = getUrlFromObject(mIndex.getResource(translation), "notes");
        if(catalogApiUrl != null) {
            String catalog = request(catalogApiUrl);
            if(catalog != null) {
                return mIndex.indexNotes(translation, catalog);
            }
        }
        return false;
    }

    /**
     * Downloads the checkingQuestions for a source translation from the server
     * @param translation
     * @return
     */
    public Boolean downloadCheckingQuestions(SourceTranslation translation) {
        String catalogApiUrl = getUrlFromObject(mIndex.getResource(translation), "checking_questions");
        if(catalogApiUrl != null) {
            String catalog = request(catalogApiUrl);
            if(catalog != null) {
                return mIndex.indexQuestions(translation, catalog);
            }
        }
        return false;
    }
}
