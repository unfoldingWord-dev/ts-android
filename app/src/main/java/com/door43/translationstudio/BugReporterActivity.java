package com.door43.translationstudio;

import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.ServerUtilities;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 1/14/2015.
 */
public class BugReporterActivity extends TranslatorBaseActivity {
    private Button mOkButton;
    private Button mCancelButton;
    private ProgressDialog mDialog;
    private EditText mCrashReportText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_reporter);

        final UploadReportsTask task = new UploadReportsTask();

        mOkButton = (Button)findViewById(R.id.okButton);
        mCancelButton = (Button)findViewById(R.id.cancelButton);
        mCrashReportText = (EditText)findViewById(R.id.crashDescriptioneditText);
        mDialog = new ProgressDialog(BugReporterActivity.this);

        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoading();
                task.execute(true);
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoading();
                task.execute(false);
            }
        });
    }

    private void showLoading() {
        // hide buttons
        mOkButton.setVisibility(View.GONE);
        mCancelButton.setVisibility(View.GONE);
        mCrashReportText.setVisibility(View.GONE);
    }

    private class UploadReportsTask extends AsyncTask<Boolean, String, Void> {

        @Override
        protected Void doInBackground(Boolean... bools) {
            Boolean upload = bools[0];
            String notes = mCrashReportText.getText().toString().trim();
            Handler handle = new Handler(getMainLooper());
            if(upload) {
                mDialog.setMessage(getResources().getString(R.string.push_msg_init));
            } else {
                mDialog.setMessage(getResources().getString(R.string.loading));
            }
            // show the dialog
            handle.post(new Runnable() {
                @Override
                public void run() {
                    mDialog.show();
                }
            });

            File logFile = new File(getExternalCacheDir(), "log.txt");

            if(upload) {
                // upload report
                try {
                    JSONObject json = new JSONObject();
                    String title;
                    // generate title
                    if(notes.length() < 30 && !notes.isEmpty()) {
                        title = notes;
                    } else if(!notes.isEmpty()) {
                        title = notes.substring(0, 29) + "...";
                    } else {
                        title = "bug report";
                    }

                    // record environment details
                    PackageInfo pInfo = null;
                    StringBuffer infoBuf = new StringBuffer();
                    if(!notes.isEmpty()) {
                        infoBuf.append("Notes\n======\n");
                        infoBuf.append(notes + "\n");
                    }
                    infoBuf.append("\nEnvironment\n======\n");
                    try {
                        pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                        infoBuf.append("version: " + pInfo.versionName + "\n");
                        infoBuf.append("build: " + pInfo.versionCode + "\n");
                    } catch (PackageManager.NameNotFoundException e) {
                        Logger.e(BugReporterActivity.this.getClass().getName(), "Could not find the package name", e);
                    }
                    infoBuf.append("UDID: " + app().getUDID() + "\n");
                    infoBuf.append("Android Release: " + Build.VERSION.RELEASE + "\n");
                    infoBuf.append("Androind SDK: " + Build.VERSION.SDK_INT + "\n");
                    infoBuf.append("Brand: " + Build.BRAND + "\n");
                    infoBuf.append("Device: " + Build.DEVICE + "\n");

                    if(logFile.exists() && logFile.length() > 0) {
                        infoBuf.append("Log history\n======\n");
                        infoBuf.append(FileUtilities.getStringFromFile(logFile));
                        // empty the log.
                        FileUtils.write(logFile, "");
                    }

                    // build payload
                    try {
                        json.put("title", title);
                        json.put("body", infoBuf.toString());
                        JSONArray labels = new JSONArray();
                        labels.put("bug report");
                        if(pInfo != null) {
                            labels.put(pInfo.versionName);
                        }
                        json.put("labels", labels);
                    } catch (JSONException e) {
                        return null;
                    }

                    List<NameValuePair> headers = new ArrayList<NameValuePair>();
                    headers.add(new BasicNameValuePair("Authorization", "token "+getResources().getString(R.string.github_oauth2)));
                    headers.add(new BasicNameValuePair("Content-Type", "application/json"));
                    String response = ServerUtilities.post("https://api.github.com/repos/Door43/translationStudio2/issues", headers, json.toString());
                    Log.d("Response", response);
                } catch (IOException e) {
                    Logger.w(this.getClass().getName(), "failed to upload bug report", e);
                }
            }
            return null;
        }

        protected void onProgressUpdate(String... items) {
        }

        protected void onPostExecute(Void item) {
            Handler handle = new Handler(getMainLooper());
            handle.post(new Runnable() {
                @Override
                public void run() {
                    mDialog.dismiss();
                }
            });
            app().showToastMessage(R.string.success);
            finish();
        }
    }
}