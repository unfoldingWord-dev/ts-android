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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by joel on 8/26/2015.
 */
public class Indexer {
    private final IndexerSQLiteHelper mDatabaseHelper;
    private final Context mContext;
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
     *                // TODO: 10/3/2015 the rootDir is deprecated.
     */
    public Indexer(Context context, String name, File rootDir, IndexerSQLiteHelper helper) {
        mId = name;
        mIndexDir = new File(rootDir, name);
        mManifest = reload();
        mDatabaseHelper = helper;
        mDatabase = mDatabaseHelper.getWritableDatabase();
        mContext = context;
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
    public synchronized void delete() {
        // reset manifest
        FileUtils.deleteQuietly(mIndexDir);
        mManifest = reload();
        // delete database
        close();
        mDatabaseHelper.deleteDatabase(mContext);
    }

    /**
     * Rebuilds the index database
     */
    public synchronized void rebuild() {
        mDatabase = mDatabaseHelper.getWritableDatabase();
    }

//    /**
//     * Returns the contents of a file in the index
//     * @param path the relative path to the indexed file
//     * @return a string or null
//     */
//    private synchronized String readFile(String hash, String path) {
//        return mDatabaseHelper.readFile(mDatabase, hash, path);
//    }

//    /**
//     * Returns the JSON contents of a file in the index
//     * @param path the relative path to the indexed file
//     * @return
//     */
//    private JSONObject readJSON(String hash, String path) {
//        String contents = readFile(hash, path);
//        if(contents != null) {
//            try {
//                return new JSONObject(contents);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//        return null;
//    }

//    /**
//     * Saves a string to a file in the index
//     * @param path the relative path to the file
//     * @param contents the contents to be written
//     * @return true if the file was new
//     */
//    private synchronized Boolean saveFile(String hash, String path, String contents) throws IOException {
//        mDatabaseHelper.replaceFile(mDatabase, hash, path, contents);
//        return true;
//    }

//    /**
//     * Creates or updates a catalog link
//     * @param md5hash
//     * @param linkPath
//     * @return
//     */
//    private synchronized Boolean createLink (String md5hash, String linkPath) {
//        mDatabaseHelper.replaceLink(mDatabase, md5hash, linkPath);
//        return true;
//    }

//    /**
//     * Generates a source link for the source translation
//     * @param translation
//     * @return
//     */
//    private String generateSourceLink (SourceTranslation translation) {
//        return generateResourceFieldLink(translation, "source");
//    }

//    /**
//     * Generates a notes link for the source translation
//     * @param translation
//     * @return
//     */
//    private String generateNotesLink (SourceTranslation translation) {
//        return generateResourceFieldLink(translation, "notes");
//    }

//    /**
//     * Generate a terms link for the source translation
//     * @param translation
//     * @return
//     */
//    private String generateTermsLink (SourceTranslation translation) {
//        return generateResourceFieldLink(translation, "terms");
//    }

//    /**
//     * Generates a term assignments link for the source translation
//     * @param translation
//     * @return
//     */
//    private String generateTermAssignmentsLink (SourceTranslation translation) {
//        return generateResourceFieldLink(translation, "tw_cat");
//    }

//    /**
//     * Generates a questions link for the source translation
//     * @param translation
//     * @return
//     */
//    private String generateQuestionsLink (SourceTranslation translation) {
//        return generateResourceFieldLink(translation, "checking_questions");
//    }

//    /**
//     * Generates a link for a field in the resources catalog.
//     * For example, the "source", or "notes"
//     *
//     * @param translation
//     * @param field
//     * @return
//     */
//    private String generateResourceFieldLink (SourceTranslation translation, String field) {
//        String catalogApiUrl = getUrlFromObject(getResource(translation), field);
//        if(catalogApiUrl != null) {
//            String md5hash = Security.md5(catalogApiUrl);
//            String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/" + field + ".link";
//            if(createLink(md5hash, catalogLinkFile)) {
//                return md5hash;
//            }
//        }
//        return null;
//    }

//    private synchronized Boolean indexItems (String md5hash, CatalogType type, String jsonString) {
//        JSONArray items;
//        try {
//            items = new JSONArray(jsonString);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return false;
//        }
//        // save items
//        if(type == CatalogType.Simple) {
//            for(int i = 0; i < items.length(); i ++ ) {
//                try {
//                    JSONObject item = items.getJSONObject(i);
//                    if(item.has("slug") || item.has("id")) {
//                        String itemPath;
//                        if(item.has("slug")) {
//                            itemPath = item.getString("slug");
//                        } else {
//                            itemPath = item.getString("id");
//                        }
//                        try {
//                            saveFile(md5hash, itemPath, item.toString());
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        } else if(type == CatalogType.Advanced) {
//            for(int chapterIndex = 0; chapterIndex < items.length(); chapterIndex ++) {
//                try {
//                    JSONObject chapter = items.getJSONObject(chapterIndex);
//                    String chapterId = chapter.getString("id");
//                    JSONArray frames = chapter.getJSONArray("frames");
//                    for(int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
//                        try {
//                            JSONObject frame = frames.getJSONObject(frameIndex);
//                            String frameId = frame.getString("id");
//                            JSONArray frameItems = frame.getJSONArray("items");
//                            for(int itemIndex = 0; itemIndex < frameItems.length(); itemIndex ++) {
//                                try {
//                                    JSONObject item = frameItems.getJSONObject(itemIndex);
//                                    String noteId = item.getString("id");
//                                    // save item
//                                    String itemPath = chapterId + "/" + frameId + "/" + noteId;
//                                    try {
//                                        saveFile(md5hash, itemPath, item.toString());
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                } catch(JSONException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        } catch(JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        } else if(type == CatalogType.Source) {
//            for(int chapterIndex = 0; chapterIndex < items.length(); chapterIndex ++ ) {
//                try {
//                    JSONObject chapter = items.getJSONObject(chapterIndex);
//                    if(chapter.has("number")) {
//                        String chapterId = chapter.getString("number");
//                        // save chapter
//                        JSONArray frames = new JSONArray();
//                        if(chapter.has("frames")) {
//                            frames = chapter.getJSONArray("frames");
//                        }
//                        chapter.remove("frames");
//                        String chapterPath = chapterId + "/chapter.json";
//                        try {
//                            saveFile(md5hash, chapterPath, chapter.toString());
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        // save frames
//                        for (int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
//                            try {
//                                JSONObject frame = frames.getJSONObject(frameIndex);
//                                String[] complexId = frame.getString("id").split("-");
//                                String frameId = complexId[1];
//                                String framePath = chapterId + "/" + frameId;
//                                try {
//                                    saveFile(md5hash, framePath, frame.toString());
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                } catch (JSONException e) {
//                    Logger.e(this.getClass().getName(), "Failed to process the source", e);
//                }
//            }
//        } else if(type == CatalogType.Terms) {
//        } else if(type == CatalogType.Questions) {
//        }
//        return true;
//    }

    /**
     * Call to start a transaction
     */
    public void beginTransaction() {
        mDatabase.beginTransactionNonExclusive();
    }

    /**
     * Call to close the transaction
     * @param sucess
     */
    public void endTransaction(boolean sucess) {
        if(sucess) {
            mDatabase.setTransactionSuccessful();
        }
        mDatabase.endTransaction();
    }

//    /**
//     * Returns an array of items indexed under an object
//     * @param itemObject
//     * @param urlProperty
//     * @return
//     */
//    private String[] getItemsArray(JSONObject itemObject, String urlProperty) {
//        return getItemsArray(itemObject, urlProperty, null);
//    }

//    /**
//     * Returns an array of items indexed under an object
//     * @param itemObject
//     * @param urlProperty
//     * @param subFolder
//     * @return
//     */
//    private synchronized String[] getItemsArray(JSONObject itemObject, String urlProperty, String subFolder) {
//        if(itemObject == null) {
//            return new String[0];
//        }
//
//        String catalogApiUrl = getUrlFromObject(itemObject, urlProperty);
//        if(catalogApiUrl == null) {
//            return new String[0];
//        }
//        String md5hash = Security.md5(catalogApiUrl);
//
//        String[] extensionFilters = {"json"};
//        return mDatabaseHelper.listDir(mDatabase, md5hash, subFolder, extensionFilters);
//    }

//    /**
//     * Returns an array of contents indexed under an object.
//     * This is just like getItemsArray except that it returns the contents of the items rather than the names
//     * @param itemObject
//     * @param urlProperty
//     * @param subFolder
//     * @return
//     */
//    private String[] getContentsArray(JSONObject itemObject, String urlProperty, String subFolder) {
//        if(itemObject == null) {
//            return new String[0];
//        }
//
//        String catalogApiUrl = getUrlFromObject(itemObject, urlProperty);
//        if(catalogApiUrl == null) {
//            return new String[0];
//        }
//        String md5hash = Security.md5(catalogApiUrl);
//        String[] extensionFilters = {"json"};
//        return mDatabaseHelper.listDirContents(mDatabase, md5hash, subFolder, extensionFilters);
//    }

//    /**
//     * Returns an array of contents for a file found in each directory.
//     * For example. if you have directorys 01, 02, and 03 each containing a file "myfile.json"
//     * this method will list the contents of reach "myfile.json" ordered by directory name.
//     *
//     * @param itemObject
//     * @param urlProperty
//     * @param subFolder
//     * @param file the file who's contents will be returned
//     * @return
//     */
//    private String[] getDirFileContentsArray(JSONObject itemObject, String urlProperty, String subFolder, String file) {
//        if(itemObject == null) {
//            return new String[0];
//        }
//
//        String catalogApiUrl = getUrlFromObject(itemObject, urlProperty);
//        if (catalogApiUrl == null) {
//            return new String[0];
//        }
//        String md5hash = Security.md5(catalogApiUrl);
//        return mDatabaseHelper.listDirFileContents(mDatabase, md5hash, subFolder, file);
//    }

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

//    /**
//     * Returns the root catalog
//     * @return
//     */
//    private JSONObject getRootCatalog() {
//        JSONObject json = new JSONObject();
//        try {
//            json.put("proj_catalog", "_");
//            return json;
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    /**
     * Removes a project from the index
     * @param projectSlug
     */
    public synchronized void deleteProject (String projectSlug) {
        mDatabaseHelper.deleteProject(mDatabase, projectSlug);
    }

    /**
     * Removes a source language from the index
     * @param projectSlug
     * @param sourceLanguageSlug
     */
    public synchronized void deleteSourceLanguage (String projectSlug, String sourceLanguageSlug) {
         mDatabaseHelper.deleteSourceLanguage(mDatabase, sourceLanguageSlug, projectSlug);
    }

    /**
     * Removes a resource from the index
     * @param sourceTranslation
     */
    private synchronized void deleteResource (SourceTranslation sourceTranslation) {
        mDatabaseHelper.deleteResource(mDatabase, sourceTranslation.resourceId, sourceTranslation.sourceLanguageId, sourceTranslation.projectId);
    }

//    /**
//     * Removes the source for the source translation from the index
//     * @param translation
//     */
//    private synchronized void deleteSource (SourceTranslation translation) {
//        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/source.link";
//        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
//    }
//
//    /**
//     * Removes the translationNotes for the source translation from the index
//     * @param translation
//     */
//    private synchronized void deleteNotes (SourceTranslation translation) {
//        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/notes.link";
//        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
//    }
//
//    /**
//     * Removes the translationWords for the source translation from the index
//     * @param translation
//     */
//    private synchronized void deleteTerms (SourceTranslation translation) {
//        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/terms.link";
//        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
//    }
//
//    private synchronized void deleteTermAssignments (SourceTranslation translation) {
//        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/tw_cat.link";
//        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
//    }

//    /**
//     * Removes the checking questions for the source translation from the index
//     * @param translation
//     */
//    private synchronized void deleteQuestions (SourceTranslation translation) {
//        String catalogLinkFile = translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/checking_questions.link";
//        mDatabaseHelper.deleteLink(mDatabase, catalogLinkFile);
//    }

//    /**
//     * Merges an index into the current index
//     * @param index
//     * @throws IOException
//     */
//    public void mergeIndex(Indexer index) throws IOException {
//        mergeIndex(index, false);
//    }

    /**
     * Merges an index into the current index
     * @param index
     * @param shallow if true none of the source translation content will be merged
     */
    @Deprecated
    public void mergeIndex(Indexer index, Boolean shallow) throws IOException {
//        for(String projectId:index.getProjectSlugs()) {
//            mergeProject(projectId, index, shallow);
//        }
    }

//    /**
//     * Merges a project into the current index
//     * @param projectId
//     * @param index
//     * @throws IOException
//     */
//    public void mergeProject(String projectId, Indexer index) throws IOException {
//        mergeProject(projectId, index, false);
//    }

    /**
     * Merges a project into the current index
     * @param index
     * @param projectId
     * @param shallow if true none of the source translation content will be merged
     */
    @Deprecated
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
    public void mergeSourceTranslationShallow(SourceTranslation sourceTranslation, Indexer index) throws IOException {
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
    @Deprecated
    public synchronized void mergeResources(SourceTranslation translation, Indexer index) throws IOException {
//        mDatabase.beginTransactionNonExclusive();

        // delete old content
//        deleteTerms(translation);
//        deleteTermAssignments(translation);
//        deleteQuestions(translation);
//        deleteNotes(translation);
//        deleteSource(translation);

        // re-create links
//        String sourceCatalogHash = generateSourceLink(translation);
//        String notesCatalogHash = generateNotesLink(translation);
//        String questionsCatalogHash = generateQuestionsLink(translation);
//        String termsCatalogHash = generateTermsLink(translation);
//        String termAssignmentsCatalogHash = generateTermAssignmentsLink(translation);

        // migrate words
//        String[] words = index.getWordsContents(translation);
//        for(String wordContents:words) {
//            try {
//                TranslationWord word = TranslationWord.generate(new JSONObject(wordContents));
//                String wordPath = word.getId();
//                saveFile(termsCatalogHash, wordPath, wordContents);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        // migrate chapters
//        String[] chapters = index.getChaptersContents(translation);
//        for(String chapterContents:chapters) {
//            try {
//                Chapter chapter = Chapter.generate(new JSONObject(chapterContents));
//                String chapterPath = chapter.getId() + "/chapter.json";
//                saveFile(sourceCatalogHash, chapterPath, chapterContents);
//
//                // migrate frames
//                String[] frames = index.getFramesContents(translation, chapter.getId());
//                for(String frameContents:frames) {
//                    try {
//                        Frame frame = Frame.generate(chapter.getId(), new JSONObject(frameContents));
//                        String framePath = chapter.getId() + "/" + frame.getId();
//                        saveFile(sourceCatalogHash, framePath, frameContents);
//
//                        // migrate notes
//                        String[] notes = index.getNotesContents(translation, chapter.getId(), frame.getId());
//                        for(String noteContents:notes) {
//                            try {
//                                TranslationNote note = TranslationNote.generate(chapter.getId(), frame.getId(), new JSONObject(noteContents));
//                                String notePath = chapter.getId() + "/" + frame.getId() + "/" + note.getId();
//                                saveFile(notesCatalogHash, notePath, noteContents);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        // migrate questions
//                        String[] questions = index.getQuestionsContents(translation, chapter.getId(), frame.getId());
//                        for(String questionContents:questions) {
//                            try {
//                                CheckingQuestion question = CheckingQuestion.generate(chapter.getId(), frame.getId(), new JSONObject(questionContents));
//                                String questionPath = chapter.getId() + "/" + frame.getId() + "/" + question.getId();
//                                saveFile(questionsCatalogHash, questionPath, questionContents);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        // migrate word assignments
//                        String[] wordAssignments = index.getWordsForFrame(translation, chapter.getId(), frame.getId());
//                        for(String wordId:wordAssignments) {
//                            try {
//                                JSONObject json = new JSONObject();
//                                json.put("id", wordId);
//                                String wordAssignmentPath = chapter.getId() + "/" + frame.getId() + "/" + wordId;
//                                saveFile(termAssignmentsCatalogHash, wordAssignmentPath, wordId);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        mDatabase.setTransactionSuccessful();
//        mDatabase.endTransaction();
    }

    /**
     * Returns the index id
     * @return
     */
    public String getIndexId() {
        return mId;
    }

    /**
     * Builds a project index from json
     * @param catalog the json formatted project catalog
     * @return
     */
    public synchronized boolean indexProjects(String catalog) {
        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        for(int i = 0; i < items.length(); i ++ ) {
            try {
                JSONObject item = items.getJSONObject(i);
                Project project = Project.generateSimple(item);
                if(project != null) {
                    JSONArray categoriesJson = item.getJSONArray("meta");
                    List<String> categorySlugs = new ArrayList<>();
                    for(int j = 0; j < categoriesJson.length(); j ++) {
                        categorySlugs.add(categoriesJson.getString(j));
                    }
                    mDatabaseHelper.addProject(mDatabase, project.getId(), project.sort, project.dateModified, project.sourceLanguageCatalog, categorySlugs.toArray(new String[categorySlugs.size()]));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Builds a source language index from json
     * @param projectSlug
     * @param catalog
     * @return
     */
    public synchronized boolean indexSourceLanguages(String projectSlug, String catalog) {
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
        //KLUDGE: end modify v2
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, projectSlug);
        if(projectId > 0) {
            for (int i = 0; i < items.length(); i++) {
                try {
                    JSONObject item = items.getJSONObject(i);
                    SourceLanguage sourceLanguage = SourceLanguage.generate(item);
                    if (sourceLanguage != null) {
                        JSONArray categoriesJson = item.getJSONArray("meta");
                        List<String> categoryNames = new ArrayList<>();
                        for(int j = 0; j < categoriesJson.length(); j ++) {
                            categoryNames.add(categoriesJson.getString(j));
                        }
                        mDatabaseHelper.addSourceLanguage(mDatabase, sourceLanguage.getId(),
                                projectId, sourceLanguage.name, sourceLanguage.projectTitle,
                                sourceLanguage.projectDescription,
                                sourceLanguage.getDirection().toString(), sourceLanguage.dateModified,
                                sourceLanguage.resourceCatalog, categoryNames.toArray(new String[categoryNames.size()]));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Builds a resource index from json
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param catalog
     * @return
     */
    public synchronized boolean indexResources(String projectSlug, String sourceLanguageSlug, String catalog) {
        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceLanguageSlug, projectId);

        for(int i = 0; i < items.length(); i ++ ) {
            try {
                JSONObject item = items.getJSONObject(i);
                Resource resource = Resource.generate(item);
                if(resource != null) {
                    mDatabaseHelper.addResource(mDatabase, resource.getId(), sourceLanguageId,
                            resource.getTitle(), resource.getCheckingLevel(), resource.getVersion(),
                            resource.getDateModified(), resource.getSourceCatalogUrl(), resource.getSourceDateModified(),
                            resource.getNotesCatalogUrl(), resource.getNotesDateModified(),
                            resource.getWordsCatalogUrl(), resource.getWordsDateModified(),
                            resource.getWordAssignmentsCatalogUrl(), resource.getWordAssignmentsDateModified(),
                            resource.getQuestionsCatalogUrl(), resource.getQuestionsDateModified());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    /**
     * Builds a source index from json
     * @param sourceTranslation
     * @param catalog
     * @return
     */
    public synchronized boolean indexSource(SourceTranslation sourceTranslation, String catalog) {
        //KLUDGE: modify v2 sources catalogJson to match expected catalogJson format
        try {
            JSONObject catalogJson = new JSONObject(catalog);
            catalog = catalogJson.getJSONArray("chapters").toString();
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "Invalid catalog json",e);
            return false;
        }
        //KLUDGE: end modify v2

        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        for(int chapterIndex = 0; chapterIndex < items.length(); chapterIndex ++ ) {
            try {
                JSONObject chapterJson = items.getJSONObject(chapterIndex);
                Chapter chapter = Chapter.generate(chapterJson);
                if(chapter != null && chapterJson.has("frames")) {
                    JSONArray frames = chapterJson.getJSONArray("frames");
                    // TODO: index chapter

                    for (int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
                        try {
                            JSONObject frameJson = frames.getJSONObject(frameIndex);
                            Frame frame = Frame.generate(chapter.getId(), frameJson);
                            if(frame != null) {
                                // TODO: index frame
                            }
                        } catch (JSONException e) {
                            Logger.e(this.getClass().getName(), "Failed to parse the frame in chapter " + chapter.getId() + " at index " + chapterIndex + " for source translation " + sourceTranslation.getId(), e);
                        }
                    }
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to parse the chapter at index " + chapterIndex + " for source translation " + sourceTranslation.getId(), e);
            }
        }
        return true;
    }

    /**
     * Builds a notes index from json
     *
     * @param translation
     * @param catalog
     * @return
     */
    public synchronized boolean indexNotes(SourceTranslation translation, String catalog) {
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
        items = jsonArray;
        //KLUDGE: end modify v2


        // index
        for(int chapterIndex = 0; chapterIndex < items.length(); chapterIndex ++) {
            try {
                JSONObject chapter = items.getJSONObject(chapterIndex);
                String chapterId = chapter.getString("id");
                JSONArray frames = chapter.getJSONArray("frames");
                for(int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
                    try {
                        JSONObject frame = frames.getJSONObject(frameIndex);
                        String frameId = frame.getString("id");
                        JSONArray frameItems = frame.getJSONArray("items");
                        for(int itemIndex = 0; itemIndex < frameItems.length(); itemIndex ++) {
                            try {
                                JSONObject item = frameItems.getJSONObject(itemIndex);
                                TranslationNote note = TranslationNote.generate(chapterId, frameId, item);
                                if(note != null) {
                                    // TODO: index the note
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
        return true;
    }

    /**
     * Builds a terms index from json
     * @param translation
     * @param catalog
     * @return
     */
    public synchronized boolean indexTerms(SourceTranslation translation, String catalog) {
        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        for(int i = 0; i < items.length(); i ++ ) {
            try {
                JSONObject item = items.getJSONObject(i);
                TranslationWord word = TranslationWord.generate(item);
                if(word != null) {
                    // TODO: index word
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    /**
     * Builds a translationWord assignment index from json
     * @param translation
     * @param catalog
     * @return
     */
    public synchronized boolean indexTermAssignments(SourceTranslation translation, String catalog) {
        //KLUDGE: modify v2 questions catalogJson to match expected catalogJson format
        JSONArray items;
        try {
            JSONObject catJson = new JSONObject(catalog);
            items = catJson.getJSONArray("chapters");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        //KLUDGE: end modify v2\

        for(int chapterIndex = 0; chapterIndex < items.length(); chapterIndex ++) {
            try {
                JSONObject chapter = items.getJSONObject(chapterIndex);
                String chapterId = chapter.getString("id");
                JSONArray frames = chapter.getJSONArray("frames");
                for(int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
                    try {
                        JSONObject frame = frames.getJSONObject(frameIndex);
                        String frameId = frame.getString("id");
                        JSONArray frameItems = frame.getJSONArray("items");
                        for(int itemIndex = 0; itemIndex < frameItems.length(); itemIndex ++) {
                            try {
                                JSONObject item = frameItems.getJSONObject(itemIndex);
                                // TODO: index term assignments
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

        return true;
    }

    /**
     * Builds a questions index from json
     * @param translation
     * @param catalog
     * @return
     */
    public synchronized boolean indexQuestions(SourceTranslation translation, String catalog) {
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
        items = jsonArray;
        //KLUDGE: end modify v2

        for(int chapterIndex = 0; chapterIndex < items.length(); chapterIndex ++) {
            try {
                JSONObject chapter = items.getJSONObject(chapterIndex);
                String chapterId = chapter.getString("id");
                JSONArray frames = chapter.getJSONArray("frames");
                for(int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
                    try {
                        JSONObject frame = frames.getJSONObject(frameIndex);
                        String frameId = frame.getString("id");
                        JSONArray frameItems = frame.getJSONArray("items");
                        for(int itemIndex = 0; itemIndex < frameItems.length(); itemIndex ++) {
                            try {
                                JSONObject item = frameItems.getJSONObject(itemIndex);
                                CheckingQuestion question = CheckingQuestion.generate(chapterId, frameId, item);
                                if(question != null) {
                                    // TODO: index question
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
        return true;
    }

    /**
     * Returns an array of projectS
     * @return
     */
    public String[] getProjectSlugs() {
        // // TODO: 10/16/2015 see if we can avoid using this method
        return mDatabaseHelper.getProjectSlugs(mDatabase);
    }

    /**
     * Returns a list of project contents
     * @return
     */
    @Deprecated
    public String[] getProjectsContents() {
        return new String[0];
//        return getContentsArray(getRootCatalog(), "proj_catalog", null);
    }

    /**
     * Returns an array of source language ids
     * @param projectId
     * @return
     */
    public String[] getSourceLanguages(String projectId) {
        // TODO: 10/16/2015 this should return an array of source language objects
        return new String[0];
//        return getItemsArray(getProject(projectId), "lang_catalog");
    }

    /**
     * Returns an array of resource ids
     * @param projectId
     * @param sourceLanguageId
     * @return
     */
    public String[] getResources(String projectId, String sourceLanguageId) {
        // TODO: 10/16/2015 this should return an array of resource objects
        return new String[0];
//        return getItemsArray(getSourceLanguage(projectId, sourceLanguageId), "res_catalog");
    }

    /**
     * Returns an array of chapter ids
     * @param translation
     * @return
     */
    public String[] getChapters(SourceTranslation translation) {
        // TODO: 10/16/2015 this should return an array of chapter objects
        return new String[0];
//        return getItemsArray(getResource(translation), "source");
    }

    /**
     * Returns a list of chapter contents
     * @return
     */
    @Deprecated
    public String[] getChaptersContents(SourceTranslation translation) {
        return new String[0];
//        return getDirFileContentsArray(getResource(translation), "source", null, "chapter.json");
    }

    /**
     * Returns an array of frame ids
     * @param translation
     * @param chapterId
     * @return
     */
    public String[] getFrames(SourceTranslation translation, String chapterId) {
        // TODO: 10/16/2015 this should return an array of frame objects
        return new String[0];
//        return getItemsArray(getResource(translation), "source", chapterId);
    }

    /**
     * Returns an array of frame contents
     * @param translation
     * @param chapterId
     * @return
     */
    @Deprecated
    public String[] getFramesContents(SourceTranslation translation, String chapterId) {
        return new String[0];
//        return getContentsArray(getResource(translation), "source", chapterId);
    }

    /**
     * Returns an array of translationNote ids
     * @param translation
     * @param chapterId
     * @param frameId
     * @return
     */
    public String[] getNotes(SourceTranslation translation, String chapterId, String frameId) {
        // TODO: 10/16/2015 this should return an array of note objects
        return new String[0];
//        return getItemsArray(getResource(translation), "notes", chapterId + "/" + frameId);
    }

    /**
     * Returns an array of note contents
     * @param translation
     * @param chapterId
     * @return
     */
    @Deprecated
    public String[] getNotesContents(SourceTranslation translation, String chapterId, String frameId) {
        return new String[0];
//        return getContentsArray(getResource(translation), "notes", chapterId + "/" + frameId);
    }

    /**
     * Returns an array of translationWord ids for a single frame
     * @param translation
     * @param chapterId
     * @param frameId
     * @return
     */
    public String[] getWordsForFrame(SourceTranslation translation, String chapterId, String frameId) {
        // TODO: 10/16/2015 this should return an array of translation word objects
        return new String[0];
//        return getItemsArray(getResource(translation), "tw_cat", chapterId + "/" + frameId);
    }

    /**
     * Returns an array of translationWords for the sourceTranslation
     * @param translation
     * @return
     */
    public String[] getWords(SourceTranslation translation) {
        // TODO: 10/16/2015 this should return an array of translation word objects
        return new String[0];
//        return getItemsArray(getResource(translation), "terms");
    }

    /**
     * Returns an array of word contents
     * @param translation
     * @return
     */
    @Deprecated
    public String[] getWordsContents(SourceTranslation translation) {
        return new String[0];
//        return getContentsArray(getResource(translation), "terms", null);
    }

    /**
     * Returns a single translatonWord
     * @param translation
     * @param termId
     * @return
     */
    public JSONObject getWord(SourceTranslation translation, String termId) {
        // TODO: 10/16/2015 this should return a translation word object
//        String md5hash = readWordsLink(translation);
//        if(md5hash != null) {
//            return readJSON(md5hash, termId);
//        }
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
        // TODO: 10/16/2015 this should return an array of question objects
        return new String[0];
//        return getItemsArray(getResource(translation), "checking_questions", chapterId + "/" + frameId);
    }

    /**
     * Returns an array of question contents
     * @param translation
     * @param chapterId
     * @return
     */
    @Deprecated
    public String[] getQuestionsContents(SourceTranslation translation, String chapterId, String frameId) {
        return new String[0];
//        return getContentsArray(getResource(translation), "checking_questions", chapterId + "/" + frameId);
    }

    /**
     * Returns the json object for a single project
     * @param projectId
     * @return
     */
    public synchronized JSONObject getProject(String projectId) {
        // TODO: 10/16/2015 this should return a project object
//        String md5hash = mDatabaseHelper.readLink(mDatabase, "projects_catalog.link");
//        if(md5hash == null) {
//            return null;
//        }
//        return readJSON(md5hash, projectId);
        return null;
    }

    /**
     * Returns the json object for a single source language
     * @param projectId
     * @param sourcLanguageId
     * @return
     */
    public synchronized JSONObject getSourceLanguage(String projectId, String sourcLanguageId) {
//        String md5hash = mDatabaseHelper.readLink(mDatabase, projectId + "/languages_catalog.link");
//        if(md5hash == null) {
//            return null;
//        }
//        return readJSON(md5hash, sourcLanguageId);
        // TODO: 10/16/2015 this should return a source language object
        return null;
    }

    /**
     * Returns the json object for a single resource
     * @param translation
     * @return
     */
    public synchronized JSONObject getResource(SourceTranslation translation) {
//        String md5hash = mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/resources_catalog.link");
//        if(md5hash == null) {
//            return null;
//        }
//        return readJSON(md5hash, translation.resourceId);
        // TODO: 10/16/2015 this should return a resource object
        return null;
    }

    /**
     * Returns the json object for a single chapter
     * @param translation
     * @param chapterId
     * @return
     */
    public JSONObject getChapter(SourceTranslation translation, String chapterId) {
//        String md5hash = readSourceLink(translation);
//        if(md5hash == null) {
//            return null;
//        }
//        return readJSON(md5hash, chapterId + "/chapter.json");
        // TODO: 10/16/2015 this should return a chapter object
        return null;
    }

    /**
     * Returns the json object for a single frame
     * @param translation
     * @param chapterId
     * @param frameId
     * @return
     */
    public JSONObject getFrame(SourceTranslation translation, String chapterId, String frameId) {
//        String md5hash = readSourceLink(translation);
//        if(md5hash == null) {
//            return null;
//        }
//        return readJSON(md5hash, chapterId + "/" + frameId);
        // TODO: 10/16/2015 this should return a frame object
        return null;
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
//        String md5hash = readQuestionsLink(translation);
//        if(md5hash == null) {
//            return null;
//        }
//        return readJSON(md5hash, chapterId + "/" + frameId + "/" + questionId);
        // TODO: 10/16/2015 this should return a question object
        return null;
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
//        String md5hash = readNotesLink(translation);
//        if(md5hash == null) {
//            return null;
//        }
//        return readJSON(md5hash, chapterId + "/" + frameId + "/" + noteId);
        // TODO: 10/16/2015 this should return a note object
        return null;
    }

//    /**
//     * Returns a file pointing to a specific set of data
//     * @param md5hash the data to retrieve
//     * @return the file to the data dir or null if the hash is null
//     */
//    @Deprecated
//    public File getDataDir (String md5hash) {
//        return null;
//    }

//    /**
//     * Reads the data key from the source link
//     * @param translation
//     * @return
//     */
//    public synchronized String readSourceLink(SourceTranslation translation) {
//        return mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/source.link");
//    }

//    /**
//     * Reads the data key from the questions link
//     * @param translation
//     * @return
//     */
//    public synchronized String readQuestionsLink(SourceTranslation translation) {
//        return mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/checking_questions.link");
//    }

//    /**
//     * Reads the data key from the terms link
//     * @param translation
//     * @return
//     */
//    public synchronized String readWordsLink(SourceTranslation translation) {
//        return mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/terms.link");
//    }

//    /**
//     * Reads the data key from the term assignments link
//     * @param translation
//     * @return
//     */
//    public synchronized String readWordAssignmentsLink(SourceTranslation translation) {
//        return mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/tw_cat.link");
//    }
//
//    /**
//     * Reads the data key from the notes link
//     * @param translation
//     * @return
//     */
//    public synchronized String readNotesLink(SourceTranslation translation) {
//        return mDatabaseHelper.readLink(mDatabase, translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/notes.link");
//    }
}
