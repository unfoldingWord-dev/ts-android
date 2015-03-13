package com.door43.translationstudio.library;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.door43.translationstudio.R;

import com.door43.translationstudio.library.temp.LibraryTempData;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.tasks.DownloadProjectImageTask;
import com.door43.translationstudio.util.AnimationUtilities;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.StringFragmentKeySet;
import com.door43.translationstudio.util.TabbedViewPagerAdapter;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;

/**
 * A fragment representing a single Project detail screen.
 * This fragment is either contained in a {@link ProjectLibraryListActivity}
 * in two-pane mode (on tablets) or a {@link ProjectLibraryDetailActivity}
 * on handsets.
 */
public class ProjectLibraryDetailFragment extends TranslatorBaseFragment implements ManagedTask.OnFinishedListener {
    public static final String ARG_ITEM_INDEX = "item_id";
    private Project mProject;
    private ArrayList<StringFragmentKeySet> mTabs = new ArrayList<>();
    private LanguagesTabFragment mLanguagesTab = new LanguagesTabFragment();
    private ViewPager mViewPager;
    private int mDefaultPage = 0;
    private ImageView mIcon;
    private String mImagePath;
    private int mTaskId = -1;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProjectLibraryDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_INDEX)) {
            mProject = LibraryTempData.getProject(getArguments().getInt(ARG_ITEM_INDEX));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_library_detail, container, false);

        // Tabs
        mLanguagesTab.setArguments(getArguments()); // pass args to tab so it can look up the languages
        mTabs.add(new StringFragmentKeySet(getResources().getString(R.string.languages), mLanguagesTab));

        // view pager
        mViewPager = (ViewPager) rootView.findViewById(R.id.projectBrowserViewPager);
        TabbedViewPagerAdapter tabbedViewPagerAdapter = new TabbedViewPagerAdapter(getFragmentManager(), mTabs);
        mViewPager.setAdapter(tabbedViewPagerAdapter);

        // sliding tabs layout
        PagerSlidingTabStrip slidingTabLayout = (PagerSlidingTabStrip) rootView.findViewById(R.id.projectBrowserTabs);
        slidingTabLayout.setViewPager(mViewPager);
        slidingTabLayout.setIndicatorColor(getResources().getColor(R.color.blue));
        slidingTabLayout.setDividerColor(Color.TRANSPARENT);

        selectTab(mDefaultPage);

        // project info
        TextView projectTitle = (TextView)rootView.findViewById(R.id.modelTitle);
        projectTitle.setText(mProject.getTitle());
        TextView projectDescription = (TextView)rootView.findViewById(R.id.modelDescription);
        projectDescription.setText(mProject.getDescription());
        mIcon = (ImageView)rootView.findViewById(R.id.modelImage);
        mIcon.setVisibility(View.GONE);

        if(mImagePath == null) {
            // download project image
            if (TaskManager.getTask(mTaskId) != null) {
                // connect to existing task
                DownloadProjectImageTask task = (DownloadProjectImageTask) TaskManager.getTask(mTaskId);
                task.setOnFinishedListener(this);
            } else {
                // begin downloading the image
                DownloadProjectImageTask task = new DownloadProjectImageTask();
                task.setOnFinishedListener(this);
                mTaskId = TaskManager.addTask(task);
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
            if(mTabs.size() > i && i >= 0) {
                // select the tab
                mViewPager.setCurrentItem(i);
                // notify the tab list adapter that it should reload
                ((TabsFragmentAdapterNotification) mTabs.get(i).getFragment()).NotifyAdapterDataSetChanged();
            }
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
                        mIcon.setVisibility(View.VISIBLE);
                        AnimationUtilities.fadeIn(mIcon, 100);
                    }
                }
            });
        }
    }
}
