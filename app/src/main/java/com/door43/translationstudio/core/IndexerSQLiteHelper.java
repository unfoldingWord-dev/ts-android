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

import java.io.IOException;
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
    private static final int DATABASE_VERSION = 2;
    private static final long ROOT_FILE_ID = 1;
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
        db.execSQL(mSchema);

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
        if(oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS `file`");
            db.execSQL("DROP TABLE IF EXISTS `link`");
        }

        onCreate(db);
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
    public void addProject(SQLiteDatabase db, String slug, int sort, int dateModified, String sourceLanguageCatalogUrl, String[] categorySlugs) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("sort", sort);
        values.put("modified_at", dateModified);
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
    public void addSourceLanguage(SQLiteDatabase db, String slug, long projectId, String name, String projectName, String projectDescription, String direction, int dateModified, String resourceCatalogUrl, String[] categoryNames) {
        ContentValues values = new ContentValues();
        values.put("slug", slug);
        values.put("project_id", projectId);
        values.put("name", name);
        values.put("project_name", projectName);
        values.put("project_description", projectDescription);
        values.put("direction", direction);
        values.put("modified_at", dateModified);
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
    }

    /**
     * Adds the names for categories
     * @param db
     * @param sourceLanguageId
     * @param categoryNames
     */
    public void addSourceLanguageCategories(SQLiteDatabase db, long projectId, long sourceLanguageId, String[] categoryNames) {
        if(categoryNames != null && categoryNames.length > 0) {
            Cursor cursor = db.rawQuery("SELECT `id` from `category` AS `c`"
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
     * @param slug
     */
    public void deleteSourceLanguage(SQLiteDatabase db, String slug, long projectId) {
        db.delete("source_language", "`slug`=? AND `project_id`=" + projectId, new String[]{slug});
    }

    /**
     * Creates or updates a link
     * @param db
     * @param md5hash
     * @param linkPath
     */
    @Deprecated
    public void replaceLink(SQLiteDatabase db, String md5hash, String linkPath) {
        String oldHash = readLink(db, linkPath);
        if(oldHash == null) {
            // insert new link
            ContentValues values = new ContentValues();
            values.put("name", linkPath);
            values.put("catalog_hash", md5hash);
            db.insertOrThrow(IndexerSQLiteHelper.TABLE_LINKS, null, values);
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
            id = db.insertOrThrow(IndexerSQLiteHelper.TABLE_FILES, null, values);
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
}
