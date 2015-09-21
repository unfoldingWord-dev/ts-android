package com.door43.translationstudio.tasks;

import com.door43.translationstudio.newui.publish.ValidationItem;
import com.door43.util.tasks.ManagedTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 9/20/2015.
 */
public class ValidationTask extends ManagedTask {
    public static final String TASK_ID = "validation_task";
    private final String mTargetTranslationId;
    private final String mSourceTranslationId;
    private List<ValidationItem> mValidations = new ArrayList<>();

    public ValidationTask(String targetTranslationId, String sourceTranslationId) {
        mTargetTranslationId = targetTranslationId;
        mSourceTranslationId = sourceTranslationId;
    }

    @Override
    public void start() {
        // TODO: perform some real validation here

        mValidations.add(ValidationItem.generateValidGroup("John 1-3", true));
        mValidations.add(ValidationItem.generateInvalidGroup("John 4"));
        mValidations.add(ValidationItem.generateValidFrame("John 4:1-3", false));
        mValidations.add(ValidationItem.generateInvalidFrame("John 4:4-5", "All life is in the Word, so he could give life to everything and everyone. The Word was God's light that shone on...", mTargetTranslationId, "04", "01"));
        mValidations.add(ValidationItem.generateValidFrame("John 4:6-30", true));
    }

    /**
     * Returns an array of validation items
     *
     * @return
     */
    public ValidationItem[] getValidations() {
        return mValidations.toArray(new ValidationItem[mValidations.size()]);
    }
}
