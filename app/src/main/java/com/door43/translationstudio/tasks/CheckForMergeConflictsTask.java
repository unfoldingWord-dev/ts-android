package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.ui.translate.ListItem;

import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.List;

/**
 * Created by blm on 10/21/16.
 */

public class CheckForMergeConflictsTask extends ManagedTask {
    public static final String TASK_ID = "check_for_merge_conflicts_task";
    private boolean mFoundMergeConflict;
    List<ListItem> mItems;
    ResourceContainer mSourceContainer;
    TargetTranslation mTargetTranslation;

    public CheckForMergeConflictsTask(List<ListItem> items, ResourceContainer sourceContainer, TargetTranslation targetTranslation) {
        mFoundMergeConflict = false;
        mItems = items;
        mSourceContainer = sourceContainer;
        mTargetTranslation = targetTranslation;
    }

    @Override
    public void start() {
        mFoundMergeConflict = false;
        if(mItems != null) {
            for (ListItem item : mItems) {
                item.load(mSourceContainer, mTargetTranslation);
                if(item.hasMergeConflicts) {
                    mFoundMergeConflict = true;
                    break; // done - only need to find the first
                }
            }
        }
    }

    public boolean hasMergeConflict() {
        return mFoundMergeConflict;
    }
}

