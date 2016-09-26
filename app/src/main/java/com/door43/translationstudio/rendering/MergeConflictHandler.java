package com.door43.translationstudio.rendering;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;

import com.door43.translationstudio.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 2/16/2016.
 * This cannot be used because our current rendering implimentation is insufficient.
 */
public class MergeConflictHandler {

    public static final String MergeConflict =  "(?:<<<<<<< HEAD.*\\n)([^=======]*)(?:=======.*\\n)([^<<<<<<<]+)(?:>>>>>>>.*\\n)";
    public static Pattern MergeConflictPattern =  Pattern.compile(MergeConflict);
    public static final int MergeHeadPart = 1;
    public static final int MergeTailPart = 2;

    /**
     * Renders merge conflict selecting specific source
     * @param in
     * @param sourceGroup
     * @return
     */
    public CharSequence renderMergeConflict(CharSequence in, int sourceGroup) {
        return renderMergeConflict( in, sourceGroup, R.color.default_background_color);
    }

    /**
     * Renders merge conflict selecting specific source and highlighting the changes
     * @param in
     * @param sourceGroup
     * @param highlightColor
     * @return
     */
    public CharSequence renderMergeConflict(CharSequence in, int sourceGroup, int highlightColor) {
        CharSequence out = "";
        Matcher matcher = MergeConflictPattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            String groupText = matcher.group(sourceGroup);
            SpannableStringBuilder span = new SpannableStringBuilder(groupText);
            span.setSpan(new BackgroundColorSpan(highlightColor), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), span);
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * Detects merge conflict tags
     * @param in
     * @return
     */
    static public boolean isMergeConflicted(CharSequence in) {
        Matcher matcher = MergeConflictPattern.matcher(in);
        boolean matchFound = matcher.find();
        return matchFound;
    }
}
