package com.door43.translationstudio.newui.publish;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.door43.translationstudio.R;
import com.door43.translationstudio.tasks.ValidationTask;
import com.door43.util.DummyDialogListener;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import java.security.InvalidParameterException;

/**
 * Created by joel on 9/20/2015.
 */
public class ValidationFragment extends PublishStepFragment implements ManagedTask.OnFinishedListener, ValidationAdapter.OnClickListener {
    private LinearLayout mLoadingLayout;
    private RecyclerView mRecyclerView;
    private ValidationAdapter mValidationAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_publish_validation_list, container, false);

        Bundle args = getArguments();
        String targetTranslationId = args.getString(PublishActivity.EXTRA_TARGET_TRANSLATION_ID);
        String sourceTranslationId = args.getString(ARG_SOURCE_TRANSLATION_ID);
        if(targetTranslationId == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }
        if(sourceTranslationId == null) {
            throw new InvalidParameterException("a valid source translation id is required");
        }

        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mValidationAdapter = new ValidationAdapter(getActivity());
        mValidationAdapter.setOnClickListener(this);
        mRecyclerView.setAdapter(mValidationAdapter);
        mLoadingLayout = (LinearLayout)rootView.findViewById(R.id.loading_layout);

        // display loading view
        mRecyclerView.setVisibility(View.GONE);
        mLoadingLayout.setVisibility(View.VISIBLE);

        // start task to validate items
        ValidationTask task = (ValidationTask) TaskManager.getTask(ValidationTask.TASK_ID);
        if(task != null) {
            task.addOnFinishedListener(this);
        } else {
            // start new task
            task = new ValidationTask(targetTranslationId, sourceTranslationId);
            task.addOnFinishedListener(this);
            TaskManager.addTask(task);
        }

        return rootView;
    }

    @Override
    public void onFinished(final ManagedTask task) {
        TaskManager.clearTask(task);
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                mValidationAdapter.setValidations(((ValidationTask) task).getValidations());
                mRecyclerView.setVisibility(View.VISIBLE);
                mLoadingLayout.setVisibility(View.GONE);
                // TODO: animate
            }
        });
    }

    @Override
    public void onClickReview(String targetTranslationId, String chapterId, String frameId) {
        openReview(targetTranslationId, chapterId, frameId);
    }

    @Override
    public void onClickNext() {
        if(mValidationAdapter.getItemCount() > 2) {
            // when there are more than two items (success card and next button) there were validation issues
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.dialog_validation_warnings)
                    .setMessage(R.string.validation_warnings)
                    .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            nextStep();
                        }
                    })
                    .setNegativeButton(R.string.title_cancel, new DummyDialogListener()).show();
        } else {
            nextStep();
        }
    }

    private void nextStep() {
        // TODO: proceed to the next step
    }

    @Override
    public void onDestroy() {
        mValidationAdapter.setOnClickListener(null);
        super.onDestroy();
    }
}
