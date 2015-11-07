package com.door43.translationstudio.newui.home;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.R;

/**
 * Created by joel on 11/6/2015.
 */
public class RestoreFromCloudDialog extends DialogFragment {

    public static final String ARG_TARGET_TRANSLATIONS = "arg_target_translation_slugs";

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_restore_from_cloud, container, false);

        Bundle args = getArguments();
        String[] targetTranslationSlugs = new String[0];
        if(args != null) {
            targetTranslationSlugs = args.getStringArray(ARG_TARGET_TRANSLATIONS);
        }

        Button dismissButton = (Button)v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        if(targetTranslationSlugs.length > 0) {
            ListView list = (ListView) v.findViewById(R.id.list);
            RestoreFromCloudAdapter adapter = new RestoreFromCloudAdapter(targetTranslationSlugs);
            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // todo download backup
                }
            });
        } else {
            dismiss();
        }

        return v;
    }
}
