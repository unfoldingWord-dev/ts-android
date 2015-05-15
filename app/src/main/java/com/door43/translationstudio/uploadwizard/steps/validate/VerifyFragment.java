package com.door43.translationstudio.uploadwizard.steps.validate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.tasks.GenericTaskWatcher;
import com.door43.translationstudio.tasks.ValidateTranslationTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.threads.ManagedTask;
import com.door43.util.threads.TaskManager;
import com.door43.util.wizard.WizardFragment;

import java.util.ArrayList;

/**
 * Created by joel on 5/14/2015.
 */
public class VerifyFragment extends WizardFragment implements GenericTaskWatcher.OnCanceledListener, GenericTaskWatcher.OnFinishedListener {
    private ArrayList<UploadValidationItem> mValidationItems = new ArrayList<>();
    private UploadValidationAdapter mAdapter;
    private GenericTaskWatcher mTaskWatcher;
    private boolean mClosingActivity = true;
    private ValidateTranslationTask mTask;
    private Button mNextBtn;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mClosingActivity = true;
        View v = inflater.inflate(R.layout.fragment_upload_verify, container, false);
        ListView list = (ListView)v.findViewById(R.id.validationListView);
        Button backBtn = (Button)v.findViewById(R.id.backButton);
        mNextBtn = (Button)v.findViewById(R.id.nextButton);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSkip(-1);
            }
        });
        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTask != null) {
                    if(mTask.hasWarnings()) {
                        // TODO: confirm continue if there are validation warnings
                    } else {
                        onNext();
                    }
                } else {
                    onSkip(-1);
                }
            }
        });

        mAdapter = new UploadValidationAdapter(mValidationItems, getActivity());
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UploadValidationItem item = mAdapter.getItem(position);
                if(!item.getDescription().isEmpty()) {
                    AppContext.context().showMessageDialog(item.getTitle(), item.getDescription());
                }
            }
        });

        mTaskWatcher = new GenericTaskWatcher(getActivity(), R.string.loading);
        mTaskWatcher.setOnCanceledListener(this);
        mTaskWatcher.setOnFinishedListener(this);
        ValidateTranslationTask task = (ValidateTranslationTask)TaskManager.getTask(ValidateTranslationTask.TASK_ID);
        if(task != null) {
            mTaskWatcher.watch(task);
        } else {
            // start validating tasks
            task = new ValidateTranslationTask();
            mTaskWatcher.watch(task);
            TaskManager.addTask(task);
        }

        return v;
    }

    public void onResume() {
        super.onResume();
        mTaskWatcher.watch(ValidateTranslationTask.TASK_ID);
    }

    @Override
    public void onCanceled(ManagedTask task) {
        onPrevious();
    }

    @Override
    public void onFinished(ManagedTask task) {
        if(((ValidateTranslationTask)task).hasErrors()) {
            // TODO: disable the next button if there are validation errors

        }
        mTask = (ValidateTranslationTask)task;
        // TODO: populate the list with the validation items
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mClosingActivity = false;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        // clean up the validation task for this project
        if(mClosingActivity) {
            ValidateTranslationTask task = (ValidateTranslationTask)TaskManager.getTask(ValidateTranslationTask.TASK_ID);
            if(task != null) {
                TaskManager.cancelTask(task);
                TaskManager.clearTask(task);
            }
        }
        super.onDestroy();
    }
}
