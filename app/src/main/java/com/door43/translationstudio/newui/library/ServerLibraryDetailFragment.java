package com.door43.translationstudio.newui.library;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.newui.BaseFragment;
import com.door43.translationstudio.tasks.DownloadProjectImageTask;
import com.door43.translationstudio.tasks.DownloadSourceLanguageTask;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ServerLibraryDetailFragment extends BaseFragment implements ManagedTask.OnFinishedListener {
    public static final String ARG_PROJECT_ID = "item_id";
    private static final String TAB_SOURCE_LANGUAGES = "tab_source_languages";
    private static final String TAB_DRAFT_LANGUAGES = "tab_draft_languages";
    private static final String STATE_SELECTED_TAB = "state_selected_tab";
    public static final String DOWNLOAD_SOURCE_LANGUAGE_TASK_GROUP = "download_source_language_task";
    private Project mProject;
    private String mImagePath;
    private Library mServerLibrary;
    private ViewHolder mHolder;
    private String mSelectedTab = TAB_SOURCE_LANGUAGES;
    private OnEventListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mServerLibrary = AppContext.getLibrary();

        if (getArguments().containsKey(ARG_PROJECT_ID)) {
            String projectId = getArguments().getString(ARG_PROJECT_ID);
            mProject = mServerLibrary.getProject(projectId, Locale.getDefault().getLanguage());
            // TODO: handle null project

        }

        if(savedInstanceState != null) {
            mSelectedTab = savedInstanceState.getString(STATE_SELECTED_TAB, TAB_SOURCE_LANGUAGES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_server_library_detail, container, false);

        mHolder = new ViewHolder(rootView);

        rebuildLayout();
        return rootView;
    }

    public void rebuildLayout() {
        // disconnect tasks listeners
        List<ManagedTask> tasks = TaskManager.getGroupedTasks(ServerLibraryDetailFragment.DOWNLOAD_SOURCE_LANGUAGE_TASK_GROUP);
        for(ManagedTask task:tasks) {
            task.removeAllOnFinishedListener();
            task.removeAllOnProgressListener();
        }

        if(mHolder != null) {
            // project info
            mHolder.mDateModified.setText("v. " + mProject.dateModified);
            mHolder.mProjectTitle.setText(mProject.name);
            mHolder.mProjectDescription.setText(mProject.description);
            mHolder.mIcon.setBackgroundResource(R.drawable.ic_library_books_black_24dp);

            // delete project
            if (AppContext.getLibrary().projectHasSource(mProject.getId())) {
                mHolder.mDeleteButton.setVisibility(View.VISIBLE);
                mHolder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TRICKY: we can't let users delete projects that have target Translations
                        TargetTranslation[] targetTranslations = AppContext.getTranslator().getTargetTranslations();
                        boolean projectHasTargetTranslations = false;
                        for(TargetTranslation targetTranslation:targetTranslations){
                            if(targetTranslation.getProjectId().equals(mProject.getId())) {
                                projectHasTargetTranslations = true;
                                break;
                            }
                        }
                        if(projectHasTargetTranslations) {
                            new android.support.v7.app.AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.label_delete)
                                    .setIcon(R.drawable.ic_info_black_24dp)
                                    .setMessage(R.string.cannot_delete_project_with_translations)
                                    .setPositiveButton(R.string.dismiss, null)
                                    .show();
                        } else {
                            new android.support.v7.app.AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.label_delete)
                                    .setIcon(R.drawable.ic_delete_black_24dp)
                                    .setMessage(R.string.confirm_delete_project)
                                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            AppContext.getLibrary().deleteProject(mProject.getId());
                                            mListener.onProjectDeleted(mProject.getId());
                                            rebuildLayout();
                                        }
                                    })
                                    .setNegativeButton(R.string.no, null)
                                    .show();
                        }
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

            // tabs
            final List<SourceLanguage> sourceLanguages = getSourceLanguages();
            final List<SourceLanguage> draftLanguages = getDraftLanguages();
            mHolder.mTabLayout.removeAllTabs();
            if(sourceLanguages.size() > 0) {
                TabLayout.Tab tab = mHolder.mTabLayout.newTab();
                tab.setText(R.string.languages);
                tab.setTag(TAB_SOURCE_LANGUAGES);
                mHolder.mTabLayout.addTab(tab);
            }
            if(draftLanguages.size() > 0) {
                TabLayout.Tab tab = mHolder.mTabLayout.newTab();
                tab.setText(R.string.drafts);
                tab.setTag(TAB_DRAFT_LANGUAGES);
                mHolder.mTabLayout.addTab(tab);
            }

            // select correct tab
            for(int i = 0; i < mHolder.mTabLayout.getTabCount(); i ++) {
                TabLayout.Tab tab = mHolder.mTabLayout.getTabAt(i);
                if(tab.getTag().equals(mSelectedTab)) {
                    tab.select();
                    break;
                }
            }

            // tab listener
            mHolder.mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    mSelectedTab = (String) tab.getTag();
                    if(mSelectedTab.equals(TAB_DRAFT_LANGUAGES)) {
                        mHolder.mAdapter.changeDataSet(mProject.getId(), ServerLibraryCache.getAvailableUpdates(), draftLanguages);
                    } else {
                        mHolder.mAdapter.changeDataSet(mProject.getId(), ServerLibraryCache.getAvailableUpdates(), sourceLanguages);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });

            // load languages
            if(mSelectedTab.equals(TAB_DRAFT_LANGUAGES)) {
                mHolder.mAdapter.changeDataSet(mProject.getId(), ServerLibraryCache.getAvailableUpdates(), draftLanguages);
            } else {
                mHolder.mAdapter.changeDataSet(mProject.getId(), ServerLibraryCache.getAvailableUpdates(), sourceLanguages);
            }

            // list
            mHolder.mLanguageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final ServerLibraryLanguageAdapter.ListItem item = mHolder.mAdapter.getItem(position);
                    boolean isDownloaded = AppContext.getLibrary().sourceLanguageHasSource(mProject.getId(), item.sourceLanguage.getId());
                    if(!isDownloaded) {
                        // just download the update
                        DownloadSourceLanguageTask task = new DownloadSourceLanguageTask(mProject.getId(), item.sourceLanguage.getId());
                        task.addOnFinishedListener(ServerLibraryDetailFragment.this);
                        TaskManager.addTask(task, mProject.getId() + "-" + item.sourceLanguage.getId());
                        TaskManager.groupTask(task, DOWNLOAD_SOURCE_LANGUAGE_TASK_GROUP);
                        task.addOnFinishedListener(item.onFinishedListener);
                        task.addOnProgressListener(item.onProgressListener);
                    } else {
                        // confirm with the user
                        new android.support.v7.app.AlertDialog.Builder(getActivity())
                                .setTitle(R.string.download)
                                .setIcon(R.drawable.ic_refresh_black_24dp)
                                .setMessage(R.string.source_language_already_downloaded)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DownloadSourceLanguageTask task = new DownloadSourceLanguageTask(mProject.getId(), item.sourceLanguage.getId());
                                        task.addOnFinishedListener(ServerLibraryDetailFragment.this);
                                        TaskManager.addTask(task, mProject.getId() + "-" + item.sourceLanguage.getId());
                                        TaskManager.groupTask(task, DOWNLOAD_SOURCE_LANGUAGE_TASK_GROUP);
                                        task.addOnFinishedListener(item.onFinishedListener);
                                        task.addOnProgressListener(item.onProgressListener);
                                    }
                                })
                                .setNegativeButton(R.string.no, null)
                                .show();
                    }
                }
            });
        }
    }

    public void onSaveInstanceState(Bundle out) {
        out.putString(STATE_SELECTED_TAB, mSelectedTab);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        List<ManagedTask> tasks = TaskManager.getGroupedTasks(ServerLibraryDetailFragment.DOWNLOAD_SOURCE_LANGUAGE_TASK_GROUP);
        for(ManagedTask task:tasks) {
            task.removeAllOnFinishedListener();
            task.removeAllOnProgressListener();
        }
        super.onDestroy();
    }

    private List<SourceLanguage> getSourceLanguages() {
        // TRICKY: we convert the source translations to source languages so we only get
        // languages that meet the minimum checking level
        SourceTranslation[] sourceTranslations = mServerLibrary.getSourceTranslations(mProject.getId());
        Map<String, SourceLanguage> sourceLanguages = new HashMap<>();
        for(SourceTranslation sourceTranslation:sourceTranslations) {
            SourceLanguage sourceLanguage = mServerLibrary.getSourceLanguage(mProject.getId(), sourceTranslation.sourceLanguageSlug);
            // TRICKY: a source language could be represented several times due to multiple resources
            if(!sourceLanguages.containsKey(sourceLanguage.getId())) {
                sourceLanguages.put(sourceLanguage.getId(), sourceLanguage);
            }
        }
        return new ArrayList(sourceLanguages.values());
    }

    private List<SourceLanguage> getDraftLanguages() {
        // TRICKY: we convert the source translations to source languages so we only get
        // languages that meet the minimum checking level
        SourceTranslation[] draftTranslations = mServerLibrary.getDraftTranslations(mProject.getId());
        Map<String, SourceLanguage> draftLanguages = new HashMap<>();
        for(SourceTranslation sourceTranslation:draftTranslations) {
            SourceLanguage sourceLanguage = mServerLibrary.getSourceLanguage(mProject.getId(), sourceTranslation.sourceLanguageSlug);
            // TRICKY: a source language could be represented several times due to multiple resources
            if(!draftLanguages.containsKey(sourceLanguage.getId())) {
                draftLanguages.put(sourceLanguage.getId(), sourceLanguage);
            }
        }
        return new ArrayList<>(draftLanguages.values());
    }

    /**
     * Called when the image download is complete
     * @param task
     */
    @Override
    public void onFinished(final ManagedTask task) {
        TaskManager.clearTask(task);

        if(task instanceof DownloadProjectImageTask) {
            mImagePath = ((DownloadProjectImageTask) task).getImagePath();
            // TODO: do we need to run this on the main thread?
            loadImage();
        } else if(task instanceof DownloadSourceLanguageTask) {
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    rebuildLayout();
                    if (((DownloadSourceLanguageTask) task).getSuccess()) {
                        ServerLibraryCache.getAvailableUpdates().removeSourceLanguageUpdate(((DownloadSourceLanguageTask) task).getProjectId(), ((DownloadSourceLanguageTask) task).getSourceLanguageId());
                        mListener.onSourceLanguageDownloaded(((DownloadSourceLanguageTask) task).getProjectId(), ((DownloadSourceLanguageTask) task).getSourceLanguageId());
                    }
                }
            });
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnEventListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnEventListener");
        }
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
     * Sets the id of the project to be displayed
     * @param projectId
     */
    public void setProjectId(String projectId) {
        mProject = mServerLibrary.getProject(projectId, Locale.getDefault().getLanguage());
        // TODO: handle null project
        rebuildLayout();
    }

    private class ViewHolder {
        public final TextView mDateModified;
        public final TextView mProjectTitle;
        public final TextView mProjectDescription;
        public final ImageView mIcon;
        public final Button mDeleteButton;
        public final TabLayout mTabLayout;
        private final ListView mLanguageList;
        private final ServerLibraryLanguageAdapter mAdapter;

        public ViewHolder(View v) {
            mLanguageList = (ListView)v.findViewById(R.id.language_list);
            mAdapter = new ServerLibraryLanguageAdapter(getActivity());
            mLanguageList.setAdapter(mAdapter);
            mTabLayout = (TabLayout)v.findViewById(R.id.language_tabs);
            mDateModified = (TextView)v.findViewById(R.id.dateModifiedTextView);
            mProjectTitle = (TextView)v.findViewById(R.id.modelTitle);
            mProjectDescription = (TextView)v.findViewById(R.id.modelDescription);
            mIcon = (ImageView)v.findViewById(R.id.modelImage);
            mDeleteButton = (Button)v.findViewById(R.id.deleteProjectButton);
        }
    }

    public interface OnEventListener {
        void onSourceLanguageDownloaded(String projectId, String sourceLanguageId);
        void onProjectDeleted(String projectId);
    }
}
