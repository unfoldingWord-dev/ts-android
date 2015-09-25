package com.door43.translationstudio.library;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.library.temp.ServerLibraryCache;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.tasks.DownloadProjectImageTask;
import com.door43.translationstudio.util.AnimationUtilities;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TabsAdapter;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

/**
 * A fragment representing a single Project detail screen.
 * This fragment is either contained in a {@link ServerLibraryActivity}
 * in two-pane mode (on tablets) or a {@link ProjectLibraryDetailActivity}
 * on handsets.
 */
public class ProjectLibraryDetailFragment extends TranslatorBaseFragment implements ManagedTask.OnFinishedListener {
    public static final String ARG_ITEM_ID = "item_id";
    private static final String IMAGE_TASK_PREFIX = "project-image-";
    private Project mProject;
    private ViewPager mViewPager;
    private int mDefaultPage = 0;
    private ImageView mIcon;
    private String mImagePath;
    private int mTaskId = -1;
    private TabsAdapter mTabsAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProjectLibraryDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            String id = getArguments().getString(ARG_ITEM_ID);
            mProject = ServerLibraryCache.getProject(id);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_library_detail, container, false);

        // tabs
        mViewPager = (ViewPager) rootView.findViewById(R.id.projectBrowserViewPager);
        mTabsAdapter = new TabsAdapter(getActivity(), mViewPager);
        mTabsAdapter.addTab(getResources().getString(R.string.languages), LanguagesTab.class, getArguments());
//        if(AppContext.projectManager().isProjectDownloaded(mProject.getId())) {
        mTabsAdapter.addTab(getResources().getString(R.string.drafts), TranslationDraftsTab.class, getArguments());
//        }

        // sliding tabs layout
        PagerSlidingTabStrip slidingTabLayout = (PagerSlidingTabStrip) rootView.findViewById(R.id.projectBrowserTabs);
        slidingTabLayout.setViewPager(mViewPager);
        slidingTabLayout.setIndicatorColor(getResources().getColor(R.color.accent));
        slidingTabLayout.setDividerColor(Color.TRANSPARENT);

        selectTab(mDefaultPage);

        // project info
        TextView dateModifiedText = (TextView)rootView.findViewById(R.id.dateModifiedTextView);
        dateModifiedText.setText("v. "+mProject.getDateModified());
        TextView projectTitle = (TextView)rootView.findViewById(R.id.modelTitle);
        projectTitle.setText(mProject.getTitle());
        TextView projectDescription = (TextView)rootView.findViewById(R.id.modelDescription);
        projectDescription.setText(mProject.getDescription());
        mIcon = (ImageView)rootView.findViewById(R.id.modelImage);
        mIcon.setBackgroundResource(R.drawable.icon_library_white);

        // delete project
        Button deleteButton = (Button)rootView.findViewById(R.id.deleteProjectButton);
        if(ServerLibraryCache.getEnableEditing()) {
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: place this in a task
                    Logger.i(ProjectLibraryDetailFragment.class.getName(), "Deleting project " + mProject.getId());
                    AppContext.projectManager().deleteProject(mProject.getId());
                    ServerLibraryCache.organizeProjects();
                    if(getActivity() != null && getActivity() instanceof LibraryCallbacks) {
                        ((LibraryCallbacks)getActivity()).refreshUI();
                    }
                }
            });
        } else {
            deleteButton.setVisibility(View.GONE);
        }

        // set graphite fontface
        Typeface typeface = AppContext.graphiteTypeface(mProject.getSelectedSourceLanguage());
        projectDescription.setTypeface(typeface, 0);
        projectTitle.setTypeface(typeface, 0);

        // set font size
        float fontsize = AppContext.typefaceSize();
        projectDescription.setTextSize((float)(fontsize*.7));
        projectTitle.setTextSize(fontsize);

        if(mImagePath == null) {
            // download project image
            if (TaskManager.getTask(mTaskId) != null) {
                // connect to existing task
                DownloadProjectImageTask task = (DownloadProjectImageTask) TaskManager.getTask(IMAGE_TASK_PREFIX+mProject.getId());
                task.addOnFinishedListener(this);
            } else {
                // begin downloading the image
                DownloadProjectImageTask task = new DownloadProjectImageTask(mProject);
                task.addOnFinishedListener(this);
                TaskManager.addTask(task, IMAGE_TASK_PREFIX+mProject.getId());
            }
        } else {
            loadImage();
        }
        return rootView;
    }

    /**
     * Changes the selected tab
     * @param i
     */
    public void selectTab(int i) {
        if(mViewPager != null) {
//            if(mTabs.size() > i && i >= 0) {
                // select the tab
                mViewPager.setCurrentItem(i);
                mTabsAdapter.notifyDataSetChanged();
                // notify the tab list adapter that it should reload
//                ((TabsAdapterNotification) mTabs.get(i).getFragment()).NotifyAdapterDataSetChanged();
//            }
        } else {
            mDefaultPage = i;
        }
    }

    /**
     * Called when the image download is complete
     * @param task
     */
    @Override
    public void onFinished(ManagedTask task) {
        mImagePath = ((DownloadProjectImageTask)task).getImagePath();
        // TODO: do we need to run this on the main thread?
        loadImage();
    }

    /**
     * Loads the image from the disk
     */
    private void loadImage() {
        if(mImagePath != null) {
            AppContext.context().getImageLoader().loadImage(mImagePath, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    if (mIcon != null) {
                        mIcon.setImageBitmap(loadedImage);
                        AnimationUtilities.fadeIn(mIcon, 100);
                    }
                }
            });
        }
    }

    /**
     * Removes the drafts tab
     */
    public void hideDraftsTab() {
        // TODO: remove the drafts tab
//        if(mTabs.size() > 1) {
//            mTabs.remove(1);
//        }
        if(mTabsAdapter != null) {
            mTabsAdapter.notifyDataSetChanged();
        }
    }

    public void setProjectId(String projectId) {
        // TODO: this is rather ugly we need to clean this up.
        mProject = ServerLibraryCache.getProject(projectId);

        // update project info
        View rootView = getView();
        TextView dateModifiedText = (TextView)rootView.findViewById(R.id.dateModifiedTextView);
        dateModifiedText.setText("v. "+mProject.getDateModified());
        TextView projectTitle = (TextView)rootView.findViewById(R.id.modelTitle);
        projectTitle.setText(mProject.getTitle());
        TextView projectDescription = (TextView)rootView.findViewById(R.id.modelDescription);
        projectDescription.setText(mProject.getDescription());
        mIcon = (ImageView)rootView.findViewById(R.id.modelImage);
        mIcon.setBackgroundResource(R.drawable.icon_library_white);

        // set graphite fontface
        Typeface typeface = AppContext.graphiteTypeface(mProject.getSelectedSourceLanguage());
        projectDescription.setTypeface(typeface, 0);
        projectTitle.setTypeface(typeface, 0);

        // set font size
        float fontsize = AppContext.typefaceSize();
        projectDescription.setTextSize((float)(fontsize*.7));
        projectTitle.setTextSize(fontsize);

        if(mImagePath == null) {
            // download project image
            if (TaskManager.getTask(mTaskId) != null) {
                // connect to existing task
                DownloadProjectImageTask task = (DownloadProjectImageTask) TaskManager.getTask(IMAGE_TASK_PREFIX+mProject.getId());
                task.addOnFinishedListener(this);
            } else {
                // begin downloading the image
                DownloadProjectImageTask task = new DownloadProjectImageTask(mProject);
                task.addOnFinishedListener(this);
                TaskManager.addTask(task, IMAGE_TASK_PREFIX+mProject.getId());
            }
        } else {
            loadImage();
        }

        // update tabs
        if(mTabsAdapter != null) {
            LanguagesTab languagesTab = (LanguagesTab) mTabsAdapter.getFragmentForPosition(0);
            if (languagesTab != null) {
                languagesTab.setProject(getArguments().getString(ARG_ITEM_ID));
            }
            TranslationDraftsTab draftsTab = (TranslationDraftsTab) mTabsAdapter.getFragmentForPosition(1);
            if (draftsTab != null) {
                draftsTab.setProject(getArguments().getString(ARG_ITEM_ID));
            }
        }
    }
}
