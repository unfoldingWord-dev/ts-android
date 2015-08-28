package com.door43.translationstudio.core;

import com.door43.util.Security;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;


/**
 * Created by joel on 8/26/2015.
 */
public class Indexer {
    private final String mId;
    private final File mIndexDir;
    private final String mDataPath = "data";
    private final String mSourcePath = "source";
    private final String mLinksPath = mDataPath + "/links.json";

    /**
     * Destroys the entire index
     */
    public void destroy() {
        FileUtils.deleteQuietly(mIndexDir);
    }

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
    public Indexer(String name, File rootDir) {
        mId = name;
        mIndexDir = new File(rootDir, name);
    }

    /**
     * Returns the contents of a file in the index
     * @param path the relative path to the indexed file
     * @return a string or null
     */
    private String readFile(String path) {
        File file = new File(mIndexDir, path);
        if(file.exists()) {
            try {
                return FileUtils.readFileToString(file);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the JSON contents of a file in the index
     * @param path the relative path to the indexed file
     * @return
     */
    private JSONObject readJSON(String path) {
        String contents = readFile(path);
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
     * Deletes a file from the index
     * @param path
     */
    private Boolean deleteFile(String path) {
        File file = new File(mIndexDir, path);
        if(file.exists()) {
            return FileUtils.deleteQuietly(file);
        }
        return true;
    }

    /**
     * Saves a string to a file in the index
     * @param path the relative path to the file
     * @param contents the contents to be written
     * @return true if successful
     */
    private Boolean saveFile(String path, String contents) {
        File file = new File(mIndexDir, path);
        file.getParentFile().mkdirs();
        try {
            FileUtils.write(file, contents);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Increments the number of sources that link to some indexed data
     * @param md5hash the data index id
     * @return true if successfully incremented
     */
    private Boolean incrementLink(String md5hash) {
        JSONObject json = readJSON(mLinksPath);
        if (json == null) {
            json = new JSONObject();
        }
        try {
            if(!json.has(md5hash)) {
                json.put(md5hash, 0);
            }
            json.put(md5hash, json.getInt(md5hash) + 1);
            return saveFile(mLinksPath, json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Decrements the number of sources that link to some indexed data
     * When the number of links drops below 1 the indexed data will be deleted.
     * @param md5hash the data index id
     * @return true if successfully
     */
    private Boolean decrementLink(String md5hash) {
        JSONObject json = readJSON(mLinksPath);
        if(json == null) {
            json = new JSONObject();
        }
        try {
            if(json.has(md5hash)) {
                int count = json.getInt(md5hash) - 1;
                if(count <= 0) {
                    count = 0;
                    File indexedDataPath = new File(mDataPath, md5hash);
                    deleteFile(mDataPath + "/" + md5hash);
                }
                json.put(md5hash, count);
                saveFile(mLinksPath, json.toString());
            }
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Boolean indexItems (String md5hash, String catalogLinkFile, CatalogType type, String jsonString) {
        String md5Path = mDataPath + "/" + md5hash;
        JSONArray items;
        try {
            items = new JSONArray(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        // save link file
        saveFile(catalogLinkFile, md5hash);
        incrementLink(md5hash);

        // save items
        if(type == CatalogType.Simple) {
            for(int i = 0; i < items.length(); i ++ ) {
                try {
                    JSONObject item = items.getJSONObject(i);
                    if(item.has("slug")) {
                        String itemPath = md5Path + "/" + item.getString("slug") + ".json";
                        saveFile(itemPath, item.toString());
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
                                    String itemPath = md5Path + "/" + chapterId + "/" + frameId + "/" + noteId + ".json";
                                    saveFile(itemPath, note.toString());
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
                        String chapterPath = md5Path + "/" + chapterId + "/chapter.json";
                        saveFile(chapterPath, chapter.toString());
                        // save frames
                        for (int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
                            try {
                                JSONObject frame = frames.getJSONObject(frameIndex);
                                String frameId = frame.getString("id").replaceFirst("/[0-9]+\\-/", "");
                                if(!frameId.isEmpty()) {
                                    String framePath = md5Path + "/" + chapterId + "/" + frameId + ".json";
                                    saveFile(framePath, frame.toString());
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
    private String[] getItemsArray(JSONObject itemObject, String urlProperty, String subFolder) {
        String[] items = new String[0];
        if(itemObject == null) {
            return items;
        }

        String catalogApiUrl = getUrlFromObject(itemObject, urlProperty);
        if(catalogApiUrl != null) {
            String md5hash = Security.md5(catalogApiUrl);
            String md5path = mDataPath + "/" + md5hash;
            if(subFolder != null) {
                md5path = md5path + "/" + subFolder;
            }
            File itemDir = new File(mIndexDir, md5path);
            if(itemDir.exists()) {
                items = itemDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return !filename.equals("meta.json") && !filename.equals("chapter.json") && !filename.equals(".") && !filename.equals("..");
                    }
                });
                if(items == null) {
                    items = new String[0];
                }
            }
        }
        return items;
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
                String[] list = json.getString(urlProperty).split("/\\?/");
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
     * Deletes a resource from the index
     * @param translation
     */
    private void deleteResource (SourceTranslation translation) {
        // TODO: delete questions
        // TODO: delete notes
        // TODO: delete terms

        // delete resources
        String resourceCatalogPath =  mSourcePath + "/" + translation.projectId + "/" + translation.sourceLanguageId + "/resources_catalog.link";
        String md5hash = readFile(resourceCatalogPath);
        if(md5hash != null) {
            String md5path = mDataPath + "/" + md5hash;
            String resourcePath = md5path + "/" + translation.resourceId  + ".json";
            deleteFile(resourcePath);

            // delete empty resource catalog
            File resourceDir = new File(mIndexDir, md5path);
            String[] names = resourceDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return !filename.equals("meta.json") && !filename.equals(".") && !filename.equals("..");
                }
            });
            if(names != null || names.length == 0) {
                decrementLink(md5hash);
                deleteFile(resourceCatalogPath);
            }
        }
    }

    public void deleteSourceLanguage (String projectId, String sourceLanguageId) {

    }

    public void deleteProject (String projectId) {

    }

    public void mergeIndex(Indexer index) {

    }

    public void mergeProject( Indexer index, String projectId) {

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
            String catalogLinkFile = mSourcePath + "/projects_catalog.link";
            return indexItems(md5hash, catalogLinkFile, CatalogType.Simple, catalog);
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
            String catalogLinkFile = mSourcePath + "/" + projectId + "/languages_catalog.link";
            return indexItems(md5hash, catalogLinkFile, CatalogType.Simple, catalog);
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
            String catalogLinkFile = mSourcePath + "/" + projectId + "/" + sourceLanguageId + "/resources_catalog.link";
            return indexItems(md5hash, catalogLinkFile, CatalogType.Simple, catalog);
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
            e.printStackTrace();
            return false;
        }
        //KLUDGE: end modify v2

        String catalogApiUrl = getUrlFromObject(getResource(translation), "source");
        if(catalogApiUrl != null) {
            String md5hash = Security.md5(catalogApiUrl);
            String catalogLinkFile = mSourcePath + "/" + translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/source.link";
            return indexItems(md5hash, catalogLinkFile, CatalogType.Source, catalog);
        }
        return false;
    }

    /**
     * Builds a notes index from json
     * @param translation
     * @param catalog
     * @return
     */
    public Boolean indexNotes(SourceTranslation translation, String catalog) {
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

        String catalogApiUrl = getUrlFromObject(getResource(translation), "notes");
        if(catalogApiUrl != null) {
            String md5hash = Security.md5(catalogApiUrl);
            String catalogLinkFile = mSourcePath + "/" + translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/notes.link";
            return indexItems(md5hash, catalogLinkFile, CatalogType.Advanced, catalog);
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
        String catalogApiUrl = getUrlFromObject(getResource(translation), "terms");
        if(catalogApiUrl != null) {
            String md5hash = Security.md5(catalogApiUrl);
            String catalogLinkFile = mSourcePath + "/" + translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/terms.link";
            return indexItems(md5hash, catalogLinkFile, CatalogType.Terms, catalog);
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

        String catalogApiUrl = getUrlFromObject(getResource(translation), "checking_questions");
        if(catalogApiUrl != null) {
            String md5hash = Security.md5(catalogApiUrl);
            String catalogLinkFile = mSourcePath + "/" + translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/checking_questions.link";
            return indexItems(md5hash, catalogLinkFile, CatalogType.Advanced, catalog);
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
     * Returns an array of translationWord ids
     * @param translation
     * @param chapterId
     * @param frameId
     * @return
     */
    public String[] getTerms(SourceTranslation translation, String chapterId, String frameId) {
        String md5hash = readFile(mSourcePath + "/" + translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "terms.link");
        if(md5hash == null) {
            return null;
        }
        // TODO: 8/26/2015 Finish implimenting this. This depends on indexing the terms
        return new String[0];
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
    public JSONObject getProject(String projectId) {
        String md5hash = readFile(mSourcePath + "/projects_catalog.link");
        if(md5hash == null) {
            return null;
        }
        return readJSON(mDataPath + "/" + md5hash + "/" + projectId + ".json");
    }

    /**
     * Returns the json object for a single source language
     * @param projectId
     * @param sourcLanguageId
     * @return
     */
    public JSONObject getSourceLanguage(String projectId, String sourcLanguageId) {
        String md5hash = readFile(mSourcePath + "/" + projectId + "/languages_catalog.link");
        if(md5hash == null) {
            return null;
        }
        return readJSON(mDataPath + "/" + md5hash + "/" + sourcLanguageId + ".json");
    }

    /**
     * Returns the json object for a single resource
     * @param translation
     * @return
     */
    public JSONObject getResource(SourceTranslation translation) {
        String md5hash = readFile(mSourcePath + "/" + translation.projectId + "/" + translation.sourceLanguageId + "/resources_catalog.link");
        if(md5hash == null) {
            return null;
        }
        return readJSON(mDataPath + "/" + md5hash + "/" + translation.resourceId + ".json");
    }

    /**
     * Returns the json object for a single chapter
     * @param translation
     * @param chapterId
     * @return
     */
    public JSONObject getChapter(SourceTranslation translation, String chapterId) {
        String md5hash = readFile(mSourcePath + "/" + translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/source.link");
        if(md5hash == null) {
            return null;
        }
        return readJSON(mDataPath + "/" + md5hash + "/" + chapterId + "/chapter.json");
    }

    /**
     * Returns the json object for a single frame
     * @param translation
     * @param chapterId
     * @param frameId
     * @return
     */
    public JSONObject getFrame(SourceTranslation translation, String chapterId, String frameId) {
        String md5hash = readFile(mSourcePath + "/" + translation.projectId + "/" + translation.sourceLanguageId + "/" + translation.resourceId + "/source.link");
        if(md5hash == null) {
            return null;
        }
        return readJSON(mDataPath + "/" + md5hash + "/" + chapterId + "/" + frameId + ".json");
    }
}
