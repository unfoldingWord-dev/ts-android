package com.door43.translationstudio.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by joel on 10/1/2015.
 */
public class IndexerSQLiteHelper extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_CATALOGS = "catalog";
    public static final String TABLE_FILES = "file";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_HASH = "hash";
    public static final String COLUMN_IS_DIR = "is_dir";
    public static final String COLUMN_NUM_LINKS = "num_links";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PARENT_ID = "parent_id";
    public static final String COLUMN_CATALOG_ID = "catalog_id";
    private final Context mContext;
    private final String mDatabaseName;

    public IndexerSQLiteHelper(Context context, String name) {
        super(context, name, null, DATABASE_VERSION);
        mContext = context;
        mDatabaseName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String catalogTable = "CREATE TABLE `catalog` ( `id`  INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, `hash`  TEXT NOT NULL UNIQUE, `num_links` INTEGER NOT NULL DEFAULT 0, `updated_at`  INTEGER NOT NULL);";
        String fileTable = "CREATE TABLE `file` ( `id`  INTEGER NOT NULL UNIQUE, `name`  text NOT NULL, `parent_id` INTEGER NOT NULL DEFAULT 0, `catalog_id`  INTEGER NOT NULL, `content` text, `is_dir`  INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id));";
        String linkTable ="CREATE TABLE `link` ( `id`  INTEGER NOT NULL UNIQUE, `name`  TEXT NOT NULL UNIQUE, `catalog_id`  INTEGER NOT NULL, PRIMARY KEY(id));";
        db.execSQL(catalogTable);
        db.execSQL(fileTable);
        db.execSQL(linkTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: perform any nessesary updates as necessary
        onCreate(db);
    }

    /**
     * Destroys the database
     */
    public void deleteDatabase() {
        mContext.deleteDatabase(mDatabaseName);
    }

    public void replaceLink(String md5hash, String linkPath) {

    }
}
