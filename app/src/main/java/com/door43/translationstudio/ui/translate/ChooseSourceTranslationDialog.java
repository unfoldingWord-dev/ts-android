package com.door43.translationstudio.ui.translate;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ContainerCache;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.tasks.DownloadResourceContainerTask;
import com.door43.widget.ViewUtil;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

/**
 * Created by joel on 9/15/2015.
 */
public class ChooseSourceTranslationDialog extends DialogFragment implements ManagedTask.OnFinishedListener, ManagedTask.OnProgressListener {
    public static final String ARG_TARGET_TRANSLATION_ID = "arg_target_translation_id";
    public static final String TAG = ChooseSourceTranslationDialog.class.getSimpleName();
    private static final String TASK_DOWNLOAD_CONTAINER = "download-container";
    private static final String TASK_INIT = "init-data";
    private Translator mTranslator;
    private TargetTranslation mTargetTranslation;
    private OnClickListener mListener;
    private ChooseSourceTranslationAdapter mAdapter;
    private Door43Client mLibrary;
    public static final boolean ENABLE_DRAFTS = false;
    private ProgressDialog downloadDialog = null;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_choose_source_translation, container, false);

        mTranslator = App.getTranslator();
        mLibrary = App.getLibrary();

        Bundle args = getArguments();
        if(args == null) {
            dismiss();
        } else {
            String targetTranslationId = args.getString(ARG_TARGET_TRANSLATION_ID, null);
            mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
            if(mTargetTranslation == null) {
                // missing target translation
                dismiss();
            }
        }

        EditText searchView = (EditText) v.findViewById(R.id.search_text);
        searchView.setHint(R.string.choose_source_translations);
        searchView.setEnabled(false);
        ImageButton searchBackButton = (ImageButton) v.findViewById(R.id.search_back_button);
        searchBackButton.setVisibility(View.GONE);
        ImageView searchIcon = (ImageView) v.findViewById(R.id.search_mag_icon);
        // TODO: set up search

        mAdapter = new ChooseSourceTranslationAdapter(getActivity());

        ListView listView = (ListView) v.findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                if(mAdapter.isSelectableItem(position)) {
                    final ChooseSourceTranslationAdapter.ViewItem item = mAdapter.getItem(position);
                    if(item.hasUpdates || !item.downloaded) {
                        // download
                        String format;
                        if(item.hasUpdates) {
                            format = getResources().getString(R.string.update_source_language);
                        } else {
                            format = getResources().getString(R.string.download_source_language);
                        }
                        String message = String.format(format, item.title);
                        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                                .setTitle(R.string.title_download_source_language)
                                .setMessage(message)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DownloadResourceContainerTask task = new DownloadResourceContainerTask(item.sourceTranslation);
                                        task.addOnFinishedListener(ChooseSourceTranslationDialog.this);
                                        task.addOnProgressListener(ChooseSourceTranslationDialog.this);
                                        task.TAG = position;
                                        TaskManager.addTask(task, TASK_DOWNLOAD_CONTAINER);
                                    }
                                })
                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if(item.downloaded) {
                                            // allow selecting if downloaded already
                                            mAdapter.toggleSelection(position);
                                        }
                                    }
                                })
                                .show();
                    } else {
                        // toggle
                        mAdapter.toggleSelection(position);
                    }
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final ChooseSourceTranslationAdapter.ViewItem item = mAdapter.getItem(position);
                if(item.downloaded && mAdapter.isSelectableItem(position)) {
                    new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                            .setTitle(R.string.label_delete)
                            .setMessage(R.string.confirm_delete_project)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mLibrary.delete(item.containerSlug);
                                    mAdapter.markItemDeleted(position);
                                }
                            })
                            .setNegativeButton(R.string.menu_cancel, null)
                            .show();
                    return true;
                }
                return false;
            }
        });

        Button cancelButton = (Button) v.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null) {
                    mListener.onCancelTabsDialog(mTargetTranslation.getId());
                }
                dismiss();
            }
        });
        Button confirmButton = (Button) v.findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // collect selected source translations
                int count = mAdapter.getCount();
                List<String> resourceContainerSlugs = new ArrayList<>();
                for(int i = 0; i < count; i ++) {
                    if(mAdapter.isSelectableItem(i)) {
                        ChooseSourceTranslationAdapter.ViewItem item = mAdapter.getItem(i);
                        if(item.selected) {
                            resourceContainerSlugs.add(item.containerSlug);
                        }
                    }
                }
                if(mListener != null) {
                    mListener.onConfirmTabsDialog(mTargetTranslation.getId(), resourceContainerSlugs);
                }
                dismiss();
            }
        });

        loadData();

        return v;
    }

    private void loadData() {
        ManagedTask initTask = TaskManager.getTask(TASK_INIT);
        if(initTask == null) {
            // begin init
            initTask = new ManagedTask() {
                @Override
                public void start() {
                    // add selected source translations
                    String[] sourceTranslationSlugs = App.getSelectedSourceTranslations(mTargetTranslation.getId());
                    for (String slug : sourceTranslationSlugs) {
                        Translation st = mLibrary.index.getTranslation(slug);
                        if (st != null) addSourceTranslation(st, true);
                    }

                    List<Translation> availableTranslations = mLibrary.index.findTranslations(null, mTargetTranslation.getProjectId(), null, "book", null, App.MIN_CHECKING_LEVEL, -1);
                    for (Translation sourceTranslation : availableTranslations) {
                        addSourceTranslation(sourceTranslation, false);
                    }
                }
            };

            initTask.addOnFinishedListener(this);
            TaskManager.addTask(initTask, TASK_INIT);
        } else {
            // connect to existing
            initTask.addOnFinishedListener(this);
        }

        // connect to tasks
        ManagedTask downloadTask = TaskManager.getTask(TASK_DOWNLOAD_CONTAINER);
        if(downloadTask != null) {
            downloadTask.addOnProgressListener(this);
            downloadTask.addOnFinishedListener(this);
        }
    }

    /**
     * adds this source translation to the adapter
     * @param sourceTranslation
     * @param selected
     */
    private void addSourceTranslation(final Translation sourceTranslation, final boolean selected) {
        final String title = sourceTranslation.language.name + " - " + sourceTranslation.resource.name;

        final boolean isDownloaded = mLibrary.exists(sourceTranslation.resourceContainerSlug);
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.addItem(new ChooseSourceTranslationAdapter.ViewItem(title, sourceTranslation, selected, isDownloaded));
                mAdapter.sort();
            }
        });
    }

    /**
     * Assigns a listener for this dialog
     * @param listener
     */
    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onTaskFinished(ManagedTask task) {
        TaskManager.clearTask(task);
        if(task instanceof DownloadResourceContainerTask) {
            final DownloadResourceContainerTask t = (DownloadResourceContainerTask)task;
            for(ResourceContainer rc:t.getDownloadedContainers()) {
                // reset cached containers that were downloaded
                ContainerCache.remove(rc.slug);
            }
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if(downloadDialog != null && downloadDialog.isShowing()) downloadDialog.dismiss();
                    downloadDialog = null;
                    if(t.success()) {
                        mAdapter.markItemDownloaded(t.TAG);
                    } else {
                        Snackbar snack = Snackbar.make(ChooseSourceTranslationDialog.this.getView(), getResources().getString(R.string.download_failed), Snackbar.LENGTH_LONG);
                        ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                        snack.show();
                    }
                }
            });
        }
    }

    @Override
    public void onTaskProgress(final ManagedTask task, final double progress, String message, boolean secondary) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                // init dialog
                if(downloadDialog == null) {
                    downloadDialog = new ProgressDialog(getActivity());
                    downloadDialog.setCancelable(true);
                    downloadDialog.setCanceledOnTouchOutside(false);
                    downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    downloadDialog.setOnCancelListener(ChooseSourceTranslationDialog.this);
                    downloadDialog.setIcon(R.drawable.ic_cloud_download_black_24dp);
                    downloadDialog.setTitle(R.string.downloading);
                }

                // dismiss if finished
                if(task.isFinished()) {
                    downloadDialog.dismiss();
                    return;
                }

                // progress
                downloadDialog.setMax(task.maxProgress());
                if(progress > 0) {
                    downloadDialog.setIndeterminate(false);
                    downloadDialog.setProgressStyle((int)progress);
                    downloadDialog.setProgressNumberFormat("%1d/$2d");
                    downloadDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                } else {
                    downloadDialog.setIndeterminate(true);
                    downloadDialog.setProgress(downloadDialog.getMax());
                    downloadDialog.setProgressNumberFormat(null);
                    downloadDialog.setProgressPercentFormat(null);
                }

                // show
                if(!downloadDialog.isShowing()) downloadDialog.show();
            }
        });


    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ManagedTask task = TaskManager.getTask(TASK_DOWNLOAD_CONTAINER);
        if(task != null) TaskManager.cancelTask(task);
    }

    public interface OnClickListener {
        void onCancelTabsDialog(String targetTranslationId);
        void onConfirmTabsDialog(String targetTranslationId, List<String> sourceTranslationIds);
    }

    @Override
    public void onDestroy() {
        ManagedTask task = TaskManager.getTask(TASK_DOWNLOAD_CONTAINER);
        if(task != null) {
            task.removeOnProgressListener(this);
            task.removeOnFinishedListener(this);
        }
        if(downloadDialog != null) downloadDialog.dismiss();
        super.onDestroy();
    }
}
