package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;

/**
 * This class creates a dialog to display a list of frames
 */
public class FramesReaderDialog extends DialogFragment {
    public static final String ARG_PROJECT_ID = "project_id";
    public static final String ARG_CHAPTER_ID = "chapter_id";
    public static final String ARG_DISPLAY_OPTION_ORDINAL = "display_option";

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_translation_reader, container, false);

        ListView list = (ListView)v.findViewById(R.id.listView);
        Bundle args = getArguments();
        Model[] frames = {};
        if(args != null) {
            String projectId = args.getString(ARG_PROJECT_ID, "-1");
            String chapterId = args.getString(ARG_CHAPTER_ID, "-1");
            int displayOrdinal = args.getInt(ARG_DISPLAY_OPTION_ORDINAL, FramesListAdapter.DisplayOption.SOURCE_TRANSLATION.ordinal());
            Project p = AppContext.projectManager().getProject(projectId);
            if(p != null) {
                Chapter c = p.getChapter(chapterId);
                if(c != null) {
                    frames = c.getFrames();
                }
            }
            list.setAdapter(new FramesListAdapter(AppContext.context(), frames, FramesListAdapter.DisplayOption.values()[displayOrdinal]));
        } else {
            Logger.w(this.getClass().getName(), "The dialog was not configured properly");
            list.setAdapter(new FramesListAdapter(AppContext.context(), new Model[]{}, FramesListAdapter.DisplayOption.SOURCE_TRANSLATION));
            dismiss();
        }
        return v;
    }
}
