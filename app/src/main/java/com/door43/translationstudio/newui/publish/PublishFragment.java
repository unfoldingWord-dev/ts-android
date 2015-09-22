package com.door43.translationstudio.newui.publish;

import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.tasks.UploadProjectTask;
import com.door43.translationstudio.tasks.UploadTargetTranslationTask;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.widget.ViewUtil;

import java.security.InvalidParameterException;

/**
 * Created by joel on 9/20/2015.
 */
public class PublishFragment extends PublishStepFragment implements ManagedTask.OnFinishedListener {

    private static final String STATE_UPLOADED = "state_uploaded";
    private boolean mUploaded = false;
    private Button mUploadButton;
    private LinearLayout mUploadSuccess;

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

        // receive uploaded status from activity (overrides save state from fragment)
        mUploaded = args.getBoolean(ARG_PUBLISH_FINISHED, mUploaded);

        final TargetTranslation targetTranslation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);

        mUploadSuccess = (LinearLayout)rootView.findViewById(R.id.upload_success);
        mUploadButton = (Button)rootView.findViewById(R.id.upload_button);

        if(mUploaded) {
            mUploadButton.setVisibility(View.GONE);
            mUploadSuccess.setVisibility(View.VISIBLE);
        } else {
            mUploadButton.setVisibility(View.VISIBLE);
            mUploadSuccess.setVisibility(View.GONE);
        }

        // give the user some happy feedback in case they feel like clicking again
        mUploadSuccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.success, Snackbar.LENGTH_SHORT);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(AppContext.context().isNetworkAvailable()) {
                    // marks the publish step as done
                    getListener().finishPublishing();
                    // begin upload
                    UploadTargetTranslationTask task = new UploadTargetTranslationTask(targetTranslation);
                    task.addOnFinishedListener(PublishFragment.this);
                    TaskManager.addTask(task, UploadTargetTranslationTask.TASK_ID);
                    // TODO: display progress dialog
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.internet_not_available, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });

        ImageView wifiIcon = (ImageView)rootView.findViewById(R.id.wifi_icon);
        ViewUtil.tintViewDrawable(wifiIcon, getResources().getColor(R.color.dark_secondary_text));

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
                mUploadButton.setVisibility(View.GONE);
                mUploadSuccess.setVisibility(View.VISIBLE);
                Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.success, Snackbar.LENGTH_SHORT);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_UPLOADED, mUploaded);
        super.onSaveInstanceState(out);
    }
}
