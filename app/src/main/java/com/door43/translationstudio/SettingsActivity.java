package com.door43.translationstudio;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TTFAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 *
 * NOTE: if you add new preference categories be sure to update MainApplication to load their default values.
 */
public class SettingsActivity extends PreferenceActivity {
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    public static final String KEY_PREF_AUTOSAVE = "autosave";
    public static final String KEY_PREF_AUTH_SERVER = "auth_server";
    public static final String KEY_PREF_AUTH_SERVER_PORT = "auth_server_port";
    public static final String KEY_PREF_GIT_SERVER = "git_server";
    public static final String KEY_PREF_GIT_SERVER_PORT = "git_server_port";
    public static final String KEY_PREF_REMEMBER_POSITION = "remember_position";
    public static final String KEY_PREF_MEDIA_SERVER = "media_server";
    public static final String KEY_PREF_EXPORT_FORMAT = "export_format";
    public static final String KEY_PREF_TRANSLATION_TYPEFACE = "translation_typeface";
    public static final String KEY_PREF_TYPEFACE_SIZE = "typeface_size";
    public static final String KEY_PREF_HIGHLIGHT_KEY_TERMS = "highlight_key_terms";
    public static final String KEY_PREF_ADVANCED_SETTINGS = "advanced_settings";
    public static final String KEY_PREF_LOGGING_LEVEL = "logging_level";

    /**
     * TRICKY: this was added after API 19 to fix a vulnerability.
     * @param fragmentName
     * @return
     */
    @Override
    protected boolean isValidFragment(String fragmentName) {
        // add any new preference fragments here so we can validate them
        String[] validFragments = new String[] {
                GeneralPreferenceFragment.class.getName(),
                AdvancedPreferenceFragment.class.getName(),
                SavePreferenceFragment.class.getName(),
                SharingPreferenceFragment.class.getName()
        };
        boolean isValid = false;
        for(String name:validFragments) {
            if(fragmentName.equals(name)) {
                isValid  = true;
            }
        }
        return isValid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences_legacy);

        Toolbar actionbar = (Toolbar) findViewById(R.id.actionbar);
        actionbar.setTitle("Settings");

        actionbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_ab_back_holo_light_am));
        actionbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsActivity.this.finish();
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // NOTE: this is a copy paste from GeneralPreferenceFragment
        // identify all typefaces in the assets directory
        AssetManager am = getResources().getAssets();
        String fileList[] = null;
        ArrayList<String> entries = new ArrayList<String>();
        ArrayList<String> entryValues = new ArrayList<String>();
        try {
            fileList = am.list("fonts");
        } catch (IOException e) {
            Logger.e(this.getClass().getName(), "failed to load font assets", e);
        }
        if (fileList != null)
        {
            for (int i = 0; i<fileList.length; i++)
            {
                File typeface = AppContext.context().getAssetAsFile("fonts/" + fileList[i]);
                if (typeface != null) {
                    TTFAnalyzer analyzer = new TTFAnalyzer();
                    String fontname = "";
                    fontname = analyzer.getTtfFontName(typeface.getAbsolutePath());
                    if(fontname != null) {
                        // add valid fonts to the list
                        entries.add(fontname);
                        entryValues.add(fileList[i]);
                    }
                }
            }
        }

        ListPreference pref = (ListPreference)findPreference(KEY_PREF_TRANSLATION_TYPEFACE);
        pref.setEntries(entries.toArray(new CharSequence[entries.size()]));
        pref.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
        bindPreferenceSummaryToValue(pref);
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_TYPEFACE_SIZE));

        // Add 'sharing' preferences, and a corresponding header.
        PreferenceCategory preferenceHeader = new PreferenceCategory(this);
        preferenceHeader.setTitle(R.string.pref_header_sharing);
        getPreferenceScreen().addPreference(preferenceHeader);
        addPreferencesFromResource(R.xml.pref_sharing);

        // Add 'upload' preferences, and a corresponding header.
        preferenceHeader = new PreferenceCategory(this);
        preferenceHeader.setTitle(R.string.pref_header_synchronization);
        getPreferenceScreen().addPreference(preferenceHeader);
        addPreferencesFromResource(R.xml.pref_save_and_sync);

        // add advanced preferences and coresponding hreader
        preferenceHeader = new PreferenceCategory(this);
        preferenceHeader.setTitle(R.string.pref_header_advanced);
        getPreferenceScreen().addPreference(preferenceHeader);
        addPreferencesFromResource(R.xml.pref_advanced);
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_LOGGING_LEVEL));

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTOSAVE));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTH_SERVER));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_GIT_SERVER));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTH_SERVER_PORT));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_GIT_SERVER_PORT));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_EXPORT_FORMAT));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_MEDIA_SERVER));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_TRANSLATION_TYPEFACE));
    }

    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /** {@inheritDoc} */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            // TODO: this should be done once when the app is installed or updated. The results can be cached in a config file.
            // identify all typefaces in the assets directory
            AssetManager am = getResources().getAssets();
            String fileList[] = null;
            ArrayList<String> entries = new ArrayList<String>();
            ArrayList<String> entryValues = new ArrayList<String>();
            try {
                 fileList = am.list("fonts");
            } catch (IOException e) {
                Logger.e(this.getClass().getName(), "failed to load font assets", e);
            }
            if (fileList != null)
            {
                for (int i = 0; i<fileList.length; i++)
                {
                    File typeface = AppContext.context().getAssetAsFile("fonts/" + fileList[i]);
                    if (typeface != null) {
                        TTFAnalyzer analyzer = new TTFAnalyzer();
                        String fontname = "";
                        fontname = analyzer.getTtfFontName(typeface.getAbsolutePath());
                        if(fontname != null) {
                            // add valid fonts to the list
                            entries.add(fontname);
                            entryValues.add(fileList[i]);
                        }
                    }
                }
            }

            ListPreference pref = (ListPreference)findPreference(KEY_PREF_TRANSLATION_TYPEFACE);
            pref.setEntries(entries.toArray(new CharSequence[entries.size()]));
            pref.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
            bindPreferenceSummaryToValue(pref);
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_TYPEFACE_SIZE));
        }
    }

    /**
     * This fragment shows save preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class SavePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_save_and_sync);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTOSAVE));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTH_SERVER));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_GIT_SERVER));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTH_SERVER_PORT));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_GIT_SERVER_PORT));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_MEDIA_SERVER));
        }
    }

    /**
     * This fragment shows security preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class SharingPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_sharing);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_EXPORT_FORMAT));
        }
    }

    /**
     * This fragment shows security preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class AdvancedPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_advanced);
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_LOGGING_LEVEL));
        }
    }
}
