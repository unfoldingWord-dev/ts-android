package com.door43.translationstudio.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.door43.tools.reporting.Logger;
import com.door43.util.Manifest;
import com.door43.util.Security;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;


/**
 * Created by joel on 8/26/2015.
 */
public class Indexer {
    private final IndexerSQLiteHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;
    private Manifest mManifest;
    private final String mId;
    private final File mIndexDir;

    private enum CatalogType {
        Simple,
        Source,
        Advanced,
        Questions,
        Terms
    };

    /**
     * Creates a new instance of the index
     * @param name the name of the index
     * @param rootDir the directory where the index's are stored
     */
    public Indexer(Context context, String name, File rootDir) {
        mId = name;
        mIndexDir = new File(rootDir, name);
        mManifest = reload();
        mDatabaseHelper = new IndexerSQLiteHelper(context, name);
        mDatabase = mDatabaseHelper.getWritableDatabase();
    }

    /**
     * Closes the index database
     */
    public synchronized void close() {
        mDatabaseHelper.close();
    }

    /**
     * Loads the index manifest and prepares the index for use.
     */
    public Manifest reload() {
        mIndexDir.mkdirs();
        Manifest m = Manifest.generate(mIndexDir);
        if(!m.has("version")) {
            m.put("version", 1);
        }
        return m;
    }

    /**
     * Returns the version of the indexer
     * @return
     */
    public int getVersion() {
        try {
            return mManifest.getInt("version");
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Destroys the entire index
     */
    public synchronized void destroy() {
        // reset manifest
        FileUtils.deleteQuietly(mIndexDir);
        mManifest = reload();
        // delete database
        close();
        mDatabaseHelper.deleteDatabase();
        // rebuild database
        mDatabase = mDatabaseHelper.getWritableDatabase();
    }

    /**
     * Returns the contents of a file in the index
     * @param path the relative path to the indexed file
     * @return a string or null
     */
    private synchronized String readFile(String hash, String path) {
        return mDatabaseHelper.readFile(mDatabase, hash, path);
    }

    /**
     * Returns the JSON contents of a file in the index
     * @param path the relative path to the indexed file
     * @return
     */
    private JSONObject readJSON(String hash, String path) {
        String contents = readFile(hash, path);
        if(contents != null) {
            try {
                return new JSONObject(contents);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Saves a string to a file in the index
     * @param path the relative path to the file
     * @param contents the contents to be written
     * @return true if the file was new
     */
    private synchronized Boolean saveFile(String hash, String path, String contents) throws IOException {
        mDatabaseHelper.replaceFile(mDatabase, hash, path, contents);
        return true;
    }

    /**
     * Creates or updates a catalog link
     * @param md5hash
     * @param linkPath
     * @return
     */
    private synchronized Boolean createLink (String md5hash, String linkPath) {
        mDatabaseHelper.replaceLink(mDatabase, md5hash, linkPath);
        return true;
    }

    /**
     * Generates a source link for the source translation
     * @param translation
     * @return
     */
    private String generateSourceLink (SourceTranslation translation) {
        return generateResourceFieldLink(translation, "source");
    }

    /**
     * Generates a notes link for the source translation
     * @param translation
     * @return
     */
    private String generateNotesLink (SourceTranslation translation) {
        return generateResourceFieldLink(translation, "notes");
    }

    /**
     * Generate a terms link for the source translation
     * @param translation
     * @return
     */
    private String generateTermsLink (SourceTranslation translation) {
        return generateResourceFieldLink(translation, "terms");
    }

    /**
     * Generates a term assignments link for the source translation
     * @param translation
     * @return
     */
    private String generateTermAssignmentsLink (SourceTranslation translation) {
        return generateResourceFieldLink(translation, "tw_cat");
    }

    /**
     * Generates a questions link for the source translation
     * @param translation
     * @return
     */
    private String generateQuestionsLink (SourceTranslation translation) {
        return generateResourceFieldLink(translation, "checking_questions");
    }

    /**
     * Generates a link for a field in the resources catalog.
     * For example, the "source", or "notes"
     *
     * @param translation
     * @param field
     * @return
     */
    private String generateResourceFieldLink (SourceTranslation translation, String field) {
        String catalogApiUrl = getUrlFromObject(getResource(translation), field);
        if(catalogApiUrl != null) {
            String md5hash = Security.md5(catalogApiUrl);
            String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/" + field + ".link";
            if(createLink(md5hash, catalogLinkFile)) {
                return md5hash;
            }
        }
        return null;
    }

    private synchronized Boolean indexItems (String md5hash, CatalogType type, String jsonString) {
        JSONArray items;
        try {
            items = new JSONArray(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        mDatabase.beginTransaction();
        // save items
        if(type == CatalogType.Simple) {
            for(int i = 0; i < items.length(); i ++ ) {
                try {
                    JSONObject item = items.getJSONObject(i);
                    if(item.has("slug") || item.has("id")) {
                        String itemPath;
                        if(item.has("slug")) {
                            itemPath = item.getString("slug");
                        } else {
                            itemPath = item.getString("id");
                        }
                        try {
                            saveFile(md5hash, itemPath, item.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if(type == CatalogType.Advanced) {
            for(int chapterIndex = 0; chapterIndex < items.length(); chapterIndex ++) {
                try {
                    JSONObject chapter = items.getJSONObject(chapterIndex);
                    String chapterId = chapter.getString("id");
                    JSONArray frames = chapter.getJSONArray("frames");
                    for(int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
                        try {
                            JSONObject frame = frames.getJSONObject(frameIndex);
                            String frameId = frame.getString("id");
                            JSONArray notes = frame.getJSONArray("items");
                            for(int noteIndex = 0; noteIndex < notes.length(); noteIndex ++) {
                                try {
                                    JSONObject note = notes.getJSONObject(noteIndex);
                                    String noteId = note.getString("id");
                                    // save note
                                    String itemPath = chapterId + "/" + frameId + "/" + noteId;
                                    try {
                                        saveFile(md5hash, itemPath, note.toString());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } catch(JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if(type == CatalogType.Source) {
            for(int chapterIndex = 0; chapterIndex < items.length(); chapterIndex ++ ) {
                try {
                    JSONObject chapter = items.getJSONObject(chapterIndex);
                    if(chapter.has("number")) {
                        String chapterId = chapter.getString("number");
                        // save chapter
                        JSONArray frames = new JSONArray();
                        if(chapter.has("frames")) {
                            frames = chapter.getJSONArray("frames");
                        }
                        chapter.remove("frames");
                        String chapterPath = chapterId + "/chapter.json";
                        try {
                            saveFile(md5hash, chapterPath, chapter.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // save frames
                        for (int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
                            try {
                                JSONObject frame = frames.getJSONObject(frameIndex);
                                String[] complexId = frame.getString("id").split("-");
                                String frameId = complexId[1];
                                String framePath = chapterId + "/" + frameId;
                                try {
                                    saveFile(md5hash, framePath, frame.toString());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (JSONException e) {
                    Logger.e(this.getClass().getName(), "Failed to process the source", e);
                }
            }
        } else if(type == CatalogType.Terms) {
            // TODO: eventually we'll index the terms dictionary here
        } else if(type == CatalogType.Questions) {
            // TODO: eventually we'll index the checking question dictionary here
        }
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        return true;
    }

    /**
     * Returns an array of items indexed under an object
     * @param itemObject
     * @param urlProperty
     * @return
     */
    private String[] getItemsArray(JSONObject itemObject, String urlProperty) {
        return getItemsArray(itemObject, urlProperty, null);
    }

    /**
     * Returns an array of items indexed under an object
     * @param itemObject
     * @param urlProperty
     * @param subFolder
     * @return
     */
    private synchronized String[] getItemsArray(JSONObject itemObject, String urlProperty, String subFolder) {
        if(itemObject == null) {
            return new String[0];
        }

        String catalogApiUrl = getUrlFromObject(itemObject, urlProperty);
        if(catalogApiUrl == null) {
            return new String[0];
        }
        String md5hash = Security.md5(catalogApiUrl);

        String[] extensionFilters = {"json"};
        return mDatabaseHelper.listDir(mDatabase, md5hash, subFolder, extensionFilters);
    }

    /**
     * Returns an array of contents indexed under an object.
     * This is just like getItemsArray except that it returns the contents of the items rather than the names
     * @param itemObject
     * @param urlProperty
     * @param subFolder
     * @return
     */
    private String[] getContentsArray(JSONObject itemObject, String urlProperty, String subFolder) {
        if(itemObject == null) {
            return new String[0];
        }

        String catalogApiUrl = getUrlFromObject(itemObject, urlProperty);
        if(catalogApiUrl == null) {
            return new String[0];
        }
        String md5hash = Security.md5(catalogApiUrl);
        String[] extensionFilters = {"json"};
        return mDatabaseHelper.listDirContents(mDatabase, md5hash, subFolder, extensionFilters);
    }

    /**
     * Returns the url from an object without any url parameters
     * @param json
     * @param urlProperty
     * @return
     */
    private String getUrlFromObject(JSONObject json, String urlProperty) {
        if(json != null && json.has(urlProperty)) {
            try {
                String[] list = json.getString(urlProperty).split("\\?");
                if (list.length > 0) {
                    return list[0];
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Returns the root catalog
     * @return
     */
    private JSONObject getRootCatalog() {
        JSONObject json = new JSONObject();
        try {
            json.put("proj_catalog", "_");
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Removes a project from the index
     * @param projectId
     */
    public synchronized void deleteProject (String projectId) {
        String catalogApiUrl = getUrlFromObject(getRootCatalog(), "proj_catalog");
        if(catalogApiUrl != null) {
            String md5hash = Security.md5(catalogApiUrl);
            String projectPath = projectId;
            mDatabaseHelper.deleteFile(mDatabase, md5hash, projectPath);
        }
    }

    /**
     * Removes a source language from the index
     * @param projectId
     * @param sourceLanguageId
     */
    public synchronized void deleteSourceLanguage (String projectId, String sourceLanguageId) {
        mDatabase.beginTransaction();
        for(String resourceId:getResources(projectId, sourceLanguageId)) {
            deleteResource(SourceTranslation.simple(projectId, sourceLanguageId, resourceId));
        }

        // delete the language catalog when there are no more source languages
        if(getSourceLanguages(projectId).length == 0) {
            String catalogLinkFile = projectId + "/languages_catalog.link";
            mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
        }
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Removes a resource from the index
     * @param translation
     */
    private synchronized void deleteResource (SourceTranslation translation) {
        deleteTerms(translation);
        deleteTermAssignments(translation);
        deleteQuestions(translation);
        deleteNotes(translation);
        deleteSource(translation);

        // TODO: we should not destroy then entire resources catalog here
        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/resources_catalog.link";
        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
    }

    /**
     * Removes the source for the source translation from the index
     * @param translation
     */
    private synchronized void deleteSource (SourceTranslation translation) {
        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/source.link";
        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
    }

    /**
     * Removes the translationNotes for the source translation from the index
     * @param translation
     */
    private synchronized void deleteNotes (SourceTranslation translation) {
        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/notes.link";
        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
    }

    /**
     * Removes the translationWords for the source translation from the index
     * @param translation
     */
    private synchronized void deleteTerms (SourceTranslation translation) {
        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/terms.link";
        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
    }

    private synchronized void deleteTermAssignments (SourceTranslation translation) {
        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/tw_cat.link";
        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
    }

    /**
     * Removes the checking questions for the source translation from the index
     * @param translation
     */
    private synchronized void deleteQuestions (SourceTranslation translation) {
        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/checking_questions.link";
        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
    }

    /**
     * Merges an index itno the current index
     * @param index
     * @throws IOException
     */
    public void mergeIndex(Indexer index) throws IOException {
        mergeIndex(index, false);
    }

    /**
     * Merges an index into the current index
     * @param index
     * @param shallow if true none of the source translation content will be merged
     */
    public void mergeIndex(Indexer index, Boolean shallow) throws IOException {
        for(String projectId:index.getProjects()) {
            mergeProject(projectId, index, shallow);
        }
    }

    /**
     * Merges a project into the current index
     * @param projectId
     * @param index
     * @throws IOException
     */
    public void mergeProject(String projectId, Indexer index) throws IOException {
        mergeProject(projectId, index, false);
    }

    /**
     * Merges a project into the current index
     * @param index
     * @param projectId
     * @param shallow if true none of the source translation content will be merged
     */
    public void mergeProject(String projectId, Indexer index, Boolean shallow) throws IOException {
        JSONObject newProject = index.getProject(projectId);
        if(newProject != null) {
            JSONArray projectJson = new JSONArray();
            projectJson.put(newProject);
            // update/add project
            indexProjects(projectJson.toString());

            for(String sourceLanguageId:index.getSourceLanguages(projectId)) {
                JSONObject newSourceLanguage = index.getSourceLanguage(projectId, sourceLanguageId);
                if(newSourceLanguage != null) {
                    JSONArray sourceLanguageJson = new JSONArray();
                    sourceLanguageJson.put(newSourceLanguage);
                    // update/add source language
                    indexSourceLanguages(projectId, sourceLanguageJson.toString());

                    for(String resourceId:index.getResources(projectId, sourceLanguageId)) {
                        SourceTranslation translation = SourceTranslation.simple(projectId, sourceLanguageId, resourceId);
                        JSONObject newResource = index.getResource(translation);
                        if(newResource != null) {
                            JSONArray resourceJson = new JSONArray();
                            resourceJson.put(newResource);
                            // update/add resource
                            indexResources(projectId, sourceLanguageId, resourceJson.toString());

                            // update/add source translation
                            if(!shallow) {
                                mergeResources(translation, index);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Merges a project into the current index.
     * This will only peform a shallow merge. That is, none of the resources will be merged.
     * Just the project, source language, and resource catalogs are merged.
     * @param sourceTranslation
     * @throws IOException
     */
    public void mergeSourceTranslationShalow(SourceTranslation sourceTranslation, Indexer index) throws IOException {
        JSONObject newProject = index.getProject(sourceTranslation.projectId);
        if(newProject != null) {
            JSONArray projectJson = new JSONArray();
            projectJson.put(newProject);
            // update/add project
            indexProjects(projectJson.toString());

            JSONObject newSourceLanguage = index.getSourceLanguage(sourceTranslation.projectId, sourceTranslation.sourceLanguageId);
            if(newSourceLanguage != null) {
                JSONArray sourceLanguageJson = new JSONArray();
                sourceLanguageJson.put(newSourceLanguage);
                // update/add source language
                indexSourceLanguages(sourceTranslation.projectId, sourceLanguageJson.toString());

                JSONObject newResource = index.getResource(sourceTranslation);
                if(newResource != null) {
                    JSONArray resourceJson = new JSONArray();
                    resourceJson.put(newResource);
                    // update/add resource
                    indexResources(sourceTranslation.projectId, sourceTranslation.sourceLanguageId, resourceJson.toString());
                }
            }
        }
    }

    /**
     * Merges a source translation into the current index
     * This consists of the source, notes, questions, and terms
     *
     * Note: this does NOT include the resource catalog since that is included in the definition
     * of a source translation not the actual content.
     *
     * @param translation
     * @param index
     */
    public void mergeResources(SourceTranslation translation, Indexer index) throws IOException {
        // delete old content
        deleteTerms(translation);
        deleteTermAssignments(translation);
        deleteQuestions(translation);
        deleteNotes(translation);
        deleteSource(translation);

        // re-create links
        generateSourceLink(translation);
        generateNotesLink(translation);
        generateQuestionsLink(translation);
        generateTermsLink(translation);
        generateTermAssignmentsLink(translation);

        // TODO: 10/2/2015 The below code is no longer valid. We should attache the two databases and import the data directly
        // http://stackoverflow.com/questions/10801567/is-it-possible-to-get-data-from-multi-databases-in-android-sqlite-or-any-quick
        // import source
        File sourceDir = index.getDataDir(index.readSourceLink(translation));
        File destSourceDir = getDataDir(readSourceLink(translation));
        if(sourceDir != null && sourceDir.exists()) {
            FileUtils.copyDirectory(sourceDir, destSourceDir);
        }

        // import notes
        File notesDir = index.getDataDir(index.readNotesLink(translation));
        File destNotesDir = getDataDir(readNotesLink(translation));
        if(notesDir != null && notesDir.exists()) {
            FileUtils.copyDirectory(notesDir, destNotesDir);
        }

        // import questions
        File questionsDir = index.getDataDir(index.readQuestionsLink(translation));
        File destQuestionsDir = getDataDir(readQuestionsLink(translation));
        if(questionsDir != null && questionsDir.exists()) {
            FileUtils.copyDirectory(questionsDir, destQuestionsDir);
        }

        // import terms
        File termsDir = index.getDataDir(index.readWordsLink(translation));
        File destTermsDir = getDataDir(readWordsLink(translation));
        if(termsDir != null && termsDir.exists()) {
            FileUtils.copyDirectory(termsDir, destTermsDir);
        }

        // import term assignments
        File termAssignmentsDir = index.getDataDir(index.readWordAssignmentsLink(translation));
        File destTermAssignmentsDir = getDataDir(readWordAssignmentsLink(translation));
        if(termAssignmentsDir != null && termAssignmentsDir.exists()) {
            FileUtils.copyDirectory(termAssignmentsDir, destTermAssignmentsDir);
        }
    }

    /**
     * Returns the index id
     * @return
     */
    public String getIndexId() {
        return mId;
    }

    /**
     * Returns the directory where this index is stored
     * @return
     */
    public File getIndexDir() {
        return mIndexDir;
    }

    /**
     * Builds a project index from json
     * @param catalog the json formatted project catalog
     * @return
     */
    public Boolean indexProjects(String catalog) {
        String catalogApiUrl = getUrlFromObject(getRootCatalog(), "proj_catalog");
        if(catalogApiUrl != null) {
            String md5hash = Security.md5(catalogApiUrl);
            String catalogLinkFile = "projects_catalog.link";
            if(createLink(md5hash, catalogLinkFile)) {
                return indexItems(md5hash, CatalogType.Simple, catalog);
            }
        }
        return false;
    }

    /**
     * Builds a source language index from json
     * @param projectId
     * @param catalog
     * @return
     */
    public Boolean indexSourceLanguages(String projectId, String catalog) {
        //KLUDGE: modify v2 sourceLanguages catalogJson to match expected catalogJson format
        JSONArray items = null;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        for (int i = 0; i < items.length(); i ++) {
            try {
                JSONObject item = items.getJSONObject(i);
                JSONObject language = item.getJSONObject("language");
                Iterator<String> keys = language.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    item.put(key, language.get(key));
                }
                item.remove("language");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        catalog = items.toString();
        //KLUDGE: end modify v2

        String catalogApiUrl = getUrlFromObject(getProject(projectId), "lang_catalog");
        if (catalogApiUrl != null) {
            String md5hash = Security.md5(catalogApiUrl);
            String catalogLinkFile = projectId + "/languages_catalog.link";
            if(createLink(md5hash, catalogLinkFile)) {
                return indexItems(md5hash, CatalogType.Simple, catalog);
            }
        }
        return false;
    }

    /**
     * Builds a resource index from json
     * @param projectId
     * @param sourceLanguageId
     * @param catalog
     * @return
     */
    public Boolean indexResources(String projectId, String sourceLanguageId, String catalog) {
        String catalogApiUrl = getUrlFromObject(getSourceLanguage(projectId, sourceLanguageId), "res_catalog");
        if(catalogApiUrl != null) {
            String md5hash = Security.md5(catalogApiUrl);
            String catalogLinkFile = projectId + "/" + sourceLanguageId + "/resources_catalog.link";
            if(createLink(md5hash, catalogLinkFile)) {
                return indexItems(md5hash, CatalogType.Simple, catalog);
            }
        }
        return false;
    }

    /**
     * Builds a source index from json
     * @param translation
     * @param catalog
     * @return
     */
    public Boolean indexSource(SourceTranslation translation, String catalog) {
        //KLUDGE: modify v2 sources catalogJson to match expected catalogJson format
        try {
            JSONObject catalogJson = new JSONObject(catalog);
            catalog = catalogJson.getJSONArray("chapters").toString();
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "Invalid catalog json",e);
            return false;
        }
        //KLUDGE: end modify v2

        String md5hash = generateSourceLink(translation);
        if(md5hash != null) {
            Logger.i(this.getClass().getName(), "indexing source for " + translation.getId());
            return indexItems(md5hash, CatalogType.Source, catalog);
        }
        return false;
    }

    /**
     * Builds a notes index from json
     *
     * @param translation
     * @param catalog
     * @return
     */
    public Boolean indexNotes(SourceTranslation translation, String catalog) {
        // TODO: 9/1/2015 eventually this catalog will include the terms and questions. We'll probably rename it at that point.

        //KLUDGE: modify v2 notes catalogJson to match expected catalogJson format
        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        JSONObject formattedCatalog = new JSONObject();
        for (int i = 0; i < items.length(); i ++) {
            try {
                JSONObject item = items.getJSONObject(i);
                String[] complexId = item.getString("id").split("-");
                String chapterId = complexId[0];
                String frameId = complexId[1];
                JSONArray notesJson = item.getJSONArray("tn");
                for(int j = 0; j < notesJson.length(); j ++) {
                    JSONObject note = notesJson.getJSONObject(j);
                    String noteId = Security.md5(note.getString("ref").trim().toLowerCase());
                    note.put("id", noteId);
                }

                // build chapter
                if(!formattedCatalog.has(chapterId)) {
                    JSONObject newChapterJson = new JSONObject();
                    newChapterJson.put("id", chapterId);
                    newChapterJson.put("frames", new JSONArray());
                    formattedCatalog.put(chapterId, newChapterJson);
                }

                // build frame
                JSONArray framesJson = formattedCatalog.getJSONObject(chapterId).getJSONArray("frames");
                JSONObject newFrameJson = new JSONObject();
                newFrameJson.put("id", frameId);
                newFrameJson.put("items", notesJson);
                framesJson.put(newFrameJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // repackage as json array
        Iterator x = formattedCatalog.keys();
        JSONArray jsonArray = new JSONArray();
        while (x.hasNext()){
            String key = (String) x.next();
            try {
                jsonArray.put(formattedCatalog.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        catalog = jsonArray.toString();
        //KLUDGE: end modify v2

        String md5hash = generateNotesLink(translation);
        if(md5hash != null) {
            return indexItems(md5hash, CatalogType.Advanced, catalog);
        }
        return false;
    }

    /**
     * Builds a terms index from json
     * @param translation
     * @param catalog
     * @return
     */
    public Boolean indexTerms(SourceTranslation translation, String catalog) {
        String md5hash = generateTermsLink(translation);
        if(md5hash != null) {
            return indexItems(md5hash, CatalogType.Simple, catalog);
        }

        return false;
    }

    /**
     * Builds a translationWord assignment index from json
     * @param translation
     * @param catalog
     * @return
     */
    public boolean indexTermAssignments(SourceTranslation translation, String catalog) {
        //KLUDGE: modify v2 questions catalogJson to match expected catalogJson format
        try {
            JSONObject items = new JSONObject(catalog);
            catalog = items.getJSONArray("chapters").toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        //KLUDGE: end modify v2\

        String md5hash = generateTermAssignmentsLink(translation);
        if(md5hash != null) {
            return indexItems(md5hash, CatalogType.Advanced, catalog);
        }

        return false;
    }

    /**
     * Builds a questions index from json
     * @param translation
     * @param catalog
     * @return
     */
    public Boolean indexQuestions(SourceTranslation translation, String catalog) {
        //KLUDGE: modify v2 questions catalogJson to match expected catalogJson format
        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        JSONObject formattedCatalog = new JSONObject();
        for (int i = 0; i < items.length(); i ++) {
            try {
                JSONObject item = items.getJSONObject(i);
                String chapterId = item.getString("id");

                // build chapter
                if(!formattedCatalog.has(chapterId)) {
                    JSONObject newChapterJson = new JSONObject();
                    newChapterJson.put("id", chapterId);
                    newChapterJson.put("frames", new JSONObject());
                    formattedCatalog.put(chapterId, newChapterJson);
                }
                JSONObject framesJson = formattedCatalog.getJSONObject(chapterId).getJSONObject("frames");

                // parse questions
                JSONArray questionsJson = item.getJSONArray("cq");
                for(int j = 0; j < questionsJson.length(); j ++) {
                    try {
                        JSONObject question = questionsJson.getJSONObject(j);
                        String questionId = Security.md5(question.getString("q").trim().toLowerCase());
                        question.put("id", questionId);

                        JSONArray referencesJson = question.getJSONArray("ref");
                        for (int k = 0; k < referencesJson.length(); k++) {
                            String[] complexId = referencesJson.getString(k).split("-");
                            String frameId = complexId[1];

                            // build frame
                            if (!framesJson.has(frameId)) {
                                JSONObject newFrameJson = new JSONObject();
                                newFrameJson.put("id", frameId);
                                newFrameJson.put("items", new JSONArray());
                                framesJson.put(frameId, newFrameJson);
                            }

                            // add questions
                            framesJson.getJSONObject(frameId).getJSONArray("items").put(question);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // repackage as json array
        Iterator x = formattedCatalog.keys();
        JSONArray jsonArray = new JSONArray();
        while (x.hasNext()){
            String chapterKey = (String) x.next();
            try {
                JSONObject chapter = formattedCatalog.getJSONObject(chapterKey);
                JSONArray frames = new JSONArray();
                Iterator y = chapter.getJSONObject("frames").keys();
                while(y.hasNext()) {
                    String frameKey = (String) y.next();
                    try {
                        frames.put(chapter.getJSONObject("frames").get(frameKey));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                chapter.put("frames", frames);
                jsonArray.put(chapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        catalog = jsonArray.toString();
        //KLUDGE: end modify v2

        String md5hash = generateQuestionsLink(translation);
        if(md5hash != null) {
            return indexItems(md5hash, CatalogType.Advanced, catalog);
        }
        return false;
    }

    /**
     * Returns an array of project ids
     * @return
     */
    public String[] getProjects() {
        return getItemsArray(getRootCatalog(), "proj_catalog");
    }

    /**
     * Returns a list of project contents
     * @return
     */
    public String[] getProjectsContents() {
        return getContentsArray(getRootCatalog(), "proj_catalog", null);
    }

    /**
     * Returns an array of source language ids
     * @param projectId
     * @return
     */
    public String[] getSourceLanguages(String projectId) {
        return getItemsArray(getProject(projectId), "lang_catalog");
    }

    /**
     * Returns an array of resource ids
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    public String[] getResources(String projectId, String sourceLanguageId) {
        return getItemsArray(getSourceLanguage(projectId, sourceLanguageId), "res_catalog");
    }

    /**
     * Returns an array of chapter ids
     * @param translation
     * @return
     */
    public String[] getChapters(SourceTranslation translation) {
        return getItemsArray(getResource(translation), "source");
    }

    /**
     * Returns an array of frame ids
     * @param translation
     * @param chapterId
     * @return
     */
    public String[] getFrames(SourceTranslation translation, String chapterId) {
        return getItemsArray(getResource(translation), "source", chapterId);
    }

    /**
     * Returns an array of translationNote ids
     * @param translation
     * @param chapterId
     * @param frameId
     * @return
     */
    public String[] getNotes(SourceTranslation translation, String chapterId, String frameId) {
        return getItemsArray(getResource(translation), "notes", chapterId + "/" + frameId);
    }

    /**
     * Returns an array of translationWord ids for a single frame
     * @param translation
     * @param chapterId
     * @param frameId
     * @return
     */
    public String[] getWords(SourceTranslation translation, String chapterId, String frameId) {
        return getItemsArray(getResource(translation), "tw_cat", chapterId + "/" + frameId);
    }

    /**
     * Returns an array of translationWords for the sourceTranslation
     * @param translation
     * @return
     */
    public String[] getWords(SourceTranslation translation) {
        return getItemsArray(getResource(translation), "terms");
    }

    /**
     * Returns a single translatonWord
     * @param translation
     * @param termId
     * @return
     */
    public JSONObject getWord(SourceTranslation translation, String termId) {
        String md5hash = readWordsLink(translation);
        if(md5hash != null) {
            return readJSON(md5hash, termId);
        }
        return null;
    }

    /**
     * Returns an array of checkingQuestion ids
     * @param translation
     * @param chapterId
     * @param frameId
     * @return
     */
    public String[] getQuestions(SourceTranslation translation, String chapterId, String frameId) {
        return getItemsArray(getResource(translation), "checking_questions", chapterId + "/" + frameId);
    }

    /**
     * Returns the json object for a single project
     * @param projectId
     * @return
     */
    public synchronized JSONObject getProject(String projectId) {
        String md5hash = mDatabaseHelper.readLink(mDatabase, "projects_catalog.link");
        if(md5hash == null) {
            return null;
        }
        return readJSON(md5hash, projectId);
    }

    /**
     * Returns the json object for a single source language
     * @param projectId
     * @param sourcLanguageId
     * @return
     */
    public synchronized JSONObject getSourceLanguage(String projectId, String sourcLanguageId) {
        String md5hash = mDatabaseHelper.readLink(mDatabase, projectId + "/languages_catalog.link");
        if(md5hash == null) {
            return null;
        }
        return readJSON(md5hash, sourcLanguageId);
    }

    /**
     * Returns the json object for a single resource
     * @param translation
     * @return
     */
    public synchronized JSONObject getResource(SourceTranslation translation) {
        String md5hash = mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/resources_catalog.link");
        if(md5hash == null) {
            return null;
        }
        return readJSON(md5hash, translation.resourceId);
    }

    /**
     * Returns the json object for a single chapter
     * @param translation
     * @param chapterId
     * @return
     */
    public JSONObject getChapter(SourceTranslation translation, String chapterId) {
        String md5hash = readSourceLink(translation);
        if(md5hash == null) {
            return null;
        }
        return readJSON(md5hash, chapterId + "/chapter.json");
    }

    /**
     * Returns the json object for a single frame
     * @param translation
     * @param chapterId
     * @param frameId
     * @return
     */
    public JSONObject getFrame(SourceTranslation translation, String chapterId, String frameId) {
        String md5hash = readSourceLink(translation);
        if(md5hash == null) {
            return null;
        }
        return readJSON(md5hash, chapterId + "/" + frameId);
    }

    /**
     * Returns the json object for a single checkingQuestion
     * @param translation
     * @param chapterId
     * @param frameId
     * @param questionId
     * @return
     */
    public JSONObject getQuestion(SourceTranslation translation, String chapterId, String frameId, String questionId) {
        String md5hash = readQuestionsLink(translation);
        if(md5hash == null) {
            return null;
        }
        return readJSON(md5hash, chapterId + "/" + frameId + "/" + questionId);
    }

    /**
     * Returns the json object for a single translationNote
     * @param translation
     * @param chapterId
     * @param frameId
     * @param noteId
     * @return
     */
    public JSONObject getNote(SourceTranslation translation, String chapterId, String frameId, String noteId) {
        String md5hash = readNotesLink(translation);
        if(md5hash == null) {
            return null;
        }
        return readJSON(md5hash, chapterId + "/" + frameId + "/" + noteId);
    }

    /**
     * Returns a file pointing to a specific set of data
     * @param md5hash the data to retrieve
     * @return the file to the data dir or null if the hash is null
     */
    @Deprecated
    public File getDataDir (String md5hash) {
        return null;
    }

    /**
     * Reads the data key from the source link
     * @param translation
     * @return
     */
    public synchronized String readSourceLink(SourceTranslation translation) {
        return mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/source.link");
    }

    /**
     * Reads the data key from the questions link
     * @param translation
     * @return
     */
    public synchronized String readQuestionsLink(SourceTranslation translation) {
        return mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/checking_questions.link");
    }

    /**
     * Reads the data key from the terms link
     * @param translation
     * @return
     */
    public synchronized String readWordsLink(SourceTranslation translation) {
        return mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/terms.link");
    }

    /**
     * Reads the data key from the term assignments link
     * @param translation
     * @return
     */
    public synchronized String readWordAssignmentsLink(SourceTranslation translation) {
        return mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/tw_cat.link");
    }

    /**
     * Reads the data key from the notes link
     * @param translation
     * @return
     */
    public synchronized String readNotesLink(SourceTranslation translation) {
        return mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/notes.link");
    }
}
