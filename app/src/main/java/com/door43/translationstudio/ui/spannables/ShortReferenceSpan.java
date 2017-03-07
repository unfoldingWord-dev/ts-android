package com.door43.translationstudio.ui.spannables;

import com.door43.util.StringUtilities;

import java.util.regex.Pattern;

/**
 * Created by joel on 3/7/17.
 */

public class ShortReferenceSpan extends Span {
    public static final Pattern PATTERN = Pattern.compile("\\b(\\d+)\\:(\\d+)\\b");
    private String mChapter;
    private String mVerse;

    public ShortReferenceSpan(String reference) {
        super(reference, reference);
        String[] pieces = reference.split("\\:");
        try {
            mChapter = StringUtilities.normalizeSlug(pieces[0]);
        } catch (Exception e) {
            e.printStackTrace();
            mChapter = pieces[0];
        }
        try {
            mVerse = StringUtilities.normalizeSlug(pieces[1]);
        } catch (Exception e) {
            e.printStackTrace();
            mVerse = pieces[1];
        }
    }

    public String getVerse() {
        return mVerse;
    }

    public String getChapter() {
        return mChapter;
    }
}
