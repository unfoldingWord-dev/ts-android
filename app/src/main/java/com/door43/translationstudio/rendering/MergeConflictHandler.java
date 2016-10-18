package com.door43.translationstudio.rendering;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

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
    public static final int MERGE_HEAD_PART = 1;
    public static final int MERGE_TAIL_PART = 2;
    private boolean mNested = false;
    private boolean mNestedHead = false;
    private boolean mNestedTail = false;
    private CharSequence mHeadText;
    private CharSequence mTailText;
    private boolean mFullBlockMergeConflict = false;

    /**
     * if the merge conflict covers the whole string
     * @return
     */
    public boolean isFullBlockMergeConflict() {
        return mFullBlockMergeConflict;
    }

    /**
     * check if text part has nested merge conflicts
     * @param sourceGroup
     * @return
     */
    public boolean isNested(int sourceGroup) {
        if(sourceGroup == MERGE_HEAD_PART) {
            return mNestedHead;
        } else {
            return mNestedTail;
        }
    }

    /**
     * get text part discovered during renderMergeConflict()
     * @param sourceGroup
     * @return
     */
    public CharSequence getConflictPart(int sourceGroup) {
        if(sourceGroup == MERGE_HEAD_PART) {
            return mHeadText;
        } else {
            return mTailText;
        }
    }

    /**
     * Renders merge conflict selecting specific source and highlighting the changes
     * @param in
     * @param mergeConflictColor
     * @return
     */
    public void renderMergeConflict(CharSequence in, int mergeConflictColor) {
        mHeadText = "";
        mTailText = "";
        Matcher matcher = MergeConflictPatternHead.matcher(in);
        int lastIndex = 0;
        int sectionStart = -1;
        mNestedHead = mNestedTail = mNested = false;
        mFullBlockMergeConflict = false;
        while(matcher.find(lastIndex)) {
            if(sectionStart < 0) {
                sectionStart = matcher.start();
            }

            mNested = false;
            int firstSectionStart = matcher.end();
            FoundRange middleMatcher = findNextDivision( in, firstSectionStart, MergeConflictPatternMiddle);
            if(middleMatcher == null) {
                break;
            }
            int firstSectionEnd = middleMatcher.start;
            if(mNested) {
                mNestedHead = true;
            }

            mNested = false;
            int secondSectionStart = middleMatcher.end;
            FoundRange endMatcher = findNextDivision( in, secondSectionStart, MergeConflictPatternEnd);
            int secondSectionEnd;
            int endSection;

            if(mNested) {
                mNestedTail = true;
            }

            if(endMatcher == null) {
                secondSectionEnd = in.length();
                endSection = secondSectionEnd;
            } else {
                secondSectionEnd = endMatcher.start;
                endSection = endMatcher.end;
            }

            CharSequence previousText = in.subSequence(lastIndex, matcher.start());

            // add head text
            String headText = in.subSequence(firstSectionStart, firstSectionEnd).toString();
            SpannableStringBuilder span = new SpannableStringBuilder(headText);
            span.setSpan(new ForegroundColorSpan(mergeConflictColor), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mHeadText = TextUtils.concat(mHeadText, previousText, span);

            // add tail text
            String endText = in.subSequence(secondSectionStart, secondSectionEnd).toString();
            span = new SpannableStringBuilder(endText);
            span.setSpan(new ForegroundColorSpan(mergeConflictColor), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTailText = TextUtils.concat(mTailText, previousText, span);

            lastIndex = endSection;
        }

        CharSequence remainingText = in.subSequence(lastIndex, in.length());
        mHeadText = TextUtils.concat(mHeadText, remainingText);
        mTailText = TextUtils.concat(mTailText, remainingText);

        if( (sectionStart == 0)
                && (lastIndex >= in.length())) {
            mFullBlockMergeConflict = true;
        }
    }

    /**
     * search for first merge conflict
     * TODO: instead of this we need to take users to a filtered mode that just displays the merge conflicts
     * @param targetTranslationId
     * @return
     */
    @Deprecated
    static public CardLocation findFirstMergeConflict(String targetTranslationId) {
        return null;
//        Translator translator = App.getTranslator();
//
//        TargetTranslation targetTranslation = translator.getTargetTranslation(targetTranslationId);
//
//        SourceTranslation[] sourceTranslations = library.getSourceTranslations(targetTranslation.getProjectId());
//        if(sourceTranslations.length <= 0) {
//            return null;
//        }
//        SourceTranslation sourceTranslation = sourceTranslations[0];
//        Chapter[] chapters = library.getChapters(sourceTranslation);
//
//        //check for project title
//        String projectTitle = sourceTranslation.getProjectTitle();
//        if(projectTitle != null) {
//            ProjectTranslation projectTranslation = targetTranslation.getProjectTranslation();
//            if(isMergeConflicted(projectTranslation.getTitle())) {
//                return new CardLocation("00","00");
//            }
//        }
//
//        for(int i = 0; i < chapters.length; i ++) {
//            Chapter chapter = chapters[i];
//            String chapterStr = chapter.getId();
//
//            Frame[] frames = library.getFrames(sourceTranslation, chapter.getId());
//
//            ChapterTranslation chapterTranslation = targetTranslation.getChapterTranslation(chapter.getId());
//            boolean isInvalidChapterTitle = (chapter.title != null) && (!chapter.title.isEmpty());
//            if(!isInvalidChapterTitle) {
//                if(MergeConflictHandler.isMergeConflicted(chapterTranslation.title)) {
//                    return new CardLocation(chapterStr, "00");
//                }
//            }
//
//            boolean isInvalidRef = (chapter.reference != null) && (!chapter.reference.isEmpty());
//            if(!isInvalidRef) {
//                if(MergeConflictHandler.isMergeConflicted(chapterTranslation.reference)) {
//                    return new CardLocation(chapterStr, "00");
//                }
//            }
//
//            for(int j = 0; j < frames.length; j ++) {
//                Frame frame = frames[j];
//                boolean isValidFrame = (frame.body != null) && !frame.body.isEmpty();
//                if(isValidFrame) {
//                    FrameTranslation frameTranslation = targetTranslation.getFrameTranslation(frame);
//                    if(MergeConflictHandler.isMergeConflicted(frameTranslation.body)) {
//                        return new CardLocation(chapterStr, frame.getId());
//                    }
//                }
//            }
//        }
//        return null;
    }

    /**
     * finds next part and handle nesting
     * @param in
     * @return
     */
    private FoundRange findNextDivision(CharSequence in, int startPos, Pattern pattern) {
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

                    mNested = true;

                    //find the end of nesting
                    FoundRange endNested = findNextDivision(in, nestedMatcher.end, MergeConflictPatternEnd);
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
