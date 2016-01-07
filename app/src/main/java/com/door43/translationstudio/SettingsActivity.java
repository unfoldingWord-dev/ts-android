package com.door43.translationstudio;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.newui.DeviceNetworkAliasDialog;
import com.door43.translationstudio.newui.ProfileDialog;
import com.door43.translationstudio.newui.legal.LegalDocumentActivity;
import com.door43.translationstudio.service.BackupService;
import com.door43.util.TTFAnalyzer;

import org.apache.commons.io.FilenameUtils;

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
//    public static final String KEY_PREF_AUTOSAVE = "autosave";
    public static final String KEY_PREF_AUTH_SERVER = "auth_server";
    public static final String KEY_PREF_AUTH_SERVER_PORT = "auth_server_port";
    public static final String KEY_PREF_GIT_SERVER = "git_server";
    public static final String KEY_PREF_GIT_SERVER_PORT = "git_server_port";
    public static final String KEY_PREF_REMEMBER_POSITION = "remember_position";
    public static final String KEY_PREF_ALWAYS_SHARE = "always_share";
    public static final String KEY_PREF_MEDIA_SERVER = "media_server";
//    public static final String KEY_PREF_EXPORT_FORMAT = "export_format";
    public static final String KEY_PREF_TRANSLATION_TYPEFACE = "translation_typeface";
    public static final String KEY_PREF_TYPEFACE_SIZE = "typeface_size";
//    public static final String KEY_PREF_HIGHLIGHT_KEY_TERMS = "highlight_key_terms";
//    public static final String KEY_PREF_ADVANCED_SETTINGS = "advanced_settings";
    public static final String KEY_PREF_LOGGING_LEVEL = "logging_level";
    public static final String KEY_PREF_BACKUP_INTERVAL = "backup_interval";
    public static final String KEY_PREF_DEVICE_ALIAS = "device_name";
    public static final String KEY_PREF_PROFILES = "profiles";
    public static final String KEY_SDCARD_ACCESS_URI = "internal_uri_extsdcard";
    public static final String KEY_SDCARD_ACCESS_FLAGS = "internal_flags_extsdcard";

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
                LegalPreferenceFragment.class.getName(),
//                SharingPreferenceFragment.class.getName()
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_settings, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

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
        addPreferencesFromResource(R.xml.general_preferences);

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
                    if(fontname == null) {
                        fontname = FilenameUtils.removeExtension(typeface.getName());
                    }
                    entries.add(fontname);
                    entryValues.add(fileList[i]);
                }
            }
        }

        ListPreference pref = (ListPreference)findPreference(KEY_PREF_TRANSLATION_TYPEFACE);
        pref.setEntries(entries.toArray(new CharSequence[entries.size()]));
        pref.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
        bindPreferenceSummaryToValue(pref);

        // Add 'sharing' preferences, and a corresponding header.
//        PreferenceCategory preferenceHeader = new PreferenceCategory(this);
//        preferenceHeader.setTitle(R.string.pref_header_sharing);
//        getPreferenceScreen().addPreference(preferenceHeader);
//        addPreferencesFromResource(R.xml.sharing_preferences);

        // Add 'server' preferences, and a corresponding header.
        PreferenceCategory preferenceHeader = new PreferenceCategory(this);
        preferenceHeader.setTitle(R.string.pref_header_synchronization);
        getPreferenceScreen().addPreference(preferenceHeader);
        addPreferencesFromResource(R.xml.server_preferences);

        // Add 'legal' preferences, and a corresponding header.
        preferenceHeader = new PreferenceCategory(this);
        preferenceHeader.setTitle(R.string.pref_header_legal);
        getPreferenceScreen().addPreference(preferenceHeader);
        addPreferencesFromResource(R.xml.legal_preferences);

        // add 'advanced' preferences and coresponding hreader
        preferenceHeader = new PreferenceCategory(this);
        preferenceHeader.setTitle(R.string.pref_header_advanced);
        getPreferenceScreen().addPreference(preferenceHeader);
        addPreferencesFromResource(R.xml.advanced_preferences);

        // bind the correct legal document to the preference intent
        bindPreferenceClickToLegalDocument(findPreference("license_agreement"), R.string.license);
        bindPreferenceClickToLegalDocument(findPreference("statement_of_faith"), R.string.statement_of_faith);
        bindPreferenceClickToLegalDocument(findPreference("translation_guidelines"), R.string.translation_guidlines);
        bindPreferenceClickToLegalDocument(findPreference("software_licenses"), R.string.software_licenses);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
//        bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTOSAVE));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTH_SERVER));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_GIT_SERVER));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTH_SERVER_PORT));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_GIT_SERVER_PORT));
//        bindPreferenceSummaryToValue(findPreference(KEY_PREF_EXPORT_FORMAT));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_MEDIA_SERVER));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_LOGGING_LEVEL));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_BACKUP_INTERVAL));
        bindPreferenceSummaryToValue(findPreference(KEY_PREF_TYPEFACE_SIZE));
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
            loadHeadersFromResource(R.xml.headers, target);
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

            if(preference.getKey().equals(KEY_PREF_BACKUP_INTERVAL)) {
                // restart the backup service.
                if(BackupService.isRunning()) {
                    // TODO: only restart if changed
                    Intent backupIntent = new Intent(AppContext.context(), BackupService.class);
                    AppContext.context().stopService(backupIntent);
                    AppContext.context().startService(backupIntent);
                }
            } else if(preference.getKey().equals(KEY_PREF_LOGGING_LEVEL)) {
                // TODO: only re-configure if changed
                AppContext.context().configureLogger(Integer.parseInt((String)value));
            }

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
            addPreferencesFromResource(R.xml.general_preferences);

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
                        if(fontname == null) {
                            fontname = FilenameUtils.removeExtension(typeface.getName());
                        }
                        // add valid fonts to the list
                        entries.add(fontname);
                        entryValues.add(fileList[i]);
                    }
                }
            }

            bindPreferenceSummaryToValue(findPreference(KEY_PREF_DEVICE_ALIAS));

            final Preference profilesPref = findPreference(KEY_PREF_PROFILES);
            profilesPref.setSummary(AppContext.getProfileSummary());
            profilesPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ProfileDialog d = new ProfileDialog();
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    d.show(ft, ProfileDialog.TAG);
                    d.setOnDismissListener(new ProfileDialog.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            profilesPref.setSummary(AppContext.getProfileSummary());
                        }
                    });
                    return true;
                }
            });

            ListPreference fontPref = (ListPreference)findPreference(KEY_PREF_TRANSLATION_TYPEFACE);
            fontPref.setEntries(entries.toArray(new CharSequence[entries.size()]));
            fontPref.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
            bindPreferenceSummaryToValue(fontPref);

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
            addPreferencesFromResource(R.xml.server_preferences);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
//            bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTOSAVE));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTH_SERVER));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_GIT_SERVER));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_AUTH_SERVER_PORT));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_GIT_SERVER_PORT));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_MEDIA_SERVER));
        }
    }

    /**
     * This frgment shows legal documents.
     * It is used when the activity is showing a two-pane settings UI.
     */
    public static class LegalPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.legal_preferences);

            bindPreferenceClickToLegalDocument(findPreference("license_agreement"), R.string.license);
            bindPreferenceClickToLegalDocument(findPreference("statement_of_faith"), R.string.statement_of_faith);
            bindPreferenceClickToLegalDocument(findPreference("translation_guidelines"), R.string.translation_guidlines);
            bindPreferenceClickToLegalDocument(findPreference("software_licenses"), R.string.software_licenses);
        }
    }

    /**
     * Intercepts clicks and passes the resource to the intent.
     * The preference should be configured as an action to an intent for the LegalDocumentActivity
     * @param preference
     * @param res
     */
    public static void bindPreferenceClickToLegalDocument(Preference preference, final int res) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = preference.getIntent();
                intent.putExtra(LegalDocumentActivity.ARG_RESOURCE, res);
                preference.setIntent(intent);
                return false;
            }
        });
    }

    /**
     * This fragment shows security preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
//    public static class SharingPreferenceFragment extends PreferenceFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.sharing_preferences);
//
//            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
//            // to their values. When their values change, their summaries are
//            // updated to reflect the new value, per the Android Design
//            // guidelines.
////            bindPreferenceSummaryToValue(findPreference(KEY_PREF_EXPORT_FORMAT));
//        }
//    }

    /**
     * This fragment shows security preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class AdvancedPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.advanced_preferences);

            bindPreferenceSummaryToValue(findPreference(KEY_PREF_LOGGING_LEVEL));
            bindPreferenceSummaryToValue(findPreference(KEY_PREF_BACKUP_INTERVAL));
        }
    }
}
