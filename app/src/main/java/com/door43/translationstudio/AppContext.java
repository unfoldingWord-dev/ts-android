package com.door43.translationstudio;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.Nullable;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.core.ArchiveDetails;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Util;
import com.door43.translationstudio.util.SdUtils;
import com.door43.util.StorageUtils;
import com.door43.util.StringUtilities;
import com.door43.util.Zip;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class provides global access to the application context as well as other important tools
 */
public class AppContext {
    private static final String PREFERENCES_NAME = "com.door43.translationstudio.general";
    private static final String DEFAULT_LIBRARY_ZIP = "library.zip";
    private static final String TARGET_TRANSLATIONS_DIR = "translations";

    public static final String PROFILES_DIR = "profiles";
    public static final String TRANSLATION_STUDIO = "translationStudio";
    public static final String LAST_VIEW_MODE = "last_view_mode_";
    public static final String LAST_FOCUS_CHAPTER = "last_focus_chapter_";
    public static final String LAST_FOCUS_FRAME = "last_focus_frame_";
    public static final String OPEN_SOURCE_TRANSLATIONS = "open_source_translations_";
    public static final String SELECTED_SOURCE_TRANSLATION = "selected_source_translation_";
    public static final String LAST_CHECKED_SERVER_FOR_UPDATES = "last_checked_server_for_updates";
    public static final String LAST_TRANSLATION = "last_translation";
    public static final String EXTRA_SOURCE_DRAFT_TRANSLATION_ID = "extra_source_translation_id";
    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    public static final String EXTRA_CHAPTER_ID = "extra_chapter_id";
    public static final String EXTRA_FRAME_ID = "extra_frame_id";
    public static final String EXTRA_VIEW_MODE = "extra_view_mode_id";

    public static final String TAG = AppContext.class.toString();
    private static MainApplication mContext;
    public static final Bundle args = new Bundle();

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
    @Nullable
    public static Library getLibrary() {
        // NOTE: rather than keeping the library around we rebuild it so that changes to the user settings will work
        String server = mContext.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, mContext.getResources().getString(R.string.pref_default_media_server));
        String rootApiUrl = server + mContext.getResources().getString(R.string.root_catalog_api);
        try {
            return new Library(mContext, rootApiUrl);
        } catch (IOException e) {
            Logger.e(TAG, "Failed to create the library", e);
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
     * Returns an instance of the translator.
     * Target translations are stored in the public directory so that they persist if the app is uninstalled.
     * @return
     */
    public static Translator getTranslator() {
        return new Translator(mContext, new File(getPublicDirectory(), TARGET_TRANSLATIONS_DIR));
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
     * Returns the file to the external public downloads directory
     * @return
     */
    public static File getPublicDownloadsDirectory() {
        File dir;
        // TRICKY: KITKAT introduced changes to the external media that made sd cards read only
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // || Root.isDeviceRooted()
            StorageUtils.StorageInfo removeableMediaInfo = StorageUtils.getRemoveableMediaDevice();
            if(removeableMediaInfo != null) {
                dir = new File("/storage/" + removeableMediaInfo.getMountName() + SdUtils.DOWNLOAD_TRANSLATION_STUDIO_FOLDER);
            } else {
                dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), TRANSLATION_STUDIO);
            }
        } else {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), TRANSLATION_STUDIO);
        }
        dir.mkdirs();
        return dir;
    }

    /**
     * Creates a backup of a target translation in all the right places
     * @param targetTranslation the target translation that will be backed up
     * @param orphaned if true this backup will be orphaned (time stamped)
     * @return true if the backup was actually performed
     */
    public static boolean backupTargetTranslation(TargetTranslation targetTranslation, Boolean orphaned) throws Exception {
        if(targetTranslation != null) {
            String name = targetTranslation.getId();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.US);
            if (orphaned) {
                name += "." + sdf.format(new Date());
            }
            File temp = null;
            try {
                temp = File.createTempFile(name, "." + Translator.ARCHIVE_EXTENSION);
                targetTranslation.setDefaultContributor(getProfile().getNativeSpeaker());
                getTranslator().exportArchive(targetTranslation, temp);
                if (temp.exists() && temp.isFile()) {
                    // copy into backup locations
                    File downloadsBackup = new File(getPublicDownloadsDirectory(), name + "." + Translator.ARCHIVE_EXTENSION);
                    File publicBackup = new File(getPublicDirectory(), "backups/" + name + "." + Translator.ARCHIVE_EXTENSION);
                    downloadsBackup.getParentFile().mkdirs();
                    publicBackup.getParentFile().mkdirs();

                    // check if we need to backup
                    if(!orphaned) {
                        ArchiveDetails downloadsDetails = ArchiveDetails.newInstance(downloadsBackup, "en", getLibrary());
                        ArchiveDetails publicDetails = ArchiveDetails.newInstance(publicBackup, "en", getLibrary());
                        // TRICKY: we only generate backups with a single target translation inside.
                        if(downloadsDetails != null && downloadsDetails.targetTranslationDetails[0].commitHash.equals(targetTranslation.getCommitHash())
                            && publicDetails != null && publicDetails.targetTranslationDetails[0].commitHash.equals(targetTranslation.getCommitHash())) {
                            return false;
                        }
                    }

                    FileUtils.copyFile(temp, downloadsBackup);
                    FileUtils.copyFile(temp, publicBackup);
                    return true;
                }
            } catch (Exception e) {
                if (temp != null) {
                    FileUtils.deleteQuietly(temp);
                }
                throw e;
            }
        }
        return false;
    }
    
    /**
     * Returns the path to the public files directory.
     * Files saved in this directory will not be removed when the application is uninstalled
     * @return
     */
    public static File getPublicDirectory() {
        File dir = new File(Environment.getExternalStorageDirectory(), TRANSLATION_STUDIO);
        dir.mkdirs();
        return dir;
    }

    /**
     * Sets the last opened view mode for a target translation
     * @param targetTranslationId
     * @param viewMode
     */
    public static void setLastViewMode(String targetTranslationId, TranslationViewMode viewMode) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LAST_VIEW_MODE + targetTranslationId, viewMode.toString());
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
        TranslationViewMode viewMode = TranslationViewMode.get(prefs.getString(LAST_VIEW_MODE + targetTranslationId, null));
        if(viewMode == null) {
            return TranslationViewMode.READ;
        }
        return viewMode;
    }

    /**
     * Returns the last focused target translation
     * @return
     */
    public static String getLastFocusTargetTranslation() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAST_TRANSLATION, null);
    }


    /**
     * Sets the last focused target translation
     * @param targetTranslationId
     */
    public static void setLastFocusTargetTranslation(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LAST_TRANSLATION, targetTranslationId);
        editor.apply();
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
        editor.putString(LAST_FOCUS_CHAPTER + targetTranslationId, chapterId);
        editor.putString(LAST_FOCUS_FRAME + targetTranslationId, frameId);
        editor.apply();
        setLastFocusTargetTranslation(targetTranslationId);
    }

    /**
     * Returns the id of the chapter that was last in focus for this target translation
     * @param targetTranslationId
     * @return
     */
    public static String getLastFocusChapterId(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAST_FOCUS_CHAPTER + targetTranslationId, null);
    }

    /**
     * Returns the id of the frame that was last in focus for this target translation
     * @param targetTranslationId
     * @return
     */
    public static String getLastFocusFrameId(String targetTranslationId) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAST_FOCUS_FRAME + targetTranslationId, null);
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
            editor.putString(OPEN_SOURCE_TRANSLATIONS + targetTranslationId, newIdSet);
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
            editor.putString(OPEN_SOURCE_TRANSLATIONS + targetTranslationId, newIdSet);
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
        String idSet = prefs.getString(OPEN_SOURCE_TRANSLATIONS + targetTranslationId, "").trim();
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
            editor.putString(SELECTED_SOURCE_TRANSLATION + targetTranslationId, sourceTranslationId);
        } else {
            editor.remove(SELECTED_SOURCE_TRANSLATION + targetTranslationId);
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
        String selectedSourceTranslationId = prefs.getString(SELECTED_SOURCE_TRANSLATION + targetTranslationId, null);
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
        editor.remove(SELECTED_SOURCE_TRANSLATION + targetTranslationId);
        editor.remove(OPEN_SOURCE_TRANSLATIONS + targetTranslationId);
        editor.remove(LAST_FOCUS_FRAME + targetTranslationId);
        editor.remove(LAST_FOCUS_CHAPTER + targetTranslationId);
        editor.remove(LAST_VIEW_MODE + targetTranslationId);
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
        return prefs.getLong(LAST_CHECKED_SERVER_FOR_UPDATES, 0L);
    }

    /**
     * Sets the last time we checked the server for updates to the library
     * @param timeMillis
     */
    public static void setLastCheckedForUpdates(long timeMillis) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_CHECKED_SERVER_FOR_UPDATES, timeMillis);
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
    @Nullable
    public static String getDeviceNetworkAlias() {
        String name = getUserString(SettingsActivity.KEY_PREF_DEVICE_ALIAS, "");
        if(name.isEmpty()) {
            return null;
        } else {
            return name;
        }
    }

    /**
     * Returns information about the user of the application.
     *
     * <p>This is a readable collection of information, not a "live-updating" reference to storage.
     * To change the profiles in use, call {@link #setProfiles(List)}</p>.
     *
     * @return A list of {@link Profile} objects; or an empty list if not set, or on error.
     */
    @Nullable
    public static List<Profile> getProfiles() {
        try {
            String profilesEncoded = getUserString(SettingsActivity.KEY_PREF_PROFILES, null);
            if (profilesEncoded != null) {
                JSONArray profilesJson = new JSONArray(profilesEncoded);
                return Profile.decodeJsonArray(profilesJson);
            }
        }
        catch (Exception e) {
            // There are lots of ways for this to fail, none of which are particularly serious.
            // In this case, log the result but allow the data to be lost.
            Logger.e(TAG, "getProfiles: Failed to parse profile data", e);
        }
        return new ArrayList<>();
    }

    /**
     * Returns the currently opened user profile
     * @return
     */
    public static Profile getProfile() {
        // TODO: 2/19/2016 we need to fix profiles
        List<Profile> profiles = getProfiles();
        if(profiles.size() > 0) {
            return getProfiles().get(0);
        } else {
            return new Profile("test", "test", "test");
        }
    }

    /**
     * Set the user's default profile, used to populate translator information when creating a new
     * translation.
     *
     * <p>This persists the information but does not retain a reference to it. Changes to the
     * argument made after this call are not persisted.</p>
     *
     * @param profiles A list of profile objects.
     */
    public static void setProfiles(List<Profile> profiles) {
        try {
            String profilesJson = Profile.encodeJsonArray(profiles).toString();
            setUserString(SettingsActivity.KEY_PREF_PROFILES, profilesJson);
        }
        catch (JSONException e) {
            // Failures to save are not particularly severe. Log and continue.
            Logger.e(TAG, "setProfiles: Failed to encode profile data", e);
        }
    }

    /**
     * Returns a human-readable string summarizing the user's profile settings, suitable for
     * display as a single string.
     *
     * @return A string, or the empty string if nothing is set
     */
    public static String getProfileSummary() {
        List<Profile> profiles = getProfiles();

        StringBuilder all = new StringBuilder();
        StringBuilder single = new StringBuilder();

        for (Profile profile : profiles) {
            // Prepare a summary of the profile we're examining right now.
            String[] fields = { profile.name, profile.email, profile.phone };
            for (String field : fields) {
                if (field == null || field.isEmpty()) {
                    continue;
                }

                if (single.length() > 0) {
                    single.append(", ");
                }

                single.append(field);
            }

            // Add the single-profile summary to the overall summary. But only include it if
            // this profile has a summary (i.e., prefer "foo, bar; baz" to "foo, bar; ; baz").
            if (all.length() > 0 && single.length() > 0) {
                all.append("; ");
            }
            all.append(single);
            single.delete(0, single.length());
        }

        return all.toString();
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
