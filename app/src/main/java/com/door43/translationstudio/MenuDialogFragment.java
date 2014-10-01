package com.door43.translationstudio;

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

import com.door43.translationstudio.util.MainContextLink;

/**
 * This is the contextual menu dialog fragment
 */
public class MenuDialogFragment extends DialogFragment {
    private final MenuDialogFragment me = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.menu_fragment_dialog, container, false);

        // display app version
        TextView versionText = (TextView)v.findViewById(R.id.app_version);
        PackageInfo pInfo = null;
        try {
            pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            versionText.setText("version "+pInfo.versionName+" build "+pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // hook up buttons

        Button syncBtn = (Button)v.findViewById(R.id.sync_btn);
        if(MainContextLink.getContext().hasRegisteredKeys()) {
            syncBtn.setText("Upload Translation");
        } else {
            syncBtn.setText("Request Upload Permission");
        }
        syncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                MainContextLink.getContext().getSharedTranslationManager().sync();
                me.dismiss();
            }
        });


        Button shareBtn = (Button)v.findViewById(R.id.share_btn);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: display the sharing interface.
                MainContextLink.getContext().showToastMessage("Sharing has not been built yet.");
                me.dismiss();
            }
        });
        Button settingsBtn = (Button)v.findViewById(R.id.settings_btn);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(me.getActivity(), SettingsActivity.class);
                startActivity(intent);
                me.dismiss();
            }
        });
        return v;
    }
}
