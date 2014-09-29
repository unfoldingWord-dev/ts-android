package com.door43.translationstudio;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.door43.delegate.DelegateListener;
import com.door43.delegate.DelegateResponse;
import com.door43.translationstudio.translations.TranslationSyncResponse;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.menu_fragment_dialog, container, false);

        // TODO: check if our key has been submitted to the server first. otherwise just display a connect to server button.
        Button syncBtn = (Button)v.findViewById(R.id.sync_btn);
        if(MainContextLink.getContext().hasRegistered()) {
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
