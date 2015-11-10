package com.door43.translationstudio.newui.home;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.tasks.GetCloudBackupsTask;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

/**
 * Created by joel on 11/6/2015.
 */
public class RestoreFromCloudDialog extends DialogFragment implements GenericTaskWatcher.OnFinishedListener {
    private static final String STATE_TARGET_TRANSLATIONS = "state_target_translations";
    private GenericTaskWatcher taskWatcher;
    private RestoreFromCloudAdapter adapter;
    private String[] targetTranslationSlugs = new String[0];
    private Translator translator;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_restore_from_cloud, container, false);

        this.taskWatcher = new GenericTaskWatcher(getActivity(), R.string.loading);
        this.taskWatcher.setOnFinishedListener(this);

        this.translator = AppContext.getTranslator();

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
                    // TODO: confirm restore then just clone to a temporary location and just replace the current state. or do we want the entire history to be overwritten?
                } else {
                    // TODO: clone backup
                }
            }
        });

        // connect to existing task
        GetCloudBackupsTask task = (GetCloudBackupsTask)TaskManager.getTask(GetCloudBackupsTask.TASK_ID);
        if(task != null) {
            taskWatcher.watch(task);
            // display loading icon
        }

        if(savedInstanceState == null) {
            // start task
            task = new GetCloudBackupsTask();
            taskWatcher.watch(task);
            TaskManager.addTask(task, GetCloudBackupsTask.TASK_ID);
        } else if (task == null) {
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
        targetTranslationSlugs = ((GetCloudBackupsTask)task).getTargetTranslationSlugs();
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if(targetTranslationSlugs.length > 0) {
                    adapter.setTargetTranslations(targetTranslationSlugs);
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.import_from_online)
                            .setMessage(R.string.no_backups_online)
                            .setNeutralButton(R.string.dismiss, null)
                            .show();
                    dismiss();
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putStringArray(STATE_TARGET_TRANSLATIONS, targetTranslationSlugs);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        taskWatcher.stop();
        super.onDestroy();
    }
}
