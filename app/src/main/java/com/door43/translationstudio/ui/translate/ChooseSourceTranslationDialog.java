package com.door43.translationstudio.ui.translate;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
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
    private ProgressDialog mProgressDialog;
    private boolean mInitializing = true;

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

        final EditText searchText = (EditText) v.findViewById(R.id.search_text);
        searchText.setHint(R.string.choose_source_translations);
        searchText.setEnabled(true);
        searchText.setFocusable(true);
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mAdapter != null) {
                    mAdapter.applySearch(s.toString());
                }
            }
        });

        ImageButton searchBackButton = (ImageButton) v.findViewById(R.id.search_back_button);
        searchBackButton.setVisibility(View.GONE);
        ImageView searchIcon = (ImageView) v.findViewById(R.id.search_mag_icon);
        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchText.requestFocus();
                App.showKeyboard(getActivity(),searchText, false);
            }
        });

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
                        String format = getResources().getString(R.string.download_source_language);
                        String message = String.format(format, item.title);
                        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                                .setTitle(R.string.title_download_source_language)
                                .setMessage(message)
                                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DownloadResourceContainerTask task = new DownloadResourceContainerTask(item.sourceTranslation);
                                        task.addOnFinishedListener(ChooseSourceTranslationDialog.this);
                                        task.addOnProgressListener(ChooseSourceTranslationDialog.this);
                                        task.TAG = position;
                                        mProgressDialog = null;
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
                        mAdapter.checkForItemUpdates(item);
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
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
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

        Button updateButton = (Button) v.findViewById(R.id.updateButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                        .setTitle(R.string.warning_title)
                        .setMessage(R.string.update_warning)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(mListener != null) {
                                    mListener.onUpdateSources();
                                }
                                dismiss();
                            }
                        })
                        .setNegativeButton(R.string.menu_cancel, null)
                        .show();
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

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                loadData();
            }
        });
        return v;
    }

    private void loadData() {
        mProgressDialog = null;
        ManagedTask initTask = TaskManager.getTask(TASK_INIT);
        if(initTask == null) {
            // begin init
            initTask = new ManagedTask() {
                @Override
                public void start() {
                    this.publishProgress(-1,"");

                    // add selected source translations
                    String[] sourceTranslationSlugs = App.getOpenSourceTranslations(mTargetTranslation.getId());
                    for (String slug : sourceTranslationSlugs) {
                        Translation st = mLibrary.index.getTranslation(slug);
                        if (st != null) {
                            this.publishProgress(-1,st.resourceContainerSlug);
                            addSourceTranslation(st, true);
                        }
                    }

                    List<Translation> availableTranslations = mLibrary.index.findTranslations(null, mTargetTranslation.getProjectId(), null, "book", null, App.MIN_CHECKING_LEVEL, -1);
                    for (Translation sourceTranslation : availableTranslations) {
                        this.publishProgress(-1,sourceTranslation.resourceContainerSlug);
                        addSourceTranslation(sourceTranslation, false);
                    }
                }
            };

            initTask.addOnFinishedListener(this);
            initTask.addOnProgressListener(this);
            TaskManager.addTask(initTask, TASK_INIT);
        } else {
            // connect to existing
            initTask.addOnFinishedListener(this);
            initTask.addOnProgressListener(this);
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
        final String title = sourceTranslation.language.name + " (" + sourceTranslation.language.slug + ") - " + sourceTranslation.resource.name;
        final boolean isDownloaded = mLibrary.exists(sourceTranslation.resourceContainerSlug);
        mAdapter.addItem(new ChooseSourceTranslationAdapter.ViewItem(title, sourceTranslation, selected, isDownloaded), selected);
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
                    if(mProgressDialog != null && mProgressDialog.isShowing()) mProgressDialog.dismiss();
                    if(t.success()) {
                        mAdapter.markItemDownloaded(t.TAG);
                    } else {
                        Snackbar snack = Snackbar.make(ChooseSourceTranslationDialog.this.getView(), getResources().getString(R.string.download_failed), Snackbar.LENGTH_LONG);
                        ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                        snack.show();
                    }
                }
            });
        } else if(TASK_INIT.equals(task.getTaskId())) { // loading task
            mInitializing = false;
            if(mProgressDialog != null && mProgressDialog.isShowing()) mProgressDialog.dismiss();
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.sort();
                }
            });
        }
    }

    @Override
    public void onTaskProgress(final ManagedTask task, final double progress, final String message, boolean secondary) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                // init dialog
                if(mProgressDialog == null) {
                    mProgressDialog = new ProgressDialog(getActivity());
                    mProgressDialog.setCancelable(true);
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    mProgressDialog.setOnCancelListener(ChooseSourceTranslationDialog.this);
                    int titleId = R.string.loading_sources;
                    if(!mInitializing) { // doing download
                        mProgressDialog.setIcon(R.drawable.ic_cloud_download_black_24dp);
                        titleId = R.string.downloading;
                    } else {
                        mProgressDialog.setMessage(""); // make room for message
                    }
                    mProgressDialog.setTitle(titleId);
                }

                if(mProgressDialog == null) {
                    return;
                }

                // dismiss if finished
                if(task.isFinished()) {
                    mProgressDialog.dismiss();
                    return;
                }

                // progress
                mProgressDialog.setMax(task.maxProgress());
                if(progress > 0) {
                    mProgressDialog.setIndeterminate(false);
                    mProgressDialog.setProgressStyle((int)progress);
                    mProgressDialog.setProgressNumberFormat("%1d/$2d");
                    mProgressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                } else {
                    mProgressDialog.setIndeterminate(true);
                    mProgressDialog.setProgress(mProgressDialog.getMax());
                    mProgressDialog.setProgressNumberFormat(null);
                    mProgressDialog.setProgressPercentFormat(null);
                }

                if((message != null) && (!message.isEmpty()) ) {
                    mProgressDialog.setMessage(message);
                }

                // show
                if(!mProgressDialog.isShowing()) mProgressDialog.show();
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ManagedTask task = TaskManager.getTask(TASK_DOWNLOAD_CONTAINER);
        if(task != null) {
            task.removeOnProgressListener(this);
            task.removeOnFinishedListener(this);
            TaskManager.cancelTask(task);
        }
        task = TaskManager.getTask(TASK_INIT);
        if(task != null) {
            task.removeOnProgressListener(this);
            task.removeOnFinishedListener(this);
            TaskManager.cancelTask(task);
        }
    }

    public interface OnClickListener {
        void onCancelTabsDialog(String targetTranslationId);
        void onConfirmTabsDialog(String targetTranslationId, List<String> sourceTranslationIds);
        void onUpdateSources();
    }

    @Override
    public void onDestroy() {
        ManagedTask task = TaskManager.getTask(TASK_DOWNLOAD_CONTAINER);
        if(task != null) {
            task.removeOnProgressListener(this);
            task.removeOnFinishedListener(this);
        }
        task = TaskManager.getTask(TASK_INIT);
        if(task != null) {
            task.removeOnProgressListener(this);
            task.removeOnFinishedListener(this);
        }
        if(mProgressDialog != null) mProgressDialog.dismiss();
        super.onDestroy();
    }
}
