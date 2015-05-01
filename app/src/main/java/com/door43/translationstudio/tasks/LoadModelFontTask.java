package com.door43.translationstudio.tasks;

import android.graphics.Typeface;

import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;

import java.lang.reflect.Type;

/**
 * Created by joel on 4/30/2015.
 */
public class LoadModelFontTask extends ManagedTask {

    private final Model mModel;
    private final boolean mIndicateSelected;
    private Typeface typeface;
    private Typeface descriptionTypeface;

    public LoadModelFontTask(Model model, boolean indicateSelected) {
        mModel = model;
        mIndicateSelected = indicateSelected;
    }

    @Override
    public void start() {
        // don't flood the ui with updates
        sleep(100);
        if(interrupted()) return;
        if(mModel.getSelectedSourceLanguage() != null) {
            typeface = AppContext.graphiteTypeface(mModel.getSelectedSourceLanguage());
        } else {
            // use english as default
            typeface = AppContext.graphiteTypeface(AppContext.projectManager().getLanguage("en"));
        }
        descriptionTypeface = typeface;

        if(interrupted()) return;
        // use selected project language in pseudo project description fontface
        if (mIndicateSelected && mModel.getClass().getName().equals(PseudoProject.class.getName())) {
            if (mModel.isSelected() && AppContext.projectManager().getSelectedProject() != null) {
                descriptionTypeface = AppContext.graphiteTypeface(AppContext.projectManager().getSelectedProject().getSelectedSourceLanguage());
            }
        }
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public Typeface getDescriptionTypeface() {
        return descriptionTypeface;
    }

    @Override
    public int maxProgress() {
        return 1;
    }
}
