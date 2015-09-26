package com.door43.translationstudio.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.ProjectCategory;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Navigator;
import com.door43.translationstudio.projects.ProjectManager;
import com.door43.translationstudio.spannables.Char;
import com.door43.util.StorageUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import java.io.File;
import java.io.IOException;

//import org.sil.palaso.Graphite;

/**
 * This class provides global access to the application context as well as other important tools
 */
public class AppContext {
    private static final String PREFERENCES_NAME = "com.door43.translationstudio.general";
    private static MainThreadBus mEventBus;
    private static MainApplication mContext;
    private static Navigator mNavigator;
    private static ProjectManager mProjectManager;
    public static final Bundle args = new Bundle();
    private static boolean sEnableGraphite = false;
    private static boolean loaded;

    /**
     * Initializes the basic functions context.
     * It is very important to initialize the class before using it because it assumes
     * the context has already been set.
     * @param context The application context. This can only be set once.
     */
    public AppContext(MainApplication context) {
        if(mContext == null) {
            if(sEnableGraphite && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO && Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                try {
//                    Graphite.loadGraphite();
                } catch (UnsatisfiedLinkError e) {
                    // graphite failed to load
                    Logger.e(this.getClass().getName(), "failed to load graphite", e);
                    sEnableGraphite = false;
                }
            }
            mContext = context;
            mProjectManager = new ProjectManager(context);
            mNavigator = new Navigator(context, mProjectManager, getEventBus());
        }
    }

    /**
     * Returns an instance of the library
     * @return
     */
    public static Library getLibrary() {
        // NOTE: rather than keeping the library around we rebuild it so that changes to the user settings will work
        String server = mContext.getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, mContext.getResources().getString(R.string.pref_default_media_server));
        String rootApiUrl = server + mContext.getResources().getString(R.string.root_catalog_api);
        return new Library(mContext, new File(mContext.getFilesDir(), "library"), new File(mContext.getCacheDir(), "library"), rootApiUrl);
    }

    /**
     * Returns an instance of the translator
     * @return
     */
    public static Translator getTranslator() {
        return new Translator(mContext, new File(mContext.getFilesDir(), "translations"));
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
     * Returns the global event bus
     * @deprecated please use com.door43.util.threads.TaskManager instead
     * @return
     */
    public static Bus getEventBus() {
        if(mEventBus == null) {
            mEventBus = new MainThreadBus(ThreadEnforcer.ANY);
        }
        return mEventBus;
    }

    /**
     * Returns the global project manager
     * @return
     */
    public static ProjectManager projectManager() {
        return mProjectManager;
    }

    /**
     * Returns the global navigation system
     * @return
     */
    public static Navigator navigator() {
        return mNavigator;
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
        // TRICKY: KITKAT introduced changes to the external media that made sd cards read only
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // || Root.isDeviceRooted()
            StorageUtils.StorageInfo removeableMediaInfo = StorageUtils.getRemoveableMediaDevice();
            return removeableMediaInfo != null;
        } else {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        }
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
                dir = new File("/storage/" + removeableMediaInfo.getMountName() + "/Download/translationStudio");
            } else {
                dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "translationStudio");
            }
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
     * Returns the graphite typeface to be used with the given language.
     * When setting the typeface make sure to disable the style e.g. setTypeface(typeFace, 0);
     * @return if no typeface is found the default typeface will be returned
     */
    public static Typeface graphiteTypeface(Language l) {
        String typeFace = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_TRANSLATION_TYPEFACE, AppContext.context().getResources().getString(R.string.pref_default_translation_typeface));
        File font = AppContext.context().getAssetAsFile("fonts/" + typeFace);
        if (font != null) {
            try {
                Typeface customTypeface;
//                if (sEnableGraphite) {
//                    TTFAnalyzer analyzer = new TTFAnalyzer();
//                    String fontname = analyzer.getTtfFontName(font.getAbsolutePath());
//                    if (fontname != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO && Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
//                        // assets container, font asset, font name, rtl, language, feats (what's this for????)
//                        int translationRTL = l.getDirection() == Language.Direction.RightToLeft ? 1 : 0;
//                        try {
////                            customTypeface = (Typeface) Graphite.addFontResource(mContext.getAssets(), "fonts/" + typeFace, fontname, translationRTL, l.getId(), "");
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            customTypeface = Typeface.createFromFile(font);
//                        }
//                    } else {
//                        customTypeface = Typeface.createFromFile(font);
//                    }
//                } else {
                    customTypeface = Typeface.createFromAsset(AppContext.context().getAssets(), "fonts/" + typeFace); //.createFromFile(font);
//                }
                return customTypeface;
            } catch (Exception e) {
                Logger.e(AppContext.class.getName(), "Could not load the typeface " + typeFace, e);
                return Typeface.DEFAULT;
            }
        } else {
            Logger.w(AppContext.class.getName(), "Could not load the typeface " + typeFace);
            return Typeface.DEFAULT;
        }
    }

    /**
     * Returns the sp size to be used for typefaces.
     * When setting the typeface size be sure to use the complex unit sp e.g. setTextSize(TypedValue.COMPLEX_UNIT_SP, typefaceSize);
     * @return
     */
    public static float typefaceSize() {
        return Integer.parseInt(AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_TYPEFACE_SIZE, AppContext.context().getResources().getString(R.string.pref_default_typeface_size)));
    }

    /**
     * Returns the minimum checking level required for a translation to be considered a valid source language
     * @return
     */
    public static int minCheckingLevel() {
        return mContext.getResources().getInteger(R.integer.min_source_lang_checking_level);
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

    private static class MainThreadBus extends Bus {
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        public MainThreadBus() {
            super();
        }

        public MainThreadBus(ThreadEnforcer enforcer) {
            super(enforcer);
        }

        public MainThreadBus(String identifier) {
            super(identifier);
        }

        public MainThreadBus(ThreadEnforcer enforcer, String identifier) {
            super(enforcer, identifier);
        }

        @Override
        public void post(final Object event) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                super.post(event);
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        MainThreadBus.super.post(event);
                    }
                });
            }
        }
    }

    /**
     * Returns the address for the media server
     * @return
     */
    public static String getMediaServer() {
        String url = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_MEDIA_SERVER, AppContext.context().getResources().getString(R.string.pref_default_media_server));
        return ltrim(url, '/');
    }

    private static String ltrim(String str, char target) {
        if (str.length() > 0 && str.charAt(str.length()-1)==target) {
            str = str.substring(0, str.length()-1);
        }
        return str;
    }
}
