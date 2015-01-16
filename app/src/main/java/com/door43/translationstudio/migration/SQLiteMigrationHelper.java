package com.door43.translationstudio.migration;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.util.MainContext;

import java.io.File;

/**
 * This class handles the db migration from 1.x to 2.x.
 */
public class SQLiteMigrationHelper extends SQLiteOpenHelper {
    private static final ProjectManager mProjectManager = MainContext.getContext().getSharedProjectManager();
    // these percents are just guestimates for how much work will be required to migrate. They don't have to be exact.
    private final double PERCENT_FRAMES = 80.0;
    private final double PERCENT_CHAPTERS = 20.0;
    private double mProgress = 0;
    private static MainApplication mContext;
    private static final String TAG = "MigrationSQL";
    private static String mDatabasePath;
    private static String mInfoDatabasePath;

    public SQLiteMigrationHelper(MainApplication context, String applicationDirectory) {
        super(context, "Database", null, 1);
        mContext = context;
        mDatabasePath = applicationDirectory + "/app_webview/databases/file__0/1";
        mInfoDatabasePath = applicationDirectory + "/app_webview/Local Storage/file__0.localstorage";
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
        if(mProjectManager.getSelectedProject() == null || !mProjectManager.getSelectedProject().getId().equals(p.getId())) {
            // load the project silently
            mProjectManager.fetchProjectSource(p, false);
        }

        if(p != null) {
            int selectedTargetLanguageDBId = -1;
            int selectedFrameDBId = -1;
            int selectedChapterDBId = -1;

            // load the translations
            if(!new File(mDatabasePath).exists()) {
                Log.d(TAG, "The database could not be found at "+mDatabasePath);
                return;
            }
            SQLiteDatabase db = SQLiteDatabase.openDatabase(mDatabasePath, null, SQLiteDatabase.OPEN_READONLY);

            // fetch the selected target language and last frame and chapter
            if(new File(mInfoDatabasePath).exists()) {
                SQLiteDatabase infoDB = SQLiteDatabase.openDatabase(mInfoDatabasePath, null, SQLiteDatabase.OPEN_READONLY);
                String query = "SELECT key, CAST(value AS TEXT) AS value FROM ItemTable";
                Cursor cursor = infoDB.rawQuery(query, null);
                if(cursor.moveToFirst()) {
                    // find preferences
                    do {
                        String key = cursor.getString(cursor.getColumnIndex("key"));
                        if(key.equals("agree_to_terms")) {
                            String value = cursor.getString(cursor.getColumnIndex("value"));
                            mContext.setHasAcceptedTerms(value.equals("YES"));
                        } else if(key.equals("selected_target_language_id")) {
                            int value = cursor.getInt(cursor.getColumnIndex("value"));
                            selectedTargetLanguageDBId = value;
                        } else if(key.equals("translation_rtl")) {
                            // TODO: we don't offer rtl support yet.
                        } else if(key.equals("book")) {
                            // we know this will always be 'obs' in 1.x
                        } else if(key.equals("frame")) {
                            int value = cursor.getInt(cursor.getColumnIndex("value"));
                            if(value <= 0) value = 1;
                            selectedFrameDBId = value;
                        } else if(key.equals("story")) {
                            int value = cursor.getInt(cursor.getColumnIndex("value"));
                            if(value <= 0) value = 1;
                            selectedChapterDBId = value;
                        }
                    } while(cursor.moveToNext());

                    // load selected language
                    query = "SELECT language_code FROM languages WHERE id = '"+selectedTargetLanguageDBId+"'";
                    cursor = db.rawQuery(query, null);
                    if(cursor.moveToFirst()) {
                        p.setSelectedTargetLanguage(cursor.getString(0));
                    }
                    // load selcted chapter
                    query = "SELECT story FROM stories WHERE id = '"+selectedChapterDBId+"'";
                    cursor = db.rawQuery(query, null);
                    if(cursor.moveToFirst()) {
//                        mContext.setActiveChapter(cursor.getString(0));
                        p.setSelectedChapter(cursor.getString(0));
                    }
                    // load selcted frame
                    query = "SELECT frame FROM frames WHERE id = '"+selectedFrameDBId+"'";
                    cursor = db.rawQuery(query, null);
                    if(cursor.moveToFirst()) {
//                        mContext.setActiveFrame(cursor.getString(0));
                        p.getSelectedChapter().setSelectedFrame(cursor.getString(0));
                    }

                    // we disable the welcome because we want them to see the last active frame they had.
                    mContext.setShouldShowWelcome(false);
                }
            }

            // migrate the stories
            String query = "SELECT s.story, s.story_title, s.story_ref, l.language_code FROM stories AS s LEFT JOIN languages AS l ON l.id=s.language_id WHERE s.source=0";
            Cursor cursor = db.rawQuery(query, null);
            int numRows = cursor.getCount();
            if (cursor.moveToFirst()) {
                do {
                    mProgress += PERCENT_CHAPTERS / numRows;
                    Chapter c = p.getChapter(cursor.getString(cursor.getColumnIndex("story")));
                    Language l = mProjectManager.getLanguage(cursor.getString(cursor.getColumnIndex("language_code")));
                    if(c != null && l != null) {
                        callback.onProgress(mProgress, "Migrating "+c.getId());
                        c.setTitleTranslation(cursor.getString(cursor.getColumnIndex("story_title")), l);
                        c.setReferenceTranslation(cursor.getString(cursor.getColumnIndex("story_ref")), l);
                        c.save();
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

            // migrate the selected language

        } else {
            Log.d(TAG, "The migration project could not be found");
        }
    }

    public interface OnProgressCallback {
        void onProgress(double progress, String message);
    }
}
