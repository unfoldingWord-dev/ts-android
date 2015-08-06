package com.door43.translationstudio.tasks;

import android.graphics.Typeface;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;

/**
 * Created by joel on 4/30/2015.
 */
public class LoadModelFontTask extends ManagedTask {

    private final Model mModel;
    private final boolean mIndicateSelected;
    private Typeface mTypeface;
    private String mTypefaceName;
    private Typeface mDescriptionTypeface;

    public LoadModelFontTask(Model model, boolean indicateSelected) {
        mModel = model;
        mIndicateSelected = indicateSelected;
    }

    @Override
    public void start() {
        if(interrupted()) return;
        if(mModel.getSelectedSourceLanguage() != null) {
            mTypeface = AppContext.graphiteTypeface(mModel.getSelectedSourceLanguage());
        } else {
            // use english as default
            mTypeface = AppContext.graphiteTypeface(AppContext.projectManager().getLanguage("en"));
        }
        mTypefaceName = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_TRANSLATION_TYPEFACE, AppContext.context().getResources().getString(R.string.pref_default_translation_typeface));
        mDescriptionTypeface = mTypeface;

        if(interrupted()) return;
        // use selected project language in pseudo project description fontface
        if (mIndicateSelected && mModel.getClass().getName().equals(PseudoProject.class.getName())) {
            if (mModel.isSelected() && AppContext.projectManager().getSelectedProject() != null) {
                mDescriptionTypeface = AppContext.graphiteTypeface(AppContext.projectManager().getSelectedProject().getSelectedSourceLanguage());
            }
        }
    }

    public Typeface getTypeface() {
        return mTypeface;
    }

    public String getTypefaceName() {
        return mTypefaceName;
    }

    @Override
    public int maxProgress() {
        return 1;
    }

    public Typeface getDescriptionTypeface() {
        return mDescriptionTypeface;
    }
}
