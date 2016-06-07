package com.door43.translationstudio.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import com.door43.tools.reporting.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 10/1/2015.
 * TODO: these methods need to throw exeptions so we can log the error
 */
public class LibrarySQLiteHelper extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 8;
    private final String databaseName;
    private final String schema;

    /**
     * Creates a new sql helper for the indexer.
     * This currently expects an asset named schema.sql
     * @param context
     * @param name
     * @throws IOException
     */
    public LibrarySQLiteHelper(Context context, String schemaAsset, String name) throws IOException {
        super(context, name, null, DATABASE_VERSION);
        this.schema = Util.readStream(context.getAssets().open(schemaAsset));
        this.databaseName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            db.execSQL("PRAGMA foreign_keys=OFF;");
        }
        String[] queries = schema.split(";");
        for (String query : queries) {
            query = query.trim();
            if(!query.isEmpty()) {
                try {
                    db.execSQL(query);
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to execute query", e);
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
        if(newVersion == 1) {
            onCreate(db);
        }
        if(oldVersion < 2) {
            db.beginTransaction();

            // new tables
            db.execSQL("DROP TABLE IF EXISTS `file`");
            db.execSQL("DROP TABLE IF EXISTS `link`");
            onCreate(db);

            db.setTransactionSuccessful();
            db.endTransaction();
        }
        if(oldVersion < 3) {
            db.beginTransaction();

            // add columns
            db.execSQL("ALTER TABLE `project` ADD COLUMN `source_language_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE `project` ADD COLUMN `source_language_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE `source_language` ADD COLUMN `resource_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE `source_language` ADD COLUMN `resource_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0;");

            db.setTransactionSuccessful();
            db.endTransaction();
        }
        if(oldVersion < 5) {
            db.beginTransaction();

            // alter project table with chunk_marker catalog
            db.execSQL("CREATE TABLE `project_new` (" +
                    "  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "  `slug` TEXT NOT NULL," +
                    "  `sort` INTEGER NOT NULL DEFAULT 0," +
                    "  `modified_at` INTEGER NOT NULL," +
                    "  `source_language_catalog_url` TEXT NOT NULL," +
                    "  `source_language_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0," +
                    "  `source_language_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0," +
                    "  `chunk_marker_catalog_url` TEXT NULL DEFAULT NULL," +
                    "  `chunk_marker_catalog_local_modified_at` INTEGER NOT NULL DEFAULT 0," +
                    "  `chunk_marker_catalog_server_modified_at` INTEGER NOT NULL DEFAULT 0," +
                    "  UNIQUE (`slug`)" +
                    ");");
            db.execSQL("INSERT INTO `project_new` (`id`, `slug`, `sort`, `modified_at`," +
                    " `source_language_catalog_url`, `source_language_catalog_local_modified_at`," +
                    " `source_language_catalog_server_modified_at`) SELECT * FROM `project`;");
            db.execSQL("DROP TABLE IF EXISTS `project`");
            db.execSQL("ALTER TABLE `project_new` RENAME TO `project`");

            // add chunk_marker table
            db.execSQL("CREATE TABLE `chunk_marker` (" +
                    "  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "  `project_id` INTEGER NOT NULL," +
                    "  `chapter_slug` TEXT NOT NULL," +
                    "  `first_verse_slug` TEXT NOT NULL," +
                    "  UNIQUE (`project_id`, 'chapter_slug', 'first_verse_slug')," +
                    "  FOREIGN KEY (project_id) REFERENCES `project` (`id`) ON DELETE CASCADE" +
                    ");");

            db.setTransactionSuccessful();
            db.endTransaction();
        }
        if(oldVersion < 6) {
            db.beginTransaction();

            // add tables for the new target language questionnaire
            db.execSQL("CREATE TABLE `new_target_language_questionnaire` (\n" +
                    "  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
                    "  `questionnaire_td_id` INTEGER NOT NULL,\n" +
                    "  `language_slug` TEXT NOT NULL,\n" +
                    "  `language_name` TEXT NOT NULL,\n" +
                    "  `language_direction` TEXT NOT NULL,\n" +
                    "  UNIQUE (`questionnaire_td_id`)\n" +
                    ");");
            db.execSQL("CREATE TABLE `new_target_language_question` (\n" +
                    "  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
                    "  `new_target_language_questionnaire_id` INTEGER NOT NULL,\n" +
                    "  `question_td_id` INTEGER NOT NULL,\n" +
                    "  `text` TEXT NOT NULL,\n" +
                    "  `help` TEXT NOT NULL,\n" +
                    "  `is_required` INTEGER NOT NULL DEFAULT 0,\n" +
                    "  `input_type` TEXT NOT NULL,\n" +
                    "  `sort` INTEGER NOT NULL DEFAULT 0,\n" +
                    "  `depends_on` INTEGER DEFAULT NULL,\n" +
                    "  UNIQUE (`question_td_id`, `new_target_language_questionnaire_id`),\n" +
                    "  FOREIGN KEY (new_target_language_questionnaire_id) REFERENCES `new_target_language_questionnaire` (`id`) ON DELETE CASCADE\n" +
                    ");");

            db.setTransactionSuccessful();
            db.endTransaction();
        }
        if(oldVersion < 7) {
            db.beginTransaction();

            db.execSQL("CREATE TABLE `temp_target_language` (\n" +
                    "  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
                    "  `slug` TEXT NOT NULL,\n" +
                    "  `name` TEXT NOT NULL,\n" +
                    "  `direction` TEXT NOT NULL,\n" +
                    "  `region` TEXT NOT NULL,\n" +
                    "  UNIQUE (`slug`)\n" +
                    ");");

            db.setTransactionSuccessful();
            db.endTransaction();
        }
        if(oldVersion < 8) {
            db.beginTransaction();

            db.execSQL("CREATE TABLE `approved_temp_target_language` (\n" +
                    "  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
                    "  `target_language_id` INTEGER NOT NULL,\n" +
                    "  `temp_target_language_id` INTEGER NOT NULL,\n" +
                    "  UNIQUE (`target_language_id`, `temp_target_language_id`),\n" +
                    "  FOREIGN KEY (target_language_id) REFERENCES `target_language` (`id`) ON DELETE CASCADE,\n" +
                    "  FOREIGN KEY (temp_target_language_id) REFERENCES `temp_target_language` (`id`) ON DELETE CASCADE\n" +
                    ");");

            db.setTransactionSuccessful();
            db.endTransaction();
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
        context.deleteDatabase(this.databaseName);
    }
}
