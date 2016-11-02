package com.door43.translationstudio.tasks;

import android.os.Process;
import android.text.TextUtils;

import com.door43.translationstudio.rendering.MergeConflictHandler;
import com.door43.translationstudio.ui.translate.ReviewModeAdapter;

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

public class ParseMergeConflictsTask extends ManagedTask {
    public static final String TASK_ID = "parse_merge_conflicts_task";

    public static final String MergeConflictInner =  "(?:<<<<<<<\\s+HEAD\\n)([^<>]*)=======\\n([^<>]*)(?:>>>>>>>.*\\n)";
    public static Pattern MergeConflictPatternInner =  Pattern.compile(MergeConflictInner);
    public static final String MergeConflictFallback =  "(?:<<<<<<<\\s+HEAD\\n)([^<>]*)(=======\\n)?([^<>]*)(?:>>>>>>>.*\\n)";
    public static Pattern MergeConflictPatternFallback =  Pattern.compile(MergeConflictFallback);
    public static final String MergeConflictMiddle =  "(?:=======.*\\n)";
    public static Pattern MergeConflictPatternMiddle =  Pattern.compile(MergeConflictMiddle);

    final private int mMergeConflictColor;
    final private String mSearchText;
    private boolean mFullMergeConflict = false;
    private List<ReviewModeAdapter.MergeConflictCard> mMergeConflictItems = null;
    private MergeConflictHandler renderer = null;

    private Matcher mMatcher;
    private CharSequence mHeadText = "";
    private CharSequence mTailText = "";

    /**
     * do a merge of translations
     * @param mergeConflictColor
     * @param searchText
     */
    public ParseMergeConflictsTask(int mergeConflictColor, String searchText) {
        setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        this.mMergeConflictColor = mergeConflictColor;
        this.mSearchText = searchText;
    }

    @Override
    public void start() {
        mFullMergeConflict = false;
        mMergeConflictItems = new ArrayList<>();
        boolean fullMergeConflict = false;

        boolean found = parseMergeConflicts(mSearchText);

        if(found) {
            mMergeConflictItems.add(new ReviewModeAdapter.MergeConflictCard(mHeadText, fullMergeConflict));
            mMergeConflictItems.add(new ReviewModeAdapter.MergeConflictCard(mTailText, fullMergeConflict));

            // look for nested changes
            boolean changeFound = true;
            while (changeFound) {
                changeFound = false;

                for (int i = 0; i < mMergeConflictItems.size(); i++) {
                    ReviewModeAdapter.MergeConflictCard mergeConflictCard = mMergeConflictItems.get(i);
                    CharSequence mergeText = mergeConflictCard.text;
                    boolean mergeConflicted = MergeConflictHandler.isMergeConflicted(mergeText);
                    if (mergeConflicted) {
                        changeFound = true;
                        found = parseMergeConflicts(mergeText);
                        if(found) {
                            mMergeConflictItems.remove(i); // remove the original since it has been split
                            i--; // back up

                            int count = lookForMultipleMiddles(mHeadText);
                            if(count == 0) {
                                mMergeConflictItems.add(new ReviewModeAdapter.MergeConflictCard(mHeadText, fullMergeConflict));
                            }
                            if(mTailText != null) {
                                count = lookForMultipleMiddles(mTailText);
                                if(count == 0) {
                                    mMergeConflictItems.add(new ReviewModeAdapter.MergeConflictCard(mTailText, fullMergeConflict));
                                }
                            }
                        } else {
                            Logger.e(TASK_ID, "Failed to extract merge conflict from: " + mergeText );
                        }
                    }
                }
            }
        } else { // no merge conflict found
            mMergeConflictItems.add(new ReviewModeAdapter.MergeConflictCard(mSearchText, true)); // only one card found
        }

        // remove duplicates
        for (int i = 0; i < mMergeConflictItems.size() - 1; i++) {
            ReviewModeAdapter.MergeConflictCard conflict1 = mMergeConflictItems.get(i);
            for(int j = i + 1; j < mMergeConflictItems.size(); j++) {
                ReviewModeAdapter.MergeConflictCard conflict2 = mMergeConflictItems.get(j);
                if(conflict1.text.equals(conflict2.text)) {
                    mMergeConflictItems.remove(j); j--; // backup
                }
            }
        }
        mFullMergeConflict = fullMergeConflict && (mMergeConflictItems.size() == 2);
    }

    public int lookForMultipleMiddles(CharSequence searchText) {
        int splitCount = 0;

        if(searchText == null) {
            return 0;
        }
        boolean mergeConflicted = MergeConflictHandler.isMergeConflicted(searchText);
        if (mergeConflicted) { // if we have more unprocessed merges, then skip
            return 0;
        }

        Matcher mMatcher = MergeConflictPatternMiddle.matcher(searchText);
        int startPos = 0;
        CharSequence text = null;
        while(mMatcher.find(startPos)) {
            text = searchText.subSequence(startPos, mMatcher.start());
            mMergeConflictItems.add(new ReviewModeAdapter.MergeConflictCard(text, false));
            splitCount++;
            startPos = mMatcher.end();
        }

        if(splitCount > 0) {
            text = searchText.subSequence(startPos,searchText.length());
            mMergeConflictItems.add(new ReviewModeAdapter.MergeConflictCard(text, false));
            splitCount++;
        }

        return splitCount;
    }

    /**
      * parse nested merge conflicts
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
        if(!found) {
            return false;
        }

        mHeadText = "";
        mTailText = "";

        while(found) {
            CharSequence text = mMatcher.group(1);
            if(text == null) {
                text = "";
            }
            mHeadText = TextUtils.concat(mHeadText, searchText.subSequence(startPos, mMatcher.start()), text);
            text = mMatcher.group(2);
            if(text == null) {
                haveFirstPartOnly = true;
            } else {
                mTailText = TextUtils.concat(mTailText, searchText.subSequence(startPos, mMatcher.start()), text);
            }

            startPos = mMatcher.end();
            found = mMatcher.find(startPos);
        }
        mHeadText = TextUtils.concat(mHeadText, searchText.subSequence(startPos, searchText.length()));
        if(!haveFirstPartOnly) {
            mTailText = TextUtils.concat(mTailText, searchText.subSequence(startPos, searchText.length()));
        } else {
            mTailText = null;
        }
        return true;
    }

    public boolean isFullMergeConflict() {
        return mFullMergeConflict;
    }

    public List<ReviewModeAdapter.MergeConflictCard> getMergeConflictItems() {
        return mMergeConflictItems;
    }

}
