package com.door43.translationstudio.panes.left.tabs;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.GetMoreProjectsActivity;
import com.door43.translationstudio.LanguageSelectorActivity;
import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ChooseProjectDialog;
import com.door43.translationstudio.dialogs.ModelItemAdapter;
import com.door43.translationstudio.events.ChoseProjectEvent;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.projects.Resource;
import com.door43.translationstudio.projects.SourceLanguage;
import com.door43.translationstudio.projects.TranslationManager;
import com.door43.translationstudio.projects.data.IndexStore;
import com.door43.translationstudio.tasks.GenericTaskWatcher;
import com.door43.translationstudio.tasks.IndexResourceTask;
import com.door43.translationstudio.tasks.LoadChaptersTask;
import com.door43.translationstudio.tasks.LoadFramesTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.squareup.otto.Subscribe;

/**
 * Created by joel on 8/29/2014.
 */
public class ProjectsTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification, GenericTaskWatcher.OnFinishedListener {
    private static final int SOURCE_LANGUAGE_REQUEST = 0;
    private static final int TARGET_LANGUAGE_REQUEST = 1;
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

        // connect adapter
        listView.setAdapter(mModelItemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TranslationManager.save();
                if (getActivity() != null) {
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
            if (previousProject == null || !previousProject.getId().equals(p.getId())) {
                AppContext.projectManager().setSelectedProject(p.getId());

                // free memory
                if(previousProject != null) {
                    previousProject.flush();
                }

                // load the project
                if(!IndexStore.hasIndex(p)) {
                    // TODO: index the project then choose language then index the resources then load the chapters
                    Log.d(null, "need to index the project");
                } else {
                    if(p.hasSelectedSourceLanguage()) {
                        if(IndexStore.hasResourceIndex(p)) {
                            loadChapters(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
                        } else {
                            indexResources(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
                        }
                    } else {
                        requestSourceLanguage();
                    }
                }
            } else {
                // open current project
                if(p.hasSelectedSourceLanguage()) {
                    if (p.getChapters().length == 0) {
                        loadChapters(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
                    } else {
                        openChaptersTab();
                    }
                } else {
                    requestSourceLanguage();
                }
            }
        }
    }

    /**
     * Starts up a task to index the resources
     * @param p
     * @param l
     * @param r
     */
    private void indexResources(Project p, SourceLanguage l, Resource r) {
        IndexResourceTask task = new IndexResourceTask(p, l, r);
        mTaskWatcher.watch(task);
        TaskManager.addTask(task, task.TASK_ID);
    }

    /**
     * Opens the source language selection dialog
     */
    private void requestSourceLanguage() {
        Intent languageIntent = new Intent(getActivity(), LanguageSelectorActivity.class);
        languageIntent.putExtra("sourceLanguages", true);
        startActivityForResult(languageIntent, SOURCE_LANGUAGE_REQUEST);
    }

    private void requestTargetLanguage() {
        Intent languageIntent = new Intent(getActivity(), LanguageSelectorActivity.class);
        languageIntent.putExtra("sourceLanguages", false);
        startActivityForResult(languageIntent, TARGET_LANGUAGE_REQUEST);
    }

    /**
     * Reloads and opens the chapters tab
     */
    private void openChaptersTab() {
        if(AppContext.projectManager().getSelectedProject().hasSelectedTargetLanguage()) {
            ((MainActivity) getActivity()).reload();
            ((MainActivity) getActivity()).openChaptersTab();
            // TODO: clear the frames tab
            NotifyAdapterDataSetChanged();
        } else {
            requestTargetLanguage();
        }
    }

    /**
     * Starts up a task to load the chapters
     * @param p
     * @param l
     * @param r
     */
    private void loadChapters(Project p, SourceLanguage l, Resource r) {
        LoadChaptersTask task = new LoadChaptersTask(p, l, r);
        mTaskWatcher.watch(task);
        TaskManager.addTask(task, task.TASK_ID);
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
                if (activity != null) {
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
            Project p = AppContext.projectManager().getSelectedProject();
            if(p.getSelectedChapter() != null && p.getSelectedChapter().getFrames().length == 0) {
                // load frames
                LoadFramesTask newTask = new LoadFramesTask(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource(), p.getSelectedChapter());
                mTaskWatcher.watch(newTask);
                TaskManager.addTask(newTask, newTask.TASK_ID);
            } else {
                ((MainActivity) getActivity()).reloadFramesTab();
                openChaptersTab();
            }
        } else if(task instanceof IndexResourceTask) {
            // load the chapters
            Project p = AppContext.projectManager().getProject(((IndexResourceTask) task).getProject().getId());
            loadChapters(p, ((IndexResourceTask) task).getSourceLanguage(), ((IndexResourceTask) task).getResource());
        } else if(task instanceof LoadFramesTask) {
            ((MainActivity) getActivity()).reloadFramesTab();
            openChaptersTab();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SOURCE_LANGUAGE_REQUEST) {
            if(resultCode == getActivity().RESULT_OK) {
                String id = data.getExtras().getString(LanguageSelectorActivity.EXTRA_LANGUAGE);
                Project p = AppContext.projectManager().getSelectedProject();
                p.setSelectedSourceLanguage(id);
                indexResources(p, p.getSelectedSourceLanguage(), p.getSelectedSourceLanguage().getSelectedResource());
            }
        } else if(requestCode == TARGET_LANGUAGE_REQUEST) {
            if(resultCode == getActivity().RESULT_OK) {
                String id = data.getExtras().getString(LanguageSelectorActivity.EXTRA_LANGUAGE);
                Project p = AppContext.projectManager().getSelectedProject();
                p.setSelectedTargetLanguage(id);
                openChaptersTab();
            }
        }
    }
}
