package com.door43.translationstudio;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.provider.DocumentFile;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Util;
import com.door43.util.StorageUtils;
import com.door43.util.StringUtilities;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides global access to the application context as well as other important tools
 */
public class AppContext {
    private static final String PREFERENCES_NAME = "com.door43.translationstudio.general";
    private static final String DEFAULT_LIBRARY_ZIP = "library.zip";
    private static final String TARGET_TRANSLATIONS_DIR = "translations";
    public static final String PROFILES_DIR = "profiles";
    public static final String DOWNLOAD_FOLDER = "/Download";
    public static final String DOWNLOAD_TRANSLATION_STUDIO_FOLDER = DOWNLOAD_FOLDER + "/translationStudio";
    private static MainApplication mContext;
    public static final Bundle args = new Bundle();
    private static boolean loaded;
    private static String sdCardPath = "";
    private static boolean alreadyReadSdCardDirectory = false;
    private static String verifiedSdCardPath = "";
    public static final int REQUEST_CODE_STORAGE_ACCESS = 42;

    /**
     * Initializes the basic functions context.
     * It is very important to initialize the class before using it because it assumes
     * the context has already been set.
     * @param context The application context. This can only be set once.
     */
    public AppContext(MainApplication context) {
        mContext = context;
    }

    /**
     * Returns an instance of the library
     * @return
     */
    public static Library getLibrary() {
        // NOTE: rather than keeping the library around we rebuild it so that changes to the user settings will work
        String server = mContext.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, mContext.getResources().getString(R.string.pref_default_media_server));
        String rootApiUrl = server + mContext.getResources().getString(R.string.root_catalog_api);
        try {
            return new Library(mContext, rootApiUrl);
        } catch (IOException e) {
            Logger.e(AppContext.class.getName(), "Failed to create the library", e);
        }
        return null;
    }

    /**
     * Checks if the device is a tablet
     * @return
     */
    public static boolean isTablet() {
        return (mContext.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Deploys the default index and the target languages
     * The
     * @throws Exception
     */
    public static void deployDefaultLibrary() throws Exception {
        Library library = getLibrary();
        File archive = mContext.getCacheDir().createTempFile("index", ".zip");
        Util.writeStream(mContext.getAssets().open(DEFAULT_LIBRARY_ZIP), archive);
        File tempLibraryDir = new File(mContext.getCacheDir(), System.currentTimeMillis() + "");
        tempLibraryDir.mkdirs();
        Zip.unzip(archive, tempLibraryDir);
        File[] dbs = tempLibraryDir.listFiles();
        if(dbs.length == 1) {
            library.deploy(dbs[0]);
        } else {
            FileUtils.deleteQuietly(archive);
            FileUtils.deleteQuietly(tempLibraryDir);
            throw new Exception("Invalid index count in '" + DEFAULT_LIBRARY_ZIP + "'. Expecting 1 but found " + dbs.length);
        }

        // clean up
        FileUtils.deleteQuietly(archive);
        FileUtils.deleteQuietly(tempLibraryDir);
    }

    /**
     * Returns an instance of the translator
     * @return
     */
    public static Translator getTranslator() {
        return new Translator(mContext, new File(mContext.getFilesDir(), TARGET_TRANSLATIONS_DIR));
    }

    /**
     * Checks if the package asset exists
     * @param path
     * @return
     */
    public static boolean assetExists(String path) {
        try {
            mContext.getAssets().open(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns the main application context
     * @return
     */
    public static MainApplication context() {
        return mContext;
    }

    /**
     * Returns the unique device id for this device
     * @return
     */
    public static String udid() {
        return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Returns the directory in which the ssh keys are stored
     * @return
     */
    public static File getKeysFolder() {
        File folder = new File(mContext.getFilesDir() + "/" + mContext.getResources().getString(R.string.keys_dir) + "/");
        if(!folder.exists()) {
            folder.mkdir();
        }
        return folder;
    }

    /**
     * get external storage folder - may not be mounted
     * @return
     */
    private static File getLegacyExternalStorageDirectory() {
        String path = System.getenv("EXTERNAL_STORAGE");
        return new File(path);
    }


    /**
     * Checks if the external media is mounted and writeable
     * @return
     */
    public static boolean isSdCardAccessable() {
        // TRICKY: KITKAT introduced changes to the external media that made sd cards read only,
        //      and now starting with Lollipop the user has to grant access permission
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            File sdCard = getSdCardDirectory();
            return sdCard != null;
        } else {
            DocumentFile sdCard = sdCardMkdirs(null);
            return sdCard != null;
        }
    }

    /**
     * Searches and verifies write location on SD card
     * @return
     */
    public static String findSdCardFolder() {

        if(!verifiedSdCardPath.isEmpty()) {
            return verifiedSdCardPath;
        }

        String[] mounts = getExternalDirectories();
        String sdPath = null;

        final String testFolder = "__testing.dir__";

        if(null == mounts) {
            return null;
        }

        DocumentFile sdCardTempFolder = sdCardMkdirs(testFolder);
        boolean success = sdCardTempFolder != null;
        if (success) {
            if (sdCardTempFolder.canWrite()) {
                DocumentFile file = sdCardTempFolder.createFile("text/plain", "_zzztestzzz_.txt"); // make sure URI write accessable
                String testData = "test Data";
                success = documentFolderWrite(file, testData, false); // make sure we can write
                if(file.length() < testData.length()) {
                    success = false;
                }
                file.delete(); // cleanup after use

                try {
                    if(success) {
                        if (mounts.length > 0) {

                            for (String mount : mounts) {

                                final String externalStorageState = EnvironmentCompat.getStorageState(new File(mount));
                                boolean mounted = Environment.MEDIA_MOUNTED.equals(externalStorageState);
                                if (!mounted) { // do a double check
                                    continue;
                                }

                                File testFolderFile = new File(mount, testFolder);
                                boolean isPresent = testFolderFile.exists();
                                if (isPresent) {
                                    Logger.i(AppContext.class.toString(), "found folder: " + testFolderFile.toString());
                                    sdPath = mount;
                                    break;
                                }
                            } // end for mounts
                        }

                        if (null == sdPath) {
                            Logger.i(AppContext.class.toString(), "SD card folder not found");
                        } else {
                            verifiedSdCardPath = sdPath;
                        }
                    }

                    sdCardTempFolder.delete(); // remove test folder

                } catch (Exception e) {
                    Logger.w(AppContext.class.toString(),"Error getting external card folder", e);
                }
            }
        }
        return sdPath;
    }

    /**
     * write string to document file
     * @param document - document to write
     * @param data - text to write to file
     * @param append - if true then data is appended to file, if false then it is overwritten
     * @return
     */
    public static boolean documentFolderWrite(final DocumentFile document, final String data, final boolean append) {

        boolean success = true;
        OutputStream fout = null;

        try {
            fout = mContext.getContentResolver().openOutputStream(document.getUri());
            fout.write(data.getBytes());
            fout.close();
        } catch (Exception e) {
            Logger.i(AppContext.class.getName(), "Could not write to folder");
            success = false; // write failed
        } finally {
            IOUtils.closeQuietly(fout);
        }

        return success;
    }

    /**
     * creates and returns the selected subfolder on SD card.  Returns null if error.
     * @param subFolderName - name of subfolder to move to
     * @return
     */
    public static DocumentFile sdCardMkdirs(final String subFolderName) {

        String sdCardFolderUriStr = getSdCardAccessUriStr();
        if(null == sdCardFolderUriStr) {
            return null;
        }

        Uri sdCardFolderUri = Uri.parse(sdCardFolderUriStr);
        DocumentFile document = DocumentFile.fromTreeUri(mContext, sdCardFolderUri);
        DocumentFile subDocument = documentFileMkdirs(document, subFolderName);
        if ( (subDocument != null) && subDocument.isDirectory() && subDocument.canWrite() ) {
            return subDocument;
        }

        return null;
    }

    /**
     * creates and returns the selected subfolder.  Returns null if error.
     * @param baseFolder - base folder
     * @param subFolderName - name of subfolder to move to
     * @return
     */
    public static DocumentFile documentFileMkdirs(final DocumentFile baseFolder, final String subFolderName) {
        return traverseSubDocFolders(baseFolder, subFolderName, true);
    }

    /**
     * get the selected subfolder recursively or null if not present
     * @param baseFolder - base folder
     * @param subFolderName - name of subfolder to move to
     * @return
     */
    public static DocumentFile documentFileChgdirs(final DocumentFile baseFolder, final String subFolderName) {
        return traverseSubDocFolders(baseFolder, subFolderName, false);
    }

    /**
     * get the selected subfolder recursively or null if error
     * @param baseFolder - base folder
     * @param subFolderName - name of subfolder to move to
     * @param createFolders - if true then missing folders will be created
     * @return
     */
    private static DocumentFile traverseSubDocFolders( DocumentFile baseFolder, final String subFolderName, boolean createFolders) {
        if(null == baseFolder) {
            return null;
        }

        if(subFolderName != null) {
            String[] parts = subFolderName.split("\\/");
            if (parts.length < 1) {
                return null;
            }

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].isEmpty()) { // skip over extraneous slashes
                    continue;
                }

                DocumentFile nextDocument = documentFolderChgdir(baseFolder, parts[i], createFolders);
                if (null == nextDocument) {
                    return null;
                }

                baseFolder = nextDocument;
            }
        }

        return baseFolder;
    }

    /**
     * get the selected subfolder or null if error
     * @param baseFolder - base folder
     * @param subFolderName - name of subfolder to move to
     * @param createFolders - if true then missing folders will be created
     * @return
     */
    private static DocumentFile documentFolderChgdir(final DocumentFile baseFolder, final String subFolderName, boolean createFolders) {

        if(baseFolder == null) {
            return null;
        }

        DocumentFile nextDocument = baseFolder.findFile(subFolderName);
        try {

            if( (nextDocument == null) && createFolders) {
                nextDocument = baseFolder.createDirectory(subFolderName);
            }

        } catch (Exception e) {
            Logger.w(AppContext.class.getName(),"Failed to create folder", e);
            return null;
        }

        return nextDocument;
    }

    /**
     * find first instance of file in folder or null if not found
     * @param folder - folder to search
     * @param fileName - filename to find
     * @return
     */
    public static DocumentFile documentFileFind(final DocumentFile folder, final String fileName) {

        if(folder == null) {
            return null;
        }

        DocumentFile nextDocument = folder.findFile(fileName);
        return nextDocument;
    }

    /**
     * finds first instance of file type in folder
     * @param baseFolder - base folder for search
     * @param extension - extension to match
     * @return
     */
    public static String searchFolderAndParentsForDocFile(final DocumentFile baseFolder, final String extension) {

        if(null == baseFolder) {
            return null;
        }

        String[] paths = new String[]{ DOWNLOAD_TRANSLATION_STUDIO_FOLDER, DOWNLOAD_FOLDER, ""}; // try in this order

        for(String path: paths) {
            DocumentFile document = AppContext.documentFileChgdirs(baseFolder, path);
            if (null == document) {
                continue;
            }

            DocumentFile[] files = document.listFiles();
            for(DocumentFile file: files) {
                String ext = FilenameUtils.getExtension(file.getName());
                if(ext != null) {
                    if (ext.toLowerCase().equals(extension)) {
                        if ((file != null) && file.canRead()) {
                            return path;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns true if an external SD card is present and writeable on Android Lollipop or greater
     * @return
     */
    public static boolean isSdCardPresentLollipop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!verifiedSdCardPath.isEmpty()) { // see if we already have detected an SD card this session
                return true;
            }

            // see if we already have enabled access
            if( getSdCardAccessUriStr() != null) { // if user has already chosen a path for SD card
                if(sdCardMkdirs(null) != null) { // verify card is still present and writeable
                    return true;
                }
            }

            // rough check to see if SD card is currently present
            File sdCard = getSdCardDirectory();
            if(sdCard != null) {
                return true;
            }
        }

        return false;
    }

    /**
    * Returns the SD card directory.  Warning this may not be writeable.
     * Just a rough check to see if one is possibly present.
    * @return
    */
    public static File getSdCardDirectory() {

        if (!verifiedSdCardPath.isEmpty()) {
            return new File(verifiedSdCardPath);
        }

        if(alreadyReadSdCardDirectory) {
            if (!sdCardPath.isEmpty()) {
                return new File(sdCardPath);
            } else {
                return null;
            }
        }

        alreadyReadSdCardDirectory = true;

        String[] mounts = getExternalDirectories();

        try {
            if( (mounts != null) && (mounts.length > 0)) {
                String path = null;
                for(String mount:mounts) {
                    File mountFile = new File(mount);
                    String state = EnvironmentCompat.getStorageState(mountFile);
                    boolean mounted = Environment.MEDIA_MOUNTED.equals(state);
                    if(mounted) {
                        if(mount.toLowerCase().indexOf("emulated") < 0) {
                            path = mount;
                            break;
                        }
                    }
                }
                if(path != null) {
                    File absolute = new File(path).getCanonicalFile();
                    sdCardPath = absolute.toString(); // cache value
                    return absolute;
                }
                sdCardPath = "";
            }
        } catch (Exception e) {
            Logger.w(AppContext.class.toString(),"Error getting external card folder", e);
        }
        return null;
    }

    /**
     * Returns list of all the external directories.  Requires additional checking to see if any of
     * these belong to external SD card.
     * @return
     */
    public static String[] getExternalDirectories() {

        List<String> mounts = new ArrayList<>();
        try {
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec("mount");
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            String line;
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                if (line.contains("secure")) continue;
                if (line.contains("asec")) continue;

//                Logger.i(AppContext.class.getName(),"Checking: " + line);

                if (line.contains("fat")) {//TF card
                    String columns[] = line.split(" ");
                    if (columns != null && columns.length > 1) {
                        mounts.add(0,columns[1]);
                        Logger.i(AppContext.class.getName(), "Adding: " + columns[1]);
                    }
                } else if (line.contains("fuse")) {//internal storage
                    String columns[] = line.split(" ");
                    if (columns != null && columns.length > 1) {
                        mounts.add(columns[1]);
                        Logger.i(AppContext.class.getName(), "Adding: " + columns[1]);
                    }
                }
            }

            return mounts.toArray(new String[mounts.size()]);

        } catch (Exception e) {
            Logger.w(AppContext.class.toString(),"Error getting external card folder", e);
        }

        return null;
    }

    /**
     * returns true if we need to enable SD card access
     */
    public static boolean doWeNeedToRequestSdCardAccess() {
        Logger.i(AppContext.class.getName(), "version API: " + Build.VERSION.SDK_INT);
        Logger.i(AppContext.class.getName(), "Environment.getExternalStorageDirectory(): " + Environment.getExternalStorageDirectory());
        Logger.i(AppContext.class.getName(), "Environment.getExternalStorageState(): " + Environment.getExternalStorageState());

        restoreSdCardWriteAccess(); // only does something if supported on device
        if (!isSdCardAccessable()) { // if accessable, we do not need to request access
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // can only request access if lollipop or greater
                if( isSdCardPresentLollipop() ) {
                    return true; // if there is an SD card present, is there any point in requesting access
                }
            }
        }

        return false;
    }


    /**
     * if available, this triggers browser dialog for user to select SD card folder to allow access
     * @param context
     */
    public static void triggerStorageAccessFramework(final Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // doesn't look like this is possible on Kitkat
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            context.startActivityForResult(intent, REQUEST_CODE_STORAGE_ACCESS);
        } else {
            Logger.w(AppContext.class.toString(),"triggerStorageAccessFramework: not supported for " + Build.VERSION.SDK_INT);
        }
    }

    /**
     * persists write permission for SD card access
     * @param sdUri - uri to persist
     * @param flags - permission flags
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean validateSdCardWriteAccess(final Uri sdUri, final int flags) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return true;
        }

        boolean success = persistSdCardWriteAccess(sdUri, flags);

        String sdCardActualFolder = findSdCardFolder();
        if(sdCardActualFolder != null) {
            Logger.i(AppContext.class.getName(), "found card at = " + sdCardActualFolder);
        } else {
            Logger.i(AppContext.class.getName(), "invalid access Uri = " + sdUri);
            storeSdCardAccess(null, 0); // clear value since invalid
            success = false;
        }

        return success;
    }

    /**
     * persists write permission for SD card access
     * @param sdUri - uri to persist
     * @param flags - permission flags
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean persistSdCardWriteAccess(final Uri sdUri, final int flags) {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return true;
        }

        storeSdCardAccess(sdUri, flags);

        restoreSdCardWriteAccess(); // apply settings

        boolean success = isSdCardAccessable();
        if(!success) {
            storeSdCardAccess(null, 0); // clear value since invalid
        }
        return success;
    }

    private static void storeSdCardAccess(Uri sdUri, int flags) {
        String uriStr = (null == sdUri) ? null : sdUri.toString();
        AppContext.setUserString(SettingsActivity.KEY_SDCARD_ACCESS_URI, uriStr);
        AppContext.setUserString(SettingsActivity.KEY_SDCARD_ACCESS_FLAGS, String.valueOf(flags));
        Logger.i(AppContext.class.getName(), "URI = " + sdUri);
        verifiedSdCardPath = ""; // reset persisted path to SD card, will need to find it again
    }

    /**
     * restores previously granted write permission for SD card access
     * @return
     */
    public static boolean restoreSdCardWriteAccess() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String flagStr = AppContext.getUserString(SettingsActivity.KEY_SDCARD_ACCESS_FLAGS, null);
            String path = AppContext.getUserString(SettingsActivity.KEY_SDCARD_ACCESS_URI, null);
            if ((path != null) && (flagStr != null)) {

                Integer flags = Integer.parseInt(flagStr);
                Uri sdUri = Uri.parse(path);
                Logger.i(AppContext.class.getName(), "Restore URI = " + sdUri.toString());
                applyPermissions(sdUri, flags);
                return true;
            }
        }
        return false;
    }

    public static boolean applyPermissions(Uri sdUri, Integer flags) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Logger.i(AppContext.class.getName(), "Apply permissions to URI '" + sdUri.toString() + "' flags: " + flags);
            int takeFlags = flags
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            mContext.grantUriPermission(mContext.getPackageName(), sdUri, takeFlags); //TODO 12/22/2015 need to find way to remove this warning
            mContext.getContentResolver().takePersistableUriPermission(sdUri, takeFlags);
            return true;
        }
        return false;
    }

    /**
     * reads the stored URI for SD card access
     * @return
     */
    public static String getSdCardAccessUriStr() {
        String path = AppContext.getUserString(SettingsActivity.KEY_SDCARD_ACCESS_URI, null);
        return path;
    }

    /**
     * gets the SD card downloads folder
     * @return
     */
    public static DocumentFile getSdCardDownloadsFolder() {
        DocumentFile downloadFolder = sdCardMkdirs( DOWNLOAD_FOLDER);
        return downloadFolder;
    }

    /**
     * Returns the external public downloads directory
     * @return
     */
    public static File getPublicDownloadsDirectory() {
        File dir;
        // TRICKY: KITKAT introduced changes to the external media that made sd cards read only
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // || Root.isDeviceRooted()
            StorageUtils.StorageInfo removeableMediaInfo = StorageUtils.getRemoveableMediaDevice();
            if(removeableMediaInfo != null) {
                dir = new File("/storage/" + removeableMediaInfo.getMountName() + DOWNLOAD_TRANSLATION_STUDIO_FOLDER);
            } else {
                dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "translationStudio");
            }
        } else {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "translationStudio");
        }
        dir.mkdirs();
        return dir;
    }

    /**
     * Returns the public downloads directory
     * @return
     */
    public static File getPublicDirectory() {
        File dir = new File(Environment.getExternalStorageDirectory(), "translationStudio");
        dir.mkdirs();
        return dir;
    }

    /**
     * Checks if the app
     * @return
     */
    public static boolean isLoaded() {
        return loaded;
    }

    public static void setLoaded(boolean loaded) {
        AppContext.loaded = loaded;
    }

    /**
     * Sets the last opened view mode for a target translation
     * @param targetTranslationId
     * @param viewMode
     */
    public static void setLastViewMode(String targetTranslationId, TranslationViewMode viewMode) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_view_mode_" + targetTranslationId, viewMode.toString());
        editor.apply();
    }

    /**
     * Returns the last view mode of the target translation.
     * The default view mode will be returned if there is no recorded last view mode
     *
     * @param targetTranslationId
     * @return
     */
    public static TranslationViewMode getLastViewMode(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        TranslationViewMode viewMode = TranslationViewMode.get(prefs.getString("last_view_mode_" + targetTranslationId, null));
        if(viewMode == null) {
            return TranslationViewMode.READ;
        }
        return viewMode;
    }

    /**
     * Sets the last focused chapter and frame for a target translation
     * @param targetTranslationId
     * @param chapterId
     * @param frameId
     */
    public static void setLastFocus(String targetTranslationId, String chapterId, String frameId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_focus_chapter_" + targetTranslationId, chapterId);
        editor.putString("last_focus_frame_" + targetTranslationId, frameId);
        editor.apply();
    }

    /**
     * Returns the id of the chapter that was last in focus for this target translation
     * @param targetTranslationId
     * @return
     */
    public static String getLastFocusChapterId(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString("last_focus_chapter_" + targetTranslationId, null);
    }

    /**
     * Returns the id of the frame that was last in focus for this target translation
     * @param targetTranslationId
     * @return
     */
    public static String getLastFocusFrameId(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString("last_focus_frame_" + targetTranslationId, null);
    }

    /**
     * Adds a source translation to the list of open tabs on a target translation
     * @param targetTranslationId
     * @param sourceTranslationId
     */
    public static void addOpenSourceTranslation(String targetTranslationId, String sourceTranslationId) {
        if(sourceTranslationId != null && !sourceTranslationId.isEmpty()) {
            SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String[] sourceTranslationIds = getOpenSourceTranslationIds(targetTranslationId);
            String newIdSet = "";
            for (String id : sourceTranslationIds) {
                if (!id.equals(sourceTranslationId)) {
                    newIdSet += id + "|";
                }
            }
            newIdSet += sourceTranslationId;
            editor.putString("open_source_translations_" + targetTranslationId, newIdSet);
            editor.apply();
        }
    }

    /**
     * Removes a source translation from the list of open tabs on a target translation
     * @param targetTranslationId
     * @param sourceTranslationId
     */
    public static void removeOpenSourceTranslation(String targetTranslationId, String sourceTranslationId) {
        if(sourceTranslationId != null && !sourceTranslationId.isEmpty()) {
            SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String[] sourceTranslationIds = getOpenSourceTranslationIds(targetTranslationId);
            String newIdSet = "";
            for (String id : sourceTranslationIds) {
                if (!id.equals(sourceTranslationId)) {
                    if (newIdSet.isEmpty()) {
                        newIdSet = id;
                    } else {
                        newIdSet += "|" + id;
                    }
                } else if(id.equals(getSelectedSourceTranslationId(targetTranslationId))) {
                    // unset the selected tab if it is removed
                    setSelectedSourceTranslation(targetTranslationId, null);
                }
            }
            editor.putString("open_source_translations_" + targetTranslationId, newIdSet);
            editor.apply();
        }
    }

    /**
     * Returns an array of open source translation tabs on a target translation
     * @param targetTranslationId
     * @return
     */
    public static String[] getOpenSourceTranslationIds(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String idSet = prefs.getString("open_source_translations_" + targetTranslationId, "").trim();
        if(idSet.isEmpty()) {
            return new String[0];
        } else {
            return idSet.split("\\|");
        }
    }

    /**
     * Sets or removes the selected open source translation tab on a target translation
     * @param targetTranslationId
     * @param sourceTranslationId if null the selection will be unset
     */
    public static void setSelectedSourceTranslation(String targetTranslationId, String sourceTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if(sourceTranslationId != null && !sourceTranslationId.isEmpty()) {
            editor.putString("selected_source_translation_" + targetTranslationId, sourceTranslationId);
        } else {
            editor.remove("selected_source_translation_"  + targetTranslationId);
        }
        editor.apply();
    }

    /**
     * Returns the selected open source translation tab on the target translation
     * If there is no selection the first open tab will be set as the selected tab
     * @param targetTranslationId
     * @return
     */
    public static String getSelectedSourceTranslationId(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String selectedSourceTranslationId = prefs.getString("selected_source_translation_" + targetTranslationId, null);
        if(selectedSourceTranslationId == null || selectedSourceTranslationId.isEmpty()) {
            // default to first tab
            String[] openSourceTranslationIds = getOpenSourceTranslationIds(targetTranslationId);
            if(openSourceTranslationIds.length > 0) {
                selectedSourceTranslationId = openSourceTranslationIds[0];
                setSelectedSourceTranslation(targetTranslationId, selectedSourceTranslationId);
            }
        }
        return selectedSourceTranslationId;
    }

    /**
     * Removes all settings for a target translation
     * @param targetTranslationId
     */
    public static void clearTargetTranslationSettings(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("selected_source_translation_" + targetTranslationId);
        editor.remove("open_source_translations_" + targetTranslationId);
        editor.remove("last_focus_frame_" + targetTranslationId);
        editor.remove("last_focus_chapter_" + targetTranslationId);
        editor.remove("last_view_mode_" + targetTranslationId);
        editor.apply();
    }

    /**
     * Returns the address for the media server
     * @return
     */
    public static String getMediaServer() {
        String url = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, AppContext.context().getResources().getString(R.string.pref_default_media_server));
        return StringUtilities.ltrim(url, '/');
    }

    /**
     * Returns the sharing directory
     * @return
     */
    public static File getSharingDir() {
        File file = new File(mContext.getCacheDir(), "sharing");
        file.mkdirs();
        return file;
    }

    /**
     * Returns the last time we checked the server for updates
     * @return
     */
    public static long getLastCheckedForUpdates() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getLong("last_checked_server_for_updates", 0L);
    }

    /**
     * Sets the last time we checked the server for updates to the library
     * @param timeMillis
     */
    public static void setLastCheckedForUpdates(long timeMillis) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("last_checked_server_for_updates", timeMillis);
        editor.apply();
    }

    /**
     * Sets the alias to be displayed when others see this device on the network
     * @param alias
     */
    public static void setDeviceNetworkAlias(String alias) {
        if(alias.trim().isEmpty()) {
            alias = null;
        }
        setUserString(SettingsActivity.KEY_PREF_DEVICE_ALIAS, alias);
    }

    /**
     * Returns the alias to be displayed when others see this device on the network
     * @return
     */
    public static String getDeviceNetworkAlias() {
        String name = getUserString(SettingsActivity.KEY_PREF_DEVICE_ALIAS, "");
        if(name.isEmpty()) {
            return null;
        } else {
            return name;
        }
    }

    /**
     * Returns the string value of a user preference or the default value
     * @param preferenceKey
     * @param defaultResource
     * @return
     */
    public static String getUserString(String preferenceKey, int defaultResource) {
        return getUserString(preferenceKey, mContext.getResources().getString(defaultResource));
    }

    /**
     * Returns the string value of a user preference or the default value
     * @param preferenceKey
     * @param defaultValue
     * @return
     */
    public static String getUserString(String preferenceKey, String defaultValue) {
        return mContext.getUserPreferences().getString(preferenceKey, defaultValue);
    }

    /**
     * Sets the value of a user string.
     * @param preferenceKey
     * @param value if null the string will be removed
     */
    public static void setUserString(String preferenceKey, String value) {
        SharedPreferences.Editor editor = mContext.getUserPreferences().edit();
        if(value == null) {
            editor.remove(preferenceKey);
        } else {
            editor.putString(preferenceKey, value);
        }
        editor.apply();
    }
}
