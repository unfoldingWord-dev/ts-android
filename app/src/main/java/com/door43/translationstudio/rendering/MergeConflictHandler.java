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

    public static final String MergeConflictHead =  "(?:<<<<<<< HEAD.*\\n)";
    public static Pattern MergeConflictPatternHead =  Pattern.compile(MergeConflictHead);
    public static final String MergeConflictMiddle =  "(?:=======.*\\n)";
    public static Pattern MergeConflictPatternMiddle =  Pattern.compile(MergeConflictMiddle);
    public static final String MergeConflictEnd =  "(?:>>>>>>>.*\\n)";
    public static Pattern MergeConflictPatternEnd =  Pattern.compile(MergeConflictEnd);
    public static final int MergeHeadPart = 1;
    public static final int MergeTailPart = 2;

    /**
     * Renders merge conflict selecting specific source
     * @param in
     * @param sourceGroup
     * @return
     */
    static public CharSequence renderMergeConflict(CharSequence in, int sourceGroup) {
        return renderMergeConflict( in, sourceGroup, R.color.default_background_color);
    }

    /**
     * Renders merge conflict selecting specific source and highlighting the changes
     * @param in
     * @param sourceGroup
     * @param highlightColor
     * @return
     */
    static public CharSequence renderMergeConflict(CharSequence in, int sourceGroup, int highlightColor) {
        CharSequence out = "";
        Matcher matcher = MergeConflictPatternHead.matcher(in);
        int lastIndex = 0;
        while(matcher.find(lastIndex)) {
            int firstSectionStart = matcher.end();
            foundRange middleMatcher = findNestedSection( in, firstSectionStart, MergeConflictPatternMiddle);
            if(middleMatcher == null) {
                break;
            }
            int firstSectionEnd = middleMatcher.start;

            int secondSectionStart = middleMatcher.end;
            foundRange endMatcher = findNestedSection( in, secondSectionStart, MergeConflictPatternEnd);
            int secondSectionEnd;
            int endSection;
            if(endMatcher == null) {
                secondSectionEnd = in.length();
                endSection = secondSectionEnd;
            } else {
                secondSectionEnd = endMatcher.start;
                endSection = endMatcher.end;
            }

            String groupText;
            if(sourceGroup == MergeHeadPart) {
                groupText = in.subSequence(firstSectionStart, firstSectionEnd).toString();
            } else  {
                groupText = in.subSequence(secondSectionStart, secondSectionEnd).toString();
            }

            SpannableStringBuilder span = new SpannableStringBuilder(groupText);
            span.setSpan(new BackgroundColorSpan(highlightColor), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), span);
            lastIndex = endSection;
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * finds next part and handle nesting
     * @param in
     * @return
     */
    static private foundRange findNestedSection(CharSequence in, int startPos, Pattern pattern) {
        foundRange matcher = findFirst(in, startPos, pattern);
        if(matcher != null) {
            int newStartPos = startPos;
            while(true) {
                foundRange nestedMatcher = findFirst(in, newStartPos, MergeConflictPatternHead);
                if( nestedMatcher == null) { // if no more nesting found
                    return matcher;
                } else {
                    if( nestedMatcher.start > matcher.start ) {
                        return matcher;
                    }

                    //find the end of nesting
                    foundRange endNested = findNestedSection(in, nestedMatcher.end, MergeConflictPatternEnd);
                    if (endNested == null) { // if no end found
                        return new foundRange(matcher.start, in.length());
                    }

                    newStartPos = endNested.end;
                    foundRange endMatcher = findFirst(in, endNested.end, pattern); // try again to find pattern
                    if(endMatcher == null) {
                        return new foundRange(matcher.start, in.length());
                    }

                    matcher = endMatcher;
                    newStartPos = endMatcher.end;
                }
            }
         }
        return null;
    }

    static private foundRange findFirst(CharSequence in, int startPos, Pattern pattern) {
        Matcher matcher = pattern.matcher(in);
        if (matcher.find(startPos)) {
            return new foundRange(matcher);
        }
        return null;
    }

    /**
     * Detects merge conflict tags
     * @param text
     * @return
     */
    static public boolean isMergeConflicted(CharSequence text) {
        if(text != null) {
            Matcher matcher = MergeConflictPatternHead.matcher(text);
            boolean matchFound = matcher.find();
            return matchFound;
        }
        return false;
    }

    static public class foundRange {
        public final int start;
        public final int end;

        foundRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        foundRange(Matcher matcher) {
            if(matcher == null) {
                this.start = -1;
                this.end = -1;
            } else {
                this.start = matcher.start();
                this.end = matcher.end();
            }
        }
    }
}
