package com.door43.translationstudio.core;

import org.unfoldingword.tools.logger.Logger;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.util.FileUtilities;
import com.door43.util.Zip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

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
            Logger.e(this.getClass().getName(), "Failed to download file " + apiUrl, e);
            return null;
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private boolean requestToFile(String apiUrl, File outputFile, long expectedSize, Library.OnProgressListener listener) {
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

            ReadableByteChannel channel = Channels.newChannel(conn.getInputStream());
            FileOutputStream os = new FileOutputStream(outputFile);

            long listenerInterval = 1048 * 10; // how much must be downloaded before each listener update
            long bytesRead;
            long pos = 0;
            long lastUpdate = 0;
            do {
                bytesRead = os.getChannel().transferFrom(channel, pos, expectedSize);
                pos += bytesRead;
                if(pos - lastUpdate >= listenerInterval) {
                    lastUpdate = pos;
                    listener.onProgress((int) pos, (int) expectedSize);
                }
            } while (bytesRead > 0);
            listener.onProgress((int)pos, (int) expectedSize);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Downloads the project catalog from the server
     */
    public boolean downloadProjectList(LibraryData targetIndex) {
        String catalog = request(mRootApiUrl);
        if(catalog != null) {
            return targetIndex.indexProjects(catalog);
        }
        return false;
    }

    /**
     * Downloads the chunk markers for a project from the server
     * @param projectSlug
     * @param targetIndex
     * @return
     */
    public boolean downloadChunkMarkerList(String projectSlug, LibraryData targetIndex) {
        Project project = targetIndex.getProject(projectSlug, App.getDeviceLanguageCode());
        if(project != null && project.chunkMarkerCatalog != null
                && (project.chunkMarkerCatalogLocalDateModified < project.chunkMarkerCatalogServerDateModified
                || project.chunkMarkerCatalogServerDateModified == 0)) {
            String catalog = request(project.chunkMarkerCatalog);
            if(catalog != null) {
                if(targetIndex.indexChunkMarkers(projectSlug, catalog)) {
                    targetIndex.markChunkMarkerCatalogUpToDate(projectSlug);
                }
            }
        }
        return true;
    }

    /**
     * Downloads the source languages for a project from the server
     * @param projectSlug
     * @param targetIndex
     * @return
     */
    public boolean downloadSourceLanguageList(String projectSlug, LibraryData targetIndex) {
        Project project = targetIndex.getProject(projectSlug, App.getDeviceLanguageCode());
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
    public boolean downloadResourceList(String projectSlug, String sourceLanguageSlug, LibraryData targetIndex) {
        SourceLanguage sourceLanguage = targetIndex.getSourceLanguage(projectSlug, sourceLanguageSlug);
        if(sourceLanguage != null && sourceLanguage.resourceCatalog != null
                && (sourceLanguage.resourceCatalogLocalDateModified < sourceLanguage.resourceCatalogServerDateModified
                || sourceLanguage.resourceCatalogServerDateModified == 0)) {
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
    public boolean downloadSource(SourceTranslation translation, LibraryData targetIndex) {
        Resource resource = targetIndex.getResource(translation);
        if(resource != null && resource.getSourceCatalogUrl() != null
                && (resource.getSourceDateModified() < resource.getSourceServerDateModified()
                || resource.getSourceServerDateModified() == 0)) {
            String catalog = request(resource.getSourceCatalogUrl());
            if(catalog != null) {
                return targetIndex.indexSource(translation, catalog);
            } else {
                Logger.w(this.getClass().getName(), "Failed to fetch the catalog from " + resource.getSourceCatalogUrl());
                return false;
            }
        }
        return true;
    }

    /**
     * Downloads the translationWords for a source translation from the server
     * @param translation
     * @param targetIndex the index to which the terms will be downloaded
     * @return
     */
    public boolean downloadWords(SourceTranslation translation, LibraryData targetIndex) {
        Resource resource = targetIndex.getResource(translation);
        if(resource != null && resource.getWordsCatalogUrl() != null
                && (resource.getWordsDateModified() < resource.getWordsServerDateModified()
                || resource.getWordsServerDateModified() == 0)) {
            String catalog = request(resource.getWordsCatalogUrl());
            if(catalog != null) {
                return targetIndex.indexTranslationWords(translation, catalog);
            } else {
                Logger.w(this.getClass().getName(), "Failed to fetch the catalog from " + resource.getWordsCatalogUrl());
            }
        }
        return true;
    }

    /**
     * Downloads the translationWord assignments for a source translation from the server
     * @param translation
     * @param targetIndex the index to which the term assignments will be downloaded
     * @return
     */
    public boolean downloadWordAssignments(SourceTranslation translation, LibraryData targetIndex) {
        Resource resource = targetIndex.getResource(translation);
        if(resource != null && resource.getWordAssignmentsCatalogUrl() != null
                && (resource.getWordAssignmentsDateModified() < resource.getWordAssignmentsServerDateModified()
                || resource.getWordAssignmentsServerDateModified() == 0)) {
            String catalog = request(resource.getWordAssignmentsCatalogUrl());
            if(catalog != null) {
                return targetIndex.indexTermAssignments(translation, catalog);
            } else {
                Logger.w(this.getClass().getName(), "Failed to fetch the catalog from " + resource.getWordAssignmentsCatalogUrl());
            }
        }
        return true;
    }

    /**
     * Downloads the translationNotes for a source translation from the server
     * @param translation
     * @param targetIndex the index to which the notes will be downloaded
     * @return
     */
    public boolean downloadNotes(SourceTranslation translation, LibraryData targetIndex) {
        Resource resource = targetIndex.getResource(translation);
        if(resource != null && resource.getNotesCatalogUrl() != null
                && (resource.getNotesDateModified() < resource.getNotesServerDateModified()
                || resource.getNotesServerDateModified() == 0)) {
            String catalog = request(resource.getNotesCatalogUrl());
            if(catalog != null) {
                return targetIndex.indexTranslationNotes(translation, catalog);
            } else {
                Logger.w(this.getClass().getName(), "Failed to fetch the catalog from " + resource.getNotesCatalogUrl());
            }
        }
        return true;
    }

    /**
     * Downloads the checkingQuestions for a source translation from the server
     * @param translation
     * @param targetIndex the index to which the checking questions will be downloaded
     * @return
     */
    public boolean downloadCheckingQuestions(SourceTranslation translation, LibraryData targetIndex) {
        Resource resource = targetIndex.getResource(translation);
        if(resource != null && resource.getQuestionsCatalogUrl() != null
                && (resource.getQuestionsDateModified() < resource.getQuestionsServerDateModified()
                || resource.getQuestionsServerDateModified() == 0)) {
            String catalog = request(resource.getQuestionsCatalogUrl());
            if(catalog != null) {
                return targetIndex.indexQuestions(translation, catalog);
            } else {
                Logger.w(this.getClass().getName(), "Failed to fetch the catalog from " + resource.getQuestionsCatalogUrl());
            }
        }
        return true;
    }

    public boolean downloadImages(Library.OnProgressListener listener) {
        // TODO: 1/21/2016 we need to be sure to download images for the correct project. right now only obs has images
        // eventually the api will be updated so we can easily download the correct images.

        String url = Resource.getImagesCatalogUrl();
        String filename = url.replaceAll(".*/", "");
        File imagesDir = App.getLibrary().getImagesDir();
        File fullPath = new File(String.format("%s/%s", imagesDir, filename));
        if (!(imagesDir.isDirectory() || imagesDir.mkdirs())) {
            return false;
        }
        boolean success = requestToFile(url, fullPath, Resource.getImagesCatalogSize(), listener);

        if (success) {
            try {
                File tempDir = new File(imagesDir, "temp");
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
                                FileUtilities.moveOrCopyQuietly(f, new File(imagesDir, f.getName()));
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

    /**
     * Downloads the target languages from the server
     * @param targetIndex
     * @return
     */
    public boolean downloadTargetLanguages(LibraryData targetIndex) {
        // TODO: 10/19/2015 don't hardcode the url
        String catalog = request("http://td.unfoldingword.org/exports/langnames.json");
        if(catalog != null) {
            return targetIndex.indexTargetLanguages(catalog);
        }
        return false;
    }

    /**
     * Downloads the target languages from the server
     * @param targetIndex
     * @return
     */
    public boolean downloadTempTargetLanguages(LibraryData targetIndex) {
        // TODO: 10/19/2015 this will eventually be pulled from the library data
        String catalog = request("http://td-demo.unfoldingword.org/api/templanguages/");
        if(catalog != null) {
            return targetIndex.indexTempTargetLanguages(catalog);
        }
        return false;
    }

    /**
     * Downloads the target languages from the server
     * @param targetIndex
     * @return
     */
    public boolean downloadTempTargetLanguageAssignments(LibraryData targetIndex) {
        // TODO: 10/19/2015 this will eventually be pulled from the library data
        String catalog = request("http://td-demo.unfoldingword.org/api/templanguages/assignment/changed/");
        if(catalog != null) {
            return targetIndex.indexTempTargetLanguageAssignments(catalog);
        }
        return false;
    }

    /**
     * Downloads the new language questionnaire from the server
     * @param targetIndex
     * @return
     */
    public boolean downloadNewLanguageQuestionnaire(LibraryData targetIndex) {
        // TODO: eventually this will be pulled from the library data
        String catalog = request(App.context().getResources().getString(R.string.questionnaire_api));
        if(catalog != null) {
            return targetIndex.indexQuestionnaire(catalog);
        }
        return false;
    }
}
