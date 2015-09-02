package com.door43.translationstudio.util;

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
