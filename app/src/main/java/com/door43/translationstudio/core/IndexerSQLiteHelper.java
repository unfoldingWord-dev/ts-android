package com.door43.translationstudio.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by joel on 10/1/2015.
 */
public class IndexerSQLiteHelper extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_FILES = "file";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_DIR = "dir";
    public static final String COLUMN_FILE = "file";
    private final Context mContext;
    private final String mDatabaseName;

    public IndexerSQLiteHelper(Context context, String name) {
        super(context, name, null, DATABASE_VERSION);
        mContext = context;
        mDatabaseName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FILES + " (" + COLUMN_PATH + " text, " + COLUMN_DIR + " text, " + COLUMN_FILE + " text, " + COLUMN_CONTENT + " text);");
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
}
