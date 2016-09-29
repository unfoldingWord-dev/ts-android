package com.door43.translationstudio.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import org.unfoldingword.tools.logger.Logger;

import com.door43.util.FileUtilities;
import com.door43.util.Security;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Created by joel on 8/26/2015.
 * TODO: we might make this static in the library as well
 */
@Deprecated
public class LibraryData {
    private static LibrarySQLiteHelper librarySQLiteHelper;
    public final static String DATABASE_NAME = "library";
    private final Context context;
    private SQLiteDatabase database;

    /**
     * Creates a new instance of the index
     */
    public LibraryData(Context context) throws IOException {
        synchronized (this) {
            if (this.librarySQLiteHelper == null) {
                this.librarySQLiteHelper = new LibrarySQLiteHelper(context, "schema.sqlite", DATABASE_NAME);
            }
        }
        this.database = this.librarySQLiteHelper.getWritableDatabase();
        this.context = context;
    }

    /**
     * Closes the index database
     */
    public synchronized void close() {
        librarySQLiteHelper.close();
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
     * Returns the index id
     * @return
     */
    public String getIndexId() {
        return DATABASE_NAME;
    }

    /**
     * Destroys the entire library
     */
    public synchronized void delete() {
        close();
        librarySQLiteHelper.deleteDatabase(context);
    }

    /**
     * Deploys a new library database.
     * This will close the existing db and replace it.
     * You will need to create a new LibraryData instance in order to connect to the new database.
     * @param newDatabasePath
     */
    public synchronized void deploy(File newDatabasePath) throws IOException {
        librarySQLiteHelper.close();
        librarySQLiteHelper.deleteDatabase(context);
        File databasePath = context.getDatabasePath(librarySQLiteHelper.getDatabaseName());
        databasePath.getParentFile().mkdirs();
        FileUtilities.moveOrCopyQuietly(newDatabasePath, databasePath);
        librarySQLiteHelper = null;
    }

    /**
     * Rebuilds the index database
     */
    public synchronized void rebuild() {
        database = librarySQLiteHelper.getWritableDatabase();
    }

    /**
     * Call to start a transaction
     */
    public void beginTransaction() {
        database.beginTransactionNonExclusive();
    }

    /**
     * Call to close the transaction
     * @param sucess
     */
    public void endTransaction(boolean sucess) {
        if(sucess) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();
    }

    /**
     * Removes a project from the library
     * @param slug
     */
    public synchronized void deleteProject(String slug) {
        this.database.delete("project", "`slug`=?", new String[]{slug});
    }

    /**
     * Removes a source language from the library
     * @param projectSlug
     * @param sourceLanguageSlug
     */
    public synchronized void deleteSourceLanguage(String projectSlug, String sourceLanguageSlug) {
        this.database.execSQL("DELETE FROM `source_language`"
                + " WHERE `id` IN ("
                + "  SELECT `sl`.`id` from `source_language` AS `sl`"
                + "  LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + "  WHERE `sl`.`slug`=? AND `p`.`slug`=?"
                + " )", new String[]{sourceLanguageSlug, projectSlug});
    }

    /**
     * Removes a resource from the index
     * @param sourceTranslation
     */
    private synchronized void deleteResource (SourceTranslation sourceTranslation) {
        this.database.execSQL("DELETE FROM `resource` AS `r`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + " WHERE `r`.`slug`=? AND `sl`.`slug`=? AND `p`.`slug`=?", new String[]{sourceTranslation.resourceSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.projectSlug});
    }

    /**
     * Removes a resource from the index
     * @param resourceId
     */
    public synchronized void deleteResource(long resourceId) {
        this.database.delete("resource", "id=" + resourceId, null);
    }

    /**
     * Inserts or replaces a resource in the library
     * @param resource
     */
    public void saveResource(Resource resource, long sourceLanguageId) {
        ContentValues values = new ContentValues();
        if(resource.getDBId() > 0) {
            values.put("id", resource.getDBId());
        }
        values.put("slug", resource.getId());
        values.put("source_language_id", sourceLanguageId);
        values.put("name", resource.getTitle());
        values.put("checking_level", resource.getCheckingLevel());
        values.put("version", resource.getVersion());
        values.put("modified_at", resource.getDateModified());
        values.put("source_catalog_url", resource.getSourceCatalogUrl());
        values.put("source_catalog_server_modified_at", resource.getSourceServerDateModified());
        values.put("translation_notes_catalog_url", resource.getNotesCatalogUrl());
        values.put("translation_notes_catalog_server_modified_at", resource.getNotesServerDateModified());
        values.put("translation_words_catalog_url", resource.getWordsCatalogUrl());
        values.put("translation_words_catalog_server_modified_at", resource.getWordsServerDateModified());
        values.put("translation_word_assignments_catalog_url", resource.getWordAssignmentsCatalogUrl());
        values.put("translation_word_assignments_catalog_server_modified_at", resource.getWordAssignmentsServerDateModified());
        values.put("checking_questions_catalog_url", resource.getQuestionsCatalogUrl());
        values.put("checking_questions_catalog_server_modified_at", resource.getQuestionsServerDateModified());
        this.database.insertWithOnConflict("resource", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Returns a branch of the category list
     * @param sourcelanguageSlug
     * @param parentCategoryId
     * @return
     */
    public ProjectCategory[] getCategoryBranch(String sourcelanguageSlug, long parentCategoryId) {
        // TODO: 7/6/16 this method will not return the correctly localized title as is
        // this needs to be updated to do so.
        List<ProjectCategory> categories = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT * FROM ("
                + " SELECT `c`.`slug` AS `category_slug`, `slc`.`category_name` AS `title`, NULL AS `project_slug`, 0 AS `sort`, `c`.`id` AS `category_id` FROM `category` AS `c`"
                + " LEFT JOIN `source_language__category` AS `slc` ON `slc`.`category_id`=`c`.`id`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`slc`.`source_language_id`"
                + " WHERE `sl`.`slug` IN (?, 'en') AND `c`.`parent_id`=" + parentCategoryId
                + " GROUP BY `c`.`id`"
                + " UNION"
                + " SELECT `c`.`slug` AS `category_slug`, `sl`.`project_name` AS `title`, `p`.`slug` AS `project_id`, `p`.`sort` AS `sort`, " + parentCategoryId + " AS `category_id` FROM `project` AS `p`"
                + " LEFT JOIN `project__category` AS `pc` ON `pc`.`project_id`=`p`.`id`"
                + " LEFT JOIN `category` AS `c` ON `c`.`id`=`pc`.`category_id`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + " WHERE CASE WHEN " + parentCategoryId + "=0 THEN `pc`.`category_id` IS NULL ELSE `pc`.`category_id`=" + parentCategoryId + " END AND `sl`.`slug` IN (?, 'en')"
                + ") ORDER BY `sort` ASC", new String[]{sourcelanguageSlug, sourcelanguageSlug});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            String categorySlug = cursor.getString(0);
            String title = cursor.getString(1);
            String projectSlug = cursor.getString(2);
            int sort = cursor.getInt(3);
            long categoryId = cursor.getLong(4);
            categories.add(new ProjectCategory(title, categorySlug, projectSlug, sourcelanguageSlug, categoryId));
            cursor.moveToNext();
        }
        cursor.close();
        return categories.toArray(new ProjectCategory[categories.size()]);
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
            Logger.e(this.getClass().getName(), "Failed to parse the projects catalog", e);
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
                    // TODO: eventually we will pass in the chunk marker catalog info
                    addProject(project.getId(), project.sort, project.dateModified, project.sourceLanguageCatalog, project.sourceLanguageCatalogServerDateModified, categorySlugs.toArray(new String[categorySlugs.size()]));
                    this.database.yieldIfContendedSafely();
                }
            } catch (JSONException e) {
                Logger.w(this.getClass().getName(), "Failed to parse a project", e);
            }
        }
        return true;
    }

    /**
     * Inserts or or updates a project
     * @param slug
     * @param sort
     * @param dateModified
     * @param sourceLanguageCatalogUrl
     * @param sourceLanguageCatalogServerModifiedAt
     * @param categorySlugs
     * @return
     */
    public long addProject(String slug, int sort, int dateModified, String sourceLanguageCatalogUrl, int sourceLanguageCatalogServerModifiedAt, String[] categorySlugs ) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("sort", sort);
        values.put("modified_at", dateModified);
        values.put("source_language_catalog_server_modified_at", sourceLanguageCatalogServerModifiedAt);
        values.put("source_language_catalog_url", sourceLanguageCatalogUrl);

        // add project
        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `project` WHERE `slug`=?", new String[]{slug});
        long projectId;
        if(cursor.moveToFirst()) {
            // update
            projectId = cursor.getLong(0);
            this.database.update("project", values, "`id`=" + projectId, new String[]{});
        } else {
            // insert
            projectId = this.database.insert("project", null, values);
        }
        cursor.close();

        // add categories
        this.database.delete("project__category", "project_id=" + projectId, null);
        addProjectCategories(projectId, categorySlugs);
        return projectId;
    }

    /**
     * Adds the project categories and links the project to the last category
     * @param projectId
     * @param categorySlugs
     * @return
     */
    private void addProjectCategories(long projectId, String[] categorySlugs) {
        if(categorySlugs != null && categorySlugs.length > 0) {
            long categoryId = 0L;
            for (String catSlug : categorySlugs) {
                Cursor cursor = this.database.rawQuery("SELECT `id` FROM `category` WHERE `slug`=? AND `parent_id`=" + categoryId, new String[]{catSlug});
                if (cursor.moveToFirst()) {
                    // follow
                    categoryId = cursor.getLong(0);
                } else {
                    // insert
                    ContentValues values = new ContentValues();
                    values.put("slug", catSlug);
                    values.put("parent_id", categoryId);
                    categoryId = this.database.insert("category", null, values);
                }
                cursor.close();
            }
            ContentValues values = new ContentValues();
            values.put("project_id", projectId);
            values.put("category_id", categoryId);
            this.database.insert("project__category", null, values);
        }
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
        long projectId = getProjectDBId(projectSlug);
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
                        addSourceLanguage(sourceLanguage.getId(),
                                projectId, sourceLanguage.name, sourceLanguage.projectTitle,
                                sourceLanguage.projectDescription,
                                sourceLanguage.getDirection().toString(), sourceLanguage.dateModified,
                                sourceLanguage.resourceCatalog, sourceLanguage.resourceCatalogServerDateModified, categoryNames.toArray(new String[categoryNames.size()]));
                        this.database.yieldIfContendedSafely();
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
     * Inserts or updates a source language
     * @param slug
     * @param projectId
     * @param name
     * @param projectName
     * @param projectDescription
     * @param direction
     * @param dateModified
     * @param resourceCatalogUrl
     */
    public long addSourceLanguage(String slug, long projectId, String name, String projectName, String projectDescription, String direction, int dateModified, String resourceCatalogUrl, int resourceCatalogServerModifiedAt, String[] categoryNames) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("project_id", projectId);
        values.put("name", name);
        values.put("project_name", projectName);
        values.put("project_description", projectDescription);
        values.put("direction", direction);
        values.put("modified_at", dateModified);
        values.put("resource_catalog_server_modified_at", resourceCatalogServerModifiedAt);
        values.put("resource_catalog_url", resourceCatalogUrl);

        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `source_language` WHERE `slug`=? AND `project_id`=" + projectId, new String[]{slug});
        long sourceLanguageId;
        if(cursor.moveToFirst()) {
            // update
            sourceLanguageId = cursor.getLong(0);
            this.database.update("source_language", values, "`id`=" + sourceLanguageId, new String[]{});
        } else {
            // insert
            sourceLanguageId = this.database.insert("source_language", null, values);
        }
        cursor.close();

        this.database.delete("source_language__category", "source_language_id=" + sourceLanguageId, null);
        addSourceLanguageCategories(projectId, sourceLanguageId, categoryNames);
        return sourceLanguageId;
    }

    /**
     * Adds the names for categories
     * @param sourceLanguageId
     * @param categoryNames
     */
    private void addSourceLanguageCategories(long projectId, long sourceLanguageId, String[] categoryNames) {
        if(categoryNames != null && categoryNames.length > 0) {
            Cursor cursor = this.database.rawQuery("SELECT `c`.`id` from `category` AS `c`"
                    + " LEFT JOIN `project__category` AS `pc` ON `pc`.`category_id`=`c`.`id`"
                    + " WHERE `pc`.`project_id`=" + projectId, null);
            if (cursor.moveToFirst()) {
                // bottom category
                long categoryId = cursor.getLong(0);
                cursor.close();

                // name categories from bottom to top
                for (String name : categoryNames) {
                    ContentValues values = new ContentValues();
                    values.put("source_language_id", sourceLanguageId);
                    values.put("category_id", categoryId);
                    values.put("category_name", name);
                    this.database.insert("source_language__category", null, values);

                    // move up in categories
                    cursor = this.database.rawQuery("SELECT `parent_id` FROM `category` WHERE `id`=" + categoryId, null);
                    if(cursor.moveToFirst()) {
                        categoryId = cursor.getLong(0);
                        if(categoryId == 0L) {
                            // stop when we reach the top
                            cursor.close();
                            break;
                        }
                    }
                    cursor.close();
                }
            } else {
                cursor.close();
            }
        }
    }

    /**
     * Returns the database id of a project
     * @param projectSlug
     * @return returns 0 if no record was found
     */
    public long getProjectDBId(String projectSlug) {
        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `project` WHERE `slug`=?", new String[]{projectSlug});
        long projectId = 0;
        if(cursor.moveToFirst()) {
            projectId = cursor.getLong(0);
        }
        cursor.close();
        return projectId;
    }

    /**
     * Returns the database id of a source language
     * @param slug
     * @param projectId
     * @return returns 0 if no record was found
     */
    public long getSourceLanguageDBId(String slug, long projectId) {
        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `source_language` WHERE `slug`=? AND `project_id`=" + projectId, new String[]{slug});
        long sourceLanguageId = 0;
        if(cursor.moveToFirst()) {
            sourceLanguageId = cursor.getLong(0);
        }
        cursor.close();
        return sourceLanguageId;
    }

    /**
     * Returns the database id for the resource
     * @param slug
     * @param sourceLanguageId
     * @return
     */
    public long getResourceDBId(String slug, long sourceLanguageId) {
        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `resource` WHERE `slug`=? AND `source_language_id`=" + sourceLanguageId, new String[]{slug});
        long resourceId = 0;
        if(cursor.moveToFirst()) {
            resourceId = cursor.getLong(0);
        }
        cursor.close();
        return resourceId;
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
            Logger.e(this.getClass().getName(), "Failed to parse the resources catalog for " + projectSlug + "-" + sourceLanguageSlug, e);
            return false;
        }

        long projectId = getProjectDBId(projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceLanguageSlug, projectId);

        for(int i = 0; i < items.length(); i ++ ) {
            try {
                JSONObject item = items.getJSONObject(i);
                Resource resource = Resource.generate(item);
                if(resource != null) {
                    addResource(resource.getId(), sourceLanguageId,
                            resource.getTitle(), resource.getCheckingLevel(), resource.getVersion(),
                            resource.getDateModified(), resource.getSourceCatalogUrl(), resource.getSourceServerDateModified(),
                            resource.getNotesCatalogUrl(), resource.getNotesServerDateModified(),
                            resource.getWordsCatalogUrl(), resource.getWordsServerDateModified(),
                            resource.getWordAssignmentsCatalogUrl(), resource.getWordAssignmentsServerDateModified(),
                            resource.getQuestionsCatalogUrl(), resource.getQuestionsServerDateModified());
                    this.database.yieldIfContendedSafely();
                }
            } catch (Exception e) {
                Logger.w(this.getClass().getName(), "Failed to parse a resource for " + projectSlug + "-" + sourceLanguageSlug, e);
            }
        }

        return true;
    }

    /**
     * Inserts or updates a resource
     * @param slug
     * @param sourceLanguageId
     * @param name
     * @param checkingLevel
     * @param version
     * @param dateModified
     * @param sourceCatalog
     * @param sourceServerDateModified
     * @param notesCatalog
     * @param notesServerDateModified
     * @param wordsCatalog
     * @param wordsServerDateModified
     * @param wordAssignmentsCatalog
     * @param wordAssignmentsServerDateModified
     * @param questionsCatalog
     * @param questionsServerDateModified
     */
    public long addResource(String slug, long sourceLanguageId, String name,
                            int checkingLevel, String version, int dateModified, String sourceCatalog,
                            int sourceServerDateModified, String notesCatalog, int notesServerDateModified,
                            String wordsCatalog, int wordsServerDateModified, String wordAssignmentsCatalog,
                            int wordAssignmentsServerDateModified, String questionsCatalog, int questionsServerDateModified) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("source_language_id", sourceLanguageId);
        values.put("name", name);
        values.put("checking_level", checkingLevel);
        values.put("version", version);
        values.put("modified_at", dateModified);
        values.put("source_catalog_url", sourceCatalog);
        values.put("source_catalog_server_modified_at", sourceServerDateModified);
        values.put("translation_notes_catalog_url", notesCatalog);
        values.put("translation_notes_catalog_server_modified_at", notesServerDateModified);
        values.put("translation_words_catalog_url", wordsCatalog);
        values.put("translation_words_catalog_server_modified_at", wordsServerDateModified);
        values.put("translation_word_assignments_catalog_url", wordAssignmentsCatalog);
        values.put("translation_word_assignments_catalog_server_modified_at", wordAssignmentsServerDateModified);
        values.put("checking_questions_catalog_url", questionsCatalog);
        values.put("checking_questions_catalog_server_modified_at", questionsServerDateModified);

        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `resource` WHERE `slug`=? AND `source_language_id`=" + sourceLanguageId, new String[]{slug});
        long resourceId;
        if(cursor.moveToFirst()) {
            // update
            resourceId = cursor.getLong(0);
            this.database.update("resource", values, "`id`=" + resourceId, new String[]{});
        } else {
            // insert
            resourceId = this.database.insert("resource", null, values);
        }
        cursor.close();
        return resourceId;
    }

    /**
     * Builds a translation academy index from json
     * @param sourceTranslation
     * @param catalog
     */
    public synchronized boolean indexTranslationAcademy(SourceTranslation sourceTranslation, String catalog) {
        JSONArray items;
        try {
            JSONObject catalogJson = new JSONObject(catalog);
            items = catalogJson.getJSONArray("volumes");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        Resource resource = getResource(sourceTranslation);

        if(resource != null) {
            for(int i = 0; i < items.length(); i ++) {
                try {
                    JSONObject volume = items.getJSONObject(i);
                    JSONArray manuals = volume.getJSONArray("manuals");

                    // index volume
                    String volSlug = volume.getString("id");
                    String volTitle = volume.getString("title");
                    long volumeId = addTranslationAcademyVolume(volSlug, resource.getDBId(), Security.md5(resource.getAcademyCatalogUrl()), volTitle);

                    for(int j  = 0; j < manuals.length(); j ++) {
                        try {
                            JSONObject manual = manuals.getJSONObject(j);
                            JSONArray articles = manual.getJSONArray("articles");

                            // index manual
                            String manSlug = manual.getString("id");
                            String manTitle = manual.getString("title");
                            long manualId = addTranslationAcademyManual(manSlug, volumeId, manTitle);

                            for(int k = 0; k < articles.length(); k ++) {
                                try {
                                    JSONObject article = articles.getJSONObject(k);

                                    // index article
                                    String artSlug = article.getString("id");
                                    String artTitle = article.getString("title");
                                    String artRef = article.getString("reference");
                                    String artText = article.getString("text");
                                    addTranslationAcademyArticle(artSlug, manualId, artTitle, artText, artRef);
                                } catch (JSONException e) {
                                    Logger.w(this.getClass().getName(), "Failed to parse a translation academy article for " + sourceTranslation.getId(), e);
                                }
                            }
                        } catch (JSONException e) {
                            Logger.w(this.getClass().getName(), "Failed to parse a translation academy manual for " + sourceTranslation.getId(), e);
                        }
                    }

                } catch (JSONException e) {
                    Logger.w(this.getClass().getName(), "Failed to parse a translation academy volume for " + sourceTranslation.getId(), e);
                }
            }
        }

        return true;
    }

    /**
     * Adds a translation academy volume to the library
     * @param volSlug
     * @param resourceDBId
     * @param cataloghash
     * @param volTitle
     * @return the database id of the volume
     */
    private long addTranslationAcademyVolume(String volSlug, long resourceDBId, String cataloghash, String volTitle) {
        ContentValues values = new ContentValues();
        values.put("slug", volSlug);
        values.put("catalog_hash", cataloghash);
        values.put("title", volTitle);

        Cursor cursor = this.database.rawQuery("SELECT `id` from `translation_academy_volume` WHERE `slug`=? AND `catalog_hash`=?", new String[]{volSlug, cataloghash});
        long volumeId;
        if(cursor.moveToFirst()) {
            // update
            volumeId = cursor.getLong(0);
            this.database.update("translation_academy_volume", values, "`id`=" + volumeId, new String[]{});
        } else {
            // insert
            volumeId = this.database.insert("translation_academy_volume", null, values);
        }
        cursor.close();

        // link volume to resource
        ContentValues linkValues = new ContentValues();
        linkValues.put("resource_id", resourceDBId);
        linkValues.put("translation_academy_volume_id", volumeId);
        this.database.insertWithOnConflict("resource__translation_academy_volume", null, linkValues, SQLiteDatabase.CONFLICT_IGNORE);

        return volumeId;
    }

    /**
     * Adds a translation academy manual to the library
     * @param manualSlug
     * @param volumeDBId
     * @param manualTitle
     * @return the database id of the manual
     */
    private long addTranslationAcademyManual(String manualSlug, long volumeDBId, String manualTitle) {
        ContentValues values = new ContentValues();
        values.put("slug", manualSlug);
        values.put("translation_academy_volume_id", volumeDBId);
        values.put("title", manualTitle);

        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `translation_academy_manual` WHERE `slug`=? AND `translation_academy_volume_id`=" + volumeDBId, new String[]{manualSlug});
        long manualId;
        if(cursor.moveToFirst()) {
            // update
            manualId = cursor.getLong(0);
            this.database.update("translation_academy_manual", values, "`id`=" + manualId, new String[]{});
        } else {
            // insert
            manualId = this.database.insert("translation_academy_manual", null, values);
        }
        cursor.close();
        return manualId;
    }

    /**
     * Adds a translation academy article to the library
     * @param articleId
     * @param manualDBId
     * @param articleTitle
     * @param articleText
     * @return the database id of the article
     */
    private long addTranslationAcademyArticle(String articleId, long manualDBId, String articleTitle, String articleText, String articleReference) {
        ContentValues values = new ContentValues();
        values.put("slug", articleId);
        values.put("translation_academy_manual_id", manualDBId);
        values.put("title", articleTitle);
        values.put("reference", articleReference);
        values.put("text", articleText);

        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `translation_academy_article` WHERE `slug`=? AND `translation_academy_manual_id`=" + manualDBId, new String[]{articleId});
        long articleDBId;
        if(cursor.moveToFirst()) {
            // update
            articleDBId = cursor.getLong(0);
            this.database.update("translation_academy_article", values, "`id`=" + articleDBId, new String[]{});
        } else {
            // insert
            articleDBId = this.database.insert("translation_academy_article", null, values);
        }
        cursor.close();
        return articleDBId;
    }

    /**
     *
     * @param sourceTranslation
     */
    public synchronized void markSourceCatalogUpToDate(SourceTranslation sourceTranslation) {
        this.database.execSQL("UPDATE `resource`"
                + " SET `source_catalog_local_modified_at`=`source_catalog_server_modified_at`"
                + " WHERE `source_language_id` IN ("
                + "   SELECT `sl`.`id` FROM `project` AS `p`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=?"
                + " ) AND `slug`=?", new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug});
    }

    /**
     *
     * @param sourceTranslation
     */
    public synchronized void markNotesCatalogUpToDate(SourceTranslation sourceTranslation) {
        this.database.execSQL("UPDATE `resource`"
                + " SET `translation_notes_catalog_local_modified_at`=`translation_notes_catalog_server_modified_at`"
                + " WHERE `source_language_id` IN ("
                + "   SELECT `sl`.`id` FROM `project` AS `p`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=?"
                + " ) AND `slug`=?", new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug});
    }

    /**
     *
     * @param sourceTranslation
     */
    public synchronized void markQuestionsCatalogUpToDate(SourceTranslation sourceTranslation) {
        this.database.execSQL("UPDATE `resource`"
                + " SET `checking_questions_catalog_local_modified_at`=`checking_questions_catalog_server_modified_at`"
                + " WHERE `source_language_id` IN ("
                + "   SELECT `sl`.`id` FROM `project` AS `p`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=?"
                + " ) AND `slug`=?", new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug});
    }

    /**
     *
     * @param sourceTranslation
     */
    public synchronized void markWordsCatalogUpToDate(SourceTranslation sourceTranslation) {
        this.database.execSQL("UPDATE `resource`"
                + " SET `translation_words_catalog_local_modified_at`=`translation_words_catalog_server_modified_at`"
                + " WHERE `source_language_id` IN ("
                + "   SELECT `sl`.`id` FROM `project` AS `p`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=?"
                + " ) AND `slug`=?", new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug});
    }

    /**
     *
     * @param sourceTranslation
     */
    public synchronized void markWordAssignmentsCatalogUpToDate(SourceTranslation sourceTranslation) {
        this.database.execSQL("UPDATE `resource`"
                + " SET `translation_word_assignments_catalog_local_modified_at`=`translation_word_assignments_catalog_server_modified_at`"
                + " WHERE `source_language_id` IN ("
                + "   SELECT `sl`.`id` FROM `project` AS `p`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=?"
                + " ) AND `slug`=?", new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug});
    }

    /**
     *
     * @param projectSlug
     */
    public synchronized void markChunkMarkerCatalogUpToDate(String projectSlug) {
        this.database.execSQL("UPDATE `project` SET"
                + " `chunk_marker_catalog_local_modified_at`=`chunk_marker_catalog_server_modified_at`"
                + " WHERE `slug`=?", new String[]{projectSlug});
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

        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);

        for(int chapterIndex = 0; chapterIndex < items.length(); chapterIndex ++ ) {
            try {
                JSONObject chapterJson = items.getJSONObject(chapterIndex);
                Chapter chapter = Chapter.generate(chapterJson);
                if(chapter != null && chapterJson.has("frames")) {
                    JSONArray frames = chapterJson.getJSONArray("frames");
                    long chapterId = addChapter(chapter.getId(), resourceId, chapter.reference, chapter.title);

                    for (int frameIndex = 0; frameIndex < frames.length(); frameIndex ++) {
                        try {
                            JSONObject frameJson = frames.getJSONObject(frameIndex);
                            Frame frame = Frame.generate(chapter.getId(), frameJson);
                            if(frame != null) {
                                addFrame(frame.getId(), chapterId, frame.body, frame.getFormat().toString(), frame.imageUrl);
                            }
                            this.database.yieldIfContendedSafely();
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
     * Inserts or updates a chapter
     * @param slug
     * @param resourceId
     * @param reference
     * @param title
     * @return
     */
    public long addChapter(String slug, long resourceId, String reference, String title) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("resource_id", resourceId);
        values.put("sort", Integer.parseInt(slug));
        values.put("reference", reference);
        values.put("title", title);

        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `chapter` WHERE `slug`=? AND `resource_id`=" + resourceId, new String[]{slug});
        long chapterId;
        if(cursor.moveToFirst()) {
            // update
            chapterId = cursor.getLong(0);
            this.database.update("chapter", values, "`id`=" + chapterId, new String[]{});
        } else {
            // insert
            chapterId = this.database.insert("chapter", null, values);
        }
        cursor.close();
        return chapterId;
    }

    /**
     * Inserts or updates a frame
     * @param slug
     * @param chapterId
     * @param body
     * @param format
     * @param imageUrl
     * @return
     */
    public long addFrame(String slug, long chapterId, String body, String format, String imageUrl) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("chapter_id", chapterId);
        values.put("sort", Integer.parseInt(slug));
        values.put("body", body);
        values.put("format", format);
        values.put("image_url", imageUrl);

        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `frame` WHERE `slug`=? AND `chapter_id`=" + chapterId, new String[]{slug});
        long frameId;
        if(cursor.moveToFirst()) {
            // update
            frameId = cursor.getLong(0);
            this.database.update("frame", values, "`id`=" + frameId, new String[]{});
        } else {
            // insert
            frameId = this.database.insert("frame", null, values);
        }
        cursor.close();
        return frameId;
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
                    addTargetLanguage(targetLanguage.getId(), targetLanguage.getDirection().toString(), targetLanguage.name, targetLanguage.region);
                }
                this.database.yieldIfContendedSafely();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // clean up temp target languages that conflict with real target languages
        cleanTempTargetLanguages();
        return true;
    }

    /**
     * Adds a target language
     * @param slug
     * @param direction
     * @param name
     * @param region
     */
    private long addTargetLanguage(String slug, String direction, String name, String region) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("name", name);
        values.put("direction", direction);
        values.put("region", region);
        return this.database.insertWithOnConflict("target_language", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Removes temp target languages that are found in the target_language table.
     * These can occur when importing a target translation that uses a langauge that has
     * not yet been downloaded
     */
    private void cleanTempTargetLanguages() {
        this.database.execSQL("DELETE FROM `temp_target_language` WHERE `id` IN (\n" +
                "SELECT `tl`.`id` FROM `temp_target_language` AS `ttl`\n" +
                "LEFT JOIN `target_language` AS `tl` ON `tl`.`slug`=`ttl`.`slug`\n" +
                "WHERE `tl`.`id` IS NOT null\n" +
                ")");
    }

    /**
     * Builds a index of all the temp target languages
     * @param catalog
     * @return
     */
    public boolean indexTempTargetLanguages(String catalog) {
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
                    addTempTargetLanguage(targetLanguage);
                }
                this.database.yieldIfContendedSafely();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Builds a index of all the temp target languages
     * @param catalog
     * @return
     */
    public boolean indexTempTargetLanguageAssignments(String catalog) {
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
                String tempLanguageCode = item.keys().next();
                String assignedLanguageCode = item.getString(tempLanguageCode);
                addTempTargetLanguageAssignment(tempLanguageCode, assignedLanguageCode);
                this.database.yieldIfContendedSafely();
            } catch (JSONException e) {
                Logger.w(this.getClass().getName(), "failed to parse the temp target langauge assignment", e);
            }
        }
        return true;
    }

    /**
     * Builds an index of all the new language questionnaires
     * @param catalog
     * @return
     */
    public boolean indexQuestionnaire(String catalog) {
        JSONArray items;
        try {
            JSONObject json = new JSONObject(catalog);
            items = json.getJSONArray("languages");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        // TRICKY: for now we only have one questionnaire so we always delete the existing one
        deleteQuestionnaires();

        for (int i = 0; i < items.length(); i ++) {
            try {
                JSONObject item = items.getJSONObject(i);
                Questionnaire questionnaire = Questionnaire.generate(item.toString());
                if(questionnaire != null) {
                    long questionnaireDBId = addQuestionnaire(questionnaire.door43Id, questionnaire.languageSlug, questionnaire.languageName, questionnaire.languageDirection);
                    if(item.has("questions")) {
                        JSONArray questionsJson = item.getJSONArray("questions");
                        // add questions
                        for (int j = 0; j < questionsJson.length(); j++) {
                            JSONObject questionJson = questionsJson.getJSONObject(j);
                            QuestionnaireQuestion question = QuestionnaireQuestion.generate(questionJson);
                            if (question != null) {
                                addQuestionnaireQuestion(questionnaireDBId, question.id, question.question, question.helpText, question.type, question.required, question.sort, question.reliantQuestionId);
                            }
                            database.yieldIfContendedSafely();
                        }
                    }
                    // add data fields
                    // TODO: 6/8/16 eventually this field will be renamed to "data_fields"
                    if(item.has("language_data")) {
                        JSONObject dataFieldJson = item.getJSONObject("language_data");
                        Iterator<String> keyIter = dataFieldJson.keys();
                        while (keyIter.hasNext()) {
                            String key = keyIter.next();
                            addQuestionnaireDataField(questionnaireDBId, key, dataFieldJson.getLong(key));
                            database.yieldIfContendedSafely();
                        }
                    }
                }
                database.yieldIfContendedSafely();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Adds a data field to the questionnaire
     * @param questionnaireId
     * @param field
     * @param questionDoor43Id
     * @return
     */
    private long addQuestionnaireDataField(long questionnaireId, String field, long questionDoor43Id) {
        ContentValues values = new ContentValues();
        values.put("questionnaire_id", questionnaireId);
        values.put("field", field);
        values.put("question_td_id", questionDoor43Id);
        return this.database.insert("questionnaire_data_field", null, values);
    }

    /**
     * Adds a new language questionnaire to the database
     * @param door43Id
     * @param languageSlug
     * @param languageName
     * @param languageDirection
     * @return
     */
    private long addQuestionnaire(long door43Id, String languageSlug, String languageName, LanguageDirection languageDirection) {
        ContentValues values = new ContentValues();
        values.put("questionnaire_td_id", door43Id);
        values.put("language_slug", languageSlug);
        values.put("language_name", languageName);
        values.put("language_direction", languageDirection.getLabel());
        return this.database.insert("questionnaire", null, values);
    }

    /**
     * Adds a question to the new language questionnnaire
     * @param questionnaireDBId
     * @param door43Id
     * @param question
     * @param helpText
     * @param type
     * @param required
     * @param sort
     * @param reliantQuestionId
     * @return
     */
    private long addQuestionnaireQuestion(long questionnaireDBId, long door43Id, String question, String helpText, QuestionnaireQuestion.InputType type, boolean required, int sort, long reliantQuestionId) {
        ContentValues values = new ContentValues();
        values.put("questionnaire_id", questionnaireDBId);
        values.put("question_td_id", door43Id);
        values.put("text", question);
        values.put("help", helpText);
        values.put("is_required", required);
        values.put("input_type", type.getLabel());
        values.put("sort", sort);
        values.put("depends_on", reliantQuestionId);
        return this.database.insert("questionnaire_question", null, values);
    }

    /**
     * Builds a notes index from json
     *
     * @param sourceTranslation
     * @param catalog
     * @return
     */
    public synchronized boolean indexTranslationNotes(SourceTranslation sourceTranslation, String catalog) {
        //KLUDGE: modify v2 notes catalogJson to match expected catalogJson format
        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "Failed to parse translation notes catalog for " + sourceTranslation.getId(), e);
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
                Logger.w(this.getClass().getName(), "Failed to parse a translation note for " + sourceTranslation.getId(), e);
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

        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            // index
            for (int chapterIndex = 0; chapterIndex < items.length(); chapterIndex++) {
                try {
                    JSONObject chapterJson = items.getJSONObject(chapterIndex);
                    String chapterSlug = chapterJson.getString("id");
                    long chapterId = getChapterDBId(chapterSlug, resourceId);
                    if(chapterId > 0) {
                        JSONArray frames = chapterJson.getJSONArray("frames");
                        for (int frameIndex = 0; frameIndex < frames.length(); frameIndex++) {
                            try {
                                JSONObject frameJson = frames.getJSONObject(frameIndex);
                                String frameSlug = frameJson.getString("id");
                                long frameId = getFrameDBId(frameSlug, chapterId);
                                if(frameId > 0) {
                                    JSONArray frameItems = frameJson.getJSONArray("items");
                                    for (int itemIndex = 0; itemIndex < frameItems.length(); itemIndex++) {
                                        try {
                                            JSONObject item = frameItems.getJSONObject(itemIndex);
                                            TranslationNote note = TranslationNote.generate(chapterSlug, frameSlug, item);
                                            if (note != null) {
                                                addTranslationNote(sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug, note.getId(), frameId, note.getTitle(), note.getBody());
                                            }
                                            database.yieldIfContendedSafely();
                                        } catch (JSONException e) {
                                            Logger.w(this.getClass().getName(), "Failed to parse a translation note in frame " + chapterSlug + "-" + frameSlug + " for " + sourceTranslation.getId(), e);
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                Logger.w(this.getClass().getName(), "Failed to parse a translation note in chapter " + chapterSlug + " for " + sourceTranslation.getId(), e);
                            }
                        }
                    }
                } catch (JSONException e) {
                    Logger.w(this.getClass().getName(), "Failed to parse a translation note for " + sourceTranslation.getId(), e);
                }
            }
        }
        return true;
    }

    /**
     * Inserts or updates a translation note
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @param chapterSlug
     * @param frameSlug
     * @param noteSlug
     * @param frameId
     * @param title
     * @param body
     * @return
     */
    private long addTranslationNote(String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug, String frameSlug, String noteSlug, long frameId, String title, String body) {
        ContentValues values = new ContentValues();
        values.put("slug", noteSlug);
        values.put("frame_id", frameId);
        values.put("project_slug", projectSlug);
        values.put("source_language_slug", sourceLanguageSlug);
        values.put("resource_slug", resourceSlug);
        values.put("chapter_slug", chapterSlug);
        values.put("frame_slug", frameSlug);
        values.put("title", title);
        values.put("body", body);

        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `translation_note` WHERE `slug`=? AND `frame_id`=" + frameId, new String[]{noteSlug});
        long noteId;
        if(cursor.moveToFirst()) {
            // update
            noteId = cursor.getLong(0);
            this.database.update("translation_note", values, "`id`=" + noteId, new String[]{});
        } else {
            // insert
            noteId = this.database.insert("translation_note", null, values);
        }
        cursor.close();
        return noteId;
    }

    /**
     * Builds a terms index from json
     * @param sourceTranslation
     * @param catalog
     * @return
     */
    public synchronized boolean indexTranslationWords(SourceTranslation sourceTranslation, String catalog) {
        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        Resource resource = getResource(sourceTranslation);

        if(resource != null) {
            for (int i = 0; i < items.length(); i++) {
                try {
                    JSONObject item = items.getJSONObject(i);
                    TranslationWord word = TranslationWord.generate(item);
                    if (word != null) {
                        addTranslationWord(word.getId(), resource.getDBId(), Security.md5(resource.getWordsCatalogUrl()), word.getTerm(), word.getDefinitionTitle(), word.getDefinition(), word.getExamples(), word.getAliases(), word.getSeeAlso());
                    }
                    database.yieldIfContendedSafely();
                } catch (JSONException e) {
                    Logger.w(this.getClass().getName(), "Failed to parse a translation word for " + sourceTranslation.getId(), e);
                }
            }
        }

        return true;
    }

    /**
     * inserts or replace a translation word
     * @param wordSlug
     * @param resourceId
     * @param catalogHash
     * @param term
     * @param definitionTitle
     * @param definition
     * @return
     */
    private long addTranslationWord(String wordSlug, long resourceId, String catalogHash, String term, String definitionTitle, String definition, TranslationWord.Example[] examples, String[] aliases, String[] related) {
        ContentValues values = new ContentValues();
        values.put("slug", wordSlug);
        values.put("catalog_hash", catalogHash);
        values.put("term", term);
        values.put("definition_title", definitionTitle);
        values.put("definition", definition);

        // identify existing word
        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `translation_word` WHERE `slug`=? AND `catalog_hash`=?", new String[]{wordSlug, catalogHash});
        long wordId;
        if(cursor.moveToFirst()) {
            // update
            wordId = cursor.getLong(0);
            this.database.update("translation_word", values, "`id`=" + wordId, new String[]{});
        } else {
            // insert
            wordId = this.database.insert("translation_word", null, values);
        }
        cursor.close();

        // link word to resource
        ContentValues linkValues = new ContentValues();
        linkValues.put("resource_id", resourceId);
        linkValues.put("translation_word_id", wordId);
        this.database.insertWithOnConflict("resource__translation_word", null, linkValues, SQLiteDatabase.CONFLICT_IGNORE);

        // insert examples
        for(TranslationWord.Example example:examples) {
            ContentValues exampleValues = new ContentValues();
            exampleValues.put("frame_slug", example.getFrameId());
            exampleValues.put("chapter_slug", example.getChapterId());
            exampleValues.put("body", example.getPassage());
            exampleValues.put("translation_word_id", wordId);
            this.database.insertWithOnConflict("translation_word_example", null, exampleValues, SQLiteDatabase.CONFLICT_IGNORE);
        }

        // insert aliases
        for(String alias:aliases) {
            ContentValues aliasValues = new ContentValues();
            aliasValues.put("term", alias.trim());
            aliasValues.put("translation_word_id", wordId);
            this.database.insertWithOnConflict("translation_word_alias", null, aliasValues, SQLiteDatabase.CONFLICT_IGNORE);
        }

        // insert related
        for(String relatedWordSlug:related) {
            ContentValues relatedValues = new ContentValues();
            relatedValues.put("slug", relatedWordSlug.trim());
            relatedValues.put("translation_word_id", wordId);
            this.database.insertWithOnConflict("translation_word_related", null, relatedValues, SQLiteDatabase.CONFLICT_IGNORE);
        }

        return wordId;
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
            Logger.e(this.getClass().getName(), "Failed to parse word assignments for " + sourceTranslation.getId(), e);
            return false;
        }
        //KLUDGE: end modify v2\

        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            // index
            for (int chapterIndex = 0; chapterIndex < items.length(); chapterIndex++) {
                try {
                    JSONObject chapter = items.getJSONObject(chapterIndex);
                    String chapterSlug = chapter.getString("id");
                    long chapterId = getChapterDBId(chapterSlug, resourceId);
                    if(chapterId > 0) {
                        JSONArray frames = chapter.getJSONArray("frames");
                        for (int frameIndex = 0; frameIndex < frames.length(); frameIndex++) {
                            try {
                                JSONObject frame = frames.getJSONObject(frameIndex);
                                String frameSlug = frame.getString("id");
                                long frameId = getFrameDBId(frameSlug, chapterId);
                                if(frameId > 0) {
                                    JSONArray frameItems = frame.getJSONArray("items");
                                    for (int itemIndex = 0; itemIndex < frameItems.length(); itemIndex++) {
                                        try {
                                            JSONObject item = frameItems.getJSONObject(itemIndex);
                                            if (item.has("id")) {
                                                addTranslationWordToFrame(item.getString("id"), resourceId, frameId, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug);
                                            }
                                        } catch (JSONException e) {
                                            Logger.w(this.getClass().getName(), "Failed to parse a word assignment in frame " + chapterSlug + "-" + frameSlug + " for " + sourceTranslation.getId(), e);
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                Logger.w(this.getClass().getName(), "Failed to parse a word assignment in chpater " + chapterSlug + " for " + sourceTranslation.getId(), e);
                            }
                        }
                    }
                } catch (JSONException e) {
                    Logger.w(this.getClass().getName(), "Failed to parse a word assignment for " + sourceTranslation.getId(), e);
                }
            }
        }

        return true;
    }

    /**
     * links a translation word to a frame
     * @param wordSlug
     * @param frameId
     */
    private void addTranslationWordToFrame(String wordSlug, long resourceId, long frameId, String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug, String frameSlug) {
        long wordId = getTranslationWordDBId(wordSlug, resourceId);
        if(wordId > 0) {
            this.database.execSQL("REPLACE INTO `frame__translation_word` (`frame_id`, `translation_word_id`, `project_slug`, `source_language_slug`, `resource_slug`, `chapter_slug`, `frame_slug`) VALUES (" + frameId + "," + wordId + ",?,?,?,?,?)", new String[]{projectSlug, sourceLanguageSlug, resourceSlug, chapterSlug, frameSlug});
        }
    }

    /**
     * Returns the database id for a translation word
     * @param wordSlug
     * @param resourceId
     * @return
     */
    private long getTranslationWordDBId(String wordSlug, long resourceId) {
        Cursor cursor = this.database.rawQuery("SELECT `tw`.`id` FROM `translation_word` AS `tw`"
                + " LEFT JOIN `resource__translation_word` AS `rtw` ON `rtw`.`translation_word_id`=`tw`.`id`"
                + " WHERE `tw`.`slug`=? AND `rtw`.`resource_id`=" + resourceId, new String[]{wordSlug});
        long wordId = 0;
        if(cursor.moveToFirst()) {
            wordId = cursor.getLong(0);
        }
        cursor.close();
        return wordId;
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
            Logger.e(this.getClass().getName(), "Failed to parse the questions catalog for " + sourceTranslation.getId(), e);
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
                        Logger.w(this.getClass().getName(), "Failed to parse question in chapter " + chapterId + " for " + sourceTranslation.getId(), e);
                    }
                }
            } catch (JSONException e) {
                Logger.w(this.getClass().getName(), "Failed to parse a question for " + sourceTranslation.getId(), e);
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

        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            for (int chapterIndex = 0; chapterIndex < items.length(); chapterIndex++) {
                try {
                    JSONObject chapter = items.getJSONObject(chapterIndex);
                    String chapterSlug = chapter.getString("id");
                    long chapterId = getChapterDBId(chapterSlug, resourceId);
                    if(chapterId > 0) {
                        JSONArray frames = chapter.getJSONArray("frames");
                        for (int frameIndex = 0; frameIndex < frames.length(); frameIndex++) {
                            try {
                                JSONObject frame = frames.getJSONObject(frameIndex);
                                String frameSlug = frame.getString("id");
                                long frameId = getFrameDBId(frameSlug, chapterId);
                                if(frameId > 0) {
                                    JSONArray frameItems = frame.getJSONArray("items");
                                    for (int itemIndex = 0; itemIndex < frameItems.length(); itemIndex++) {
                                        try {
                                            JSONObject item = frameItems.getJSONObject(itemIndex);
                                            CheckingQuestion question = CheckingQuestion.generate(chapterSlug, frameSlug, item);
                                            if (question != null) {
                                                addCheckingQuestion(sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug, question.getId(), frameId, chapterId, question.getQuestion(), question.getAnswer());
                                            }
                                            database.yieldIfContendedSafely();
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
     * Adds a checking question and links it to the frame
     * @param frameId
     * @param chapterId
     * @param question
     * @param answer
     */
    private long addCheckingQuestion(String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug, String frameSlug, String questionSlug, long frameId, long chapterId, String question, String answer) {
        ContentValues values = new ContentValues();
        values.put("slug", questionSlug);
        values.put("chapter_id", chapterId);
        values.put("question", question);
        values.put("answer", answer);

        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `checking_question` WHERE `slug`=? AND `chapter_id`=" + chapterId, new String[]{questionSlug});
        long questionId;
        if(cursor.moveToFirst()) {
            // update
            questionId = cursor.getLong(0);
            this.database.update("checking_question", values, "`id`=" + questionId, new String[]{});
        } else {
            // insert
            questionId = this.database.insert("checking_question", null, values);
        }
        cursor.close();

        // link question to frame
        ContentValues linkValues = new ContentValues();
        linkValues.put("frame_id", frameId);
        linkValues.put("checking_question_id", questionId);
        linkValues.put("project_slug", projectSlug);
        linkValues.put("source_language_slug", sourceLanguageSlug);
        linkValues.put("resource_slug", resourceSlug);
        linkValues.put("chapter_slug", chapterSlug);
        linkValues.put("frame_slug", frameSlug);
        this.database.replace("frame__checking_question", null, linkValues);

        return questionId;
    }

    /**
     * Builds a chunks index from json
     * @param projectSlug
     * @param catalog
     * @return
     */
    public synchronized boolean indexChunkMarkers(String projectSlug, String catalog) {
        JSONArray items;
        try {
            items = new JSONArray(catalog);
        } catch (JSONException e) {
            Logger.e(this.getClass().getName(), "Failed to parse the chunk marker catalog for " + projectSlug, e);
            return false;
        }

        long projectId = getProjectDBId(projectSlug);

        if(projectId > 0) {
            for (int i = 0; i < items.length(); i ++) {
                try {
                    JSONObject item = items.getJSONObject(i);

                    addChunkMarker(item.getString("chp"), item.getString("firstvs"), projectId);
                    database.yieldIfContendedSafely();
                } catch (JSONException e) {
                    Logger.w(this.getClass().getName(), "Failed to parse a chunk marker for " + projectSlug, e);
                }
            }
        }
        return true;
    }

    /**
     * Adds a chunk marker
     *
     * @param chapter
     * @param firstVerse
     * @param projectId
     * @return
     */
    private long addChunkMarker(String chapter, String firstVerse, long projectId) {
        ContentValues values = new ContentValues();
        values.put("chapter_slug", chapter);
        values.put("first_verse_slug", firstVerse);
        values.put("project_id", projectId);

        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `chunk_marker` WHERE `chapter_slug`=? AND `first_verse_slug`=? AND `project_id`=" + projectId, new String[]{chapter, firstVerse});
        long chunkMarkerDBId;
        if(cursor.moveToFirst()) {
            // update
            chunkMarkerDBId = cursor.getLong(0);
            // nothing to update here
        } else {
            // insert
            chunkMarkerDBId = this.database.insert("chunk_marker", null, values);
        }
        cursor.close();
        return chunkMarkerDBId;
    }

    /**
     * Returns an array of project slugs
     * @return
     */
    public String[] getProjectSlugs() {
        // // TODO: 10/16/2015 see if we can avoid using this method
        Cursor cursor = this.database.rawQuery("SELECT `slug` FROM `project` ORDER BY `sort` ASC", null);
        cursor.moveToFirst();
        List<String> slugs = new ArrayList<>();
        while(!cursor.isAfterLast()) {
            slugs.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return slugs.toArray(new String[slugs.size()]);
    }

    /**
     * Returns an array of source language ids
     * @param projectSlug
     * @return
     */
    public String[] getSourceLanguageSlugs(String projectSlug) {
        long projectId = getProjectDBId(projectSlug);
        if(projectId > 0) {
            return getSourceLanguageSlugs(projectId);
        }
        return new String[0];
    }

    /**
     * Returns an array of sorted source language slugs
     * @param projectId
     * @return
     */
    private String[] getSourceLanguageSlugs(long projectId) {
        Cursor cursor = this.database.rawQuery("SELECT `slug` FROM `source_language` WHERE `project_id`=" + projectId + " ORDER BY `slug` ASC", null);
        cursor.moveToFirst();
        List<String> slugs = new ArrayList<>();
        while(!cursor.isAfterLast()) {
            slugs.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return slugs.toArray(new String[slugs.size()]);
    }

    /**
     * Returns an array of resource slugs
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public String[] getResourceSlugs(String projectSlug, String sourceLanguageSlug) {
        long projectId = getProjectDBId(projectSlug);
        if(projectId > 0) {
            long sourceLanguageId = getSourceLanguageDBId(sourceLanguageSlug, projectId);
            if(sourceLanguageId > 0) {
                return getResourceSlugs(sourceLanguageId);
            }
        }
        return new String[0];
    }

    /**
     * Returns an array of resource slugs
     * @param sourceLanguageId
     * @return
     */
    private String[] getResourceSlugs(long sourceLanguageId) {
        Cursor cursor = this.database.rawQuery("SELECT `slug` FROM `resource` WHERE `source_language_id`=" + sourceLanguageId + " ORDER BY `slug` ASC", null);
        cursor.moveToFirst();
        List<String> slugs = new ArrayList<>();
        while(!cursor.isAfterLast()) {
            slugs.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return slugs.toArray(new String[slugs.size()]);
    }

    /**
     * Returns an array of chapter ids
     * @param sourceTranslation
     * @return
     */
    public String[] getChapterSlugs(SourceTranslation sourceTranslation) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);
        if(resourceId > 0) {
            return getChapterSlugs(resourceId);
        }
        return new String[0];
    }

    /**
     * Returns an array of chapter slugs
     * @param resourceId
     * @return
     */
    private String[] getChapterSlugs(long resourceId) {
        Cursor cursor = this.database.rawQuery("SELECT `slug` FROM `chapter` WHERE `resource_id`=" + resourceId + " ORDER BY `sort` ASC", null);
        cursor.moveToFirst();
        List<String> slugs = new ArrayList<>();
        while(!cursor.isAfterLast()) {
            slugs.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return slugs.toArray(new String[slugs.size()]);
    }

    /**
     * Returns a list of chapters
     * @return
     */
    @Deprecated
    public Chapter[] getChapters(SourceTranslation sourceTranslation) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            return getChapters(resourceId);
        }
        return new Chapter[0];
    }

    /**
     * Returns an array of chapters
     * @param resourceId
     * @return
     */
    private Chapter[] getChapters(long resourceId) {
        // we'll need to update the schema to include the slugs in the chapter table in order to do this
        List<Chapter> chapters = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `slug`, `reference`, `title` FROM `chapter` WHERE `resource_id`=" + resourceId + " ORDER BY `sort` ASC", null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            String slug = cursor.getString(0);
            String reference = cursor.getString(1);
            String title = cursor.getString(2);
            chapters.add(new Chapter(title, reference, slug));
            cursor.moveToNext();
        }
        cursor.close();
        return chapters.toArray(new Chapter[chapters.size()]);
    }

    /**
     * Returns an array of frame ids
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public String[] getFrameSlugs(SourceTranslation sourceTranslation, String chapterSlug) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);
        long chapterId = getChapterDBId(chapterSlug, resourceId);

        if(chapterId > 0) {
            return getFrameSlugs(chapterId);
        }
        return new String[0];
    }

    /**
     * Returns an array of frame slugs
     * @param chapterId
     * @return
     */
    private String[] getFrameSlugs(long chapterId) {
        Cursor cursor = this.database.rawQuery("SELECT `slug` FROM `frame` WHERE `chapter_id`=" + chapterId + " ORDER BY `sort` ASC", null);
        cursor.moveToFirst();
        List<String> slugs = new ArrayList<>();
        while(!cursor.isAfterLast()) {
            slugs.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return slugs.toArray(new String[slugs.size()]);
    }

    /**
     * Returns an array of frame contents
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public Frame[] getFrames(SourceTranslation sourceTranslation, String chapterSlug) {
        List<Frame> frames = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `f`.`id`, `f`.`slug`, `f`.`body`, `f`.`format`, `f`.`image_url` FROM `frame` AS `f`"
                + " WHERE `f`.`chapter_id` IN ("
                + "   SELECT `c`.`id` FROM `chapter` AS `c`"
                + "   LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + "   LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=? AND `c`.`slug`=?"
                + " ) ORDER BY `f`.`sort` ASC", new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            long id = cursor.getLong(0);
            String slug = cursor.getString(1);
            String body = cursor.getString(2);
            String rawFormat = cursor.getString(3);
            TranslationFormat format = TranslationFormat.get(rawFormat);
            if(format == null) {
                format = TranslationFormat.DEFAULT;
            }
            String imageUrl = cursor.getString(4);
            Frame frame = new Frame(slug, chapterSlug, body, format, imageUrl);
            frame.setDBId(id);
            frames.add(frame);
            cursor.moveToNext();
        }
        cursor.close();
        return frames.toArray(new Frame[frames.size()]);
    }

    /**
     * Returns an array of target languages.
     * This will include temp languages codes that have not yet been approved
     * @return
     */
    public TargetLanguage[] getTargetLanguages() {
        List<TargetLanguage> targetLanguages = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `slug`, `name`, `direction`, `region` FROM `target_language`\n" +
                "UNION\n" +
                "SELECT `slug`, `name`, `direction`, `region` FROM `temp_target_language`\n" +
                "WHERE `approved_target_language_slug` IS NULL\n" +
                "ORDER BY `slug` ASC, `name` DESC", null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            String slug = cursor.getString(0);
            String name = cursor.getString(1);
            LanguageDirection direction = LanguageDirection.get(cursor.getString(2));
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
            String region = cursor.getString(3);
            targetLanguages.add(new TargetLanguage(slug, name, region, direction));
            cursor.moveToNext();
        }
        cursor.close();
        return targetLanguages.toArray(new TargetLanguage[targetLanguages.size()]);
    }

    /**
     * Returns a target language.
     * This will include temp languages codes that have not yet been approved
     * @param targetLanguageSlug
     * @return
     */
    public TargetLanguage getTargetLanguage(String targetLanguageSlug) {
        Cursor cursor = this.database.rawQuery("SELECT * FROM (" +
                "SELECT `slug`, `name`, `direction`, `region` FROM `target_language`\n" +
                "UNION\n" +
                "SELECT `slug`, `name`, `direction`, `region` FROM `temp_target_language` AS `ttl`\n" +
                "WHERE `approved_target_language_slug` IS NULL)\n" +
                "WHERE `slug`=?", new String[]{targetLanguageSlug});
        TargetLanguage targetLanguage = null;
        if(cursor.moveToFirst()) {
            String name = cursor.getString(1);
            LanguageDirection direction = LanguageDirection.get(cursor.getString(2));
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
            String region = cursor.getString(3);
            targetLanguage = new TargetLanguage(targetLanguageSlug, name, region, direction);
        }
        cursor.close();
        return targetLanguage;
    }

    /**
     * Returns a target language that has been approved from a temporary language code request
     * @param tempLanguageCode the temp language code to look up
     * @return
     */
    public TargetLanguage getApprovedTargetLanguage(String tempLanguageCode) {
        Cursor cursor = this.database.rawQuery("SELECT `tl`.`slug`, `tl`.`name`, `tl`.`direction`,\n" +
                "`tl`.`region` FROM `target_language` AS `tl`\n" +
                "LEFT JOIN `temp_target_language` AS `ttl` ON `ttl`.`approved_target_language_slug`=`tl`.`slug`\n" +
                "WHERE `ttl`.`slug`=?", new String[]{tempLanguageCode});
        TargetLanguage targetLanguage = null;
        if(cursor.moveToFirst()) {
            String slug = cursor.getString(0);
            String name = cursor.getString(1);
            LanguageDirection direction = LanguageDirection.get(cursor.getString(2));
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
            String region = cursor.getString(3);
            targetLanguage = new TargetLanguage(slug, name, region, direction);
        }
        cursor.close();
        return targetLanguage;
    }

    /**
     * Searches for target languages by name
     * @param nameQuery
     * @return
     */
    public TargetLanguage[] findTargetLanguage(final String nameQuery) {
        List<TargetLanguage> targetLanguages = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT * FROM (\n" +
                "SELECT `slug`, `name`, `direction`, `region` FROM `target_language`\n" +
                "UNION\n" +
                "SELECT `slug`, `name`, `direction`, `region` FROM `temp_target_language` AS `ttl`\n" +
                "WHERE `approved_target_language_slug` IS NULL)\n" +
                "WHERE LOWER(`name`) LIKE ?\n" +
                "ORDER BY `slug` ASC, `name` DESC", new String[]{"%" + nameQuery.toLowerCase() + "%"});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            String slug = cursor.getString(0);
            String name = cursor.getString(1);
            LanguageDirection direction = LanguageDirection.get(cursor.getString(2));
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
            String region = cursor.getString(3);
            targetLanguages.add(new TargetLanguage(slug, name, region, direction));
            cursor.moveToNext();
        }
        cursor.close();
        Collections.sort(targetLanguages, new Comparator<TargetLanguage>() {
            @Override
            public int compare(TargetLanguage lhs, TargetLanguage rhs) {
                String lhId = lhs.getId();
                String rhId = rhs.getId();
                // give priority to matches with the id
                if(lhId.toLowerCase().startsWith(nameQuery.toLowerCase())) {
                    lhId = "!!" + lhId;
                }
                if(rhId.toLowerCase().startsWith(nameQuery.toLowerCase())) {
                    rhId = "!!" + rhId;
                }
                if(lhs.name.toLowerCase().startsWith(nameQuery.toLowerCase())) {
                    lhId = "!" + lhId;
                }
                if(rhs.name.toLowerCase().startsWith(nameQuery.toLowerCase())) {
                    rhId = "!" + rhId;
                }
                return lhId.compareToIgnoreCase(rhId);
            }
        });
        return targetLanguages.toArray(new TargetLanguage[targetLanguages.size()]);
    }

    /**
     * Returns the number of target languages.
     * This will include temp languages codes that have not yet been approved
     * @return
     */
    public int getNumTargetLanguages() {
        Cursor cursor = this.database.rawQuery("SELECT COUNT(*) FROM (" +
                "SELECT `slug`, `name`, `direction`, `region` FROM `target_language`\n" +
                "UNION\n" +
                "SELECT `slug`, `name`, `direction`, `region` FROM `temp_target_language` AS `ttl`\n" +
                "WHERE `approved_target_language_slug` IS NULL)", null);
        int count = 0;
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * Returns an array of translationNote slugs
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public String[] getNoteSlugs(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);
        long chapterId = getChapterDBId(chapterSlug, resourceId);
        long frameId = getFrameDBId(frameSlug, chapterId);

        if(frameId > 0) {
            return getTranslationNoteSlugs(frameId);
        }
        return new String[0];
    }

    /**
     * Returns an array of translation note slugs
     * @param frameId
     * @return
     */
    private String[] getTranslationNoteSlugs(long frameId) {
        Cursor cursor = this.database.rawQuery("SELECT `slug` FROM `translation_note` WHERE `frame_id`=" + frameId, null);
        cursor.moveToFirst();
        List<String> slugs = new ArrayList<>();
        while(!cursor.isAfterLast()) {
            slugs.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return slugs.toArray(new String[slugs.size()]);
    }

    /**
     * Returns an array of translationWord slugs
     * @param sourceTranslation
     * @return
     */
    public String[] getWordSlugs(SourceTranslation sourceTranslation) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            return getTranslationWordSlugs(resourceId);
        }
        return new String[0];
    }

    /**
     * Returns an array of translation word slugs
     * @param resourceId
     * @return
     */
    private String[] getTranslationWordSlugs(long resourceId) {
        Cursor cursor = this.database.rawQuery("SELECT `slug` FROM `translation_word`"
                + " WHERE `id` IN ("
                + "   SELECT `translation_word_id` FROM `resource__translation_word`"
                + "   WHERE `resource_id`=" + resourceId
                + ") ORDER BY `slug` ASC", null);
        cursor.moveToFirst();
        List<String> slugs = new ArrayList<>();
        while(!cursor.isAfterLast()) {
            slugs.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return slugs.toArray(new String[slugs.size()]);
    }

    /**
     * Returns an array of translationWords for a source translation
     * @param sourceTranslation
     * @return
     */
    public TranslationWord[] getWords(SourceTranslation sourceTranslation) {
        List<TranslationWord> words = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `id`, `slug`, `term`, `definition`, `definition_title` FROM `translation_word`"
                + " WHERE `id` IN ("
                + "   SELECT `translation_word_id` FROM `resource__translation_word` AS `rtw`"
                + "   LEFT JOIN `resource` AS `r` ON `r`.`id`=`rtw`.`resource_id`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + "   LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=?"
                + " ) ORDER BY `slug` ASC", new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            long wordId = cursor.getLong(0);
            String wordSlug = cursor.getString(1);
            String term = cursor.getString(2);
            String definition = cursor.getString(3);
            String definitionTitle = cursor.getString(4);

            // NOTE: we purposely do not retrieve the related terms, aliases and example passages for better performance
            words.add(new TranslationWord(wordSlug, term, definition, definitionTitle, new String[0],  new String[0],  new TranslationWord.Example[0]));
            cursor.moveToNext();
        }
        cursor.close();
        return words.toArray(new TranslationWord[words.size()]);
    }

    /**
     * Returns an array of translationWords for a single frame
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public TranslationWord[] getWordsForFrame(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        List<TranslationWord> words = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `id`, `slug`, `term`, `definition`, `definition_title` FROM `translation_word`"
                + " WHERE `id` IN ("
                + "   SELECT `translation_word_id` FROM `frame__translation_word`"
                + "   WHERE `project_slug`=? AND `source_language_slug`=? AND `resource_slug`=? AND `chapter_slug`=? AND `frame_slug`=?"
                + " ) ORDER BY `slug` ASC", new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            long wordId = cursor.getLong(0);
            String wordSlug = cursor.getString(1);
            String term = cursor.getString(2);
            String definition = cursor.getString(3);
            String definitionTitle = cursor.getString(4);

            // NOTE: we purposely do not retrieve the related terms, aliases and example passages for better performance
            words.add(new TranslationWord(wordSlug, term, definition, definitionTitle, new String[0],  new String[0],  new TranslationWord.Example[0]));
            cursor.moveToNext();
        }
        cursor.close();
        return words.toArray(new TranslationWord[words.size()]);
    }

    /**
     * Returns a translatonWord
     * @param sourceTranslation
     * @param wordSlug
     * @return
     */
    @Nullable
    public TranslationWord getWord(SourceTranslation sourceTranslation, String wordSlug) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            return getTranslationWord(wordSlug, resourceId);
        } else {
            return null;
        }
    }

    /**
     * Returns a translation word
     * @param slug
     * @param resourceId
     * @return
     */
    private TranslationWord getTranslationWord(String slug, long resourceId) {
        Cursor cursor = this.database.rawQuery("SELECT `tw`.`id`, `tw`.`term`, `tw`.`definition`, `tw`.`definition_title`, `related`.`related_words`, `aliases`.`word_aliases` FROM `translation_word` AS `tw`"
                + " LEFT JOIN ("
                + "    SELECT `translation_word_id`, GROUP_CONCAT(`slug`, ';') AS `related_words`"
                + "    FROM `translation_word_related` GROUP BY `translation_word_id`"
                + " ) AS `related` ON `related`.`translation_word_id`=`tw`.`id`"
                + " LEFT JOIN ("
                + "    SELECT `translation_word_id`, GROUP_CONCAT(`term`, ';') AS `word_aliases`"
                + "    FROM `translation_word_alias` GROUP BY `translation_word_id`"
                + " ) AS `aliases` ON `aliases`.`translation_word_id`=`tw`.`id`"
                + " LEFT JOIN `resource__translation_word` AS `rtw` ON `rtw`.`translation_word_id`=`tw`.`id`"
                + " WHERE `tw`.`slug`=? AND `rtw`.`resource_id`=" + resourceId, new String[]{slug});
        TranslationWord word = null;
        if(cursor.moveToFirst()) {
            long wordId = cursor.getLong(0);
            String term = cursor.getString(1);
            String definition = cursor.getString(2);
            String definitionTitle = cursor.getString(3);

            String rawRelated = cursor.getString(4);
            String[] relatedWords = new String[0];
            if(rawRelated != null) {
                relatedWords = rawRelated.split(";");
            }

            String rawAliases = cursor.getString(5);
            String[] wordAliases = new String[0];
            if(rawAliases != null) {
                wordAliases = rawAliases.split(";");
            }
            cursor.close();

            // retrieve examples
            Cursor examplesCursor = this.database.rawQuery("SELECT `chapter_slug`, `frame_slug`, `body` FROM `translation_word_example`"
                    + " WHERE `translation_word_id`=" + wordId, null);
            examplesCursor.moveToFirst();
            List<TranslationWord.Example> examples = new ArrayList<>();
            while(!examplesCursor.isAfterLast()) {
                examples.add(new TranslationWord.Example(examplesCursor.getString(0), examplesCursor.getString(1), examplesCursor.getString(2)));
                examplesCursor.moveToNext();
            }
            examplesCursor.close();
            word = new TranslationWord(slug, term, definition, definitionTitle, relatedWords,  wordAliases,  examples.toArray(new TranslationWord.Example[examples.size()]));
        }
        cursor.close();
        return word;
    }

    /**
     * Returns a translation academy item
     * @param sourceTranslation
     * @param volume
     * @param manual
     * @param translationArticleSlug  @return
     */
    @Nullable
    public TranslationArticle getTranslationArticle(SourceTranslation sourceTranslation, String volume, String manual, String translationArticleSlug) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);

        if(resourceId > 0) {
            return getTranslationArticle(resourceId, volume, manual, translationArticleSlug);
        } else {
            return null;
        }
    }

    /**
     * Returns a translation article
     * @param resourceId
     * @param volume
     * @param manual
     * @param referenceSlug
     * @return
     */
    private TranslationArticle getTranslationArticle(long resourceId, String volume, String manual, String referenceSlug) {
        Cursor cursor = this.database.rawQuery("SELECT `taa`.`id`, `taa`.`slug`, `taa`.`translation_academy_manual_id`, `taa`.`title`, `taa`.`text`, `taa`.`reference` FROM `translation_academy_article` AS `taa`"
                + " LEFT JOIN `translation_academy_manual` AS `tam` ON `tam`.`id`=`taa`.`translation_academy_manual_id`"
                + " LEFT JOIN `translation_academy_volume` AS `tav` ON `tav`.`id`=`tam`.`translation_academy_volume_id`"
                + " LEFT JOIN `resource__translation_academy_volume` AS `rtav` ON `rtav`.`translation_academy_volume_id`=`tav`.`id`"
                + " WHERE `taa`.`reference` LIKE ? AND `tam`.`slug`=? AND `tav`.`slug`=? AND `rtav`.`resource_id`=" + resourceId, new String[]{"%/" + referenceSlug, manual, volume});
        TranslationArticle article = null;
        if(cursor.moveToFirst()) {
            long articleId = cursor.getLong(0);
            String slug = cursor.getString(1);
            long manualDBId = cursor.getLong(2);
            String title = cursor.getString(3);
            String text = cursor.getString(4);
            String reference = cursor.getString(5);

            article = new TranslationArticle(volume, manual, slug, title, text, reference);
            article.setDBId(articleId);
        }
        cursor.close();
        return article;
    }

    /**
     * Returns an array of checkingQuestions
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public CheckingQuestion[] getCheckingQuestions(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        List<CheckingQuestion> questions = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `slug`, `question`, `answer` FROM `checking_question`"
                + " WHERE `id` IN ("
                + "   SELECT `checking_question_id` FROM `frame__checking_question`"
                + "   WHERE `project_slug`=? AND `source_language_slug`=? AND `resource_slug`=? AND `chapter_slug`=? AND `frame_slug`=?"
                + ")", new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            String questionSlug = cursor.getString(0);
            String question = cursor.getString(1);
            String answer = cursor.getString(2);

            // NOTE: we purposely do not retrieve references in the above query for better performance
            questions.add(new CheckingQuestion(questionSlug, chapterSlug, frameSlug, question, answer, new CheckingQuestion.Reference[0]));
            cursor.moveToNext();
        }
        cursor.close();
        return questions.toArray(new CheckingQuestion[questions.size()]);
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
        Project project = null;
        Cursor cursor = this.database.rawQuery("SELECT `p`.`sort`,"
                + " `p`.`modified_at`,"
                + " `p`.`source_language_catalog_url`,"
                + " COALESCE(`sl1`.`slug`, `sl2`.`slug`, `sl3`.`slug`),"
                + " COALESCE(`sl1`.`project_name`, `sl2`.`project_name`, `sl3`.`project_name`),"
                + " COALESCE(`sl1`.`project_description`, `sl2`.`project_description`, `sl3`.`project_description`),"
                + " `p`.`source_language_catalog_local_modified_at`,"
                + " `p`.`source_language_catalog_server_modified_at`,"
                + " `p`.`chunk_marker_catalog_url`,"
                + " `p`.`chunk_marker_catalog_local_modified_at`,"
                + " `p`.`chunk_marker_catalog_server_modified_at`"
                + " FROM `project` AS `p`"
                + " LEFT JOIN `source_language` AS `sl1` ON `sl1`.`project_id`=`p`.`id`AND `sl1`.`slug`=?"
                + " LEFT JOIN `source_language` AS `sl2` ON `sl2`.`project_id`=`p`.`id` AND `sl2`.`slug`='en'"
                + " LEFT JOIN `source_language` AS `sl3` ON `sl3`.`project_id`=`p`.`id`"
                + " WHERE `p`.`slug`=?"
                + " GROUP BY `p`.`id`", new String[]{sourceLanguageSlug, projectSlug});
        if(cursor.moveToFirst()) {
            int sort = cursor.getInt(0);
            int dateModified = cursor.getInt(1);
            String sourceLanguageCatalog = cursor.getString(2);
            String actualSourceLanguageSlug = cursor.getString(3);
            String projectName = cursor.getString(4);
            String projectDescription = cursor.getString(5);
            int sourceLanguageCatalogLocalModified = cursor.getInt(6);
            int sourceLanguageCatalogServerModified = cursor.getInt(7);
            String chunkMarkerCatalog = cursor.getString(8);
            int chunkMarkerCatalogLocalModified = cursor.getInt(9);
            int chunkMarkerCatalogServerModified = cursor.getInt(10);
            project = new Project(projectSlug, actualSourceLanguageSlug, projectName,
                    projectDescription, dateModified, sort, sourceLanguageCatalog,
                    sourceLanguageCatalogLocalModified, sourceLanguageCatalogServerModified,
                    chunkMarkerCatalog, chunkMarkerCatalogLocalModified, chunkMarkerCatalogServerModified);
        }
        cursor.close();
        return project;
    }

    /**
     * Returns a source language
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public synchronized SourceLanguage getSourceLanguage(String projectSlug, String sourceLanguageSlug) {
        SourceLanguage sourceLanguage = null;
        Cursor cursor = this.database.rawQuery("SELECT `sl`.`name`, `sl`.`project_name`, `sl`.`project_description`, `sl`.`direction`, `sl`.`modified_at`, `sl`.`resource_catalog_url`, `sl`.`resource_catalog_local_modified_at`, `sl`.`resource_catalog_server_modified_at` FROM `source_language` AS `sl`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id` = `sl`.`project_id`"
                + " WHERE `p`.`slug`=? AND `sl`.`slug`=?", new String[]{projectSlug, sourceLanguageSlug});

        if(cursor.moveToFirst()) {
            String sourceLanguageName = cursor.getString(0);
            String projectName = cursor.getString(1);
            String projectDescription = cursor.getString(2);
            String rawDirection = cursor.getString(3);
            int dateModified = cursor.getInt(4);
            String resourceCatalog = cursor.getString(5);
            int catalogLocalModified = cursor.getInt(6);
            int catalogServerModified = cursor.getInt(7);
            LanguageDirection direction = LanguageDirection.get(rawDirection);
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
            sourceLanguage = new SourceLanguage(sourceLanguageSlug, sourceLanguageName, dateModified, direction, projectName, projectDescription, resourceCatalog, catalogLocalModified, catalogServerModified);
        }
        cursor.close();
        return sourceLanguage;
    }

    /**
     * Returns an array of source languages in the project
     * @param projectSlug
     * @return
     */
    public synchronized SourceLanguage[] getSourceLanguages(String projectSlug) {
        List<SourceLanguage> sourceLanguages = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `sl`.`slug`, `sl`.`name`, `sl`.`project_name`, `sl`.`project_description`, `sl`.`direction`, `sl`.`modified_at`, `sl`.`resource_catalog_url`, `sl`.`resource_catalog_local_modified_at`, `sl`.`resource_catalog_server_modified_at` FROM `source_language` AS `sl`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id` = `sl`.`project_id`"
                + " WHERE `p`.`slug`=?"
                + " ORDER BY `sl`.`slug`, `sl`.`name`", new String[]{projectSlug});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            String sourceLanguageSlug = cursor.getString(0);
            String sourceLanguageName = cursor.getString(1);
            String projectName = cursor.getString(2);
            String projectDescription = cursor.getString(3);
            String rawDirection = cursor.getString(4);
            int dateModified = cursor.getInt(5);
            String resourceCatalog = cursor.getString(6);
            int catalogLocalModified = cursor.getInt(7);
            int catalogServerModified = cursor.getInt(8);
            LanguageDirection direction = LanguageDirection.get(rawDirection);
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
            sourceLanguages.add(new SourceLanguage(sourceLanguageSlug, sourceLanguageName, dateModified, direction, projectName, projectDescription, resourceCatalog, catalogLocalModified, catalogServerModified));
            cursor.moveToNext();
        }
        cursor.close();
        return sourceLanguages.toArray(new SourceLanguage[sourceLanguages.size()]);
    }

    /**
     * Returns a source translation
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @return
     */
    public SourceTranslation getSourceTranslation(String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        SourceTranslation sourceTranslation = null;
        Cursor cursor = this.database.rawQuery("SELECT `sl`.`project_name`, `sl`.`name`, `r`.`name`, `r`.`checking_level`, `r`.`modified_at`, `r`.`version`"
                + " FROM `resource` AS `r`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id` = `sl`.`project_id`"
                + " WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=?", new String[]{projectSlug, sourceLanguageSlug, resourceSlug});
        if(cursor.moveToFirst()) {
            String projectName = cursor.getString(0);
            String sourceLanguageName = cursor.getString(1);
            String resourceName = cursor.getString(2);
            int checkingLevel = cursor.getInt(3);
            int dateModified = cursor.getInt(4);
            String version = cursor.getString(5);
            TranslationFormat format = getSourceTranslationFormat(projectSlug, sourceLanguageSlug, resourceSlug);
            sourceTranslation = new SourceTranslation(projectSlug, sourceLanguageSlug, resourceSlug, projectName, sourceLanguageName, resourceName, checkingLevel, dateModified, version, format);
        }
        cursor.close();
        return sourceTranslation;
    }

    /**
     * Returns the format of the source translation
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @return
     */
    private TranslationFormat getSourceTranslationFormat(String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        Cursor cursor = this.database.rawQuery("SELECT `f`.`format` FROM `frame` AS `f`"
                + " WHERE `f`.`chapter_id` IN ("
                + "   SELECT `c`.`id` FROM `chapter` AS `c`"
                + "   LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + "   LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=?"
                + " ) AND `f`.`format` IS NOT NULL LIMIT 1", new String[]{projectSlug, sourceLanguageSlug, resourceSlug});
        TranslationFormat format = TranslationFormat.DEFAULT;
        if(cursor.moveToFirst()) {
            format = TranslationFormat.get(cursor.getString(0));
            if(format == null) {
                format = TranslationFormat.DEFAULT;
            }
        }
        cursor.close();
        return format;
    }

    /**
     * Returns an array of source translations that have updates available on the server.
     * @return
     */
    public SourceTranslation[] getSourceTranslationsWithUpdates() {
        Cursor cursor = this.database.rawQuery("SELECT `p`.`slug` AS `project_slug`, `sl`.`slug` AS `source_language_slug`, `sl`.`project_name`, `sl`.`name`, `r`.`slug` AS `resource_slug`, `r`.`name`, `r`.`checking_level`, `r`.`modified_at`, `r`.`version` FROM `resource` AS `r`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id` = `sl`.`project_id`"
                + " LEFT JOIN ("
                + "   SELECT `r`.`id` AS `resource_id`,  COUNT(*)  AS `count` FROM `chapter` AS `c`"
                + "   LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + "   GROUP BY `r`.`id`"
                + " ) AS `content` ON `content`.`resource_id`=`r`.`id`"
                + " WHERE `content`.`count` > 0 AND ("
                + "   `r`.`source_catalog_server_modified_at`>`r`.`source_catalog_local_modified_at`"
                + "   OR `r`.`translation_notes_catalog_server_modified_at`>`r`.`translation_notes_catalog_local_modified_at`"
                + "   OR `r`.`translation_words_catalog_server_modified_at`>`r`.`translation_words_catalog_local_modified_at`"
                + "   OR `r`.`translation_word_assignments_catalog_server_modified_at`>`r`.`translation_word_assignments_catalog_local_modified_at`"
                + "   OR `r`.`checking_questions_catalog_server_modified_at`>`r`.`checking_questions_catalog_local_modified_at`"
                + " )", null);
        List<SourceTranslation> sourceTranslations = new ArrayList<>();
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            String projectSlug = cursor.getString(0);
            String sourceLanguageSlug = cursor.getString(1);
            String projectName = cursor.getString(2);
            String sourceLanguageName = cursor.getString(3);
            String resourceSlug = cursor.getString(4);
            String resourceName = cursor.getString(5);
            int checkingLevel = cursor.getInt(3);
            int dateModified = cursor.getInt(4);
            String version = cursor.getString(5);
            TranslationFormat format = getSourceTranslationFormat(projectSlug, sourceLanguageSlug, resourceSlug);
            sourceTranslations.add(new SourceTranslation(projectSlug, sourceLanguageSlug, resourceSlug, projectName, sourceLanguageName, resourceName, checkingLevel, dateModified, version, format));
            cursor.moveToNext();
        }
        cursor.close();
        return sourceTranslations.toArray(new SourceTranslation[sourceTranslations.size()]);
    }

    /**
     * Returns a resource
     * @param translation
     * @return
     */
    public synchronized Resource getResource(SourceTranslation translation) {
        Resource resource = null;
        Cursor cursor = this.database.rawQuery("SELECT `r`.`name`, `r`.`checking_level`, `r`.`version`, `r`.`modified_at`,"
                + " `r`.`source_catalog_url`, `r`.`source_catalog_local_modified_at`, `r`.`source_catalog_server_modified_at`,"
                + " `r`.`translation_notes_catalog_url`, `r`.`translation_notes_catalog_local_modified_at`, `r`.`translation_notes_catalog_server_modified_at`,"
                + " `r`.`translation_words_catalog_url`, `r`.`translation_words_catalog_local_modified_at`, `r`.`translation_words_catalog_server_modified_at`,"
                + " `r`.`translation_word_assignments_catalog_url`, `r`.`translation_word_assignments_catalog_local_modified_at`, `r`.`translation_word_assignments_catalog_server_modified_at`,"
                + " `r`.`checking_questions_catalog_url`, `r`.`checking_questions_catalog_local_modified_at`, `r`.`checking_questions_catalog_server_modified_at`,"
                + " `r`.`id`, CASE WHEN `content`.`count` > 0 THEN 1 ELSE 0 END AS `is_downloaded`,"
                + " `r`.`source_language_id`"
                + " FROM `resource` AS `r`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id` = `sl`.`project_id`"
                + " LEFT JOIN ("
                + "   SELECT `r`.`id` AS `resource_id`, COUNT(*) AS `count` FROM `chapter` AS `c`"
                + "   LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + "   WHERE `r`.`slug`=?"
                + "   GROUP BY `r`.`id`"
                + " ) AS `content` ON `content`.`resource_id`=`r`.`id`"
                + " WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=?", new String[]{translation.resourceSlug, translation.projectSlug, translation.sourceLanguageSlug, translation.resourceSlug});

        if(cursor.moveToFirst()) {
            String resourceName = cursor.getString(0);
            int checkingLevel = cursor.getInt(1);
            String version = cursor.getString(2);

            int dateModified = cursor.getInt(3);

            String sourceCatalog = cursor.getString(4);
            int sourceCatalogModified = cursor.getInt(5);
            int sourceCatalogServerModified = cursor.getInt(6);

            String notesCatalog = cursor.getString(7);
            int notesCatalogModified = cursor.getInt(8);
            int notesCatalogServerModified = cursor.getInt(9);

            String termsCatalog = cursor.getString(10);
            int termsCatalogModified = cursor.getInt(11);
            int termsCatalogServerModified = cursor.getInt(12);

            String termAssignmentsCatalog = cursor.getString(13);
            int termAssignmentsCatalogModified = cursor.getInt(14);
            int termAssignmentsCatalogServerModified = cursor.getInt(15);

            String questionsCatalog = cursor.getString(16);
            int questionsCatalogModified = cursor.getInt(17);
            int questionsCatalogServerModified = cursor.getInt(18);

            long resourceId = cursor.getLong(19);

            boolean isDownloaded = cursor.getInt(20) > 0;

            long sourceLanguageDBId = cursor.getLong(21);

            resource = new Resource(resourceName, translation.resourceSlug, checkingLevel, version, isDownloaded, dateModified,
                    sourceCatalog, sourceCatalogModified, sourceCatalogServerModified,
                    notesCatalog, notesCatalogModified, notesCatalogServerModified,
                    termsCatalog, termsCatalogModified, termsCatalogServerModified,
                    termAssignmentsCatalog, termAssignmentsCatalogModified, termAssignmentsCatalogServerModified,
                    questionsCatalog, questionsCatalogModified, questionsCatalogServerModified);
            resource.setDBId(resourceId);
            resource.setSourceLanguageDBId(sourceLanguageDBId);
        }
        cursor.close();
        return resource;
    }

    /**
     * Returns an array of resources
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public synchronized Resource[] getResources(String projectSlug, String sourceLanguageSlug) {
        List<Resource> resources = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `r`.`name`, `r`.`checking_level`, `r`.`version`, `r`.`modified_at`,"
                + " `r`.`source_catalog_url`, `r`.`source_catalog_local_modified_at`, `r`.`source_catalog_server_modified_at`,"
                + " `r`.`translation_notes_catalog_url`, `r`.`translation_notes_catalog_local_modified_at`, `r`.`translation_notes_catalog_server_modified_at`,"
                + " `r`.`translation_words_catalog_url`, `r`.`translation_words_catalog_local_modified_at`, `r`.`translation_words_catalog_server_modified_at`,"
                + " `r`.`translation_word_assignments_catalog_url`, `r`.`translation_word_assignments_catalog_local_modified_at`, `r`.`translation_word_assignments_catalog_server_modified_at`,"
                + " `r`.`checking_questions_catalog_url`, `r`.`checking_questions_catalog_local_modified_at`, `r`.`checking_questions_catalog_server_modified_at`,"
                + " `r`.`id`, CASE WHEN `content`.`count` > 0 THEN 1 ELSE 0 END AS `is_downloaded`, `r`.`slug`,"
                + " `r`.`source_language_id`"
                + " FROM `resource` AS `r`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id` = `sl`.`project_id`"
                + " LEFT JOIN ("
                + "   SELECT `r`.`id` AS `resource_id`, COUNT(*) AS `count` FROM `chapter` AS `c`"
                + "   LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + "   GROUP BY `r`.`id`"
                + " ) AS `content` ON `content`.`resource_id`=`r`.`id`"
                + " WHERE `p`.`slug`=? AND `sl`.`slug`=?", new String[]{projectSlug, sourceLanguageSlug});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            String resourceName = cursor.getString(0);
            int checkingLevel = cursor.getInt(1);
            String version = cursor.getString(2);

            int dateModified = cursor.getInt(3);

            String sourceCatalog = cursor.getString(4);
            int sourceCatalogModified = cursor.getInt(5);
            int sourceCatalogServerModified = cursor.getInt(6);

            String notesCatalog = cursor.getString(7);
            int notesCatalogModified = cursor.getInt(8);
            int notesCatalogServerModified = cursor.getInt(9);

            String termsCatalog = cursor.getString(10);
            int termsCatalogModified = cursor.getInt(11);
            int termsCatalogServerModified = cursor.getInt(12);

            String termAssignmentsCatalog = cursor.getString(13);
            int termAssignmentsCatalogModified = cursor.getInt(14);
            int termAssignmentsCatalogServerModified = cursor.getInt(15);

            String questionsCatalog = cursor.getString(16);
            int questionsCatalogModified = cursor.getInt(17);
            int questionsCatalogServerModified = cursor.getInt(18);

            long resourceId = cursor.getLong(19);

            boolean isDownloaded = cursor.getInt(20) > 0;

            String resourceSlug = cursor.getString(21);

            long sourceLanguageDBId = cursor.getLong(22);

            Resource resource = new Resource(resourceName, resourceSlug, checkingLevel, version, isDownloaded, dateModified,
                    sourceCatalog, sourceCatalogModified, sourceCatalogServerModified,
                    notesCatalog, notesCatalogModified, notesCatalogServerModified,
                    termsCatalog, termsCatalogModified, termsCatalogServerModified,
                    termAssignmentsCatalog, termAssignmentsCatalogModified, termAssignmentsCatalogServerModified,
                    questionsCatalog, questionsCatalogModified, questionsCatalogServerModified);
            resource.setDBId(resourceId);
            resource.setSourceLanguageDBId(sourceLanguageDBId);
            resources.add(resource);
            cursor.moveToNext();
        }
        cursor.close();
        return resources.toArray(new Resource[resources.size()]);
    }

    /**
     * Returns a chapter
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    @Nullable
    public Chapter getChapter(SourceTranslation sourceTranslation, String chapterSlug) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);
        if(resourceId > 0) {
            return getChapter(chapterSlug, resourceId);
        }
        return null;
    }

    /**
     * Returns a chapter
     * @param slug
     * @param resourceId
     * @return
     */
    private Chapter getChapter(String slug, long resourceId) {
        Cursor cursor = this.database.rawQuery("SELECT `title`, `reference`, `slug` FROM `chapter` WHERE `slug`=? AND `resource_id`=" + resourceId, new String[]{slug});
        Chapter chapter = null;
        if(cursor.moveToFirst()) {
            chapter = new Chapter(cursor.getString(0), cursor.getString(1), cursor.getString(2));
        }
        cursor.close();
        return chapter;
    }

    /**
     * Returns a frame
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    @Nullable
    public Frame getFrame(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);
        long chapterId = getChapterDBId(chapterSlug, resourceId);
        if(chapterId > 0) {
            return getFrame(frameSlug, chapterId);
        }
        return null;
    }

    /**
     * Returns a frame
     * @param slug
     * @param chapterId
     * @return
     */
    private Frame getFrame(String slug, long chapterId) {
        Cursor cursor = this.database.rawQuery("SELECT `f`.`id`, `f`.`slug`, `c`.`slug`, `f`.`body`, `f`.`format`, `f`.`image_url` FROM `frame` AS `f`"
                + " LEFT JOIN `chapter` AS `c` ON `c`.`id`=`f`.`chapter_id`"
                + " WHERE `f`.`slug`=? AND `f`.`chapter_id`=" + chapterId, new String[]{slug});
        Frame frame = null;
        if(cursor.moveToFirst()) {
            frame = new Frame(cursor.getString(1), cursor.getString(2), cursor.getString(3), TranslationFormat.get(cursor.getString(4)), cursor.getString(5));
            frame.setDBId(cursor.getLong(0));
        }
        cursor.close();
        return frame;
    }


    /**
     * Returns the database id for the chapter
     * @param slug
     * @param resourceId
     * @return
     */
    private long getChapterDBId(String slug, long resourceId) {
        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `chapter` WHERE `slug`=? AND `resource_id`=" + resourceId, new String[]{slug});
        long chapterId = 0;
        if(cursor.moveToFirst()) {
            chapterId = cursor.getLong(0);
        }
        cursor.close();
        return chapterId;
    }

    /**
     * Returns the database id for the frame
     * @param slug
     * @param chapterId
     * @return
     */
    private long getFrameDBId(String slug, long chapterId) {
        Cursor cursor = this.database.rawQuery("SELECT `id` FROM `frame` WHERE `slug`=? AND `chapter_id`=" + chapterId, new String[]{slug});
        long frameId = 0;
        if(cursor.moveToFirst()) {
            frameId = cursor.getLong(0);
        }
        cursor.close();
        return frameId;
    }

    /**
     * Returns the json object for a single checkingQuestion
     * @param sourceTranslation
     * @param chapterSlug
     * @param questionSlug
     * @return
     */
    @Nullable
    public CheckingQuestion getCheckingQuestion(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug, String questionSlug) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);
        long chapterId = getChapterDBId(chapterSlug, resourceId);

        if(chapterId > 0) {
            return getCheckingQuestion(chapterId, frameSlug, questionSlug);
        }
        return null;
    }

    private CheckingQuestion getCheckingQuestion(long chapterId, String frameSlug, String questionSlug) {
        CheckingQuestion question = null;
        Cursor cursor = this.database.rawQuery("SELECT `c`.`slug`, `cq`.`question`, `cq`.`answer`, `ref`.`references` FROM `checking_question` AS `cq`"
                + " LEFT JOIN ("
                + "   SELECT `checking_question_id`, GROUP_CONCAT(`chapter_slug` || '-' || `frame_slug`, ',') AS `references` FROM `frame__checking_question`"
                + "   GROUP BY `checking_question_id`"
                + " ) AS `ref` ON `ref`.`checking_question_id`=`cq`.`id`"
                + " LEFT JOIN `frame__checking_question` AS `fcq` ON `fcq`.`checking_question_id`=`cq`.`id`"
                + " LEFT JOIN `frame` AS `f` ON `f`.`id`=`fcq`.`frame_id`"
                + " LEFT JOIN `chapter` AS `c` ON `c`.`id`=`f`.`chapter_id`"
                + " WHERE `f`.`slug`=? AND `cq`.`slug`=? AND `c`.`id`=" + chapterId, new String[]{frameSlug, questionSlug});
        if(cursor.moveToFirst()) {
            String chapterSlug = cursor.getString(0);
            String questionText = cursor.getString(1);
            String answer = cursor.getString(2);

            String[] referenceStrings = cursor.getString(3).split(",");
            List<CheckingQuestion.Reference> references = new ArrayList<>();
            for(String reference:referenceStrings) {
                try {
                    references.add(CheckingQuestion.Reference.generate(reference));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            question = new CheckingQuestion(questionSlug, chapterSlug, frameSlug, questionText, answer, references.toArray(new CheckingQuestion.Reference[references.size()]));
        }
        cursor.close();
        return question;
    }

    /**
     * Returns a translation note
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @param noteSlug
     * @return
     */
    @Nullable
    public TranslationNote getNote(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug, String noteSlug) {
        long projectId = getProjectDBId(sourceTranslation.projectSlug);
        long sourceLanguageId = getSourceLanguageDBId(sourceTranslation.sourceLanguageSlug, projectId);
        long resourceId = getResourceDBId(sourceTranslation.resourceSlug, sourceLanguageId);
        long chapterId = getChapterDBId(chapterSlug, resourceId);
        long frameId = getFrameDBId(frameSlug, chapterId);
        if(frameId > 0) {
            return getTranslationNote(noteSlug, frameId);
        }
        return null;
    }

    /**
     * Returns a translation note
     * @param slug
     * @param frameId
     * @return
     */
    private TranslationNote getTranslationNote(String slug, long frameId) {
        Cursor cursor = this.database.rawQuery("SELECT `c`.`slug`, `f`.`slug`, `tn`.`id`, `tn`.`title`, `tn`.`body` FROM `translation_note` AS `tn`"
                + " LEFT JOIN `frame` AS `f` ON `f`.`id`=`tn`.`frame_id`"
                + " LEFT JOIN `chapter` AS `c` ON `c`.`id`=`f`.`chapter_id`"
                + " WHERE `tn`.`slug`=? AND `tn`.`frame_id`=" + frameId, new String[]{slug});
        TranslationNote note = null;
        if(cursor.moveToFirst()) {
            note = new TranslationNote(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
        }
        cursor.close();
        return note;
    }

    /**
     * Returns the body of the chapter
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public String getChapterBody(SourceTranslation sourceTranslation, String chapterSlug) {
        Cursor cursor = this.database.rawQuery("SELECT GROUP_CONCAT(`f`.`body`, ' ') AS `body` FROM `frame` AS `f`"
                + " LEFT JOIN `chapter` AS `c` ON `c`.`id`=`f`.`chapter_id`"
                + " LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + " WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=? AND `c`.`slug`=? ORDER BY `c`.`sort`, `f`.`sort` ASC",
                new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug});
        String body = "";
        if(cursor.moveToFirst()) {
            body = cursor.getString(0);
            if(body == null) {
                body = "";
            }
        }
        cursor.close();
        return body;
    }

    /**
     * Returns the format of the chapter body
     * @param sourceTranslation
     * @param chapterSlug
     * @return
     */
    public TranslationFormat getChapterBodyFormat(SourceTranslation sourceTranslation, String chapterSlug) {
        Cursor cursor = this.database.rawQuery("SELECT `f`.`format` FROM `frame` AS `f`"
                + " WHERE `f`.`chapter_id` IN ("
                + "   SELECT `c`.`id` FROM `chapter` AS `c`"
                + "   LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + "   LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=? AND `c`.`slug`=?"
                + " ) AND `f`.`format` IS NOT NULL LIMIT 1",
                new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug});
        TranslationFormat format = TranslationFormat.DEFAULT;
        if(cursor.moveToFirst()) {
            format = TranslationFormat.get(cursor.getString(0));
            if(format == null) {
                format = TranslationFormat.DEFAULT;
            }
        }
        cursor.close();
        return format;
    }

    /**
     * Returns the translation notes in a frame
     * @param sourceTranslation
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public TranslationNote[] getTranslationNotes(SourceTranslation sourceTranslation, String chapterSlug, String frameSlug) {
        List<TranslationNote> notes = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `slug`, `title`, `body` FROM `translation_note`"
                        + " WHERE `project_slug`=? AND `source_language_slug`=? AND `resource_slug`=? AND `chapter_slug`=? AND `frame_slug`=?"
                , new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, chapterSlug, frameSlug});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            String noteSlug = cursor.getString(0);
            String title = cursor.getString(1);
            String body = cursor.getString(2);

            notes.add(new TranslationNote(chapterSlug, frameSlug, noteSlug, title, body));
            cursor.moveToNext();
        }
        cursor.close();
        return notes.toArray(new TranslationNote[notes.size()]);
    }

    /**
     * Returns the number of translatable items in the source translation.
     * This counts the frames, chapter titles, and chapter references.
     * Empty items will not be counted.
     * @param sourceTranslation
     * @return
     */
    public int numTranslatable(SourceTranslation sourceTranslation) {
        Cursor cursor = this.database.rawQuery("SELECT SUM(`count`) FROM ("
                + "   SELECT COUNT(*) AS `count` FROM `frame` AS `f`"
                + "   WHERE `f`.`chapter_id` IN ("
                + "     SELECT `c`.`id` FROM `chapter` AS `c`"
                + "     LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + "     LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + "     LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + "     WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=?"
                + "   ) AND `f`.`body` IS NOT NULL AND TRIM(`f`.`body`, ' ') <> ''"
                + "   UNION"
                + "   SELECT SUM(CASE WHEN `c`.`reference`<>'' THEN 1 ELSE 0 END)"
                + "   + SUM(CASE WHEN `c`.`title`<>'' THEN 1 ELSE 0 END) AS `count` FROM `chapter` AS `c`"
                + "   WHERE `c`.`resource_id` IN ("
                + "     SELECT `r`.`id` FROM `resource` AS `r`"
                + "     LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + "     LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + "     WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=?"
                + "   )"
                + " )", new String[]{sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug, sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug, sourceTranslation.resourceSlug});
        int count = 0;
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * Returns an array of projects
     * The source language will default to english then the first available language
     * @param sourceLanguageSlug the preferred language for project titles and descriptions
     * @return
     */
    public Project[] getProjects(String sourceLanguageSlug) {
        Cursor cursor = this.database.rawQuery("SELECT `p`.`slug`, `p`.`sort`, `p`.`modified_at`, `p`.`source_language_catalog_url`,"
                + " COALESCE(`sl1`.`slug`, `sl2`.`slug`, `sl3`.`slug`),"
                + " COALESCE(`sl1`.`project_name`, `sl2`.`project_name`, `sl3`.`project_name`),"
                + " COALESCE(`sl1`.`project_description`, `sl2`.`project_description`, `sl3`.`project_description`),"
                + " `p`.`source_language_catalog_local_modified_at`, `p`.`source_language_catalog_server_modified_at`,"
                + " `p`.`chunk_marker_catalog_url`,"
                + " `p`.`chunk_marker_catalog_local_modified_at`, `p`.`chunk_marker_catalog_server_modified_at`"
                + " FROM `project` AS `p`"
                + " LEFT JOIN `source_language` AS `sl1` ON `sl1`.`project_id`=`p`.`id`AND `sl1`.`slug`=?"
                + " LEFT JOIN `source_language` AS `sl2` ON `sl2`.`project_id`=`p`.`id` AND `sl2`.`slug`='en'"
                + " LEFT JOIN `source_language` AS `sl3` ON `sl3`.`project_id`=`p`.`id`"
                + " GROUP BY `p`.`id`"
                + " ORDER BY `p`.`sort` ASC", new String[]{sourceLanguageSlug});
        cursor.moveToFirst();
        List<Project> projects = new ArrayList<>();
        while(!cursor.isAfterLast()) {
            String projectSlug = cursor.getString(0);
            int sort = cursor.getInt(1);
            int dateModified = cursor.getInt(2);
            String sourceLanguageCatalog = cursor.getString(3);
            String actualSsourceLanguageSlug = cursor.getString(4);
            String projectName = cursor.getString(5);
            String projectDescription = cursor.getString(6);
            int sourceLanguageCatalogLocalModified = cursor.getInt(7);
            int sourceLanguageCatalogServerModified = cursor.getInt(8);
            String chunkMarkerCatalog = cursor.getString(9);
            int chunkMarkerCatalogLocalModified = cursor.getInt(10);
            int chunkMarkerCatalogServerModified = cursor.getInt(11);
            projects.add(new Project(projectSlug, actualSsourceLanguageSlug, projectName,
                    projectDescription, dateModified, sort, sourceLanguageCatalog,
                    sourceLanguageCatalogLocalModified, sourceLanguageCatalogServerModified,
                    chunkMarkerCatalog, chunkMarkerCatalogLocalModified, chunkMarkerCatalogServerModified));
            cursor.moveToNext();
        }
        cursor.close();
        return projects.toArray(new Project[projects.size()]);
    }

    /**
     * Marks the source language catalog in the project as up to date
     * @param projectSlug
     */
    public void markSourceLanguageCatalogUpToDate(String projectSlug) {
        this.database.execSQL("UPDATE `project` SET"
                + " `source_language_catalog_local_modified_at`=`source_language_catalog_server_modified_at`"
                + " WHERE `slug`=?", new String[]{projectSlug});
    }


    /**
     * Marks the resource catalog in source language has up to date
     * @param projectSlug
     * @param sourceLanguageSlug
     */
    public void markResourceCatalogUpToDate(String projectSlug, String sourceLanguageSlug) {
        this.database.execSQL("UPDATE `source_language`"
                + " SET `resource_catalog_local_modified_at`=`resource_catalog_server_modified_at`"
                + " WHERE `project_id` IN ("
                + "   SELECT `id` FROM `project` WHERE `slug`=?"
                + " ) AND `slug`=?", new String[]{projectSlug, sourceLanguageSlug});
    }

    public void setExpired() {
        database.execSQL("UPDATE `resource`"
                        + " SET `source_catalog_local_modified_at`=0,"
                        + " `translation_notes_catalog_local_modified_at`=0,"
                        + " `translation_words_catalog_local_modified_at`=0,"
                        + " `translation_word_assignments_catalog_local_modified_at`=0,"
                        + " `checking_questions_catalog_local_modified_at`=0"
        );
    }

    /**
     * This is a temporary method for injecting the chunk marker urls into the project table
     * because chunk markers are not currently available in the api
     * @return
     * @deprecated you probably shouldn't use this method
     */
    public boolean manuallyInjectChunkMarkerUrls() {
        this.database.execSQL("UPDATE `project` SET" +
            "`chunk_marker_catalog_url` = 'https://api.unfoldingword.org/bible/txt/1/' || `project`.`slug` || '/chunks.json'" +
            "WHERE `project`.`slug` <> 'obs'");
        return true;
    }

    /**
     * Returns an array of chunk markers for the project
     * @param projectSlug
     * @return
     */
    public ChunkMarker[] getChunkMarkers(String projectSlug) {
        List<ChunkMarker> chunkMarkers = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `cm`.`chapter_slug`, `cm`.`first_verse_slug` FROM `chunk_marker` AS `cm`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id` = `cm`.`project_id`"
                + " WHERE `p`.`slug`=?", new String[]{projectSlug});
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            chunkMarkers.add(new ChunkMarker(cursor.getString(0), cursor.getString(1)));
            cursor.moveToNext();
        }
        cursor.close();
        return chunkMarkers.toArray(new ChunkMarker[chunkMarkers.size()]);
    }

    /**
     * Returns an array of data fields in the questionnaire
     * @param questionnaireId
     * @return
     */
    private Map<String, Long> getQuestionnaireDataFields(long questionnaireId) {
        Map<String, Long> dataFields = new HashMap<>();
        Cursor cursor = this.database.rawQuery("SELECT `field`, `question_td_id`"
                + " FROM `questionnaire_data_field`"
                + " WHERE `questionnaire_id`=" + questionnaireId, null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            dataFields.put(cursor.getString(0), cursor.getLong(1));
            cursor.moveToNext();
        }
        cursor.close();

        return dataFields;
    }
    /**
     * Returns an array of new target language questions for the questionnaire
     * @param questionnaireId
     * @return
     */
    private QuestionnaireQuestion[] getQuestionnaireQuestions(long questionnaireId) {
        List<QuestionnaireQuestion> questions = new ArrayList<>();
        Cursor cursor = this.database.rawQuery("SELECT `question_td_id`, `text`, `help`, `input_type`,"
                + " `is_required`, `depends_on`, `sort`"
                + " FROM `questionnaire_question`"
                + " WHERE `questionnaire_id`=" + questionnaireId
                + " ORDER BY `sort` ASC", null);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            questions.add(new QuestionnaireQuestion(cursor.getInt(0), cursor.getString(1),
                    cursor.getString(2), QuestionnaireQuestion.InputType.get(cursor.getString(3)),
                    cursor.getInt(4) == 1, cursor.getInt(5), cursor.getInt(6)));
            cursor.moveToNext();
        }
        cursor.close();

        return questions.toArray(new QuestionnaireQuestion[questions.size()]);
    }

    /**
     * Deletes all the questionnaires
     */
    public void deleteQuestionnaires() {
        this.database.delete("questionnaire", null, null);
    }


    /**
     * Removes a chapter.
     * This will cascade
     * @param slug
     * @param resourceId
     */
    public void deleteChapter(String slug, long resourceId) {
        this.database.delete("chapter", "`slug`=? AND `resource_id`=" + resourceId, new String[]{slug});
    }

    /**
     * Removes a frame.
     * This will cascade
     * @param slug
     * @param chapterId
     */
    public void deleteFrame(String slug, long chapterId) {
        this.database.delete("frame", "`slug`=? AND `chapter_id`=" + chapterId, new String[]{slug});
    }

    /**
     * Adds a temporary target language to the library
     * @param tempTargetLanguage
     * @return the db id of the new temporary target language
     */
    public long addTempTargetLanguage(TargetLanguage tempTargetLanguage) {
        ContentValues values = new ContentValues();
        values.put("slug", tempTargetLanguage.slug);
        values.put("name", tempTargetLanguage.name);
        values.put("direction", tempTargetLanguage.direction.getLabel());
        values.put("region", tempTargetLanguage.region);
        return this.database.insertWithOnConflict("temp_target_language", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /**
     * Adds the target langauge assignment to the temp target language
     * @param tempTargetLangaugeSlug
     * @param assignedTargetLanguageSlug
     */
    public void addTempTargetLanguageAssignment(String tempTargetLangaugeSlug, String assignedTargetLanguageSlug) {
        this.database.execSQL("UPDATE `temp_target_language` SET\n" +
                "`approved_target_language_slug`=?\n" +
                "WHERE `slug`=?", new String[]{assignedTargetLanguageSlug, tempTargetLangaugeSlug});
    }

    /**
     * Removes a temp target language from database
     * This is a utility method for unit tests
     * @param languageCode
     */
    public void deleteTempTargetLanguage(String languageCode) {
        this.database.delete("temp_target_language", "`slug`=?", new String[]{languageCode});
    }

    /**
     * Retrives a temp target language from database
     * This is a utility method for unit tests
     * @param code
     */
    public TargetLanguage getTempTargetLanguage(String code) {
        Cursor cursor = this.database.rawQuery(
//                "SELECT * FROM (" +
//                "SELECT `slug`, `name`, `direction`, `region` FROM `target_language`\n" +
//                "UNION\n" +
                "SELECT `slug`, `name`, `direction`, `region` FROM `temp_target_language` AS `ttl`\n" +
//                "WHERE `approved_target_language_slug` IS NULL)\n" +
                "WHERE `slug`=?", new String[]{code});
        TargetLanguage targetLanguage = null;
        if(cursor.moveToFirst()) {
            String name = cursor.getString(1);
            LanguageDirection direction = LanguageDirection.get(cursor.getString(2));
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
            String region = cursor.getString(3);
            targetLanguage = new TargetLanguage(code, name, region, direction);
        }
        cursor.close();
        return targetLanguage;
    }
}
