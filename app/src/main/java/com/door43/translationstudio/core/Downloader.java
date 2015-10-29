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

    public Downloader(String apiUrl) {
        mRootApiUrl = apiUrl;
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
    public boolean downloadProjectList(Indexer targetIndex) {
        String catalog = request(mRootApiUrl);
        if(catalog != null) {
            return targetIndex.indexProjects(catalog);
        }
        return false;
    }

    /**
     * Downloads the source languages for a project from the server
     * @param projectSlug
     * @return
     */
    public boolean downloadSourceLanguageList(String projectSlug, Indexer targetIndex) {
        Project project = targetIndex.getProject(projectSlug);
        if(project != null && project.sourceLanguageCatalog != null
                && (project.sourceLanguageCatalogLocalDateModified < project.sourceLanguageCatalogServerDateModified
                || project.sourceLanguageCatalogServerDateModified == 0)) {
            String catalog = request(project.sourceLanguageCatalog);
            if(catalog != null) {
                if(targetIndex.indexSourceLanguages(projectSlug, catalog)) {
                    targetIndex.markSourceLanguageCatalogUpToDate(projectSlug);
                }
            }
        }
        return true;
    }

    /**
     * Downloads the resources for a source language from the server
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public boolean downloadResourceList(String projectSlug, String sourceLanguageSlug, Indexer targetIndex) {
        SourceLanguage sourceLanguage = targetIndex.getSourceLanguage(projectSlug, sourceLanguageSlug);
        if(sourceLanguage != null && sourceLanguage.resourceCatalog != null
                && (sourceLanguage.resourceCatalogLocalDateModified < sourceLanguage.resourceCatalogServerDateModified
                | sourceLanguage.resourceCatalogServerDateModified == 0)) {
            String catalog = request(sourceLanguage.resourceCatalog);
            if(catalog != null) {
                if(targetIndex.indexResources(projectSlug, sourceLanguageSlug, catalog)) {
                    targetIndex.markResourceCatalogUpToDate(projectSlug, sourceLanguageSlug);
                }
            }
        }
        return true;
    }

    /**
     * Downloads the source for a source translation from the server
     * @param translation
     * @param targetIndex the index into which the source will be downloaded
     * @return
     */
    public boolean downloadSource(SourceTranslation translation, Indexer targetIndex) {
        Resource resource = targetIndex.getResource(translation);
        if(resource != null && resource.getSourceCatalogUrl() != null) {
            String catalog = request(resource.getSourceCatalogUrl());
            if(catalog != null) {
                return targetIndex.indexSource(translation, catalog);
            } else {
                Logger.w(this.getClass().getName(), "Failed to fetch the catalog from " + resource.getSourceCatalogUrl());
            }
        }
        return false;
    }

    /**
     * Downloads the translationWords for a source translation from the server
     * @param translation
     * @param targetIndex the index to which the terms will be downloaded
     * @return
     */
    public boolean downloadWords(SourceTranslation translation, Indexer targetIndex) {
        Resource resource = targetIndex.getResource(translation);
        if(resource != null && resource.getWordsCatalogUrl() != null) {
            String catalog = request(resource.getWordsCatalogUrl());
            if(catalog != null) {
                return targetIndex.indexWords(translation, catalog);
            }
        }
        return false;
    }

    /**
     * Downloads the translationWord assignments for a source translation from the server
     * @param translation
     * @param targetIndex the index to which the term assignments will be downloaded
     * @return
     */
    public boolean downloadWordAssignments(SourceTranslation translation, Indexer targetIndex) {
        Resource resource = targetIndex.getResource(translation);
        if(resource != null && resource.getWordAssignmentsCatalogUrl() != null) {
            String catalog = request(resource.getWordAssignmentsCatalogUrl());
            if(catalog != null) {
                return targetIndex.indexTermAssignments(translation, catalog);
            }
        }
        return false;
    }

    /**
     * Downloads the translationNotes for a source translation from the server
     * @param translation
     * @param targetIndex the index to which the notes will be downloaded
     * @return
     */
    public boolean downloadNotes(SourceTranslation translation, Indexer targetIndex) {
        Resource resource = targetIndex.getResource(translation);
        if(resource != null && resource.getNotesCatalogUrl() != null) {
            String catalog = request(resource.getNotesCatalogUrl());
            if(catalog != null) {
                return targetIndex.indexNotes(translation, catalog);
            }
        }
        return false;
    }

    /**
     * Downloads the checkingQuestions for a source translation from the server
     * @param translation
     * @param targetIndex the index to which the checking questions will be downloaded
     * @return
     */
    public boolean downloadCheckingQuestions(SourceTranslation translation, Indexer targetIndex) {
        Resource resource = targetIndex.getResource(translation);
        if(resource != null && resource.getQuestionsCatalogUrl() != null) {
            String catalog = request(resource.getQuestionsCatalogUrl());
            if(catalog != null) {
                return targetIndex.indexQuestions(translation, catalog);
            }
        }
        return false;
    }

    /**
     * Downloads the target languages from the server
     * @param targetIndex
     * @return
     */
    public boolean downloadTargetLanguages(Indexer targetIndex) {
        // TODO: 10/19/2015 don't hardcode the url
        String catalog = request("http://td.unfoldingword.org/exports/langnames.json");
        if(catalog != null) {
            return targetIndex.indexTargetLanguages(catalog);
        }
        return false;
    }
}
