package com.door43.translationstudio.newui.publish;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.tasks.UploadProjectTask;
import com.door43.translationstudio.tasks.UploadTargetTranslationTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import java.security.InvalidParameterException;

/**
 * Created by joel on 9/20/2015.
 */
public class PublishFragment extends PublishStepFragment implements ManagedTask.OnFinishedListener {

    private static final String STATE_UPLOADED = "state_uploaded";
    private boolean mUploaded = false;
    private Button mUploadButton;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_publish_publish, container, false);

        if(savedInstanceState != null) {
            mUploaded = savedInstanceState.getBoolean(STATE_UPLOADED, false);
        }

        Bundle args = getArguments();
        final String targetTranslationId = args.getString(PublishActivity.EXTRA_TARGET_TRANSLATION_ID);
        if (targetTranslationId == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        final TargetTranslation targetTranslation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);

        mUploadButton = (Button)rootView.findViewById(R.id.upload_button);
        if(mUploaded) {
            mUploadButton.setBackgroundColor(getResources().getColor(R.color.green));
            // TODO: add done icon
        }
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // marks the publish step as done
                getListener().finishPublishing();
                // begin upload
                UploadTargetTranslationTask task = new UploadTargetTranslationTask(targetTranslation);
                task.addOnFinishedListener(PublishFragment.this);
                TaskManager.addTask(task, UploadTargetTranslationTask.TASK_ID);
                // TODO: display progress dialog
            }
        });

        UploadTargetTranslationTask task = (UploadTargetTranslationTask)TaskManager.getTask(UploadTargetTranslationTask.TASK_ID);
        if(task != null) {
            task.addOnFinishedListener(this);
            // TODO: display progress dialog
        }

        return rootView;
    }

    @Override
    public void onFinished(ManagedTask task) {
        TaskManager.clearTask(task);
        mUploaded = true;
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                mUploadButton.setBackgroundColor(getResources().getColor(R.color.green));
                // TODO: add done icon
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_UPLOADED, mUploaded);
        super.onSaveInstanceState(out);
    }
}
