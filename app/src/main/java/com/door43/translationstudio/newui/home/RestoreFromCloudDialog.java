package com.door43.translationstudio.newui.home;

import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TargetTranslationMigrator;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.tasks.CloneTargetTranslationTask;
import com.door43.translationstudio.tasks.GetCloudBackupsTask;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Created by joel on 11/6/2015.
 */
public class RestoreFromCloudDialog extends DialogFragment implements GenericTaskWatcher.OnFinishedListener {
    private static final String STATE_TARGET_TRANSLATIONS = "state_target_translations";
    private static final String STATE_RESTORE_HEAD = "state_restore_head";
    private GenericTaskWatcher taskWatcher;
    private RestoreFromCloudAdapter adapter;
    private String[] targetTranslationSlugs = new String[0];
    private Translator translator;
    private Library library;
    private boolean restoreHEAD;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_restore_from_cloud, container, false);

        this.taskWatcher = new GenericTaskWatcher(getActivity(), R.string.loading);
        this.taskWatcher.setOnFinishedListener(this);

        this.translator = AppContext.getTranslator();
        this.library = AppContext.getLibrary();

        if(savedInstanceState != null) {
            restoreHEAD = savedInstanceState.getBoolean(STATE_RESTORE_HEAD, false);
        }

        Button dismissButton = (Button)v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskWatcher.stop();
                GetCloudBackupsTask task = (GetCloudBackupsTask)TaskManager.getTask(GetCloudBackupsTask.TASK_ID);
                if(task != null) {
                    task.stop();
                    TaskManager.cancelTask(task);
                    TaskManager.clearTask(task);
                }
                dismiss();
            }
        });

        ListView list = (ListView) v.findViewById(R.id.list);
        adapter = new RestoreFromCloudAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String targetTranslationSlug = adapter.getItem(position);
                // check if the user already has this target translation
                if(translator.getTargetTranslation(targetTranslationSlug) != null) {
                    // clone target translation
                    restoreHEAD = true;
                    File destDir;
                    try {
                        destDir = File.createTempFile(targetTranslationSlug, "");
                    } catch (IOException e) {
                        Logger.e(RestoreFromCloudDialog.class.getName(), "Could not create a temp directory for cloning", e);
                        return;
                    }
                    CloneTargetTranslationTask task = new CloneTargetTranslationTask(targetTranslationSlug, destDir);
                    taskWatcher.watch(task);
                    TaskManager.addTask(task, CloneTargetTranslationTask.TASK_ID);
                } else {
                    // clone target translation
                    restoreHEAD = false;
                    File destDir = TargetTranslation.generateTargetTranslationDir(targetTranslationSlug, translator.getPath());
                    CloneTargetTranslationTask task = new CloneTargetTranslationTask(targetTranslationSlug, destDir);
                    taskWatcher.watch(task);
                    TaskManager.addTask(task, CloneTargetTranslationTask.TASK_ID);
                }
            }
        });

        // connect to existing task
        GetCloudBackupsTask checkTask = (GetCloudBackupsTask)TaskManager.getTask(GetCloudBackupsTask.TASK_ID);
        CloneTargetTranslationTask cloneTask = (CloneTargetTranslationTask)TaskManager.getTask(CloneTargetTranslationTask.TASK_ID);
        if(checkTask != null) {
            taskWatcher.watch(checkTask);
            // display loading icon
        } else if(cloneTask != null) {
            taskWatcher.watch(cloneTask);
        }

        if(savedInstanceState == null) {
            if(cloneTask == null && checkTask == null) {
                // start task
                checkTask = new GetCloudBackupsTask();
                taskWatcher.watch(checkTask);
                TaskManager.addTask(checkTask, GetCloudBackupsTask.TASK_ID);
            }
        } else if (checkTask == null) {
            // load existing data
            targetTranslationSlugs = savedInstanceState.getStringArray(STATE_TARGET_TRANSLATIONS);
            adapter.setTargetTranslations(targetTranslationSlugs);
        }

        return v;
    }

    @Override
    public void onFinished(ManagedTask task) {
        taskWatcher.stop();
        TaskManager.clearTask(task);

        if(task instanceof GetCloudBackupsTask) {
            targetTranslationSlugs = ((GetCloudBackupsTask) task).getTargetTranslationSlugs();
            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if (targetTranslationSlugs.length > 0) {
                        adapter.setTargetTranslations(targetTranslationSlugs);
                    } else {
                        CustomAlertDialog.Create(getActivity())
                                .setTitle(R.string.import_from_online)
                                .setMessage(R.string.no_backups_online)
                                .setNeutralButton(R.string.dismiss, null)
                                .show("NoBackups");
                        dismiss();
                    }
                }
            });
        } else if(task instanceof CloneTargetTranslationTask) {
            final String targetTranslationSlug = ((CloneTargetTranslationTask)task).getTargetTranslationSlug();
            if(restoreHEAD) {
                // copy HEAD of the temp repo onto the existing one
                File sourcePath = ((CloneTargetTranslationTask)task).getLocalPath();
                TargetTranslation targetTranslation = translator.getTargetTranslation(targetTranslationSlug);
                if(targetTranslation != null) {
                    Logger.i(this.getClass().getName(), "Restoring HEAD in " + targetTranslationSlug + " from the cloud");
                    final File targetPath = targetTranslation.getPath();
                    // TODO: 12/17/2015 we may need to change this to use the import method in the translator. this would be way easier to maintain.
                    // TODO: 12/17/2015 we may not want to delete the head. Just merge in the translations from the server??
                    // or perhaps we could provide some options to the user so they can choose.
                    // delete HEAD of target
                    targetPath.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            if(!filename.equals(".git")) {
                                FileUtils.deleteQuietly(new File(dir, filename));
                            }
                            return false;
                        }
                    });
                    // copy new HEAD to target
                    sourcePath.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            if(!filename.equals(".git")) {
                                File sourceFile = new File(dir, filename);
                                File targetFile = new File(targetPath, filename);
                                try {
                                    if (sourceFile.isDirectory()) {
                                        FileUtils.copyDirectory(sourceFile, targetFile);
                                    } else {
                                        FileUtils.copyFile(sourceFile, targetFile);
                                    }
                                } catch (IOException e) {
                                    Logger.e(RestoreFromCloudDialog.class.getName(), "Failed to copy the restored file into HEAD at " + targetFile.getAbsolutePath(), e);
                                }
                            }
                            return false;
                        }
                    });
                    // commit changes
                    try {
                        targetTranslation.commit();
                    } catch (Exception e) {
                        Logger.e(RestoreFromCloudDialog.class.getName(), "Failed to commit changes after restoring backup", e);
                    }

                    TargetTranslationMigrator.migrateChunkChanges(AppContext.getLibrary(), targetTranslation);
                }

                FileUtils.deleteQuietly(sourcePath);
            } else {
                Logger.i(this.getClass().getName(), "Cloned " + targetTranslationSlug + " from the cloud");
            }

            Handler hand = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    // todo: terrible hack.
                    ((HomeActivity) getActivity()).notifyDatasetChanged();
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putStringArray(STATE_TARGET_TRANSLATIONS, this.targetTranslationSlugs);
        out.putBoolean(STATE_RESTORE_HEAD, this.restoreHEAD );
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        taskWatcher.stop();
        super.onDestroy();
    }
}
