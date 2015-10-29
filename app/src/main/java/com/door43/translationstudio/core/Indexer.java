package com.door43.translationstudio.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.door43.tools.reporting.Logger;
import com.door43.util.Security;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.Source;


/**
 * Created by joel on 8/26/2015.
 * TODO: we might make this static in the library as well
 */
public class Indexer {
    private final IndexerSQLiteHelper mDatabaseHelper;
    private final Context mContext;
    private SQLiteDatabase mDatabase;
    private final String mId;

    /**
     * Creates a new instance of the index
     * @param name the name of the index
     */
    public Indexer(Context context, String name, IndexerSQLiteHelper helper) {
        mId = name;
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
     * Returns the version of the indexer
     * @return
     */
    public int getVersion() {
        // TODO: return the version of the index. We can get this info from the database.
        return 0;
    }

    /**
     * Destroys the entire index
     */
    public synchronized void delete() {
        close();
        mDatabaseHelper.deleteDatabase(mContext);
    }

    /**
     * Rebuilds the index database
     */
    public synchronized void rebuild() {
        mDatabase = mDatabaseHelper.getWritableDatabase();
    }

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
        mDatabaseHelper.deleteResource(mDatabase, sourceTranslation.resourceSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.projectSlug);
    }

    /**
     * Returns the index id
     * @return
     */
    public String getIndexId() {
        return mId;
    }

    /**
     * Returns a branch of the category list
     * @param sourcelanguageSlug
     * @param parentCategoryId
     * @return
     */
    public ProjectCategory[] getCategoryBranch(String sourcelanguageSlug, long parentCategoryId) {
        return mDatabaseHelper.getCategoryBranch(mDatabase, sourcelanguageSlug, parentCategoryId);
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
                    mDatabaseHelper.addProject(mDatabase, project.getId(), project.sort, project.dateModified, project.sourceLanguageCatalog, project.sourceLanguageCatalogServerDateModified, categorySlugs.toArray(new String[categorySlugs.size()]));
                    mDatabase.yieldIfContendedSafely();
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
        JSONArray items;
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
                        JSONArray categoriesJson = item.getJSONObject("project").getJSONArray("meta");
                        List<String> categoryNames = new ArrayList<>();
                        for(int j = 0; j < categoriesJson.length(); j ++) {
                            categoryNames.add(categoriesJson.getString(j));
                        }
                        mDatabaseHelper.addSourceLanguage(mDatabase, sourceLanguage.getId(),
                                projectId, sourceLanguage.name, sourceLanguage.projectTitle,
                                sourceLanguage.projectDescription,
                                sourceLanguage.getDirection().toString(), sourceLanguage.dateModified,
                                sourceLanguage.resourceCatalog, sourceLanguage.resourceCatalogServerDateModified, categoryNames.toArray(new String[categoryNames.size()]));
                        mDatabase.yieldIfContendedSafely();
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
                            resource.getDateModified(), resource.getSourceCatalogUrl(), resource.getSourceServerDateModified(),
                            resource.getNotesCatalogUrl(), resource.getNotesServerDateModified(),
                            resource.getWordsCatalogUrl(), resource.getWordsServerDateModified(),
                            resource.getWordAssignmentsCatalogUrl(), resource.getWordAssignmentsServerDateModified(),
                            resource.getQuestionsCatalogUrl(), resource.getQuestionsServerDateModified());
                    mDatabase.yieldIfContendedSafely();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    }

//    /**
//     * Updates the resources local date modiifed to match the server date modified
//     * @param projectSlug
//     * @param sourceLanguageSlug
//     * @return
//     */
//    public synchronized boolean normalizeResourcesModifiedDate(String projectSlug, String sourceLanguageSlug) {
//        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, projectSlug);
//        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceLanguageSlug, projectId);
//        if(sourceLanguageId > 0) {
//            mDatabaseHelper.normalizeResourcesModifiedDate(mDatabase, sourceLanguageId);
//        }
//        return true;
//    }

    /**
     * Updates the resource's local date modiifed to match the server date modified
     * @param sourceTranslation
     * @return
     */
    public synchronized boolean markResourceUpToDate(SourceTranslation sourceTranslation) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);
        if(resourceId > 0) {
            mDatabaseHelper.markResourceUpToDate(mDatabase, resourceId);
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

        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);

        for(int chapterIndex = 0; chapterIndex < items.length(); chapterIndex ++ ) {
            try {
                JSONObject chapterJson = items.getJSONObject(chapterIndex);
                Chapter chapter = Chapter.generate(chapterJson);
                if(chapter != null && chapterJson.has("frames")) {
                    JSONArray frames = chapterJson.getJSONArray("frames");
                    long chapterId = mDatabaseHelper.addChapter(mDatabase, chapter.getId(), resourceId, chapter.reference, chapter.title);

                    for (int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
                        try {
                            JSONObject frameJson = frames.getJSONObject(frameIndex);
                            Frame frame = Frame.generate(chapter.getId(), frameJson);
                            if(frame != null) {
                                mDatabaseHelper.addFrame(mDatabase, frame.getId(), chapterId, frame.body, frame.getFormat().toString(), frame.imageUrl);
                            }
                            mDatabase.yieldIfContendedSafely();
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
     * Builds a index of all the target languages
     * @param catalog
     * @return
     */
    public boolean indexTargetLanguages(String catalog) {
        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        for (int i = 0; i < items.length(); i ++) {
            try {
                JSONObject item = items.getJSONObject(i);
                TargetLanguage targetLanguage = TargetLanguage.generate(item);
                if(targetLanguage != null) {
                    mDatabaseHelper.addTargetLanguage(mDatabase, targetLanguage.getId(), targetLanguage.getDirection().toString(), targetLanguage.name, targetLanguage.region);
                }
                mDatabase.yieldIfContendedSafely();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Builds a notes index from json
     *
     * @param sourceTranslation
     * @param catalog
     * @return
     */
    public synchronized boolean indexNotes(SourceTranslation sourceTranslation, String catalog) {
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

        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            // index
            for (int chapterIndex = 0; chapterIndex < items.length(); chapterIndex++) {
                try {
                    JSONObject chapterJson = items.getJSONObject(chapterIndex);
                    String chapterSlug = chapterJson.getString("id");
                    long chapterId = mDatabaseHelper.getChapterDBId(mDatabase, chapterSlug, resourceId);
                    if(chapterId > 0) {
                        JSONArray frames = chapterJson.getJSONArray("frames");
                        for (int frameIndex = 0; frameIndex < frames.length(); frameIndex++) {
                            try {
                                JSONObject frameJson = frames.getJSONObject(frameIndex);
                                String frameSlug = frameJson.getString("id");
                                long frameId = mDatabaseHelper.getFrameDBId(mDatabase, frameSlug, chapterId);
                                if(frameId > 0) {
                                    JSONArray frameItems = frameJson.getJSONArray("items");
                                    for (int itemIndex = 0; itemIndex < frameItems.length(); itemIndex++) {
                                        try {
                                            JSONObject item = frameItems.getJSONObject(itemIndex);
                                            TranslationNote note = TranslationNote.generate(chapterSlug, frameSlug, item);
                                            if (note != null) {
                                                mDatabaseHelper.addTranslationNote(mDatabase, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug, note.getId(), frameId, note.getTitle(), note.getBody());
                                            }
                                            mDatabase.yieldIfContendedSafely();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * Builds a terms index from json
     * @param sourceTranslation
     * @param catalog
     * @return
     */
    public synchronized boolean indexWords(SourceTranslation sourceTranslation, String catalog) {
        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        Resource resource = mDatabaseHelper.getResource(mDatabase, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug);

        if(resource != null) {
            for (int i = 0; i < items.length(); i++) {
                try {
                    JSONObject item = items.getJSONObject(i);
                    TranslationWord word = TranslationWord.generate(item);
                    if (word != null) {
                        mDatabaseHelper.addTranslationWord(mDatabase, word.getId(), resource.getDBId(), Security.md5(resource.getWordsCatalogUrl()), word.getTerm(), word.getDefinitionTitle(), word.getDefinition(), word.getExamples(), word.getAliases(), word.getSeeAlso());
                    }
                    mDatabase.yieldIfContendedSafely();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    /**
     * Builds a translationWord assignment index from json
     * @param sourceTranslation
     * @param catalog
     * @return
     */
    public synchronized boolean indexTermAssignments(SourceTranslation sourceTranslation, String catalog) {
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

        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            // index
            for (int chapterIndex = 0; chapterIndex < items.length(); chapterIndex++) {
                try {
                    JSONObject chapter = items.getJSONObject(chapterIndex);
                    String chapterSlug = chapter.getString("id");
                    long chapterId = mDatabaseHelper.getChapterDBId(mDatabase, chapterSlug, resourceId);
                    if(chapterId > 0) {
                        JSONArray frames = chapter.getJSONArray("frames");
                        for (int frameIndex = 0; frameIndex < frames.length(); frameIndex++) {
                            try {
                                JSONObject frame = frames.getJSONObject(frameIndex);
                                String frameSlug = frame.getString("id");
                                long frameId = mDatabaseHelper.getFrameDBId(mDatabase, frameSlug, chapterId);
                                if(frameId > 0) {
                                    JSONArray frameItems = frame.getJSONArray("items");
                                    for (int itemIndex = 0; itemIndex < frameItems.length(); itemIndex++) {
                                        try {
                                            JSONObject item = frameItems.getJSONObject(itemIndex);
                                            if (item.has("id")) {
                                                mDatabaseHelper.addTranslationWordToFrame(mDatabase, item.getString("id"), resourceId, frameId, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug);
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    /**
     * Builds a questions index from json
     * @param sourceTranslation
     * @param catalog
     * @return
     */
    public synchronized boolean indexQuestions(SourceTranslation sourceTranslation, String catalog) {
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

        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            for (int chapterIndex = 0; chapterIndex < items.length(); chapterIndex++) {
                try {
                    JSONObject chapter = items.getJSONObject(chapterIndex);
                    String chapterSlug = chapter.getString("id");
                    long chapterId = mDatabaseHelper.getChapterDBId(mDatabase, chapterSlug, resourceId);
                    if(chapterId > 0) {
                        JSONArray frames = chapter.getJSONArray("frames");
                        for (int frameIndex = 0; frameIndex < frames.length(); frameIndex++) {
                            try {
                                JSONObject frame = frames.getJSONObject(frameIndex);
                                String frameSlug = frame.getString("id");
                                long frameId = mDatabaseHelper.getFrameDBId(mDatabase, frameSlug, chapterId);
                                if(frameId > 0) {
                                    JSONArray frameItems = frame.getJSONArray("items");
                                    for (int itemIndex = 0; itemIndex < frameItems.length(); itemIndex++) {
                                        try {
                                            JSONObject item = frameItems.getJSONObject(itemIndex);
                                            CheckingQuestion question = CheckingQuestion.generate(chapterSlug, frameSlug, item);
                                            if (question != null) {
                                                mDatabaseHelper.addCheckingQuestion(mDatabase, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug, question.getId(), frameId, chapterId, question.getQuestion(), question.getAnswer());
                                            }
                                            mDatabase.yieldIfContendedSafely();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
     * Returns an array of source language ids
     * @param projectSlug
     * @return
     */
    public String[] getSourceLanguageSlugs(String projectSlug) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, projectSlug);
        if(projectId > 0) {
            return mDatabaseHelper.getSourceLanguageSlugs(mDatabase, projectId);
        }
        return new String[0];
    }

    /**
     * Returns an array of resource ids
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public String[] getResourceSlugs(String projectSlug, String sourceLanguageSlug) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, projectSlug);
        if(projectId > 0) {
            long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceLanguageSlug, projectId);
            if(sourceLanguageId > 0) {
                return mDatabaseHelper.getResourceSlugs(mDatabase, sourceLanguageId);
            }
        }
        return new String[0];
    }

    /**
     * Returns an array of chapter ids
     * @param sourceTranslation
     * @return
     */
    public String[] getChapterSlugs(SourceTranslation sourceTranslation) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);
        if(resourceId > 0) {
            return mDatabaseHelper.getChapterSlugs(mDatabase, resourceId);
        }
        return new String[0];
    }

    /**
     * Returns a list of chapters
     * @return
     */
    @Deprecated
    public Chapter[] getChapters(SourceTranslation sourceTranslation) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            return mDatabaseHelper.getChapters(mDatabase, resourceId);
        }
        return new Chapter[0];
    }

    /**
     * Returns an array of frame ids
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public String[] getFrameSlugs(SourceTranslation sourceTranslation, String chapterSlug) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);
        long chapterId = mDatabaseHelper.getChapterDBId(mDatabase, chapterSlug, resourceId);

        if(chapterId > 0) {
            return mDatabaseHelper.getFrameSlugs(mDatabase, chapterId);
        }
        return new String[0];
    }

    /**
     * Returns an array of frame contents
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public Frame[] getFrames(SourceTranslation sourceTranslation, String chapterSlug) {
        return mDatabaseHelper.getFrames(mDatabase, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug);
    }

    /**
     * Returns an array of target languages
     * @return
     */
    public TargetLanguage[] getTargetLanguages() {
        return mDatabaseHelper.getTargetLanguages(mDatabase);
    }

    /**
     * Returns a target language
     * @param targetLanguageSlug
     * @return
     */
    public TargetLanguage getTargetLanguage(String targetLanguageSlug) {
        return mDatabaseHelper.getTargetLanguage(mDatabase, targetLanguageSlug);
    }

    /**
     * Returns a target language
     * @param targetLanguageName
     * @return
     */
    public TargetLanguage getTargetLanguageByName(String targetLanguageName) {
        return mDatabaseHelper.getTargetLanguageByName(mDatabase, targetLanguageName);
    }

    /**
     * Returns the number of target languages
     * @return
     */
    public int getNumTargetLanguages() {
        return mDatabaseHelper.getTargetLanguagesLength(mDatabase);
    }

    /**
     * Returns an array of translationNote slugs
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public String[] getNoteSlugs(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);
        long chapterId = mDatabaseHelper.getChapterDBId(mDatabase, chapterSlug, resourceId);
        long frameId = mDatabaseHelper.getFrameDBId(mDatabase, frameSlug, chapterId);

        if(frameId > 0) {
            return mDatabaseHelper.getTranslationNoteSlugs(mDatabase, frameId);
        }
        return new String[0];
    }

    /**
     * Returns an array of translationWord slugs
     * @param sourceTranslation
     * @return
     */
    public String[] getWordSlugs(SourceTranslation sourceTranslation) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            return mDatabaseHelper.getTranslationWordSlugs(mDatabase, resourceId);
        }
        return new String[0];
    }

    /**
     * Returns an array of translationWords for a single frame
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public TranslationWord[] getWordsForFrame(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        return mDatabaseHelper.getTranslationWordsForFrame(mDatabase, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug);
    }

    /**
     * Returns a translatonWord
     * @param sourceTranslation
     * @param wordSlug
     * @return
     */
    public TranslationWord getWord(SourceTranslation sourceTranslation, String wordSlug) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            return mDatabaseHelper.getTranslationWord(mDatabase, wordSlug, resourceId);
        } else {
            return null;
        }
    }

    /**
     * Returns an array of checkingQuestions
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public CheckingQuestion[] getCheckingQuestions(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        return mDatabaseHelper.getCheckingQuestions(mDatabase, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug);
    }

    /**
     * Returns a project
     * The default language will be used for the project title and description
     * @param projectSlug
     * @return
     */
    public synchronized Project getProject(String projectSlug) {
        String defaultLanguageCode = Locale.getDefault().getLanguage();
        return getProject(projectSlug, defaultLanguageCode);
    }

    /**
     * Returns a project
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public synchronized Project getProject(String projectSlug, String sourceLanguageSlug) {
        return mDatabaseHelper.getProject(mDatabase, projectSlug, sourceLanguageSlug);
    }

    /**
     * Returns a source language
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public synchronized SourceLanguage getSourceLanguage(String projectSlug, String sourceLanguageSlug) {
        return mDatabaseHelper.getSourceLanguage(mDatabase, projectSlug, sourceLanguageSlug);
    }

    /**
     * Returns a source translation
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @return
     */
    public SourceTranslation getSourceTranslation(String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        return mDatabaseHelper.getSourceTranslation(mDatabase, projectSlug, sourceLanguageSlug, resourceSlug);
    }

    /**
     * Returns an array of source translations that have updates available on the server.
     * @return
     */
    public SourceTranslation[] getSourceTranslationsWithUpdates() {
        return mDatabaseHelper.getSourceTranslationsWithUpdates(mDatabase);
    }

    /**
     * Returns a resource
     * @param translation
     * @return
     */
    public synchronized Resource getResource(SourceTranslation translation) {
        return mDatabaseHelper.getResource(mDatabase, translation.projectSlug, translation.sourceLanguageSlug, translation.resourceSlug);
    }

    /**
     * Returns a chapter
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public Chapter getChapter(SourceTranslation sourceTranslation, String chapterSlug) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);
        if(resourceId > 0) {
            return mDatabaseHelper.getChapter(mDatabase, chapterSlug, resourceId);
        }
        return null;
    }

    /**
     * Returns a frame
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public Frame getFrame(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);
        long chapterId = mDatabaseHelper.getChapterDBId(mDatabase, chapterSlug, resourceId);
        if(chapterId > 0) {
            return mDatabaseHelper.getFrame(mDatabase, frameSlug, chapterId);
        }
        return null;
    }

    /**
     * Returns the json object for a single checkingQuestion
     * @param sourceTranslation
     * @param chapterSlug
     * @param questionSlug
     * @return
     */
    public CheckingQuestion getCheckingQuestion(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug, String questionSlug) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);
        long chapterId = mDatabaseHelper.getChapterDBId(mDatabase, chapterSlug, resourceId);

        if(chapterId > 0) {
            return mDatabaseHelper.getCheckingQuestion(mDatabase, chapterId, frameSlug, questionSlug);
        }
        return null;
    }

    /**
     * Returns a translation note
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @param noteSlug
     * @return
     */
    public TranslationNote getNote(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug, String noteSlug) {
        long projectId = mDatabaseHelper.getProjectDBId(mDatabase, sourceTranslation.projectSlug);
        long sourceLanguageId = mDatabaseHelper.getSourceLanguageDBId(mDatabase, sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = mDatabaseHelper.getResourceDBId(mDatabase, sourceTranslation.resourceSlug, sourceLanguageId);
        long chapterId = mDatabaseHelper.getChapterDBId(mDatabase, chapterSlug, resourceId);
        long frameId = mDatabaseHelper.getFrameDBId(mDatabase, frameSlug, chapterId);
        if(frameId > 0) {
            return mDatabaseHelper.getTranslationNote(mDatabase, noteSlug, frameId);
        }
        return null;
    }

    /**
     * Returns the body of the chapter
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public String getChapterBody(SourceTranslation sourceTranslation, String chapterSlug) {
        return mDatabaseHelper.getChapterBody(mDatabase, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug);
    }

    /**
     * Returns the format of the chapter body
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public TranslationFormat getChapterBodyFormat(SourceTranslation sourceTranslation, String chapterSlug) {
        return mDatabaseHelper.getChapterBodyFromat(mDatabase, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug);
    }

    /**
     * Returns the translation notes in a frame
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public TranslationNote[] getTranslationNotes(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        return mDatabaseHelper.getTranslationNotes(mDatabase, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug);
    }

    /**
     * Returns the number of translatable items in the source translation.
     * This counts the frames, chapter titles, and chapter references.
     * Empty items will not be counted.
     * @param sourceTranslation
     * @return
     */
    public int numTranslatable(SourceTranslation sourceTranslation) {
        return mDatabaseHelper.countTranslatableItems(mDatabase, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug);
    }

    /**
     * Returns an array of projects
     * @param sourceLanguageSlug
     * @return
     */
    public Project[] getProjects(String sourceLanguageSlug) {
        return mDatabaseHelper.getProjects(mDatabase, sourceLanguageSlug);
    }

    /**
     * Marks the source language catalog in the project as up to date
     * @param projectSlug
     */
    public void markSourceLanguageCatalogUpToDate(String projectSlug) {
        mDatabaseHelper.markSourceLanguageCatalogUpToDate(mDatabase, projectSlug);
    }


    /**
     * Marks the resource catalog in source language has up to date
     * @param projectSlug
     * @param sourceLanguageSlug
     */
    public void markResourceCatalogUpToDate(String projectSlug, String sourceLanguageSlug) {
        mDatabaseHelper.markResourceCatalogUpToDate(mDatabase, projectSlug, sourceLanguageSlug);
    }
}
