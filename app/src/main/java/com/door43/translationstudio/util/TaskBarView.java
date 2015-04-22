package com.door43.translationstudio.util;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.R;

/**
 * Created by joel on 4/22/2015.
 */
public class TaskBarView extends LinearLayout {
    private LinearLayout layout;
    private ProgressBar progressBar;
    private TextView textView;
    private LinearLayout altLayout;
    private ProgressBar altProgressBar;
    private TextView altTextView;

    public TaskBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_task_bar, this, true);

        // default layout
        layout = (LinearLayout)findViewById(R.id.taskBarLayout);
        progressBar = (ProgressBar)findViewById(R.id.taskProgressBar);
        textView = (TextView)findViewById(R.id.taskProgressMessage);

        // alternate layout
        altLayout = (LinearLayout)findViewById(R.id.altTaskBarLayout);
        altProgressBar = (ProgressBar)findViewById(R.id.altTaskProgressBar);
        altTextView = (TextView)findViewById(R.id.altTaskProgressMessage);
    }

    /**
     * @deprecated
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.view_task_bar, container, false);

        // default layout
        layout = (LinearLayout)v.findViewById(R.id.taskBarLayout);
        progressBar = (ProgressBar)v.findViewById(R.id.taskProgressBar);
        textView = (TextView)v.findViewById(R.id.taskProgressMessage);

        // alternate layout
        altLayout = (LinearLayout)v.findViewById(R.id.altTaskBarLayout);
        altProgressBar = (ProgressBar)v.findViewById(R.id.altTaskProgressBar);
        altTextView = (TextView)v.findViewById(R.id.altTaskProgressMessage);
        return v;
    }

    /**
     * Publish the progress to the view
     * @param resId the message to display
     * @param progress the progress between 1 and 0
     */
    public void publishProgress(int resId, double progress) {
        updateProgress(progress);
        textView.setText(resId);
        altTextView.setText(resId);
    }

    /**
     * Publish the progress to the view
     * @param message the pessage to display
     * @param progress the progress between 1 an d0
     */
    public void publishProgress(String message, double progress) {
        updateProgress(progress);
        textView.setText(message);
        altTextView.setText(message);
    }

    /**
     * Updates the progress bar
     * @param progress
     */
    private void updateProgress(double progress) {
        if(progress < 0) {
            progressBar.setIndeterminate(true);
            altProgressBar.setIndeterminate(true);
        } else {
            progressBar.setIndeterminate(false);
            altProgressBar.setIndeterminate(false);
        }
        progressBar.setProgress((int) Math.round(100*progress));
        altProgressBar.setProgress((int) Math.round(100*progress));
    }

    /**
     * Display a progress bar
     */
    public void showProgress() {
        layout.setVisibility(View.GONE);
        altLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Display a progress spinner
     */
    public void hideProgress() {
        layout.setVisibility(View.VISIBLE);
        altLayout.setVisibility(View.GONE);
    }
}
