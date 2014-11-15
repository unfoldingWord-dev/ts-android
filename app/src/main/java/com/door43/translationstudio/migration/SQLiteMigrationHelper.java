package com.door43.translationstudio.migration;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.MainContext;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * This class handles the db migration from 1.x to 2.x.
 */
public class SQLiteMigrationHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Database";
    private static final ProjectManager mProjectManager = MainContext.getContext().getSharedProjectManager();
    // these percents are just guestimates for how much work will be required to migrate. They don't have to be exact.
    private final double PERCENT_FRAMES = 80.0;
    private final double PERCENT_CHAPTERS = 20.0;
    private double mProgress = 0;
    private static Context mContext;
    private static final String TAG = "MigrationSQL";
    private static String mDatabasePath;

    public SQLiteMigrationHelper(Context context, String databasePath) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        mDatabasePath = databasePath;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }

    /**
     * Imports all of the translations from 1.x into 2.x
     */
    public void migrateDatabase(OnProgressCallback callback) {
        // we can assume
        Project p = mProjectManager.getProject("obs");
        if(!mProjectManager.getSelectedProject().getId().equals(p.getId())) {
            // load the project silently
            mProjectManager.fetchProjectSource(p, false);
        }

        if(p != null) {
            if(!new File(mDatabasePath).exists()) {
                Log.d(TAG, "The database could not be found at "+mDatabasePath);
                return;
            }
            SQLiteDatabase db = SQLiteDatabase.openDatabase(mDatabasePath, null, SQLiteDatabase.OPEN_READONLY);

            // migrate the stories
            String query = "select s.story, s.story_title, s.story_ref, l.language_code from stories as s left join languages as l on l.id=s.language_id where s.source=0";
            Cursor cursor = db.rawQuery(query, null);
            int numRows = cursor.getCount();
            if (cursor.moveToFirst()) {
                do {
                    mProgress += PERCENT_CHAPTERS / numRows;
                    Chapter c = p.getChapter(cursor.getString(cursor.getColumnIndex("story")));
                    if(c != null) {
                        c.setTitleTranslation(cursor.getString(cursor.getColumnIndex("story_title")));
                        c.setReferenceTranslation(cursor.getString(cursor.getColumnIndex("story_ref")));
                        c.save();
                        Log.d(TAG, "Migrating "+c.getId());
                        callback.onProgress(mProgress, "Migrating "+c.getId());
                    } else {
                        Log.d(TAG, "the chapter could not be found " + cursor.getString(cursor.getColumnIndex("story")));
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "could not move the db cursor");
            }

            // migrate the frames
            query = "select s.story, f.frame, f.frame_text, l.language_code from frames as f left join stories as s on s.id=f.story_id left join languages as l on l.id=s.language_id where f.source=0";
            cursor = db.rawQuery(query, null);
            numRows = cursor.getCount();
            if (cursor.moveToFirst()) {
                Log.d(TAG, "importing frames");
                do {
                    mProgress += PERCENT_FRAMES / numRows;
                    Chapter c = p.getChapter(cursor.getString(cursor.getColumnIndex("story")));
                    if(c != null) {
                        Frame f = c.getFrame(cursor.getString(cursor.getColumnIndex("frame")));
                        Language l = mProjectManager.getLanguage(cursor.getString(cursor.getColumnIndex("language_code")));
                        if(f != null && l != null) {
                            Log.d(TAG, "Migrating "+f.getChapterFrameId());
                            callback.onProgress(mProgress, "Migrating "+f.getChapterFrameId());
                            f.setTranslation(cursor.getString(cursor.getColumnIndex("frame_text")), l);
                            f.save();
                        } else {
                            Log.d(TAG, "the frame or language could not be found");
                        }
                    } else {
                        Log.d(TAG, "the chapter could not be found "+cursor.getString(cursor.getColumnIndex("story")));
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "could note move the db cursor");
            }
        } else {
            Log.d(TAG, "The migration project could not be found");
        }
    }

    public interface OnProgressCallback {
        void onProgress(double progress, String message);
    }
}
