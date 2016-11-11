package com.door43.translationstudio.tasks;

import android.os.Process;
import android.text.TextUtils;

import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.Process.setThreadPriority;

/**
 * Created by blm on 11/2/16.
 */

public class MergeConflictsParseTask extends ManagedTask {
    public static final String TASK_ID = "parse_merge_conflicts_task";

    public static final String MergeConflictInner = "(?:<<<<<<<\\s+HEAD\\n)([^<>]*)=======\\n([^<>]*)(?:>>>>>>>.*\\n)";
    public static Pattern MergeConflictPatternInner = Pattern.compile(MergeConflictInner);
    public static final String MergeConflictFallback = "(?:<<<<<<<\\s+HEAD\\n)([^<>]*)(=======\\n)?([^<>]*)(?:>>>>>>>.*\\n)";
    public static Pattern MergeConflictPatternFallback = Pattern.compile(MergeConflictFallback);
    public static final String MergeConflictMiddle = "(?:=======.*\\n)";
    public static Pattern MergeConflictPatternMiddle = Pattern.compile(MergeConflictMiddle);
    public static final String MergeConflictHead = "(?:<<<<<<< HEAD.*\\n)";
    public static Pattern MergeConflictPatternHead = Pattern.compile(MergeConflictHead);

    final private String mSearchText;
    private List<CharSequence> mMergeConflictItems = null;

    private Matcher mMatcher;
    private List<CharSequence> mHeadText = null;
    private List<CharSequence> mTailText = null;

    /**
     * do a merge of translations
     *
     * @param searchText
     */
    public MergeConflictsParseTask(String searchText) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.mSearchText = searchText;
    }

    @Override
    public void start() {
        mMergeConflictItems = new ArrayList<>();
        boolean fullMergeConflict = false;

        boolean found = parseMergeConflicts(mSearchText);
        if (found) {
            // look for nested changes
            boolean changeFound = true;
            while (changeFound) {
                changeFound = false;

                for (int i = 0; i < mMergeConflictItems.size(); i++) {
                    CharSequence mergeText = mMergeConflictItems.get(i);
                    boolean mergeConflicted = isMergeConflicted(mergeText);
                    if (mergeConflicted) {
                        changeFound = true;
                        found = parseMergeConflicts(mergeText);
                        if (found) {
                            mMergeConflictItems.remove(i); // remove the original since it has been split
                            i--; // back up

                        } else {
                            Logger.e(TASK_ID, "Failed to extract merge conflict from: " + mergeText);
                        }
                    }
                }
            }
        } else { // no merge conflict found
            mMergeConflictItems.add(mSearchText); // only one card found
        }

        // remove duplicates
        for (int i = 0; i < mMergeConflictItems.size() - 1; i++) {
            CharSequence conflict1 = mMergeConflictItems.get(i);
            for (int j = i + 1; j < mMergeConflictItems.size(); j++) {
                CharSequence conflict2 = mMergeConflictItems.get(j);
                if (conflict1.equals(conflict2)) {
                    mMergeConflictItems.remove(j);
                    j--; // backup
                }
            }
        }
    }

    /**
     * parse nested merge conflicts
     *
     * @param searchText
     * @return
     */
    public boolean parseMergeConflicts(CharSequence searchText) {
        int startPos = 0;
        boolean haveFirstPartOnly = false;
        boolean fullMergeConflict = false;
        mMatcher = MergeConflictPatternInner.matcher(searchText);
        boolean found = mMatcher.find();
        if (!found) {
            mMatcher = MergeConflictPatternFallback.matcher(searchText);
            found = mMatcher.find();
        }
        if (!found) {
            return false;
        }

        mHeadText = new ArrayList<>();
        mTailText = new ArrayList<>();

        while (found) {
            CharSequence text = mMatcher.group(1);
            if (text == null) {
                text = "";
            }
            List<CharSequence> middles = extractMiddles(text, mHeadText.size());
            for (int i = 0; i < middles.size(); i++) {
                if(mHeadText.size() <= i) {
                    mHeadText.add("");
                }

                CharSequence headText = mHeadText.get(i);
                headText = TextUtils.concat(headText, searchText.subSequence(startPos, mMatcher.start()), middles.get(i));
                mHeadText.set(i, headText);
            }

            text = mMatcher.group(2);
            if ((text == null)  && (mTailText.size() <= 0)) {
                haveFirstPartOnly = true;
            } else {
                if (text == null) {
                    text = "";
                }

                middles = extractMiddles(text, mTailText.size());
                for (int i = 0; i < middles.size(); i++) {
                    if(mTailText.size() <= i) {
                        mTailText.add("");
                    }

                    CharSequence tailText = mTailText.get(i);
                    tailText = TextUtils.concat(tailText, searchText.subSequence(startPos, mMatcher.start()), middles.get(i));
                    mTailText.set(i, tailText);
                }
             }

            startPos = mMatcher.end();
            found = mMatcher.find(startPos);
        }

        CharSequence endText = searchText.subSequence(startPos, searchText.length());

        for (int i = 0; i < mHeadText.size(); i++) {
            CharSequence headText = mHeadText.get(i);
            headText = TextUtils.concat(headText, endText);
            mMergeConflictItems.add(headText);
        }

        for (int i = 0; i < mTailText.size(); i++) {
            CharSequence tailText = mTailText.get(i);
            tailText = TextUtils.concat(tailText, endText);
            mMergeConflictItems.add(tailText);
        }

        return true;
    }

    /**
     * normally there is just one middle divider, but in multiway merges there can be more.  So we check for these.
     * @param searchText
     * @param desiredCount
     * @return
     */
    public List<CharSequence> extractMiddles(CharSequence searchText, int desiredCount) {
        List<CharSequence> middles = lookForMultipleMiddles(searchText);

        // if no middle markers found, we just use the searchText
        if(middles.size() == 0) {
            middles.add(searchText);
        }

        // normalize the middles count to match previous
        while(middles.size() < desiredCount) {
            middles.add(middles.get(0)); // clone
        }

        return middles;
    }

    /**
     * search for middle markers within text
     * @param searchText
     * @return
     */
    public List<CharSequence> lookForMultipleMiddles(CharSequence searchText) {
        List<CharSequence> middles = new ArrayList<>();

        if (searchText == null) {
            return middles;
        }
        boolean mergeConflicted = isMergeConflicted(searchText);
        if (mergeConflicted) { // if we have more unprocessed merges, then skip
            return middles;
        }

        Matcher mMatcher = MergeConflictPatternMiddle.matcher(searchText);
        int startPos = 0;
        CharSequence text = null;
        while (mMatcher.find(startPos)) {
            text = searchText.subSequence(startPos, mMatcher.start());
            middles.add(text);
            startPos = mMatcher.end();
        }

        text = searchText.subSequence(startPos, searchText.length());
        middles.add(text);

        return middles;
    }


    /**
     * Detects merge conflict tags
     *
     * @param text
     * @return
     */
    static public boolean isMergeConflicted(CharSequence text) {
        if ((text != null) && (text.length() > 0)) {
            Matcher matcher = MergeConflictPatternHead.matcher(text);
            boolean matchFound = matcher.find();
            return matchFound;
        }
        return false;
    }

    public List<CharSequence> getMergeConflictItems() {
        return mMergeConflictItems;
    }

    /**
     * search for first merge conflict
     * TODO: instead of this we need to take users to a filtered mode that just displays the merge conflicts
     *
     * @param targetTranslationId
     * @return
     */
    @Deprecated
    static public CardLocation findFirstMergeConflict(String targetTranslationId) {
        return null;
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
