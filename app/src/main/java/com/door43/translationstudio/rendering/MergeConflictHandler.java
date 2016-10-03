package com.door43.translationstudio.rendering;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.ProjectTranslation;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;

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
            FoundRange middleMatcher = findNestedSection( in, firstSectionStart, MergeConflictPatternMiddle);
            if(middleMatcher == null) {
                break;
            }
            int firstSectionEnd = middleMatcher.start;

            int secondSectionStart = middleMatcher.end;
            FoundRange endMatcher = findNestedSection( in, secondSectionStart, MergeConflictPatternEnd);
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
            span.setSpan(new ForegroundColorSpan(highlightColor), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), span);
            lastIndex = endSection;
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    /**
     * search for first merge conflict
     * @param targetTranslationId
     * @return
     */
    static public CardLocation findFirstMergeConflict(String targetTranslationId) {
        Library library = App.getLibrary();
        Translator translator = App.getTranslator();

        TargetTranslation targetTranslation = translator.getTargetTranslation(targetTranslationId);
        SourceTranslation[] sourceTranslations = library.getSourceTranslations(targetTranslation.getProjectId());
        if(sourceTranslations.length <= 0) {
            return null;
        }
        SourceTranslation sourceTranslation = sourceTranslations[0];
        Chapter[] chapters = library.getChapters(sourceTranslation);

        //check for project title
        String projectTitle = sourceTranslation.getProjectTitle();
        if(projectTitle != null) {
            ProjectTranslation projectTranslation = targetTranslation.getProjectTranslation();
            if(isMergeConflicted(projectTranslation.getTitle())) {
                return new CardLocation("00","00");
            }
        }

        for(int i = 0; i < chapters.length; i ++) {
            Chapter chapter = chapters[i];
            String chapterStr = chapter.getId();

            Frame[] frames = library.getFrames(sourceTranslation, chapter.getId());

            ChapterTranslation chapterTranslation = targetTranslation.getChapterTranslation(chapter.getId());
            boolean isInvalidChapterTitle = (chapter.title != null) && (!chapter.title.isEmpty());
            if(!isInvalidChapterTitle) {
                if(MergeConflictHandler.isMergeConflicted(chapterTranslation.title)) {
                    return new CardLocation(chapterStr, "00");
                }
            }

            boolean isInvalidRef = (chapter.reference != null) && (!chapter.reference.isEmpty());
            if(!isInvalidRef) {
                if(MergeConflictHandler.isMergeConflicted(chapterTranslation.reference)) {
                    return new CardLocation(chapterStr, "00");
                }
            }

            for(int j = 0; j < frames.length; j ++) {
                Frame frame = frames[j];
                boolean isValidFrame = (frame.body != null) && !frame.body.isEmpty();
                if(isValidFrame) {
                    FrameTranslation frameTranslation = targetTranslation.getFrameTranslation(frame);
                    if(MergeConflictHandler.isMergeConflicted(frameTranslation.body)) {
                        return new CardLocation(chapterStr, frame.getId());
                    }
                }
            }
        }
        return null;
    }

    /**
     * finds next part and handle nesting
     * @param in
     * @return
     */
    static private FoundRange findNestedSection(CharSequence in, int startPos, Pattern pattern) {
        FoundRange matcher = findFirst(in, startPos, pattern);
        if(matcher != null) {
            int newStartPos = startPos;
            while(true) {
                FoundRange nestedMatcher = findFirst(in, newStartPos, MergeConflictPatternHead);
                if( nestedMatcher == null) { // if no more nesting found
                    return matcher;
                } else {
                    if( nestedMatcher.start > matcher.start ) {
                        return matcher;
                    }

                    //find the end of nesting
                    FoundRange endNested = findNestedSection(in, nestedMatcher.end, MergeConflictPatternEnd);
                    if (endNested == null) { // if no end found
                        return new FoundRange(matcher.start, in.length());
                    }

                    newStartPos = endNested.end;
                    FoundRange endMatcher = findFirst(in, endNested.end, pattern); // try again to find pattern
                    if(endMatcher == null) {
                        return new FoundRange(matcher.start, in.length());
                    }

                    matcher = endMatcher;
                    newStartPos = endMatcher.end;
                }
            }
         }
        return null;
    }

    static private FoundRange findFirst(CharSequence in, int startPos, Pattern pattern) {
        Matcher matcher = pattern.matcher(in);
        if (matcher.find(startPos)) {
            return new FoundRange(matcher);
        }
        return null;
    }

    /**
     * Detects merge conflict tags
     * @param text
     * @return
     */
    static public boolean isMergeConflicted(CharSequence text) {
        if((text != null) && (text.length() > 0) ) {
            Matcher matcher = MergeConflictPatternHead.matcher(text);
            boolean matchFound = matcher.find();
            return matchFound;
        }
        return false;
    }

    static public class FoundRange {
        public final int start;
        public final int end;

        FoundRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        FoundRange(Matcher matcher) {
            if(matcher == null) {
                this.start = -1;
                this.end = -1;
            } else {
                this.start = matcher.start();
                this.end = matcher.end();
            }
        }
    }

    static public class CardLocation {
        public final String chapterID;
        public final String frameID;

        CardLocation(String chapterID, String frameID) {
            this.chapterID = chapterID;
            this.frameID = frameID;
        }
    }
}
