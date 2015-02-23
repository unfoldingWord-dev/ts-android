package com.door43.translationstudio;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.translationstudio.util.AppContext;
import com.door43.logging.Logger;
import com.door43.translationstudio.util.ToolAdapter;
import com.door43.translationstudio.util.ToolItem;
import com.door43.translationstudio.util.TranslatorBaseActivity;

import java.util.ArrayList;

public class DeveloperToolsActivity extends TranslatorBaseActivity {

    private ArrayList<ToolItem> mDeveloperTools = new ArrayList<>();
    private ToolAdapter mAdapter;
    private String mVersionName;
    private String mVersionCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer_tools);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView versionText = (TextView)findViewById(R.id.appVersionText);
        TextView buildText = (TextView)findViewById(R.id.appBuildNumberText);
        TextView udidText = (TextView)findViewById(R.id.deviceUDIDText);

        // display app version
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mVersionName = pInfo.versionName;
            versionText.setText(String.format(getResources().getString(R.string.app_version_name), pInfo.versionName));
            mVersionCode = pInfo.versionCode+"";
            buildText.setText(String.format(getResources().getString(R.string.app_version_code) ,pInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(this.getClass().getName(), "failed to get package name", e);
        }

        // display device id
        udidText.setText(String.format(getResources().getString(R.string.app_udid), AppContext.udid()));

        // set up copy handlers
        versionText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard(mVersionName);
            }
        });
        buildText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard(mVersionCode);
            }
        });
        udidText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard(AppContext.udid());
            }
        });



        // set up developer tools
        ListView list = (ListView)findViewById(R.id.developerToolsListView);
        mAdapter = new ToolAdapter(mDeveloperTools, this);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ToolItem tool = mAdapter.getItem(i);
                if(tool.isEnabled()) {
                    tool.getAction().run();
                }
            }
        });

        // load tools
        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.regenerate_keys), getResources().getString(R.string.regenerate_keys_description), 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                ProgressDialog loading = AppContext.showLoading(DeveloperToolsActivity.this);
                AppContext.context().generateKeys();
                loading.dismiss();
                AppContext.context().showToastMessage(R.string.success);
            }
        }));
        mDeveloperTools.add(new ToolItem(getResources().getString(R.string.force_update_projects), getResources().getString(R.string.force_update_projects_description), 0, new ToolItem.ToolAction() {
            @Override
            public void run() {
                AppContext.context().showToastMessage("not implimented yet");
                // TODO: perform the forced update. We'll need to update the project manager to support this.
            }
        }, false, 0));
    }

    /**
     * Copies the text to the clipboard
     * @param text
     */
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("simple text", text);
        clipboard.setPrimaryClip(clip);
        AppContext.context().showToastMessage(R.string.copied_to_clipboard);
    }
}
