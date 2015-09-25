package com.door43.translationstudio.newui.library;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.tasks.DownloadProjectImageTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TabsAdapter;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.door43.util.tasks.ManagedTask;
import com.door43.widget.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ServerLibraryDetailFragment extends TranslatorBaseFragment implements ManagedTask.OnFinishedListener {
    public static final String ARG_PROJECT_ID = "item_id";
    private Project mProject;
    private int mDefaultPage = 0;
    private String mImagePath;
    private Library mServerLibrary;
    private ViewHolder mHolder;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ServerLibraryDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mServerLibrary = AppContext.getLibrary().getServerLibrary();

        if (getArguments().containsKey(ARG_PROJECT_ID)) {
            String projectId = getArguments().getString(ARG_PROJECT_ID);
            mProject = mServerLibrary.getProject(projectId, Locale.getDefault().getLanguage());
            // TODO: handle null project

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_server_library_detail, container, false);

        mHolder = new ViewHolder(rootView);

        // tabs
        mHolder.mTabsAdapter.addTab(getResources().getString(R.string.languages), SourceLanguagesTab.class, getArguments());
        // TODO: only display if there are drafts
        mHolder.mTabsAdapter.addTab(getResources().getString(R.string.drafts), DraftLanguagesTab.class, getArguments());

        selectTab(mDefaultPage);

        rebuildLayout();
        return rootView;
    }

    public void rebuildLayout() {
        if(mHolder != null) {
            // project info
            mHolder.mDateModified.setText("v. " + mProject.dateModified);
            mHolder.mProjectTitle.setText(mProject.name);
            mHolder.mProjectDescription.setText(mProject.description);
            mHolder.mIcon.setBackgroundResource(R.drawable.ic_library_books_black_24dp);

            // delete project
            // TODO: we need to finish providing support for deleting projects and then check if this project exists on the device
            if (false) {
                mHolder.mDeleteButton.setVisibility(View.VISIBLE);
                mHolder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO: get this working and maybe place it in a task
//                    Logger.i(ServerLibraryDetailFragment.class.getName(), "Deleting project " + mProject.getId());
//                    AppContext.projectManager().deleteProject(mProject.getId());
//                    ServerLibraryCache.organizeProjects();
//                    if(getActivity() != null && getActivity() instanceof LibraryCallbacks) {
//                        ((LibraryCallbacks)getActivity()).refreshUI();
//                    }
                    }
                });
            } else {
                mHolder.mDeleteButton.setVisibility(View.GONE);
            }


            // fonts
            SourceLanguage fontSourceLanguage = mServerLibrary.getSourceLanguage(mProject.getId(), mProject.sourceLanguageId);
            Typography.formatSub(getActivity(), mHolder.mProjectDescription, fontSourceLanguage.getId(), fontSourceLanguage.getDirection());
            Typography.formatTitle(getActivity(), mHolder.mProjectTitle, fontSourceLanguage.getId(), fontSourceLanguage.getDirection());

            // custom project icon
            if (mImagePath == null) {
                // TODO: hook this up
//            if (TaskManager.getTask(mTaskId) != null) {
//                // connect to existing task
//                DownloadProjectImageTask task = (DownloadProjectImageTask) TaskManager.getTask(IMAGE_TASK_PREFIX+mProject.getId());
//                task.addOnFinishedListener(this);
//            } else {
//                // begin downloading the image
//                DownloadProjectImageTask task = new DownloadProjectImageTask(mProject);
//                task.addOnFinishedListener(this);
//                TaskManager.addTask(task, IMAGE_TASK_PREFIX+mProject.getId());
//            }
            } else {
                loadImage();
            }

            // TRICKY: we convert the source translations to source languages so we only get
            // languages that meet the minimum checking level
            SourceTranslation[] sourceTranslations = mServerLibrary.getSourceTranslations(mProject.getId());
            Map<String, SourceLanguage> sourceLanguages = new HashMap<>();
            for(SourceTranslation sourceTranslation:sourceTranslations) {
                SourceLanguage sourceLanguage = mServerLibrary.getSourceLanguage(mProject.getId(), sourceTranslation.sourceLanguageId);
                // TRICKY: a source language could be represented several times due to multiple resources
                if(!sourceLanguages.containsKey(sourceLanguage.getId())) {
                    sourceLanguages.put(sourceLanguage.getId(), sourceLanguage);
                }
            }
            SourceLanguagesTab languagesTab = (SourceLanguagesTab) mHolder.mTabsAdapter.getFragmentForPosition(0);
            // TODO: the tabs are not intialized the first time this starts. Perhaps we should remove the pager adapter
            // and just manually watch the tab clicks like we do for the translation modes.
            if (languagesTab != null) {
                languagesTab.setLanguages(new ArrayList(sourceLanguages.values()));
            }

            // TRICKY: we convert the source translations to source languages so we only get
            // languages that meet the minimum checking level
            SourceTranslation[] draftTranslations = mServerLibrary.getDraftTranslations(mProject.getId());
            Map<String, SourceLanguage> draftLanguages = new HashMap<>();
            for(SourceTranslation sourceTranslation:draftTranslations) {
                SourceLanguage sourceLanguage = mServerLibrary.getSourceLanguage(mProject.getId(), sourceTranslation.sourceLanguageId);
                // TRICKY: a source language could be represented several times due to multiple resources
                if(!draftLanguages.containsKey(sourceLanguage.getId())) {
                    draftLanguages.put(sourceLanguage.getId(), sourceLanguage);
                }
            }
            // TODO: hide if there are no drafts
            DraftLanguagesTab draftsTab = (DraftLanguagesTab) mHolder.mTabsAdapter.getFragmentForPosition(1);
            if (draftsTab != null) {
                draftsTab.setLanguages(new ArrayList(draftLanguages.values()));
            }
        }
    }

    /**
     * Changes the selected tab
     * @param i
     */
    public void selectTab(int i) {
        if(mHolder != null) {
//            if(mTabs.size() > i && i >= 0) {
                // select the tab
                mHolder.mViewPager.setCurrentItem(i);
                mHolder.mTabsAdapter.notifyDataSetChanged();
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
        // TODO: update this
//        if(mImagePath != null) {
//            AppContext.context().getImageLoader().loadImage(mImagePath, new SimpleImageLoadingListener() {
//                @Override
//                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                    if (mIcon != null) {
//                        mIcon.setImageBitmap(loadedImage);
//                        AnimationUtilities.fadeIn(mIcon, 100);
//                    }
//                }
//            });
//        }
    }

    /**
     * Removes the drafts tab
     */
    public void hideDraftsTab() {
        // TODO: remove the drafts tab
//        if(mTabs.size() > 1) {
//            mTabs.remove(1);
//        }
        if(mHolder != null) {
            mHolder.mTabsAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Sets the id of the project to be displayed
     * @param projectId
     */
    public void setProjectCategoryId(String projectId) {
        mProject = mServerLibrary.getProject(projectId, Locale.getDefault().getLanguage());
        // TODO: handle null project

        rebuildLayout();
    }

    private class ViewHolder {

        public final ViewPager mViewPager;
        public final PagerSlidingTabStrip mSlidingTabLayout;
        public final TextView mDateModified;
        public final TextView mProjectTitle;
        public final TextView mProjectDescription;
        public final ImageView mIcon;
        public final Button mDeleteButton;
        public final TabsAdapter mTabsAdapter;

        public ViewHolder(View v) {
            this.mViewPager = (ViewPager)v.findViewById(R.id.projectBrowserViewPager);
            this.mTabsAdapter = new TabsAdapter(getActivity(), this.mViewPager);
            this.mSlidingTabLayout = (PagerSlidingTabStrip)v.findViewById(R.id.projectBrowserTabs);
            this.mSlidingTabLayout.setViewPager(this.mViewPager);
            this.mSlidingTabLayout.setIndicatorColor(getResources().getColor(R.color.accent));
            this.mSlidingTabLayout.setDividerColor(Color.TRANSPARENT);
            this.mDateModified = (TextView)v.findViewById(R.id.dateModifiedTextView);
            this.mProjectTitle = (TextView)v.findViewById(R.id.modelTitle);
            this.mProjectDescription = (TextView)v.findViewById(R.id.modelDescription);
            this.mIcon = (ImageView)v.findViewById(R.id.modelImage);
            this.mDeleteButton = (Button)v.findViewById(R.id.deleteProjectButton);
            ViewUtil.tintViewDrawable(this.mDeleteButton, getResources().getColor(R.color.dark_secondary_text));
        }
    }
}
