package com.door43.translationstudio;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.TargetLanguage;
import org.unfoldingword.tools.logger.LogLevel;
import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.core.ArchiveDetails;
import com.door43.translationstudio.core.NewLanguageRequest;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Util;
import com.door43.translationstudio.services.BackupService;
import com.door43.translationstudio.ui.SettingsActivity;
import com.door43.util.SdUtils;
import com.door43.util.FileUtilities;
import com.door43.util.StorageUtils;
import com.door43.util.StringUtilities;
import com.door43.util.Zip;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class provides global access to the application context as well as other important tools
 */
public class App extends Application {
    public static final String PREFERENCES_TAG = "com.door43.translationstudio";
    public static final String EXTRA_SOURCE_DRAFT_TRANSLATION_ID = "extra_source_translation_id";
    public static final String EXTRA_TARGET_TRANSLATION_ID = "extra_target_translation_id";
    public static final String EXTRA_START_WITH_MERGE_FILTER = "start_with_merge_filter";
    public static final String EXTRA_CHAPTER_ID = "extra_chapter_id";
    public static final String EXTRA_FRAME_ID = "extra_frame_id";
    public static final String EXTRA_VIEW_MODE = "extra_view_mode_id";
    public static final String PUBLIC_DATA_DIR = "translationStudio";
    public static final String TAG = "App";

    private static final String PREFERENCES_NAME = "com.door43.translationstudio.general";
//    private static final String DEFAULT_LIBRARY_ZIP = "library.zip";
    private static final String LAST_VIEW_MODE = "last_view_mode_";
    private static final String LAST_FOCUS_CHAPTER = "last_focus_chapter_";
    private static final String LAST_FOCUS_FRAME = "last_focus_frame_";
    private static final String OPEN_SOURCE_TRANSLATIONS = "open_source_translations_";
    private static final String SELECTED_SOURCE_TRANSLATION = "selected_source_translation_";
    private static final String LAST_CHECKED_SERVER_FOR_UPDATES = "last_checked_server_for_updates";
//    private static final String ASSETS_DIR = "assets";
    public static final int MIN_CHECKING_LEVEL = 3;
    private static ImageLoader mImageLoader;
    private static App sInstance;
    private static String targetTranslationWithUpdates = null;
    private static File imagesDir;

    public static File getImagesDir() {
        return imagesDir;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        // configure logger
        int minLogLevel = Integer.parseInt(getUserPreferences().getString(SettingsActivity.KEY_PREF_LOGGING_LEVEL, getResources().getString(R.string.pref_default_logging_level)));
        configureLogger(minLogLevel);

        File dir = new File(publicDir(), "crashes");
        Logger.registerGlobalExceptionHandler(dir);

        // initialize default settings
        // NOTE: make sure to add any new preference files here in order to have their default values properly loaded.
        PreferenceManager.setDefaultValues(this, R.xml.general_preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.server_preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.sharing_preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.advanced_preferences, false);

        // begins the backup manager service
        Intent backupIntent = new Intent(this, BackupService.class);
        startService(backupIntent);
    }

    public static void configureLogger(int minLogLevel) {
        Logger.configure(new File(publicDir(), "log.txt"), LogLevel.getLevel(minLogLevel));
    }

    /**
     * Returns an instance of the user preferences.
     * This is just the default shared preferences
     * @return
     */
    public static SharedPreferences getUserPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(sInstance);
    }

    /**
     * Checks if we have internet
     * @return
     */
    public static boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) sInstance.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Returns the langauge code used by the device.
     * This will trim off dangling special characters
     * @return
     */
    public static String getDeviceLanguageCode() {
        String code = Locale.getDefault().getLanguage();
        return code.replaceAll("[\\_-]$", "");
    }

    /**
     * A temporary utility to retrive the target language used in a target translation.
     * if the language does not exist it will be added as a temporary language if possible
     * @param t
     * @return
     */
    @Deprecated
    public static TargetLanguage languageFromTargetTranslation(TargetTranslation t) {
        Door43Client library = getLibrary();
        TargetLanguage l = library.index.getTargetLanguage(t.getTargetLanguageId());
        if(l == null && t.getTargetLanguageId().isEmpty()) {
            String name = t.getTargetLanguageName().isEmpty() ? t.getTargetLanguageId() : t.getTargetLanguageName();
            String direction = t.getTargetLanguageDirection() == null ? "ltr" : t.getTargetLanguageDirection();
            l = new TargetLanguage(t.getTargetLanguageId(), name, "", direction, "unknown", false);
            try {
                library.index.addTempTargetLanguage(l);
            } catch (Exception e) {
                l = null;
                e.printStackTrace();
            }
        }
        return l;
    }

    /**
     * Generates a new RSA key pair for use with ssh
     */
    public static void generateSSHKeys() {
        JSch jsch = new JSch();
        int type = KeyPair.RSA;
        File keysDir = getKeysFolder();
        String privateKeyPath = keysDir.getAbsolutePath() + "/id_rsa";
        String publicKeyPath = keysDir.getAbsolutePath() + "/id_rsa.pub";

        try{
            KeyPair kpair=KeyPair.genKeyPair(jsch, type);
            new File(privateKeyPath).createNewFile();
            kpair.writePrivateKey(privateKeyPath);
            new File(publicKeyPath).createNewFile();
            kpair.writePublicKey(publicKeyPath, App.udid());
            kpair.dispose();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the public key file
     * @return
     */
    public static File getPublicKey() {
        File keysDir = getKeysFolder();
        return  new File(keysDir.getAbsolutePath()+"/id_rsa.pub");
    }

    /**
     * Returns the private key file
     * @return
     */
    public static File getPrivateKey() {
        File keysDir = getKeysFolder();
        return  new File(keysDir.getAbsolutePath()+"/id_rsa");
    }

    /**
     * Checks if the ssh keys have already been generated
     * @return
     */
    public static boolean hasSSHKeys() {
        File keysDir = getKeysFolder();
        File privFile = new File(keysDir.getAbsolutePath()+"/id_rsa");
        File pubFile = new File(keysDir.getAbsolutePath()+"/id_rsa.pub");
        return privFile.exists() && pubFile.exists();
    }

    /**
     * Returns the directory in which the ssh keys are stored
     * @return
     */
    public static File getKeysFolder() {
        File folder = new File(sInstance.getFilesDir() + "/" + sInstance.getResources().getString(R.string.keys_dir) + "/");
        if(!folder.exists()) {
            folder.mkdir();
        }
        return folder;
    }

    /**
     * Generates and returns the image loader
     * @return
     */
    public static ImageLoader getImageLoader() {
        if(mImageLoader == null) {
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(sInstance).build();
            mImageLoader = ImageLoader.getInstance();
            mImageLoader.init(config);
        }
        return mImageLoader;
    }

    /**
     * Checks if this apk was installed from the playstore or sideloaded
     * @return
     */
    public static boolean isStoreVersion() {
        String installer = sInstance.getPackageManager().getInstallerPackageName(sInstance.getPackageName());
        return !TextUtils.isEmpty(installer);
    }

    /**
     * Moves an asset into the cache directory and returns a file reference to it
     * @param path
     * @return
     */
    public static File getAssetAsFile(String path) {
        // TODO: we probably don't want to do this for everything.
        // think about changing this up a bit.
        // TODO: we need to figure out when the clear out these cached files. Probably just on version bumps.
        File cacheFile = new File(sInstance.getCacheDir(), "assets/" + path);
        if(!cacheFile.exists()) {
            cacheFile.getParentFile().mkdirs();
            try {
                InputStream is = sInstance.getAssets().open(path);
                try {
                    FileOutputStream outputStream = new FileOutputStream(cacheFile);
                    try {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = is.read(buf)) > 0) {
                            outputStream.write(buf, 0, len);
                        }
                    } finally {
                        outputStream.close();
                    }
                } finally {
                    is.close();
                }

            } catch (IOException e) {
                return null;
            }
        }
        return cacheFile;
    }

    /**
     * Looks up a string preference
     * @param key
     * @return
     */
    public static String getPref(String key, String defaultValue) {
        return sInstance.getUserPreferences().getString(key, defaultValue);
    }

    /**
     * Looks up a string resource
     * @param id
     * @return
     */
    public static String getRes(int id) {
        return sInstance.getResources().getString(id);
    }

    /**
     * Returns the path to the directory where the database is stored
     * @return
     */
    public static File databaseDir() {
        return new File(publicDir(), "databases");
    }

    /**
     * Returns an instance of the door43 client
     * @return
     */
    @Nullable
    public static Door43Client getLibrary() {
        try {
            return new Door43Client(sInstance, dbFile(), containersDir());
        } catch (IOException e) {
            Logger.e(TAG, "Failed to initialize the door43 client", e);
        }
        return null;
    }

    /**
     * Returns the version of the terms of use
     * @return
     */
    public static int getTermsOfUseVersion() {
        return sInstance.getResources().getInteger(R.integer.terms_of_use_version);
    }

    /**
     * Checks if the device is a tablet
     * @return
     */
    public static boolean isTablet() {
        return (sInstance.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * The index (database) file
     * @return
     */
    private static File dbFile() {
        return new File(databaseDir(), "index.sqlite");
    }

    /**
     * The directory where all source resource containers will be stored
     * @return
     */
    private static File containersDir() {
        return new File(publicDir(), "resource_containers");
    }

    /**
     * Deploys the default index and resource containers.
     *
     * @throws Exception
     */
    public static void deployDefaultLibrary() throws Exception {
        // copy index
        Util.writeStream(sInstance.getAssets().open("index.sqlite"), dbFile());
        // extract resource containers
        Zip.unzipFromStream(sInstance.getAssets().open("containers.zip"), containersDir());
    }

    /**
     * Check if the default index and resource containers have been deployed
     * @return
     */
    public static boolean isLibraryDeployed() {
        boolean hasContainers = containersDir().exists() && containersDir().isDirectory() && containersDir().list().length > 0;
        return getLibrary().index.getSourceLanguages().size() > 0 && hasContainers;
//        return dbFile().exists() && dbFile().isFile() && ;
    }

    /**
     * Nuke all the things!
     * ... or just the source content
     */
    public static void deleteLibrary() {
        try {
            getLibrary().tearDown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FileUtilities.deleteQuietly(dbFile());
        FileUtilities.deleteQuietly(containersDir());
    }

    /**
     * Returns an instance of the translator.
     * Target translations are stored in the public directory so that they persist if the app is uninstalled.
     * @return
     */
    public static Translator getTranslator() {
        return new Translator(sInstance, getProfile(), new File(publicDir(), "translations"));
    }

    /**
     * Checks if the package asset exists
     * @param path
     * @return
     */
    public static boolean assetExists(String path) {
        try {
            sInstance.getAssets().open(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns the main application context
     * @return
     */
    public static App context() {
        return sInstance;
    }

    /**
     * Returns the unique device id for this device
     * @return
     */
    public static String udid() {
        return Settings.Secure.getString(sInstance.getContentResolver(), Settings.Secure.ANDROID_ID);
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
                dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), PUBLIC_DATA_DIR);
            }
        } else {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), PUBLIC_DATA_DIR);
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
        if(targetTranslation != null && getProfile() != null) {
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
                    File publicBackup = new File(publicDir(), "backups/" + name + "." + Translator.ARCHIVE_EXTENSION);
                    downloadsBackup.getParentFile().mkdirs();
                    publicBackup.getParentFile().mkdirs();

                    // check if we need to backup
                    if(!orphaned) {
                        ArchiveDetails downloadsDetails = ArchiveDetails.newInstance(downloadsBackup, "en", getLibrary());
                        ArchiveDetails publicDetails = ArchiveDetails.newInstance(publicBackup, "en", getLibrary());
                        // TRICKY: we only generate backups with a single target translation inside.
                        if( getCommitHash(downloadsDetails).equals(targetTranslation.getCommitHash())
                            && getCommitHash(publicDetails).equals(targetTranslation.getCommitHash())) {
                            return false;
                        }
                    }

                    FileUtilities.copyFile(temp, downloadsBackup);
                    FileUtilities.copyFile(temp, publicBackup);
                    return true;
                }
            } catch (Exception e) {
                if (temp != null) {
                    FileUtilities.deleteQuietly(temp);
                }
                throw e;
            }
        }
        return false;
    }

    /**
     * safe fetch of commit hash
     * @param details
     * @return
     */
    static private String getCommitHash(ArchiveDetails details) {
        String commitHash = null;
        if((details == null) || (details.targetTranslationDetails.length == 0)) {
            commitHash =  ""; // will not match existing commit hash
        } else {
            commitHash =  details.targetTranslationDetails[0].commitHash;
        }
        return commitHash;
    }

    /**
     * Returns the path to the public files directory.
     * Files saved in this directory will not be removed when the application is uninstalled
     * @return
     */
    public static File publicDir() {
        File dir = new File(Environment.getExternalStorageDirectory(), PUBLIC_DATA_DIR);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * check for cached value that project has changed
     * @return
     */
    public static String getNotifyTargetTranslationWithUpdates() {
        return targetTranslationWithUpdates;
    }

    /**
     * for keeping track of project that has changed
     * @param targetTranslationId
     */
    public static void setNotifyTargetTranslationWithUpdates(String targetTranslationId) {
        targetTranslationWithUpdates = targetTranslationId;
    }

    /**
     * Sets the last opened view mode for a target translation
     * @param targetTranslationId
     * @param viewMode
     */
    public static void setLastViewMode(String targetTranslationId, TranslationViewMode viewMode) {
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(LAST_VIEW_MODE + targetTranslationId, viewMode.ordinal());
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
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        try {
            int modeIndex = prefs.getInt(LAST_VIEW_MODE + targetTranslationId, TranslationViewMode.READ.ordinal());
            if (modeIndex > 0 && modeIndex < TranslationViewMode.values().length) {
                return TranslationViewMode.values()[modeIndex];
            }
        } catch (Exception e) {}
        return TranslationViewMode.READ;
    }

    /**
     * Returns the last focused target translation
     * @return
     */
    public static String getLastFocusTargetTranslation() {
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString("last_translation", null);
    }


    /**
     * Sets the last focused target translation
     * @param targetTranslationId
     */
    public static void setLastFocusTargetTranslation(String targetTranslationId) {
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_translation", targetTranslationId);
        editor.apply();
    }

    /**
     * Sets the last focused chapter and frame for a target translation
     * @param targetTranslationId
     * @param chapterId
     * @param frameId
     */
    public static void setLastFocus(String targetTranslationId, String chapterId, String frameId) {
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
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
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAST_FOCUS_CHAPTER + targetTranslationId, null);
    }

    /**
     * Returns the id of the frame that was last in focus for this target translation
     * @param targetTranslationId
     * @return
     */
    public static String getLastFocusFrameId(String targetTranslationId) {
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAST_FOCUS_FRAME + targetTranslationId, null);
    }

    /**
     * Adds a source translation to the list of open tabs on a target translation
     * @param targetTranslationId
     * @param sourceTranslationId
     */
    public static void addOpenSourceTranslation(String targetTranslationId, String sourceTranslationId) {
        if(sourceTranslationId != null && !sourceTranslationId.isEmpty()) {
            SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String[] sourceTranslationIds = getSelectedSourceTranslations(targetTranslationId);
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
            SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String[] sourceTranslationIds = getSelectedSourceTranslations(targetTranslationId);
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
    public static String[] getSelectedSourceTranslations(String targetTranslationId) {
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
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
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
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
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String selectedSourceTranslationId = prefs.getString(SELECTED_SOURCE_TRANSLATION + targetTranslationId, null);
        if(selectedSourceTranslationId == null || selectedSourceTranslationId.isEmpty()) {
            // default to first tab
            String[] openSourceTranslationIds = getSelectedSourceTranslations(targetTranslationId);
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
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
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
        String url = sInstance.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, App.context().getResources().getString(R.string.pref_default_media_server));
        return StringUtilities.ltrim(url, '/');
    }

    /**
     * Returns the sharing directory
     * @return
     */
    public static File getSharingDir() {
        File file = new File(sInstance.getCacheDir(), "sharing");
        file.mkdirs();
        return file;
    }

    /**
     * Returns the last time we checked the server for updates
     * @return
     */
    public static long getLastCheckedForUpdates() {
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(LAST_CHECKED_SERVER_FOR_UPDATES, 0L);
    }

    /**
     * Sets the last time we checked the server for updates to the library
     * @param timeMillis
     */
    public static void setLastCheckedForUpdates(long timeMillis) {
        SharedPreferences prefs = sInstance.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
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
     * Returns the current user profile
     * @return
     */
    public static Profile getProfile() {
        String profileString = getUserString("profile", null);

        try {
            if (profileString != null) {
                return Profile.fromJSON(new JSONObject(profileString));
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to parse the profile", e);
        }
        return null;
    }

    /**
     * Stores the user profile
     *
     * @param profile
     */
    public static void setProfile(Profile profile) {
        try {
            if(profile != null) {
                String profileString = profile.toJSON().toString();
                setUserString("profile", profileString);
            } else {
                setUserString("profile", null);
                FileUtilities.deleteQuietly(getKeysFolder());
            }
        } catch (JSONException e) {
            Logger.e(TAG, "setProfile: Failed to encode profile data", e);
        }
    }

    /**
     * Returns the string value of a user preference or the default value
     * @param preferenceKey
     * @param defaultResource
     * @return
     */
    public static String getUserString(String preferenceKey, int defaultResource) {
        return getUserString(preferenceKey, sInstance.getResources().getString(defaultResource));
    }

    /**
     * shows the keyboard in the given activity and view
     * @param activity
     * @param view
     */
    public static void showKeyboard(Activity activity, View view, boolean forced) {
        if(activity != null) {
            if (activity.getCurrentFocus() != null) {
                try {
                    InputMethodManager mgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(forced) {
                        mgr.showSoftInput(view, InputMethodManager.SHOW_FORCED);
                    } else {
                        mgr.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                    }
                } catch (Exception e) {
                }
            } else {
                try {
                    activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Closes the keyboard in the given activity
     * @param activity
     */
    public static void closeKeyboard(Activity activity) {
        if(activity != null) {
            if (activity.getCurrentFocus() != null) {
                try {
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
                } catch (Exception e) {
                }
            } else {
                try {
                    activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Returns the string value of a user preference or the default value
     * @param preferenceKey
     * @param defaultValue
     * @return
     */
    public static String getUserString(String preferenceKey, String defaultValue) {
        return sInstance.getUserPreferences().getString(preferenceKey, defaultValue);
    }

    /**
     * Sets the value of a user string.
     * @param preferenceKey
     * @param value if null the string will be removed
     */
    public static void setUserString(String preferenceKey, String value) {
        SharedPreferences.Editor editor = sInstance.getUserPreferences().edit();
        if(value == null) {
            editor.remove(preferenceKey);
        } else {
            editor.putString(preferenceKey, value);
        }
        editor.apply();
    }

    /**
     * Returns the new language request if it exists
     * @param language_code
     * @return
     */
    @Nullable
    public static NewLanguageRequest getNewLanguageRequest(String language_code) {
        File requestFile = new File(publicDir(), "new_languages/" + language_code + ".json");
        if(requestFile.exists() && requestFile.isFile()) {
            String data = null;
            try {
                data = FileUtilities.readFileToString(requestFile);
            } catch (IOException e) {
                Logger.e(App.class.getName(), "Failed to read the new language request", e);
            }
            return NewLanguageRequest.generate(data);
        }
        return null;
    }

    /**
     * Returns an array of new language requests
     * @return
     */
    public static NewLanguageRequest[] getNewLanguageRequests() {
        File newLanguagesDir = new File(publicDir(), "new_languages/");
        File[] requestFiles = newLanguagesDir.listFiles();
        List<NewLanguageRequest> requests = new ArrayList<>();
        if(requestFiles != null && requestFiles.length > 0) {
            for(File f:requestFiles) {
                try {
                    String data = FileUtilities.readFileToString(f);
                    NewLanguageRequest request = NewLanguageRequest.generate(data);
                    if(request != null) {
                        requests.add(request);
                    }
                } catch (IOException e) {
                    Logger.e(App.class.getName(), "Failed to read the language request file", e);
                }
            }
        }
        return requests.toArray(new NewLanguageRequest[requests.size()]);
    }

    /**
     * Adds a new language request.
     * This stores the request to the data path for later submission
     * and adds the temp language to the library for global use in the app
     * @param request
     */
    public static boolean addNewLanguageRequest(NewLanguageRequest request) {
        if(request != null) {
            File requestFile = new File(publicDir(), "new_languages/" + request.tempLanguageCode + ".json");
            requestFile.getParentFile().mkdirs();
            try {
                FileUtilities.writeStringToFile(requestFile, request.toJson());
                return getLibrary().index.addTempTargetLanguage(request.getTempTargetLanguage());
            } catch (Exception e) {
                Logger.e(App.class.getName(), "Failed to save the new langauge request", e);
            }
        }
        return false;
    }

    /**
     * Deletes the new language request from the data path
     * @param request
     */
    public static void removeNewLanguageRequest(NewLanguageRequest request) {
        if(request != null) {
            File requestFile = new File(publicDir(), "new_languages/" + request.tempLanguageCode + ".json");
            if(requestFile.exists()) {
                FileUtilities.safeDelete(requestFile);
            }
        }
    }

    public static boolean hasImages() {
        return false;
    }

    /**
     * Generates a unique temporary directory
     * @return a new directory
     */
    public static File makeTempDirectory() {
        return new File(context().getExternalCacheDir(), System.currentTimeMillis() + "_tmp");
    }
}
