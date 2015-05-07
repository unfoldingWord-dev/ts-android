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
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.tasks.GenericTaskWatcher;
import com.door43.translationstudio.tasks.IndexResourceTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;

/**
 * This class creates a dialog to display a list of frames
 */
public class FramesReaderDialog extends DialogFragment implements GenericTaskWatcher.OnFinishedListener {
    public static final String ARG_PROJECT_ID = "project_id";
    public static final String ARG_CHAPTER_ID = "chapter_id";
    public static final String ARG_DISPLAY_OPTION_ORDINAL = "display_option";
    public static final String ARG_SELECTED_FRAME_INDEX = "selected_frame_index";
    private GenericTaskWatcher mTaskWatcher;
    private FramesListAdapter mAdapter;
    private Project mProject;
    private Chapter mChapter;
    private SourceLanguage mDraft;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_frame_reader, container, false);

        mTaskWatcher = new GenericTaskWatcher(getActivity(), R.string.indexing);
        mTaskWatcher.setOnFinishedListener(this);

        ListView list = (ListView)v.findViewById(R.id.listView);

        Bundle args = getArguments();
        if(args != null) {
            String projectId = args.getString(ARG_PROJECT_ID, "-1");
            String chapterId = args.getString(ARG_CHAPTER_ID, "-1");
            int frameIndex = args.getInt(ARG_SELECTED_FRAME_INDEX, 0);
            int displayOrdinal = args.getInt(ARG_DISPLAY_OPTION_ORDINAL, FramesListAdapter.DisplayOption.SOURCE_TRANSLATION.ordinal());

            mAdapter = new FramesListAdapter(AppContext.context(), new Model[]{}, FramesListAdapter.DisplayOption.values()[displayOrdinal]);
            list.setAdapter(mAdapter);


            mProject = AppContext.projectManager().getProject(projectId);
            if(mProject != null && mProject.getChapter(chapterId) != null) {
                mChapter = mProject.getChapter(chapterId);
                if(displayOrdinal == FramesListAdapter.DisplayOption.DRAFT_TRANSLATION.ordinal()) {
                    if(IndexStore.hasIndex(mProject)) {
                        mDraft = mProject.getSourceLanguageDraft(mProject.getSelectedTargetLanguage().getId());
                        if(mDraft != null) {
                            // connect to existing task
                            IndexResourceTask task = (IndexResourceTask)TaskManager.getTask(IndexResourceTask.TASK_ID);
                            if(task != null) {
                                mTaskWatcher.watch(task);
                            } else {
                                // TODO: users should be able to choose what resource they want to view. For now we will likely only have one resource.
                                if (IndexStore.hasResourceIndex(mProject, mDraft, mDraft.getSelectedResource())) {
                                    // load the index
                                    Model[] frames = IndexStore.getFrames(mProject, mDraft, mDraft.getSelectedResource(), mChapter);
                                    mAdapter.changeDataSet(frames);
                                } else {
                                    // index the draft resources
                                    task = new IndexResourceTask(mProject, mDraft, mDraft.getSelectedResource());
                                    mTaskWatcher.watch(task);
                                    TaskManager.addTask(task, IndexResourceTask.TASK_ID);
                                }
                            }
                        }
                    } else {
                        // TODO: the project has not been indexed yet
                    }
                } else {
                    mAdapter.changeDataSet(mChapter.getFrames());
                }
            } else {
                Logger.w(this.getClass().getName(), "The project and chapter cannot be null");
                list.setAdapter(new FramesListAdapter(AppContext.context(), new Model[]{}, FramesListAdapter.DisplayOption.SOURCE_TRANSLATION));
                dismiss();
            }
        } else {
            Logger.w(this.getClass().getName(), "The dialog was not configured properly");
            list.setAdapter(new FramesListAdapter(AppContext.context(), new Model[]{}, FramesListAdapter.DisplayOption.SOURCE_TRANSLATION));
            dismiss();
        }
        return v;
    }

    @Override
    public void onFinished(ManagedTask task) {
        Model[] frames = IndexStore.getFrames(mProject, mDraft, mDraft.getSelectedResource(), mChapter);
        mAdapter.changeDataSet(frames);
    }
}
