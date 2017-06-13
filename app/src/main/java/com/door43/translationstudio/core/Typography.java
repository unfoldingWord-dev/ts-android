package com.door43.translationstudio.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.ui.SettingsActivity;

import org.json.JSONObject;

/**
 * Created by joel on 9/11/2015.
 */
public class Typography {

    private static String languageSubstituteFontsJson = "{" +
//            "        \"gu\" : \"NotoSansMultiLanguage-Regular.ttf\"," +   // this is how you would override font used in tabs and language lists for a specific language code
            "        \"default\" : \"NotoSansMultiLanguage-Regular.ttf\"" +
            "    }";
    private static JSONObject languageSubstituteFonts = null;
    private static Typeface defaultLanguageTypeface = null;

    /**
     * Formats the text in the text view using the users preferences
     * @param context
     * @param translationType
     * @param view
     * @param languageCode the spoken language of the text
     * @param direction the reading direction of the text
     */
    public static void format(Context context, TranslationType translationType, TextView view, String languageCode, String direction) {
        if(view != null) {
            Typeface typeface = getTypeface(context, translationType, languageCode, direction);
            float fontSize = getFontSize(context, translationType);

            view.setTypeface(typeface, 0);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        }
    }

    /**
     * Formats the text in the text view using the users preferences.
     * Titles are a little larger than normal text and bold
     *
     * @param context
     * @param translationType
     * @param view
     * @param languageCode the spoken language of the text
     * @param direction the reading direction of the text
     */
    public static void formatTitle(Context context, TranslationType translationType, TextView view, String languageCode, String direction) {
        if(view != null) {
            Typeface typeface = getTypeface(context, translationType, languageCode, direction);
            float fontSize = getFontSize(context, translationType) * 1.3f;

            view.setTypeface(typeface, Typeface.BOLD);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        }
    }

    /**
     * Formats the text in the text view using the users preferences.
     * Sub text is a little smaller than normal text
     *
     * @param context
     * @param translationType
     * @param view
     * @param languageCode the spoken language of the text
     * @param direction the reading direction of the text
     */
    public static void formatSub(Context context, TranslationType translationType, TextView view, String languageCode, String direction) {
        if(view != null) {
            Typeface typeface = getTypeface(context, translationType, languageCode, direction);
            float fontSize = getFontSize(context, translationType) * .7f;

            view.setTypeface(typeface, 0);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        }
    }

    /**
     * Returns a subset of user preferences (currently, just the size) as a CSS style tag.
     * @param context
     * @param translationType
     * @return Valid HTML, for prepending to unstyled HTML text
     */
    public static CharSequence getStyle(Context context, TranslationType translationType) {
        return "<style type=\"text/css\">"
                + "body {"
                + "  font-size: " + getFontSize(context, translationType) + ";"
                + "}"
                + "</style>";
    }

    /**
     * Returns the font size chosen by the user
     * @param context
     * @param translationType
     * @return
     */
    public static float getFontSize(Context context, TranslationType translationType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String typefaceSize = (translationType == com.door43.translationstudio.core.TranslationType.SOURCE) ? SettingsActivity.KEY_PREF_SOURCE_TYPEFACE_SIZE :  SettingsActivity.KEY_PREF_TRANSLATION_TYPEFACE_SIZE;
        return Integer.parseInt(prefs.getString(typefaceSize, context.getResources().getString(R.string.pref_default_typeface_size)));
    }

    /**
     * Returns the path to the font asset
     * @param context
     * @param translationType
     * @return
     */
    public static String getAssetPath(Context context, TranslationType translationType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String selectedTypeface = (translationType == com.door43.translationstudio.core.TranslationType.SOURCE) ? SettingsActivity.KEY_PREF_SOURCE_TYPEFACE : SettingsActivity.KEY_PREF_TRANSLATION_TYPEFACE;
        String fontName = prefs.getString(selectedTypeface, context.getResources().getString(R.string.pref_default_translation_typeface));
        return "assets/fonts/" + fontName;
    }

    /**
     * Returns the typeface chosen by the user
     * @param context
     * @param translationType
     * @param languageCode the spoken language
     * @param direction the reading direction
     * @return
     */
    public static Typeface getTypeface(Context context, TranslationType translationType, String languageCode, String direction) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String selectedTypeface = (translationType == com.door43.translationstudio.core.TranslationType.SOURCE) ? SettingsActivity.KEY_PREF_SOURCE_TYPEFACE : SettingsActivity.KEY_PREF_TRANSLATION_TYPEFACE;
        String fontName = prefs.getString(selectedTypeface, context.getResources().getString(R.string.pref_default_translation_typeface));

        Typeface typeface = getTypeface(context, translationType, fontName, languageCode, direction);
        return typeface;
    }

    /**
     * Returns the typeface by font name
     * @param context
     * @param translationType
     * @param languageCode the spoken language
     * @param direction the reading direction
     * @return
     */
    public static Typeface getTypeface(Context context, TranslationType translationType, String fontName, String languageCode, String direction) {

        // TODO: provide graphite support
//        File fontFile = new File(context.getCacheDir(), "assets/fonts" + fontName);
//        if(!fontFile.exists()) {
//            fontFile.getParentFile().mkdirs();
//            try {
//                Util.writeStream(context.getResourceSlugs().getAssets().open("fonts/" + fontName), fontFile);
//            } catch (Exception e) {
//                e.printStackTrace();
//                return;
//            }
//        }
//        if (sEnableGraphite) {
//            TTFAnalyzer analyzer = new TTFAnalyzer();
//            String fontname = analyzer.getTtfFontName(font.getAbsolutePath());
//            if (fontname != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO && Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
//                // assets container, font asset, font name, rtl, language, feats (what's this for????)
//                int translationRTL = l.getDirection() == Language.Direction.RightToLeft ? 1 : 0;
//                try {
//                            customTypeface = (Typeface) Graphite.addFontResource(mContext.getAssets(), "fonts/" + typeFace, fontname, translationRTL, l.getId(), "");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    customTypeface = Typeface.createFromFile(font);
//                }
//            } else {
//                customTypeface = Typeface.createFromFile(font);
//            }
//        }

        Typeface typeface = Typeface.DEFAULT;
        try {
            typeface = Typeface.createFromAsset(context.getAssets(), "fonts/" + fontName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return typeface;
    }

    /**
     * get the font to use for language code. This is the font to be used in tabs and language lists.
     *
     * @param context
     * @param translationType
     * @param code
     * @param direction
     * @return Typeface for font, or Typeface.DEFAULT on error
     */
    public static Typeface getBestFontForLanguage(Context context, TranslationType translationType, String code, String direction) {

        // substitute language font by lookup
        if(languageSubstituteFonts == null) {
            try {
                languageSubstituteFonts = new JSONObject(languageSubstituteFontsJson);
                String defaultSubstituteFont = languageSubstituteFonts.optString("default", null);
                defaultLanguageTypeface = Typography.getTypeface(context, translationType, defaultSubstituteFont, code, direction);
            } catch (Exception e) { }
        }
        if(languageSubstituteFonts != null) {
            String substituteFont = languageSubstituteFonts.optString(code, null);
            if(substituteFont != null) {
                Typeface typeface = Typography.getTypeface(context, translationType, substituteFont, code, direction);
                return typeface;
            } else {
                return defaultLanguageTypeface;
            }
        }
        return Typeface.DEFAULT;
    }
}
