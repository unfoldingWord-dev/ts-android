package com.door43.translationstudio.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 10/1/2015.
 * TODO: these methods need to throw exeptions so we can log the error
 */
public class IndexerSQLiteHelper extends SQLiteOpenHelper{

    // TRICKY: when you bump the db version you should run the library tests to generate a new index.
    // Note that the extract test will fail.
    private static final int DATABASE_VERSION = 4;
    private final String mDatabaseName;
    private final String mSchema;

    /**
     * Creates a new sql helper for the indexer.
     * This currently expects an asset named schema.sql
     * @param context
     * @param name
     * @throws IOException
     */
    public IndexerSQLiteHelper(Context context, String name) throws IOException {
        super(context, name, null, DATABASE_VERSION);
        mSchema = Util.readStream(context.getAssets().open("schema.sql"));
        mDatabaseName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            db.execSQL("PRAGMA foreign_keys=OFF;");
        }
        String[] queries = mSchema.split(";");
        for (String query : queries) {
            query = query.trim();
            if(!query.isEmpty()) {
                try {
                    db.execSQL(query);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * TRICKY: this is only supported in API 16+
     * @param db
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(false);
        } else {
            db.execSQL("PRAGMA foreign_keys=OFF;");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 2) {
            // new tables
            db.execSQL("DROP TABLE IF EXISTS `file`");
            db.execSQL("DROP TABLE IF EXISTS `link`");
            onCreate(db);
        } else if(oldVersion < 3) {
            // add columns
            db.execSQL("ALTER TABLE `project` ADD COLUMN `source_language_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE `project` ADD COLUMN `source_language_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE `source_language` ADD COLUMN `resource_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE `source_language` ADD COLUMN `resource_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0;");
        } else {
            onCreate(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(true);
        } else {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    /**
     * Destroys the database
     */
    public void deleteDatabase(Context context) {
        context.deleteDatabase(mDatabaseName);
    }

    /**
     * Inserts or updates a project
     * @param db
     * @param slug
     * @param sort
     * @param dateModified
     * @param sourceLanguageCatalogUrl
     */
    public long addProject(SQLiteDatabase db, String slug, int sort, int dateModified, String sourceLanguageCatalogUrl, int sourceLanguageCatalogServerModifiedAt, String[] categorySlugs) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("sort", sort);
        values.put("modified_at", dateModified);
        values.put("source_language_catalog_server_modified_at", sourceLanguageCatalogServerModifiedAt);
        values.put("source_language_catalog_url", sourceLanguageCatalogUrl);

        // add project
        Cursor cursor = db.rawQuery("SELECT `id` FROM `project` WHERE `slug`=?", new String[]{slug});
        long projectId;
        if(cursor.moveToFirst()) {
            // update
            projectId = cursor.getLong(0);
            db.update("project", values, "`id`=" + projectId, null);
        } else {
            // insert
            projectId = db.insert("project", null, values);
        }
        cursor.close();

        // add categories
        db.delete("project__category", "project_id=" + projectId, null);
        addProjectCategories(db, projectId, categorySlugs);
        return projectId;
    }

    /**
     * Adds the project categories and links the project to the last category
     * @param db
     * @param projectId
     * @param categorySlugs
     * @return
     */
    private void addProjectCategories(SQLiteDatabase db, long projectId, String[] categorySlugs) {
        if(categorySlugs != null && categorySlugs.length > 0) {
            long categoryId = 0L;
            for (String catSlug : categorySlugs) {
                Cursor cursor = db.rawQuery("SELECT `id` FROM `category` WHERE `slug`=? AND `parent_id`=" + categoryId, new String[]{catSlug});
                if (cursor.moveToFirst()) {
                    // follow
                    categoryId = cursor.getLong(0);
                } else {
                    // insert
                    ContentValues values = new ContentValues();
                    values.put("slug", catSlug);
                    values.put("parent_id", categoryId);
                    categoryId = db.insert("category", null, values);
                }
                cursor.close();
            }
            ContentValues values = new ContentValues();
            values.put("project_id", projectId);
            values.put("category_id", categoryId);
            db.insert("project__category", null, values);
        }
    }

    /**
     * Removes a project.
     * This will cascade
     * @param db
     * @param slug
     */
    public void deleteProject(SQLiteDatabase db, String slug) {
        db.delete("project", "`slug`=?", new String[]{slug});
    }

    /**
     * Inserts or updates a source language
     * @param db
     * @param slug
     * @param projectId
     * @param name
     * @param projectName
     * @param projectDescription
     * @param direction
     * @param dateModified
     * @param resourceCatalogUrl
     */
    public long addSourceLanguage(SQLiteDatabase db, String slug, long projectId, String name, String projectName, String projectDescription, String direction, int dateModified, String resourceCatalogUrl, int resourceCatalogServerModifiedAt, String[] categoryNames) {
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

        Cursor cursor = db.rawQuery("SELECT `id` FROM `source_language` WHERE `slug`=? AND `project_id`=" + projectId, new String[]{slug});
        long sourceLanguageId;
        if(cursor.moveToFirst()) {
            // update
            sourceLanguageId = cursor.getLong(0);
            db.update("source_language", values, "`id`=" + sourceLanguageId, null);
        } else {
            // insert
            sourceLanguageId = db.insert("source_language", null, values);
        }
        cursor.close();

        db.delete("source_language__category", "source_language_id=" + sourceLanguageId, null);
        addSourceLanguageCategories(db, projectId, sourceLanguageId, categoryNames);
        return sourceLanguageId;
    }

    /**
     * Adds the names for categories
     * @param db
     * @param sourceLanguageId
     * @param categoryNames
     */
    public void addSourceLanguageCategories(SQLiteDatabase db, long projectId, long sourceLanguageId, String[] categoryNames) {
        if(categoryNames != null && categoryNames.length > 0) {
            Cursor cursor = db.rawQuery("SELECT `c`.`id` from `category` AS `c`"
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
                    db.insert("source_language__category", null, values);

                    // move up in categories
                    cursor = db.rawQuery("SELECT `parent_id` FROM `category` WHERE `id`=" + categoryId, null);
                    if(cursor.moveToFirst()) {
                        categoryId = cursor.getLong(0);
                        if(categoryId == 0L) {
                            // stop when we reach the top
                            break;
                        }
                    }
                }
            } else {
                cursor.close();
            }
        }
    }

    /**
     * Removes a source language.
     * This will cascade
     * @param db
     * @param sourceLanguageSlug
     * @param projectSlug
     */
    public void deleteSourceLanguage(SQLiteDatabase db, String sourceLanguageSlug, String projectSlug) {
        db.execSQL("DELETE FROM `source_language`"
                   + " WHERE `id` IN ("
                   + "  SELECT `sl`.`id` from `source_language` AS `sl`"
                   + "  LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                   + "  WHERE `sl`.`slug`=? AND `p`.`slug`=?"
                   + " )", new String[]{sourceLanguageSlug, projectSlug});
    }

    /**
     * Inserts or updates a resource
     * @param db
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
    public long addResource(SQLiteDatabase db, String slug, long sourceLanguageId, String name,
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

        Cursor cursor = db.rawQuery("SELECT `id` FROM `resource` WHERE `slug`=? AND `source_language_id`=" + sourceLanguageId, new String[]{slug});
        long resourceId;
        if(cursor.moveToFirst()) {
            // update
            resourceId = cursor.getLong(0);
            db.update("resource", values, "`id`=" + resourceId, null);
        } else {
            // insert
            resourceId = db.insert("resource", null, values);
        }
        cursor.close();
        return resourceId;
    }

    /**
     * Removes a resource.
     * This will cascade
     * @param db
     * @param resourceSlug
     * @param sourceLanguageSlug
     */
    public void deleteResource(SQLiteDatabase db, String resourceSlug, String sourceLanguageSlug, String projectSlug) {
        db.execSQL("DELETE FROM `resource` AS `r`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + " WHERE `r`.`slug`=? AND `sl`.`slug`=? AND `p`.`slug`=?", new String[]{resourceSlug, sourceLanguageSlug, projectSlug});
    }

    /**
     * Removes a resource
     * @param db
     * @param resourceId
     */
    public void deleteResource(SQLiteDatabase db, long resourceId) {
        db.delete("resource", "id=" + resourceId, null);
    }

    /**
     * Adds a resource
     * if the resource exists it will be updated
     * @param db
     * @param resource
     */
    public void addResource(SQLiteDatabase db, Resource resource, long sourceLanguageId) {
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
        db.insertWithOnConflict("resource", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Inserts or updates a chapter
     * @param db
     * @param slug
     * @param resourceId
     * @param reference
     * @param title
     * @return
     */
    public long addChapter(SQLiteDatabase db, String slug, long resourceId, String reference, String title) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("resource_id", resourceId);
        values.put("sort", Integer.parseInt(slug));
        values.put("reference", reference);
        values.put("title", title);

        Cursor cursor = db.rawQuery("SELECT `id` FROM `chapter` WHERE `slug`=? AND `resource_id`=" + resourceId, new String[]{slug});
        long chapterId;
        if(cursor.moveToFirst()) {
            // update
            chapterId = cursor.getLong(0);
            db.update("chapter", values, "`id`=" + chapterId, null);
        } else {
            // insert
            chapterId = db.insert("chapter", null, values);
        }
        cursor.close();
        return chapterId;
    }

    /**
     * Removes a chapter.
     * This will cascade
     * @param db
     * @param slug
     * @param resourceId
     */
    public void deleteChapter(SQLiteDatabase db, String slug, long resourceId) {
        db.delete("chapter", "`slug`=? AND `resource_id`=" + resourceId, new String[]{slug});
    }

    /**
     * Inserts or updates a frame
     * @param db
     * @param slug
     * @param chapterId
     * @param body
     * @param format
     * @param imageUrl
     * @return
     */
    public long addFrame(SQLiteDatabase db, String slug, long chapterId, String body, String format, String imageUrl) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("chapter_id", chapterId);
        values.put("sort", Integer.parseInt(slug));
        values.put("body", body);
        values.put("format", format);
        values.put("image_url", imageUrl);

        Cursor cursor = db.rawQuery("SELECT `id` FROM `frame` WHERE `slug`=? AND `chapter_id`=" + chapterId, new String[]{slug});
        long frameId;
        if(cursor.moveToFirst()) {
            // update
            frameId = cursor.getLong(0);
            db.update("frame", values, "`id`=" + frameId, null);
        } else {
            // insert
            frameId = db.insert("frame", null, values);
        }
        cursor.close();
        return frameId;
    }

    /**
     * Removes a frame.
     * This will cascade
     * @param db
     * @param slug
     * @param chapterId
     */
    public void deleteFrame(SQLiteDatabase db, String slug, long chapterId) {
        db.delete("frame", "`slug`=? AND `chapter_id`=" + chapterId, new String[]{slug});
    }

    /**
     * Returns the database id of a project
     * @param db
     * @param projectSlug
     * @return returns 0 if no record was found
     */
    public long getProjectDBId(SQLiteDatabase db, String projectSlug) {
        Cursor cursor = db.rawQuery("SELECT `id` FROM `project` WHERE `slug`=?", new String[]{projectSlug});
        long projectId = 0;
        if(cursor.moveToFirst()) {
            projectId = cursor.getLong(0);
        }
        cursor.close();
        return projectId;
    }

    /**
     * Returns the database id of a source language
     * @param db
     * @param slug
     * @param projectId
     * @return returns 0 if no record was found
     */
    public long getSourceLanguageDBId(SQLiteDatabase db, String slug, long projectId) {
        Cursor cursor = db.rawQuery("SELECT `id` FROM `source_language` WHERE `slug`=? AND `project_id`=" + projectId, new String[]{slug});
        long sourceLanguageId = 0;
        if(cursor.moveToFirst()) {
            sourceLanguageId = cursor.getLong(0);
        }
        cursor.close();
        return sourceLanguageId;
    }

    /**
     * Returns an array of sorted project slugs
     * @param db
     * @return
     */
    public String[] getProjectSlugs(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT `slug` FROM `project` ORDER BY `sort` ASC", null);
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
     * Returns an array of sorted source language slugs
     * @param db
     * @param projectId
     * @return
     */
    public String[] getSourceLanguageSlugs(SQLiteDatabase db, long projectId) {
        Cursor cursor = db.rawQuery("SELECT `slug` FROM `source_language` WHERE `project_id`=" + projectId + " ORDER BY `slug` ASC", null);
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
     * @param db
     * @param sourceLanguageId
     * @return
     */
    public String[] getResourceSlugs(SQLiteDatabase db, long sourceLanguageId) {
        Cursor cursor = db.rawQuery("SELECT `slug` FROM `resource` WHERE `source_language_id`=" + sourceLanguageId + " ORDER BY `slug` ASC", null);
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
     * Returns the database id for the resource
     * @param db
     * @param slug
     * @param sourceLanguageId
     * @return
     */
    public long getResourceDBId(SQLiteDatabase db, String slug, long sourceLanguageId) {
        Cursor cursor = db.rawQuery("SELECT `id` FROM `resource` WHERE `slug`=? AND `source_language_id`=" + sourceLanguageId, new String[]{slug});
        long resourceId = 0;
        if(cursor.moveToFirst()) {
            resourceId = cursor.getLong(0);
        }
        cursor.close();
        return resourceId;
    }

    /**
     * Returns an array of chapter slugs
     * @param db
     * @param resourceId
     * @return
     */
    public String[] getChapterSlugs(SQLiteDatabase db, long resourceId) {
        Cursor cursor = db.rawQuery("SELECT `slug` FROM `chapter` WHERE `resource_id`=" + resourceId + " ORDER BY `sort` ASC", null);
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
     * Returns the database id for the chapter
     * @param db
     * @param slug
     * @param resourceId
     * @return
     */
    public long getChapterDBId(SQLiteDatabase db, String slug, long resourceId) {
        Cursor cursor = db.rawQuery("SELECT `id` FROM `chapter` WHERE `slug`=? AND `resource_id`=" + resourceId, new String[]{slug});
        long chapterId = 0;
        if(cursor.moveToFirst()) {
            chapterId = cursor.getLong(0);
        }
        cursor.close();
        return chapterId;
    }

    /**
     * Returns an array of frame slugs
     * @param db
     * @param chapterId
     * @return
     */
    public String[] getFrameSlugs(SQLiteDatabase db, long chapterId) {
        Cursor cursor = db.rawQuery("SELECT `slug` FROM `frame` WHERE `chapter_id`=" + chapterId + " ORDER BY `sort` ASC", null);
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
     * Returns the database id for the frame
     * @param db
     * @param slug
     * @param chapterId
     * @return
     */
    public long getFrameDBId(SQLiteDatabase db, String slug, long chapterId) {
        Cursor cursor = db.rawQuery("SELECT `id` FROM `frame` WHERE `slug`=? AND `chapter_id`=" + chapterId, new String[]{slug});
        long frameId = 0;
        if(cursor.moveToFirst()) {
            frameId = cursor.getLong(0);
        }
        cursor.close();
        return frameId;
    }

    /**
     * Inserts or updates a translation note
     * @param db
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
    public long addTranslationNote(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug, String frameSlug, String noteSlug, long frameId, String title, String body) {
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

        Cursor cursor = db.rawQuery("SELECT `id` FROM `translation_note` WHERE `slug`=? AND `frame_id`=" + frameId, new String[]{noteSlug});
        long noteId;
        if(cursor.moveToFirst()) {
            // update
            noteId = cursor.getLong(0);
            db.update("translation_note", values, "`id`=" + noteId, null);
        } else {
            // insert
            noteId = db.insert("translation_note", null, values);
        }
        cursor.close();
        return noteId;
    }

    /**
     * Returns an array of translation note slugs
     * @param db
     * @param frameId
     * @return
     */
    public String[] getTranslationNoteSlugs(SQLiteDatabase db, long frameId) {
        Cursor cursor = db.rawQuery("SELECT `slug` FROM `translation_note` WHERE `frame_id`=" + frameId + " ORDER BY `title` ASC", null);
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
     * Returns a translation note
     * @param db
     * @param slug
     * @param frameId
     * @return
     */
    public TranslationNote getTranslationNote(SQLiteDatabase db, String slug, long frameId) {
        Cursor cursor = db.rawQuery("SELECT `c`.`slug`, `f`.`slug`, `tn`.`id`, `tn`.`title`, `tn`.`body` FROM `translation_note` AS `tn`"
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
     * Returns a chapter
     * @param db
     * @param slug
     * @param resourceId
     * @return
     */
    public Chapter getChapter(SQLiteDatabase db, String slug, long resourceId) {
        Cursor cursor = db.rawQuery("SELECT `title`, `reference`, `slug` FROM `chapter` WHERE `slug`=? AND `resource_id`=" + resourceId, new String[]{slug});
        Chapter chapter = null;
        if(cursor.moveToFirst()) {
            chapter = new Chapter(cursor.getString(0), cursor.getString(1), cursor.getString(2));
        }
        cursor.close();
        return chapter;
    }

    /**
     * Returns a frame
     * @param db
     * @param slug
     * @param chapterId
     * @return
     */
    public Frame getFrame(SQLiteDatabase db, String slug, long chapterId) {
        Cursor cursor = db.rawQuery("SELECT `f`.`id`, `f`.`slug`, `c`.`slug`, `f`.`body`, `f`.`format`, `f`.`image_url` FROM `frame` AS `f`"
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
     * inserts or replace a translation word
     * @param db
     * @param wordSlug
     * @param resourceId
     * @param catalogHash
     * @param term
     * @param definitionTitle
     * @param definition
     * @return
     */
    public long addTranslationWord(SQLiteDatabase db, String wordSlug, long resourceId, String catalogHash, String term, String definitionTitle, String definition, TranslationWord.Example[] examples, String[] aliases, String[] related) {
        ContentValues values = new ContentValues();
        values.put("slug", wordSlug);
        values.put("catalog_hash", catalogHash);
        values.put("term", term);
        values.put("definition_title", definitionTitle);
        values.put("definition", definition);

        Cursor cursor = db.rawQuery("SELECT `id` FROM `translation_word` WHERE `slug`=? AND `catalog_hash`=?", new String[]{wordSlug, catalogHash});
        long wordId;
        if(cursor.moveToFirst()) {
            // update
            wordId = cursor.getLong(0);
            db.update("translation_word", values, "`id`=" + wordId, null);
        } else {
            // insert
            wordId = db.insert("translation_word", null, values);
        }
        cursor.close();

        // link word to resource
        ContentValues linkValues = new ContentValues();
        linkValues.put("resource_id", resourceId);
        linkValues.put("translation_word_id", wordId);
        db.insertWithOnConflict("resource__translation_word", null, linkValues, SQLiteDatabase.CONFLICT_IGNORE);

        // insert examples
        for(TranslationWord.Example example:examples) {
            ContentValues exampleValues = new ContentValues();
            exampleValues.put("frame_slug", example.getFrameId());
            exampleValues.put("chapter_slug", example.getChapterId());
            exampleValues.put("body", example.getPassage());
            exampleValues.put("translation_word_id", wordId);
            db.insertWithOnConflict("translation_word_example", null, exampleValues, SQLiteDatabase.CONFLICT_IGNORE);
        }

        // insert aliases
        for(String alias:aliases) {
            ContentValues aliasValues = new ContentValues();
            aliasValues.put("term", alias.trim());
            aliasValues.put("translation_word_id", wordId);
            db.insertWithOnConflict("translation_word_alias", null, aliasValues, SQLiteDatabase.CONFLICT_IGNORE);
        }

        // insert related
        for(String relatedWordSlug:related) {
            ContentValues relatedValues = new ContentValues();
            relatedValues.put("slug", relatedWordSlug.trim());
            relatedValues.put("translation_word_id", wordId);
            db.insertWithOnConflict("translation_word_related", null, relatedValues, SQLiteDatabase.CONFLICT_IGNORE);
        }

        return wordId;
    }

    /**
     * Returns an array of translation word slugs
     * @param db
     * @param resourceId
     * @return
     */
    public String[] getTranslationWordSlugs(SQLiteDatabase db, long resourceId) {
        Cursor cursor = db.rawQuery("SELECT `slug` FROM `translation_word`"
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
     * Returns a translation word
     * @param db
     * @param slug
     * @param resourceId
     * @return
     */
    public TranslationWord getTranslationWord(SQLiteDatabase db, String slug, long resourceId) {
        Cursor cursor = db.rawQuery("SELECT `tw`.`id`, `tw`.`term`, `tw`.`definition`, `tw`.`definition_title`, `related`.`related_words`, `aliases`.`word_aliases` FROM `translation_word` AS `tw`"
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
            Cursor examplesCursor = db.rawQuery("SELECT `chapter_slug`, `frame_slug`, `body` FROM `translation_word_example`"
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
     * Returns an array of translationWords in a source translation
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @return
     */
    public TranslationWord[] getTranslationWords(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        List<TranslationWord> words = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT `id`, `slug`, `term`, `definition`, `definition_title` FROM `translation_word`"
                + " WHERE `id` IN ("
                + "   SELECT `translation_word_id` FROM `resource__translation_word` AS `rtw`"
                + "   LEFT JOIN `resource` AS `r` ON `r`.`id`=`rtw`.`resource_id`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + "   LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=?"
                + " ) ORDER BY `slug` ASC", new String[]{projectSlug, sourceLanguageSlug, resourceSlug});
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
     * Returns an array of translation words that are linked to the frame
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public TranslationWord[] getTranslationWordsForFrame(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug, String frameSlug) {
        List<TranslationWord> words = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT `id`, `slug`, `term`, `definition`, `definition_title` FROM `translation_word`"
                + " WHERE `id` IN ("
                + "   SELECT `translation_word_id` FROM `frame__translation_word`"
                + "   WHERE `project_slug`=? AND `source_language_slug`=? AND `resource_slug`=? AND `chapter_slug`=? AND `frame_slug`=?"
                + " ) ORDER BY `slug` ASC", new String[]{projectSlug, sourceLanguageSlug, resourceSlug, chapterSlug, frameSlug});
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
     * links a translation word to a frame
     * @param db
     * @param wordSlug
     * @param frameId
     */
    public void addTranslationWordToFrame(SQLiteDatabase db, String wordSlug, long resourceId, long frameId, String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug, String frameSlug) {
        long wordId = getTranslationWordDBId(db, wordSlug, resourceId);
        if(wordId > 0) {
            db.execSQL("REPLACE INTO `frame__translation_word` (`frame_id`, `translation_word_id`, `project_slug`, `source_language_slug`, `resource_slug`, `chapter_slug`, `frame_slug`) VALUES (" + frameId + "," + wordId + ",?,?,?,?,?)", new String[]{projectSlug, sourceLanguageSlug, resourceSlug, chapterSlug, frameSlug});
        }
    }

    /**
     * Returns the database id for a translation word
     * @param db
     * @param wordSlug
     * @param resourceId
     * @return
     */
    private long getTranslationWordDBId(SQLiteDatabase db, String wordSlug, long resourceId) {
        Cursor cursor = db.rawQuery("SELECT `tw`.`id` FROM `translation_word` AS `tw`"
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
     * Adds a checking question and links it to the frame
     * @param db
     * @param frameId
     * @param chapterId
     * @param question
     * @param answer
     */
    public long addCheckingQuestion(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug, String frameSlug, String questionSlug, long frameId, long chapterId, String question, String answer) {
        ContentValues values = new ContentValues();
        values.put("slug", questionSlug);
        values.put("chapter_id", chapterId);
        values.put("question", question);
        values.put("answer", answer);

        Cursor cursor = db.rawQuery("SELECT `id` FROM `checking_question` WHERE `slug`=? AND `chapter_id`=" + chapterId, new String[]{questionSlug});
        long questionId;
        if(cursor.moveToFirst()) {
            // update
            questionId = cursor.getLong(0);
            db.update("checking_question", values, "`id`=" + questionId, null);
        } else {
            // insert
            questionId = db.insert("checking_question", null, values);
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
        db.replace("frame__checking_question", null, linkValues);

        return questionId;
    }

    /**
     * Returns an array of checking questions
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public CheckingQuestion[] getCheckingQuestions(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug, String frameSlug) {
        List<CheckingQuestion> questions = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT `slug`, `question`, `answer` FROM `checking_question`"
                + " WHERE `id` IN ("
                + "   SELECT `checking_question_id` FROM `frame__checking_question`"
                + "   WHERE `project_slug`=? AND `source_language_slug`=? AND `resource_slug`=? AND `chapter_slug`=? AND `frame_slug`=?"
                + ")", new String[]{projectSlug, sourceLanguageSlug, resourceSlug, chapterSlug, frameSlug});
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

    public CheckingQuestion getCheckingQuestion(SQLiteDatabase db, long chapterId, String frameSlug, String questionSlug) {
        CheckingQuestion question = null;
        Cursor cursor = db.rawQuery("SELECT `c`.`slug`, `cq`.`question`, `cq`.`answer`, `ref`.`references` FROM `checking_question` AS `cq`"
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
     * Returns a project
     * The source language will default to english then the first available language
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public Project getProject(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug) {
        Project project = null;
        Cursor cursor = db.rawQuery("SELECT `p`.`sort`, `p`.`modified_at`, `p`.`source_language_catalog_url`,"
                + " COALESCE(`sl1`.`slug`, `sl2`.`slug`, `sl3`.`slug`),"
                + " COALESCE(`sl1`.`project_name`, `sl2`.`project_name`, `sl3`.`project_name`),"
                + " COALESCE(`sl1`.`project_description`, `sl2`.`project_description`, `sl3`.`project_description`),"
                + " `p`.`source_language_catalog_local_modified_at`, `p`.`source_language_catalog_server_modified_at`"
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
            String actualSsourceLanguageSlug = cursor.getString(3);
            String projectName = cursor.getString(4);
            String projectDescription = cursor.getString(5);
            int sourceLanguageCatalogLocalModified = cursor.getInt(6);
            int sourceLanguageCatalogServerModified = cursor.getInt(7);
            project = new Project(projectSlug, actualSsourceLanguageSlug, projectName, projectDescription, dateModified, sort, sourceLanguageCatalog, sourceLanguageCatalogLocalModified, sourceLanguageCatalogServerModified);
        }
        cursor.close();
        return project;
    }

    /**
     * Returns a single source language
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public SourceLanguage getSourceLanguage(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug) {
        SourceLanguage sourceLanguage = null;
        Cursor cursor = db.rawQuery("SELECT `sl`.`name`, `sl`.`project_name`, `sl`.`project_description`, `sl`.`direction`, `sl`.`modified_at`, `sl`.`resource_catalog_url`, `sl`.`resource_catalog_local_modified_at`, `sl`.`resource_catalog_server_modified_at` FROM `source_language` AS `sl`"
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
     * @param db
     * @param projectSlug
     * @return
     */
    public SourceLanguage[] getSourceLanguages(SQLiteDatabase db, String projectSlug) {
        List<SourceLanguage> sourceLanguages = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT `sl`.`slug`, `sl`.`name`, `sl`.`project_name`, `sl`.`project_description`, `sl`.`direction`, `sl`.`modified_at`, `sl`.`resource_catalog_url`, `sl`.`resource_catalog_local_modified_at`, `sl`.`resource_catalog_server_modified_at` FROM `source_language` AS `sl`"
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
     * Returns a resource
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @return
     */
    public Resource getResource(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        Resource resource = null;
        Cursor cursor = db.rawQuery("SELECT `r`.`name`, `r`.`checking_level`, `r`.`version`, `r`.`modified_at`,"
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
                + " WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=?", new String[]{resourceSlug, projectSlug, sourceLanguageSlug, resourceSlug});

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

            resource = new Resource(resourceName, resourceSlug, checkingLevel, version, isDownloaded, dateModified,
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
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @return
     */
    public Resource[] getResources(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug) {
        List<Resource> resources = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT `r`.`name`, `r`.`checking_level`, `r`.`version`, `r`.`modified_at`,"
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
     * Returns an array of source translations that have updates available online.
     * This only includes source translations that have been previously downloaded.
     * @param db
     * @return
     */
    public SourceTranslation[] getSourceTranslationsWithUpdates(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT `p`.`slug` AS `project_slug`, `sl`.`slug` AS `source_language_slug`, `sl`.`project_name`, `sl`.`name`, `r`.`slug` AS `resource_slug`, `r`.`name`, `r`.`checking_level`, `r`.`modified_at`, `r`.`version` FROM `resource` AS `r`"
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
            TranslationFormat format = getSourceTranslationFormat(db, projectSlug, sourceLanguageSlug, resourceSlug);
            sourceTranslations.add(new SourceTranslation(projectSlug, sourceLanguageSlug, resourceSlug, projectName, sourceLanguageName, resourceName, checkingLevel, dateModified, version, format));
            cursor.moveToNext();
        }
        cursor.close();
        return sourceTranslations.toArray(new SourceTranslation[sourceTranslations.size()]);
    }

    /**
     * Returns an array of target languages
     * @param db
     * @return
     */
    public TargetLanguage[] getTargetLanguages(SQLiteDatabase db) {
        List<TargetLanguage> targetLanguages = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT `slug`, `name`, `direction`, `region` FROM `target_language`", null);
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
     * Adds a target language
     * @param db
     * @param slug
     * @param direction
     * @param name
     * @param region
     */
    public long addTargetLanguage(SQLiteDatabase db, String slug, String direction, String name, String region) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("name", name);
        values.put("direction", direction);
        values.put("region", region);
        return db.insertWithOnConflict("target_language", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Returns the number of target languages there are in the database
     * @param db
     * @return
     */
    public int getTargetLanguagesLength(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM `target_language`", null);
        int count = 0;
        if(cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * Returns a target language
     * @param db
     * @param targetLanguageSlug
     * @return
     */
    public TargetLanguage getTargetLanguage(SQLiteDatabase db, String targetLanguageSlug) {
        Cursor cursor = db.rawQuery("SELECT `name`, `direction`, `region` FROM `target_language` WHERE `slug`=?", new String[]{targetLanguageSlug});
        TargetLanguage targetLanguage = null;
        if(cursor.moveToFirst()) {
            String name = cursor.getString(0);
            LanguageDirection direction = LanguageDirection.get(cursor.getString(1));
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
            String region = cursor.getString(2);
            targetLanguage = new TargetLanguage(targetLanguageSlug, name, region, direction);
        }
        cursor.close();
        return targetLanguage;
    }

    /**
     * Returns a target language
     * @param db
     * @param targetLanguageName
     * @return
     */
    public TargetLanguage getTargetLanguageByName(SQLiteDatabase db, String targetLanguageName) {
        Cursor cursor = db.rawQuery("SELECT `name`, `direction`, `region`, `slug` FROM `target_language` WHERE LOWER(`name`)=? LIMIT 1", new String[]{targetLanguageName.toLowerCase()});
        TargetLanguage targetLanguage = null;
        if(cursor.moveToFirst()) {
            String name = cursor.getString(0);
            LanguageDirection direction = LanguageDirection.get(cursor.getString(1));
            if(direction == null) {
                direction = LanguageDirection.LeftToRight;
            }
            String region = cursor.getString(2);
            String slug = cursor.getString(3);
            targetLanguage = new TargetLanguage(slug, name, region, direction);
        }
        cursor.close();
        return targetLanguage;
    }

    /**
     * Returns the format of the source translation
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @return
     */
    public TranslationFormat getSourceTranslationFormat(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        Cursor cursor = db.rawQuery("SELECT `f`.`format` FROM `frame` AS `f`"
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
     * Returns a source translation
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @return
     */
    public SourceTranslation getSourceTranslation(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        SourceTranslation sourceTranslation = null;
        Cursor cursor = db.rawQuery("SELECT `sl`.`project_name`, `sl`.`name`, `r`.`name`, `r`.`checking_level`, `r`.`modified_at`, `r`.`version`"
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
            TranslationFormat format = getSourceTranslationFormat(db, projectSlug, sourceLanguageSlug, resourceSlug);
            sourceTranslation = new SourceTranslation(projectSlug, sourceLanguageSlug, resourceSlug, projectName, sourceLanguageName, resourceName, checkingLevel, dateModified, version, format);
        }
        cursor.close();
        return sourceTranslation;
    }

    /**
     * Returns the branch of the category list
     * @param db
     * @param sourcelanguageSlug
     * @param parentCategoryId
     * @return
     */
    public ProjectCategory[] getCategoryBranch(SQLiteDatabase db, String sourcelanguageSlug, long parentCategoryId) {
        List<ProjectCategory> categories = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM ("
                + " SELECT `c`.`slug` AS `category_slug`, `slc`.`category_name` AS `title`, NULL AS `project_slug`, 0 AS `sort`, `c`.`id` AS `category_id` FROM `category` AS `c`"
                + " LEFT JOIN `source_language__category` AS `slc` ON `slc`.`category_id`=`c`.`id`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`slc`.`source_language_id`"
                + " WHERE `sl`.`slug`=? AND `c`.`parent_id`=" + parentCategoryId
                + " UNION"
                + " SELECT `c`.`slug` AS `category_slug`, `sl`.`project_name` AS `title`, `p`.`slug` AS `project_id`, `p`.`sort` AS `sort`, " + parentCategoryId + " AS `category_id` FROM `project` AS `p`"
                + " LEFT JOIN `project__category` AS `pc` ON `pc`.`project_id`=`p`.`id`"
                + " LEFT JOIN `category` AS `c` ON `c`.`id`=`pc`.`category_id`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + " WHERE CASE WHEN " + parentCategoryId + "=0 THEN `pc`.`category_id` IS NULL ELSE `pc`.`category_id`=" + parentCategoryId + " END AND `sl`.`slug`=?"
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
     * Returns an array of chapters
     * @param db
     * @param resourceId
     * @return
     */
    public Chapter[] getChapters(SQLiteDatabase db, long resourceId) {
        // we'll need to update the schema to include the slugs in the chapter table in order to do this
        List<Chapter> chapters = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT `slug`, `reference`, `title` FROM `chapter` WHERE `resource_id`=" + resourceId + " ORDER BY `sort` ASC", null);
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
     * Returns an array of frames
     * @param db
     * @param chapterSlug
     * @return
     */
    public Frame[] getFrames(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug) {
        List<Frame> frames = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT `f`.`id`, `f`.`slug`, `f`.`body`, `f`.`format`, `f`.`image_url` FROM `frame` AS `f`"
                + " WHERE `f`.`chapter_id` IN ("
                + "   SELECT `c`.`id` FROM `chapter` AS `c`"
                + "   LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + "   LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=? AND `c`.`slug`=?"
                + " ) ORDER BY `f`.`sort` ASC", new String[]{projectSlug, sourceLanguageSlug, resourceSlug, chapterSlug});
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
     * Returns the chapter body
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @param chapterSlug
     * @return
     */
    public String getChapterBody(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug) {
        Cursor cursor = db.rawQuery("SELECT GROUP_CONCAT(`f`.`body`, ' ') AS `body` FROM `frame` AS `f`"
                + " LEFT JOIN `chapter` AS `c` ON `c`.`id`=`f`.`chapter_id`"
                + " LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + " LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + " LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + " WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=? AND `c`.`slug`=? ORDER BY `c`.`sort`, `f`.`sort` ASC", new String[]{projectSlug, sourceLanguageSlug, resourceSlug, chapterSlug});
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
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @param chapterSlug
     * @return
     */
    public TranslationFormat getChapterBodyFormat(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug) {
        Cursor cursor = db.rawQuery("SELECT `f`.`format` FROM `frame` AS `f`"
                + " WHERE `f`.`chapter_id` IN ("
                + "   SELECT `c`.`id` FROM `chapter` AS `c`"
                + "   LEFT JOIN `resource` AS `r` ON `r`.`id`=`c`.`resource_id`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`id`=`r`.`source_language_id`"
                + "   LEFT JOIN `project` AS `p` ON `p`.`id`=`sl`.`project_id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=? AND `r`.`slug`=? AND `c`.`slug`=?"
                + " ) AND `f`.`format` IS NOT NULL LIMIT 1", new String[]{projectSlug, sourceLanguageSlug, resourceSlug, chapterSlug});
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
     * Returns an array of translation notes
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @param chapterSlug
     * @param frameSlug
     * @return
     */
    public TranslationNote[] getTranslationNotes(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug, String chapterSlug, String frameSlug) {
        List<TranslationNote> notes = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT `slug`, `title`, `body` FROM `translation_note`"
                + " WHERE `project_slug`=? AND `source_language_slug`=? AND `resource_slug`=? AND `chapter_slug`=? AND `frame_slug`=?"
                , new String[]{projectSlug, sourceLanguageSlug, resourceSlug, chapterSlug, frameSlug});
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
     * Returns the number of translatable items in the resource
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     * @return
     */
    public int countTranslatableItems(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        Cursor cursor = db.rawQuery("SELECT SUM(`count`) FROM ("
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
                + " )", new String[]{projectSlug, sourceLanguageSlug, resourceSlug, projectSlug, sourceLanguageSlug, resourceSlug});
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
     * @param db
     * @param sourceLanguageSlug the preferred language for project titles and descriptions
     * @return
     */
    public Project[] getProjects(SQLiteDatabase db, String sourceLanguageSlug) {
        Cursor cursor = db.rawQuery("SELECT `p`.`slug`, `p`.`sort`, `p`.`modified_at`, `p`.`source_language_catalog_url`,"
                + " COALESCE(`sl1`.`slug`, `sl2`.`slug`, `sl3`.`slug`),"
                + " COALESCE(`sl1`.`project_name`, `sl2`.`project_name`, `sl3`.`project_name`),"
                + " COALESCE(`sl1`.`project_description`, `sl2`.`project_description`, `sl3`.`project_description`),"
                + " `p`.`source_language_catalog_local_modified_at`, `p`.`source_language_catalog_server_modified_at`"
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
            projects.add(new Project(projectSlug, actualSsourceLanguageSlug, projectName, projectDescription, dateModified, sort, sourceLanguageCatalog, sourceLanguageCatalogLocalModified, sourceLanguageCatalogServerModified));
            cursor.moveToNext();
        }
        cursor.close();
        return projects.toArray(new Project[projects.size()]);
    }

    /**
     * Updates the local source language catalog date modified to that of the server
     * @param db
     * @param projectSlug
     */
    public void markSourceLanguageCatalogUpToDate(SQLiteDatabase db, String projectSlug) {
        db.execSQL("UPDATE `project` SET"
                + " `source_language_catalog_local_modified_at`=`source_language_catalog_server_modified_at`"
                + " WHERE `slug`=?", new String[]{projectSlug});
    }

    /**
     * Updates the local resource catalog date modified to that of the server
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     */
    public void markResourceCatalogUpToDate(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug) {
        db.execSQL("UPDATE `source_language`"
                + " SET `resource_catalog_local_modified_at`=`resource_catalog_server_modified_at`"
                + " WHERE `project_id` IN ("
                + "   SELECT `id` FROM `project` WHERE `slug`=?"
                + " ) AND `slug`=?", new String[]{projectSlug, sourceLanguageSlug});
    }

    /**lkk
     *
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     */
    public void markSourceCatalogUpToDate(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        db.execSQL("UPDATE `resource`"
                + " SET `source_catalog_local_modified_at`=`source_catalog_server_modified_at`"
                + " WHERE `source_language_id` IN ("
                + "   SELECT `sl`.`id` FROM `project` AS `p`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=?"
                + " ) AND `slug`=?", new String[]{projectSlug, sourceLanguageSlug, resourceSlug});
    }

    /**
     *
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     */
    public void markNotesCatalogUpToDate(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        db.execSQL("UPDATE `resource`"
                + " SET `translation_notes_catalog_local_modified_at`=`translation_notes_catalog_server_modified_at`"
                + " WHERE `source_language_id` IN ("
                + "   SELECT `sl`.`id` FROM `project` AS `p`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=?"
                + " ) AND `slug`=?", new String[]{projectSlug, sourceLanguageSlug, resourceSlug});
    }

    /**
     *
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     */
    public void markWordsCatalogUpToDate(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        db.execSQL("UPDATE `resource`"
                + " SET `translation_words_catalog_local_modified_at`=`translation_words_catalog_server_modified_at`"
                + " WHERE `source_language_id` IN ("
                + "   SELECT `sl`.`id` FROM `project` AS `p`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=?"
                + " ) AND `slug`=?", new String[]{projectSlug, sourceLanguageSlug, resourceSlug});
    }

    /**
     *
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     */
    public void markWordAssignmentsCatalogUpToDate(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        db.execSQL("UPDATE `resource`"
                + " SET `translation_word_assignments_catalog_local_modified_at`=`translation_word_assignments_catalog_server_modified_at`"
                + " WHERE `source_language_id` IN ("
                + "   SELECT `sl`.`id` FROM `project` AS `p`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=?"
                + " ) AND `slug`=?", new String[]{projectSlug, sourceLanguageSlug, resourceSlug});
    }

    /**
     *
     * @param db
     * @param projectSlug
     * @param sourceLanguageSlug
     * @param resourceSlug
     */
    public void markQuestionsCatalogUpToDate(SQLiteDatabase db, String projectSlug, String sourceLanguageSlug, String resourceSlug) {
        db.execSQL("UPDATE `resource`"
                + " SET `checking_questions_catalog_local_modified_at`=`checking_questions_catalog_server_modified_at`"
                + " WHERE `source_language_id` IN ("
                + "   SELECT `sl`.`id` FROM `project` AS `p`"
                + "   LEFT JOIN `source_language` AS `sl` ON `sl`.`project_id`=`p`.`id`"
                + "   WHERE `p`.`slug`=? AND `sl`.`slug`=?"
                + " ) AND `slug`=?", new String[]{projectSlug, sourceLanguageSlug, resourceSlug});
    }

    /**
     *
     * @param db
     * @param volSlug
     * @param resourceDBId
     * @param cataloghash
     * @param volTitle
     * @return the database id of the volume
     */
    public long addTranslationAcademyVolume(SQLiteDatabase db, String volSlug, long resourceDBId, String cataloghash, String volTitle) {
        ContentValues values = new ContentValues();
        values.put("slug", volSlug);
        values.put("catalog_hash", cataloghash);
        values.put("title", volTitle);

        Cursor cursor = db.rawQuery("SELECT `id` from `translation_academy_volume` WHERE `slug`=? AND `catalog_hash`=?", new String[]{volSlug, cataloghash});
        long volumeId;
        if(cursor.moveToFirst()) {
            // update
            volumeId = cursor.getLong(0);
            db.update("translation_academy_volume", values, "`id`=" + volumeId, null);
        } else {
            // insert
            volumeId = db.insert("translation_academy_volume", null, values);
        }
        cursor.close();

        // link volume to resource
        ContentValues linkValues = new ContentValues();
        linkValues.put("resource_id", resourceDBId);
        linkValues.put("translation_academy_volume_id", volumeId);
        db.insertWithOnConflict("resource__translation_academy_volume", null, linkValues, SQLiteDatabase.CONFLICT_IGNORE);

        return volumeId;
    }

    /**
     *
     * @param db
     * @param manualSlug
     * @param volumeDBId
     * @param manualTitle
     * @return the database id of the manual
     */
    public long addTranslationAcademyManual(SQLiteDatabase db, String manualSlug, long volumeDBId, String manualTitle) {
        ContentValues values = new ContentValues();
        values.put("slug", manualSlug);
        values.put("translation_academy_volume_id", volumeDBId);
        values.put("title", manualTitle);

        Cursor cursor = db.rawQuery("SELECT `id` FROM `translation_academy_manual` WHERE `slug`=? AND `translation_academy_volume_id`=" + volumeDBId, new String[]{manualSlug});
        long manualId;
        if(cursor.moveToFirst()) {
            // update
            manualId = cursor.getLong(0);
            db.update("translation_academy_manual", values, "`id`=" + manualId, null);
        } else {
            // insert
            manualId = db.insert("translation_academy_manual", null, values);
        }
        cursor.close();
        return manualId;
    }

    /**
     *
     * @param db
     * @param articleId
     * @param manualDBId
     * @param articleTitle
     * @param articleText
     * @return the database id of the article
     */
    public long addTranslationAcademyArticle(SQLiteDatabase db, String articleId, long manualDBId, String articleTitle, String articleText, String articleReference) {
        ContentValues values = new ContentValues();
        values.put("slug", articleId);
        values.put("translation_academy_manual_id", manualDBId);
        values.put("title", articleTitle);
        values.put("reference", articleReference);
        values.put("text", articleText);

        Cursor cursor = db.rawQuery("SELECT `id` FROM `translation_academy_article` WHERE `slug`=? AND `translation_academy_manual_id`=" + manualDBId, new String[]{articleId});
        long articleDBId;
        if(cursor.moveToFirst()) {
            // update
            articleDBId = cursor.getLong(0);
            db.update("translation_academy_article", values, "`id`=" + articleDBId, null);
        } else {
            // insert
            articleDBId = db.insert("translation_academy_article", null, values);
        }
        cursor.close();
        return articleDBId;
    }

    /**
     * Returns a translation article
     * @param db
     * @param resourceId
     * @param volume
     * @param manual
     * @param referenceSlug
     * @return
     */
    public TranslationArticle getTranslationArticle(SQLiteDatabase db, long resourceId, String volume, String manual, String referenceSlug) {
        Cursor cursor = db.rawQuery("SELECT `taa`.`id`, `taa`.`slug`, `taa`.`translation_academy_manual_id`, `taa`.`title`, `taa`.`text`, `taa`.`reference` FROM `translation_academy_article` AS `taa`"
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
}
