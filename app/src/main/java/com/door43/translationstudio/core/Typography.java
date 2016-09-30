package com.door43.translationstudio.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;

import java.io.File;

/**
 * Created by joel on 9/11/2015.
 */
public class Typography {

    /**
     * Formats the text in the text view using the users preferences
     * @param context
     * @param view
     * @param langaugeCode the spoken language of the text
     * @param direction the reading direction of the text
     */
    public static void format(Context context, TextView view, String langaugeCode, String direction) {
        Typeface typeface = getTypeface(context, langaugeCode, direction);
        float fontSize = getFontSize(context);

        view.setTypeface(typeface, 0);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }

    /**
     * Formats the text in the text view using the users preferences.
     * Titles are a little larger than normal text and bold
     *
     * @param context
     * @param view
     * @param languageCode the spoken language of the text
     * @param direction the reading direction of the text
     */
    public static void formatTitle(Context context, TextView view, String languageCode, String direction) {
        Typeface typeface = getTypeface(context, languageCode, direction);
        float fontSize = getFontSize(context) * 1.3f;

        view.setTypeface(typeface, Typeface.BOLD);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }

    /**
     * Formats the text in the text view using the users preferences.
     * Sub text is a little smaller than normal text
     *
     * @param context
     * @param view
     * @param langaugeCode the spoken language of the text
     * @param direction the reading direction of the text
     */
    public static void formatSub(Context context, TextView view, String langaugeCode, String direction) {
        Typeface typeface = getTypeface(context, langaugeCode, direction);
        float fontSize = getFontSize(context) * .7f;

        view.setTypeface(typeface, 0);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }

    /**
     * Returns a subset of user preferences (currently, just the size) as a CSS style tag.
     * @param context
     * @return Valid HTML, for prepending to unstyled HTML text
     */
    public static CharSequence getStyle(Context context) {
        return "<style type=\"text/css\">"
                + "body {"
                + "  font-size: " + getFontSize(context) + ";"
                + "}"
                + "</style>";
    }

    /**
     * Returns the font size chosen by the user
     * @param context
     * @return
     */
    public static float getFontSize(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(SettingsActivity.KEY_PREF_TYPEFACE_SIZE, context.getResources().getString(R.string.pref_default_typeface_size)));
    }

    /**
     * Returns the path to the font asset
     * @param context
     * @return
     */
    public static String getAssetPath(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String fontName = prefs.getString(SettingsActivity.KEY_PREF_TRANSLATION_TYPEFACE, context.getResources().getString(R.string.pref_default_translation_typeface));
        return "assets/fonts/" + fontName;
    }

    /**
     * Returns the typeface chosen by the user
     * @param context
     * @param languageCode the spoken language
     * @param direction the reading direction
     * @return
     */
    public static Typeface getTypeface(Context context, String languageCode, String direction) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String fontName = prefs.getString(SettingsActivity.KEY_PREF_TRANSLATION_TYPEFACE, context.getResources().getString(R.string.pref_default_translation_typeface));

        // TODO: provide grahite support
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
}
