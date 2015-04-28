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
import com.door43.translationstudio.tasks.IndexProjectsTask;
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
public class ProjectsTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
    private ModelItemAdapter mModelItemAdapter;
    private TaskBarView mTaskBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_left_projects, container, false);
        ListView listView = (ListView)view.findViewById(R.id.projects_list_view);
        Button moreProjectsButton = (Button)view.findViewById(R.id.moreProjectsButton);

        // create adapter
        if(mModelItemAdapter == null) mModelItemAdapter = new ModelItemAdapter(app(), AppContext.projectManager().getListableProjects());

//        mTaskBar = (TaskBarView)view.findViewById(R.id.projectProcessesLayout);

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

//    @Override
//    public void onResume() {
//        super.onResume();
//        connectToProcesses();
//    }

//    public void onPause() {
//        IndexProjectsTask task = (IndexProjectsTask)TaskManager.getTask(IndexProjectsTask.TASK_ID);
//        if(task != null) {
//            task.removeOnFinishedListener(this);
//            task.removeOnProgressListener(this);
//        }
//        super.onPause();
//    }

//    private void connectToProcesses() {
//        IndexProjectsTask task = (IndexProjectsTask)TaskManager.getTask(IndexProjectsTask.TASK_ID);
//        if(task != null) {
//            mTaskBar.setVisibility(View.VISIBLE);
//            mTaskBar.publishProgress(R.string.indexing, -1);
//            task.addOnProgressListener(this);
//            task.addOnFinishedListener(this);
//        }
//    }

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
                // reload the center pane so we don't accidently overwrite a frame
                ((MainActivity) getActivity()).reload();

                AppContext.projectManager().setSelectedProject(p.getId());

                // clear out the previous project so we don't waste memory
                if(previousProject != null) {
                    previousProject.flush();
                }

                // load the project
                if(!IndexStore.hasIndex(p)) {
                    // TODO: index the project ()

                } else {
                    // TODO: load the already indexed project (not the resources)

                }

                // load the project source
                final ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setMessage(getResources().getString(R.string.loading_project_chapters));
                dialog.show();
                new ThreadableUI(this.getActivity()) {

                    @Override
                    public void onStop() {

                    }

                    @Override
                    public void run() {
                        AppContext.projectManager().fetchProjectSource(AppContext.projectManager().getSelectedProject());
                    }

                    @Override
                    public void onPostExecute() {
                        if(getActivity() != null) {
                            // populate the center pane
                            ((MainActivity) getActivity()).reload();
                            // open up the chapters tab
                            ((MainActivity) getActivity()).openChaptersTab();
                            // reload the frames tab so we don't see frames from the previous project
                            ((MainActivity) getActivity()).reloadFramesTab();
                        } else {
                            Logger.e(ProjectsTabFragment.class.getName(), "onPostExecute the activity is null");
                        }
                        dialog.dismiss();
                        NotifyAdapterDataSetChanged();
                    }
                }.start();
            } else {
                // select the project
                AppContext.projectManager().setSelectedProject(p.getId());
                // reload the center pane so we don't accidently overwrite a frame
                ((MainActivity) getActivity()).reload();
                // open up the chapters tab
                ((MainActivity) getActivity()).openChaptersTab();
                // let the adapter redraw itself so the selected project is corectly highlighted
                NotifyAdapterDataSetChanged();
            }
        } else {
            Logger.e(this.getClass().getName(), "handleProjectSelection the activity is null");
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
//
//    @Override
//    public void onFinished(ManagedTask task) {
//        TaskManager.clearTask(task.getTaskId());
//        Handler hand = new Handler(Looper.getMainLooper());
//        hand.post(new Runnable() {
//            @Override
//            public void run() {
//                mTaskBar.setVisibility(View.GONE);
//            }
//        });
//    }

//    @Override
//    public void onProgress(ManagedTask task, final double progress, final String message) {
//        Handler hand = new Handler(Looper.getMainLooper());
//        hand.post(new Runnable() {
//            @Override
//            public void run() {
//                mTaskBar.publishProgress(message, progress);
//                if(progress < 0) {
//                    mTaskBar.hideProgress();
//                } else {
//                    mTaskBar.showProgress();
//                }
//            }
//        });
//    }
}
