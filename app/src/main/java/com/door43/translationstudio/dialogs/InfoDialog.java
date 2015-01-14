package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.MainContext;

/**
 * This is the contextual menu dialog fragment
 */
public class InfoDialog extends DialogFragment {
    private final InfoDialog me = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_info, container, false);

        // display app version
        TextView versionText = (TextView)v.findViewById(R.id.app_version);
        PackageInfo pInfo = null;
        try {
            pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            versionText.setText("version "+pInfo.versionName+" build "+pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(this.getClass().getName(), "failed to get package name", e);
        }
        // display device id
        TextView deviceId = (TextView)v.findViewById(R.id.device_udid);
        deviceId.setText("udid: "+ MainContext.getContext().getUDID());

        // hook up settings button
        Button advancedSettingsBtn = (Button)v.findViewById(R.id.advanced_settings_btn);
        advancedSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)getActivity()).openAdvancedSettings();
            }
        });
        return v;
    }
}
