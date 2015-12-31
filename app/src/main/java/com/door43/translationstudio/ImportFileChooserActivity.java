package com.door43.translationstudio;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.BaseActivity;
import com.door43.translationstudio.tasks.ArchiveCrashReportTask;
import com.door43.translationstudio.tasks.CheckForLatestReleaseTask;
import com.door43.translationstudio.tasks.UploadCrashReportTask;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

/**
 * Created by blm on 12/31/15.
 */
public class ImportFileChooserActivity extends BaseActivity {
    private Button mUpButton;
    private Button mInternalButton;
    private Button mSdCardButton;
    private TextView mCurrentFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_import_file);

        mUpButton = (Button) findViewById(R.id.up_folder_button);
        mInternalButton = (Button) findViewById(R.id.internal_button);
        mSdCardButton = (Button) findViewById(R.id.sd_card_button);
        mCurrentFolder = (TextView) findViewById(R.id.current_folder);

        mCurrentFolder.setText(".");

        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mInternalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mSdCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        Intent intent = getIntent();
        String type = intent.getType();
        if (intent.getData() != null) {


        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mCurrentFolder.setText("...");

    }
}

