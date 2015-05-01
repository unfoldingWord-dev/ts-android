package com.door43.translationstudio.panes.left.tabs;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.GetMoreProjectsActivity;
import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ChooseProjectDialog;
import com.door43.translationstudio.dialogs.ModelItemAdapter;
import com.door43.translationstudio.events.ChoseProjectEvent;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.projects.TranslationManager;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.tasks.GenericTaskWatcher;
import com.door43.translationstudio.tasks.IndexProjectsTask;
import com.door43.translationstudio.tasks.IndexResourceTask;
import com.door43.translationstudio.tasks.LoadChaptersTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TaskBarView;
import com.door43.util.Logger;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;
import com.door43.util.threads.ThreadableUI;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.squareup.otto.Subscribe;

/**
 * Created by joel on 8/29/2014.
 */
public class ProjectsTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification, GenericTaskWatcher.OnFinishedListener {
    private ModelItemAdapter mModelItemAdapter;
    private GenericTaskWatcher mTaskWatcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_left_projects, container, false);
        ListView listView = (ListView)view.findViewById(R.id.projects_list_view);
        Button moreProjectsButton = (Button)view.findViewById(R.id.moreProjectsButton);

        mTaskWatcher = new GenericTaskWatcher(getActivity(), R.string.loading);
        mTaskWatcher.setOnFinishedListener(this);

        // create adapter
        if(mModelItemAdapter == null) mModelItemAdapter = new ModelItemAdapter(app(), AppContext.projectManager().getListableProjects(), null);

        // connectAsync adapter
        listView.setAdapter(mModelItemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (getActivity() != null) {
                    // save changes to the current frame first
                    TranslationManager.save();
                    Model m = mModelItemAdapter.getItem(i);
                    boolean isProject = m.getClass().equals(Project.class);

                    if (isProject) {
                        // this is a normal project
                        handleProjectSelection((Project) m);
                    } else {
                        // this is a meta project
                        handleMetaSelection((PseudoProject) m);
                    }
                }
            }
        });

        moreProjectsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), GetMoreProjectsActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        if(mModelItemAdapter != null && app() != null && AppContext.projectManager() != null) {
            mModelItemAdapter.notifyDataSetChanged();
            mModelItemAdapter.changeDataSet(AppContext.projectManager().getListableProjects());
        }
    }

    /**
     * This handles the selection of a project.
     * @param p
     */
    private void handleProjectSelection(Project p) {
        if(getActivity() != null) {
            // this is a normal project
            Project previousProject = AppContext.projectManager().getSelectedProject();

            AppContext.projectManager().setSelectedProject(p.getId());
            ((MainActivity) getActivity()).reload();
            if (previousProject == null || !previousProject.getId().equals(p.getId())) {
                // clear out the previous project so we don't waste memory
                if(previousProject != null) {
                    previousProject.flush();
                }

                // load the project
                if(!IndexStore.hasIndex(p)) {
                    // TODO: index the project and the resources then load the chapters
                    Log.d(null, "need to index the project");
                } else {
                    if(p.getSelectedSourceLanguage() != null) {
                        if(IndexStore.hasResourceIndex(p)) {
                            // load chapters
                            LoadChaptersTask task = new LoadChaptersTask(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
                            mTaskWatcher.watch(task);
                            TaskManager.addTask(task, task.TASK_ID);
                        } else {
                            // index chapters
                            IndexResourceTask task = new IndexResourceTask(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
                            mTaskWatcher.watch(task);
                            TaskManager.addTask(task, task.TASK_ID);
                        }
                    } else {
                        // TODO: ask the user to choose a source language
                    }
                }
            } else {
                // select the project
                if(p.getChapters().length == 0) {
                    LoadChaptersTask task = new LoadChaptersTask(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
                    mTaskWatcher.watch(task);
                    TaskManager.addTask(task, task.TASK_ID);
                } else {
                    ((MainActivity) getActivity()).openChaptersTab();
                    NotifyAdapterDataSetChanged();
                }
            }
        }
    }

    /**
     * Handles the selection of a meta project
     * @param p
     */
    private void handleMetaSelection(PseudoProject p) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        app().closeToastMessage();
        // Create and show the dialog.
        ChooseProjectDialog newFragment = new ChooseProjectDialog();
        Bundle args = new Bundle();
        args.putString("metaId", p.getId());
        newFragment.setArguments(args);
        newFragment.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Activity activity = getActivity();
                if(activity != null) {
                    activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                } else {
                    Logger.e(this.getClass().getName(), "handleMetaSelection the activity is null");
                }
            }
        });
        newFragment.show(ft, "dialog");
    }

    @Subscribe
    public void onSelectedProjectFromMeta(ChoseProjectEvent event) {
        handleProjectSelection(event.getProject());
        event.getDialog().dismiss();
        if(getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } else {
            Logger.e(this.getClass().getName(), "onSelectedProjectFromMeta the activity is null");
        }
    }

    public void onDestroy() {
        mTaskWatcher.stop();
        super.onDestroy();
    }

    @Override
    public void onFinished(ManagedTask task) {
        mTaskWatcher.stop();
        if(task instanceof LoadChaptersTask) {
            ((MainActivity) getActivity()).reload();
            ((MainActivity) getActivity()).openChaptersTab();
            // TODO: clear the frames tab
            NotifyAdapterDataSetChanged();
        } else if(task instanceof IndexResourceTask) {
            // load the chapters
            Project p = AppContext.projectManager().getProject(((IndexResourceTask) task).getProject().getId());
            LoadChaptersTask newTask = new LoadChaptersTask(p, ((IndexResourceTask) task).getSourceLanguage(), ((IndexResourceTask) task).getResource());
            mTaskWatcher.watch(newTask);
            TaskManager.addTask(newTask, newTask.TASK_ID);
        }
    }
}
