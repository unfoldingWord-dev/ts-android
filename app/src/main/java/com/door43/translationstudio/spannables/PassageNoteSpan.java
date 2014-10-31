package com.door43.translationstudio.spannables;

import com.door43.translationstudio.R;

/**
 * Created by joel on 10/28/2014.
 */
public class PassageNoteSpan extends FancySpan {
    private final String mDefinition;

    public PassageNoteSpan(String id, String text, String definition, OnClickListener clickListener) {
        super(id, text, clickListener);
        mDefinition = definition.replace("\"", "'");
    }

    /**
     * Converts the footnote to a spannable char sequence
     * @return
     */
    public CharSequence toCharSequence() {
        return generateSpan(generateTag(toString(), mDefinition), R.drawable.light_blue_bubble, R.color.blue, R.dimen.h5);
    }

    /**
     * Generates the proper footnote tag
     * @param title
     * @param definition
     * @return
     */
    public static String generateTag(String title, String definition) {
        return "<a def=\""+definition+"\">"+title+"</a>";
    }
}
