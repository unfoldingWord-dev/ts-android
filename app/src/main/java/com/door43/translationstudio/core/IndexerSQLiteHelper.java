package com.door43.translationstudio.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;

import com.door43.util.StringUtilities;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 10/1/2015.
 * TODO: these methods need to throw exeptions so we can log the error
 */
public class IndexerSQLiteHelper extends SQLiteOpenHelper{

//    private static final String TABLE_CATALOGS = "catalog";
    private static final String TABLE_FILES = "file";
    private static final String TABLE_LINKS = "link";
    private static final int DATABASE_VERSION = 1;
    private static final long ROOT_FILE_ID = 1;
    private final Context mContext;
    private final String mDatabaseName;

    public IndexerSQLiteHelper(Context context, String name) {
        super(context, name, null, DATABASE_VERSION);
        mContext = context;
        mDatabaseName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String fileTable = "CREATE TABLE `file` ( `file_id`  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, `name`  text NOT NULL, `parent_id` INTEGER NOT NULL DEFAULT 0, `catalog_hash`  text NOT NULL, `content` text, `is_dir`  INTEGER NOT NULL DEFAULT 0, UNIQUE (name, parent_id, catalog_hash) ON CONFLICT REPLACE, FOREIGN KEY (parent_id) REFERENCES file(file_id) ON DELETE CASCADE);";
        String linkTable ="CREATE TABLE `link` ( `name`  TEXT NOT NULL UNIQUE, `catalog_hash`  text NOT NULL, PRIMARY KEY(name));";
        db.execSQL(fileTable);
        db.execSQL(linkTable);

        // TRICKY: this root file must be added before the foreign key constraints are enabled
        replaceFile(db, "N/A", "root", "Do not remove me", ROOT_FILE_ID);

        // TRICKY: onConfigure is not available for API < 16
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    /**
     * TRICKY: this is only supported in API 16+
     * @param db
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(true);
        }
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

    /**
     * Creates or updates a link
     * @param db
     * @param md5hash
     * @param linkPath
     */
    public void replaceLink(SQLiteDatabase db, String md5hash, String linkPath) {
        String oldHash = readLink(db, linkPath);
        if(oldHash == null) {
            // insert new link
            ContentValues values = new ContentValues();
            values.put("name", linkPath);
            values.put("catalog_hash", md5hash);
            db.insert(IndexerSQLiteHelper.TABLE_LINKS, null, values);
        } else if(!oldHash.equals(md5hash)) {
            // update link
            ContentValues values = new ContentValues();
            values.put("catalog_hash", md5hash);
            String[] args = {linkPath};
            db.update(IndexerSQLiteHelper.TABLE_LINKS, values, "name=?", args);
        }
    }

    /**
     * Reads a catalog hash from a link
     * @param db
     * @param linkPath
     * @return returns the catalog hash
     */
    public String readLink(SQLiteDatabase db, String linkPath) {
        String[] columns = {"catalog_hash"};
        String[] args = {linkPath};
        Cursor cursor = db.query(TABLE_LINKS, columns, "name=?", args, null, null, null);
        String hash = null;
        if(cursor.getCount() > 0) {
            cursor.moveToNext();
            hash = cursor.getString(0);
        }
        cursor.close();
        return hash;
    }

    /**
     * Counts how many links there are to a catalog
     * @param db
     * @param hash
     */
    public long countCatalogLinks(SQLiteDatabase db, String hash) {
        String[] args = {hash};
        return DatabaseUtils.queryNumEntries(db, TABLE_LINKS, "catalog_hash=?", args);
    }

    /**
     * Deletes a link to a catalog.
     * If a catalog loses all of it's links the catalog will be deleted
     * @param db
     * @param linkPath
     */
    public void deleteLink(SQLiteDatabase db, String linkPath) {
        String hash = readLink(db, linkPath);
        String[] args = {linkPath};
        db.delete(TABLE_LINKS, "name=?", args);
        if(hash != null && countCatalogLinks(db, hash) < 1) {
            deleteCatalog(db, hash);
        }
    }

    /**
     * Deletes a catalog and all of it's related files
     * @param db
     * @param hash
     */
    public void deleteCatalog(SQLiteDatabase db, String hash) {
        String[] args = {hash};
//        db.delete(TABLE_CATALOGS, "hash=?", args);
        db.delete(TABLE_FILES, "catalog_hash=?", args);
        db.delete(TABLE_LINKS, "catalog_hash=?", args);
    }

    /**
     * Creates or updates a file
     * @param db
     * @param hash
     * @param path
     * @param contents
     */
    public void replaceFile(SQLiteDatabase db, String hash, String path, String contents) {
        replaceFile(db, hash, path, contents, ROOT_FILE_ID);
    }

    /**
     * Recursive method to build file structure
     * @param db
     * @param hash
     * @param name
     * @param contents
     * @param parent
     */
    private void replaceFile(SQLiteDatabase db, String hash, String name, String contents, long parent) {
        String[] components = StringUtilities.ltrim(name.trim(), '/').split("/", 2);

        ContentValues values = new ContentValues();
        values.put("name", components[0]);
        values.put("parent_id", parent);
        values.put("catalog_hash", hash);
        if(components.length > 1 && !components[1].trim().isEmpty()) {
            values.put("is_dir", 1);
        } else {
            values.put("is_dir", 0);
            values.put("content", contents);
        }

        // check if file exists
        String[] args = {components[0], hash};
        String[] columns = {"file_id"};
        Cursor cursor = db.query(TABLE_FILES, columns, "name=? AND parent_id=" + parent + " AND catalog_hash=?", args, null, null, null);
        long id;
        if(cursor.moveToFirst()) {
            id = cursor.getLong(0);
            // update file
            db.update(IndexerSQLiteHelper.TABLE_FILES, values, "name=? AND parent_id=" + parent + " AND catalog_hash=?", args);
        } else {
            // insert new file
            id = db.insert(IndexerSQLiteHelper.TABLE_FILES, null, values);
        }
        cursor.close();

        if(components.length > 1 && !components[1].trim().isEmpty()) {
            replaceFile(db, hash, components[1].trim(), contents, id);
        }
    }

    /**
     * Returns the contents of a file
     * @param db
     * @param hash
     * @param path
     * @return
     */
    public String readFile(SQLiteDatabase db, String hash, String path) {
        long fileId = findFile(db, hash, path, ROOT_FILE_ID);
        String content = null;
        if(fileId > 0) {
            String[] columns = {"content", "is_dir"};
            Cursor cursor = db.query(IndexerSQLiteHelper.TABLE_FILES, columns, "file_id=" + fileId, null, null, null, null);
            if(cursor.moveToFirst()) {
                int isDir = cursor.getInt(1);
                if(isDir == 0) {
                    content = cursor.getString(0);
                }
            }
            cursor.close();
        }
        return content;
    }

    /**
     * Removes a file
     * @param db
     * @param hash
     * @param path
     */
    public void deleteFile(SQLiteDatabase db, String hash, String path) {
        long fileId = findFile(db, hash, path, ROOT_FILE_ID);
        if(fileId > 0) {
            db.delete(TABLE_FILES, "file_id=" + fileId, null);
        }
    }

    /**
     * Returns an array of files in the directory
     * @param db
     * @param hash
     * @param path if null the entire catalog is listed
     * @param extensionFilters an array of extensions to skip
     * @return
     */
    public String[] listDir(SQLiteDatabase db, String hash, String path, String[] extensionFilters) {
        long fileId = 0;
        boolean listCatalog = false;
        if(path != null) {
            fileId = findFile(db, hash, path, ROOT_FILE_ID);
        } else {
            listCatalog = true;
        }
        if(listCatalog || fileId > 0) {
            String[] columns = {"name"};
            Cursor cursor;
            if(listCatalog) {
                String[] args = {hash};
                cursor = db.query(IndexerSQLiteHelper.TABLE_FILES, columns, "catalog_hash=? AND parent_id="+ROOT_FILE_ID, args, null, null, "name");
            } else {
                cursor = db.query(IndexerSQLiteHelper.TABLE_FILES, columns, "parent_id=" + fileId, null, null, null, "name");
            }
            List<String> files = new ArrayList<>();
            if(cursor.moveToFirst()) {
                while(!cursor.isAfterLast()) {
                    String name = cursor.getString(0);
                    String ext = FilenameUtils.getExtension(name);
                    boolean skip = false;
                    for(String filtered:extensionFilters) {
                        if(ext.equals(filtered)) {
                            skip = true;
                            break;
                        }
                    }
                    if(!skip) {
                        files.add(name);
                    }
                    cursor.moveToNext();
                }
            }
            cursor.close();
            return files.toArray(new String[files.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Returns an array of contents for a file found in each directory.
     * For example. if you have directorys 01, 02, and 03 each containing a file "myfile.json"
     * this method will list the contents of reach "myfile.json" ordered by directory name.
     * @param db
     * @param hash
     * @param path
     * @param file the name of the file who's contents will be returned
     * @return
     */
    public String[] listDirFileContents(SQLiteDatabase db, String hash, String path, String file) {
        long fileId = 0;
        boolean listCatalog = false;
        if(path != null) {
            fileId = findFile(db, hash, path, ROOT_FILE_ID);
        } else {
            listCatalog = true;
        }
        if(listCatalog || fileId > 0) {
            Cursor cursor;
            if(listCatalog) {
                String[] args = {file, hash};
                String query = "SELECT f.content FROM file AS f"
                        + " LEFT JOIN file AS parent ON parent.file_id=f.parent_id"
                        + " WHERE f.is_dir=0 AND f.name=? AND f.parent_id IN ("
                        + "SELECT file_id FROM file WHERE catalog_hash=? AND parent_id="+ROOT_FILE_ID+") ORDER BY parent.name";
                cursor = db.rawQuery(query, args);
            } else {
                String[] args = {file};
                String query = "SELECT f.content FROM file AS f"
                        + " LEFT JOIN file AS parent ON parent.file_id=f.parent_id"
                        + " WHERE f.is_dir=0 AND f.name=? AND f.parent_id IN ("
                        + "SELECT file_id FROM file WHERE parent_id="+fileId+") ORDER BY parent.name";
                cursor = db.rawQuery(query, args);
            }
            List<String> contentsList = new ArrayList<>();
            if(cursor.moveToFirst()) {
                while(!cursor.isAfterLast()) {
                    contentsList.add(cursor.getString(0));
                    cursor.moveToNext();
                }
            }
            cursor.close();
            return contentsList.toArray(new String[contentsList.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Returns an array of file contents in the directory.
     * This is exactly like listDir except rather than returning the file names it returns the file contents
     * @param db
     * @param hash
     * @param path if null the entire catalog is listed
     * @param extensionFilters an array of extensions to skip
     * @return
     */
    public String[] listDirContents(SQLiteDatabase db, String hash, String path, String[] extensionFilters) {
        long fileId = 0;
        boolean listCatalog = false;
        if(path != null) {
            fileId = findFile(db, hash, path, ROOT_FILE_ID);
        } else {
            listCatalog = true;
        }
        if(listCatalog || fileId > 0) {
            String[] columns = {"name", "content"};
            Cursor cursor;
            if(listCatalog) {
                String[] args = {hash};
                cursor = db.query(IndexerSQLiteHelper.TABLE_FILES, columns, "catalog_hash=? AND parent_id=" + ROOT_FILE_ID, args, null, null, "name");
            } else {
                cursor = db.query(IndexerSQLiteHelper.TABLE_FILES, columns, "parent_id=" + fileId, null, null, null, "name");
            }
            List<String> contentsList = new ArrayList<>();
            if(cursor.moveToFirst()) {
                while(!cursor.isAfterLast()) {
                    String name = cursor.getString(0);
                    String contents = cursor.getString(1);
                    String ext = FilenameUtils.getExtension(name);
                    boolean skip = false;
                    for(String filtered:extensionFilters) {
                        if(ext.equals(filtered)) {
                            skip = true;
                            break;
                        }
                    }
                    if(!skip) {
                        contentsList.add(contents);
                    }
                    cursor.moveToNext();
                }
            }
            cursor.close();
            return contentsList.toArray(new String[contentsList.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Locates a file
     * @param db
     * @param hash
     * @param path
     * @param parent
     * @return the file id or 0 if no file was found
     */
    private long findFile(SQLiteDatabase db, String hash, String path, long parent) {
        String[] components = StringUtilities.ltrim(path.trim(), '/').split("/", 2);
        String name = components[0].trim();

        String[] columns = {"file_id"};
        String[] selectionArgs = {hash, name};
        Cursor cursor = db.query(IndexerSQLiteHelper.TABLE_FILES, columns, "catalog_hash=? AND parent_id="+parent+" AND name=?", selectionArgs, null, null, null);
        if(cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            cursor.close();

            if(components.length > 1 && !components[1].trim().isEmpty()) {
                // continue searching
                return findFile(db, hash, components[1].trim(), id);
            } else {
                return id;
            }
        } else {
            cursor.close();
            return 0;
        }
    }

    /**
     * Imports the catalog from the source database into this database
     * @param db
     * @param sourceDatabaseAlias
     * @param sourceHash
     * @param targetHash
     */
    public void migrateCatalog(SQLiteDatabase db, String sourceDatabaseAlias, String sourceHash, String targetHash) {
        migrateCatalog(db, sourceDatabaseAlias, sourceHash, targetHash, ROOT_FILE_ID);
    }

    /**
     * Recursively migrates the contents of a catalog in the source database to this database
     * @param db
     * @param sourceDatabaseAlias
     * @param sourceHash
     * @param targetHash
     * @param parent
     */
    private void migrateCatalog(SQLiteDatabase db, String sourceDatabaseAlias, String sourceHash, String targetHash, long parent) {
        String query = "SELECT name, content, is_dir FROM "+sourceDatabaseAlias+".file WHERE catalog_hash=? AND parent_id="+parent;
        Cursor cursor = db.rawQuery(query, new String[]{sourceHash});
        cursor.moveToFirst();
        SQLiteStatement stmt = db.compileStatement("INSERT INTO file (name, parent_id, catalog_hash, content, is_dir) VALUES (?, ?, ?, ?, ?)");
        while(!cursor.isAfterLast()) {
            long isDir = cursor.getLong(2);
            String content = cursor.getString(1);
            if(content == null) {
                content = "";
            }
            stmt.bindString(1, cursor.getString(0));
            stmt.bindLong(2, parent);
            stmt.bindString(3, targetHash);
            stmt.bindString(4, content);
            stmt.bindLong(5, isDir);
            long id = stmt.executeInsert();
            if(isDir == 1) {
                // recurs directories
                migrateCatalog(db, sourceDatabaseAlias, sourceHash, targetHash, id);
            }
            cursor.moveToNext();
        }
        cursor.close();
    }
}
