package com.door43.translationstudio;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Util;
import com.door43.util.StorageUtils;
import com.door43.util.StringUtilities;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * This class provides global access to the application context as well as other important tools
 */
public class AppContext {
    private static final String PREFERENCES_NAME = "com.door43.translationstudio.general";
    private static final String DEFAULT_LIBRARY_ZIP = "library.zip";
    private static final String TARGET_TRANSLATIONS_DIR = "translations";
    public static final String PROFILES_DIR = "profiles";
    private static MainApplication mContext;
    public static final Bundle args = new Bundle();
    private static boolean loaded;

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
     * Checks if the external media is mounted and writeable
     * @return
     */
    public static boolean isExternalMediaAvailable() {
        final String externalStorageState = Environment.getExternalStorageState();
        boolean mounted = Environment.MEDIA_MOUNTED.equals(externalStorageState);
        if(mounted) { // do a double check
            File sdCard = Environment.getExternalStorageDirectory();
            if(sdCard.exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the file to the external public downloads directory
     * @return
     */
    public static File getPublicDownloadsDirectory() {
        File dir;
        if(isExternalMediaAvailable()) {
            dir = new File(Environment.getExternalStorageDirectory() + "/Download/translationStudio");
        } else {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "translationStudio");
        }
        dir.mkdirs();
        return dir;
    }

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
