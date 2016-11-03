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
    private CharSequence mHeadText = "";
    private CharSequence mTailText = "";

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
            mMergeConflictItems.add(mHeadText);
            mMergeConflictItems.add(mTailText);

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

                            int count = lookForMultipleMiddles(mHeadText);
                            if (count == 0) {
                                mMergeConflictItems.add(mHeadText);
                            }
                            if (mTailText != null) {
                                count = lookForMultipleMiddles(mTailText);
                                if (count == 0) {
                                    mMergeConflictItems.add(mTailText);
                                }
                            }
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

    public int lookForMultipleMiddles(CharSequence searchText) {
        int splitCount = 0;

        if (searchText == null) {
            return 0;
        }
        boolean mergeConflicted = isMergeConflicted(searchText);
        if (mergeConflicted) { // if we have more unprocessed merges, then skip
            return 0;
        }

        Matcher mMatcher = MergeConflictPatternMiddle.matcher(searchText);
        int startPos = 0;
        CharSequence text = null;
        while (mMatcher.find(startPos)) {
            text = searchText.subSequence(startPos, mMatcher.start());
            mMergeConflictItems.add(text);
            splitCount++;
            startPos = mMatcher.end();
        }

        if (splitCount > 0) {
            text = searchText.subSequence(startPos, searchText.length());
            mMergeConflictItems.add(text);
            splitCount++;
        }

        return splitCount;
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

        mHeadText = "";
        mTailText = "";

        while (found) {
            CharSequence text = mMatcher.group(1);
            if (text == null) {
                text = "";
            }
            mHeadText = TextUtils.concat(mHeadText, searchText.subSequence(startPos, mMatcher.start()), text);
            text = mMatcher.group(2);
            if (text == null) {
                haveFirstPartOnly = true;
            } else {
                mTailText = TextUtils.concat(mTailText, searchText.subSequence(startPos, mMatcher.start()), text);
            }

            startPos = mMatcher.end();
            found = mMatcher.find(startPos);
        }
        mHeadText = TextUtils.concat(mHeadText, searchText.subSequence(startPos, searchText.length()));
        if (!haveFirstPartOnly) {
            mTailText = TextUtils.concat(mTailText, searchText.subSequence(startPos, searchText.length()));
        } else {
            mTailText = null;
        }
        return true;
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
