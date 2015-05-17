package com.door43.translationstudio.uploadwizard.steps.validate;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.translationstudio.tasks.ValidateTranslationTask;
import com.door43.util.DummyDialogListener;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.util.wizard.WizardFragment;

import java.util.ArrayList;

/**
 * Created by joel on 5/14/2015.
 */
public class VerifyFragment extends WizardFragment implements GenericTaskWatcher.OnCanceledListener, GenericTaskWatcher.OnFinishedListener {
    private ArrayList<UploadValidationItem> mValidationItems = new ArrayList<>();
    private UploadValidationAdapter mAdapter;
    private GenericTaskWatcher mTaskWatcher;
    private ValidateTranslationTask mTask;
    private Button mNextBtn;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_upload_verify, container, false);
        ListView list = (ListView)v.findViewById(R.id.validationListView);
        Button backBtn = (Button)v.findViewById(R.id.backButton);
        mNextBtn = (Button)v.findViewById(R.id.nextButton);
        mNextBtn.setVisibility(View.GONE);

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
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.dialog_validation_warnings)
                                .setMessage(R.string.validation_warnings)
                                .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onNext();
                                    }
                                })
                                .setNegativeButton(R.string.title_cancel, new DummyDialogListener()).show();
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(item.getTitle())
                            .setMessage(item.getDescription())
                            .setPositiveButton(R.string.label_ok, new DummyDialogListener()).show();
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
        mTaskWatcher.stop();
        onPrevious();
    }

    @Override
    public void onFinished(ManagedTask task) {
        mTaskWatcher.stop();
        if(((ValidateTranslationTask)task).hasErrors()) {
            mNextBtn.setVisibility(View.GONE);
        } else {
            mNextBtn.setVisibility(View.VISIBLE);
        }
        mTask = (ValidateTranslationTask)task;
//        if(!mTask.hasWarnings() && !mTask.hasErrors()) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//            builder.setTitle(R.string.notice)
//                    .setMessage(R.string.translation_appears_valid)
//                    .setPositiveButton(R.string.label_ok, new DummyDialogListener())
//                    .show();
//        }
        mAdapter.changeDataset(mTask.getValidationItems());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO: it would be nice to cache the results of the validation.
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        mTaskWatcher.stop();
        super.onDestroy();
    }
}
