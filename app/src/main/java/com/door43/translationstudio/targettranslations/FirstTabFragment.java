package com.door43.translationstudio.targettranslations;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.util.AppContext;

import java.security.InvalidParameterException;

/**
 * Created by joel on 9/14/2015.
 */
public class FirstTabFragment extends Fragment implements TargetTranslationDetailActivityListener, ChooseSourceTranslationDialog.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_first_tab, container, false);

        Bundle args = getArguments();
        final String targetTranslationId = args.getString(TargetTranslationDetailActivity.EXTRA_TARGET_TRANSLATION_ID, null);
        TargetTranslation translation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);
        if(translation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        ImageButton newTabButton = (ImageButton) rootView.findViewById(R.id.newTabButton);
        LinearLayout secondaryNewTabButton = (LinearLayout) rootView.findViewById(R.id.secondaryNewTabButton);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("tabsDialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);

                ChooseSourceTranslationDialog dialog = new ChooseSourceTranslationDialog();
                Bundle args = new Bundle();
                args.putString(ChooseSourceTranslationDialog.ARG_TARGET_TRANSLATION_ID, targetTranslationId);
                dialog.setOnClickListener(FirstTabFragment.this);
                dialog.setArguments(args);
                dialog.show(ft, "tabsDialog");
            }
        };

        newTabButton.setOnClickListener(clickListener);
        secondaryNewTabButton.setOnClickListener(clickListener);

        // attach to tabs dialog
        if(savedInstanceState != null) {
            ChooseSourceTranslationDialog dialog = (ChooseSourceTranslationDialog) getFragmentManager().findFragmentByTag("tabsDialog");
            if(dialog != null) {
                dialog.setOnClickListener(this);
            }
        }

        return rootView;
    }

    @Override
    public void onScrollProgressUpdate(int scrollProgress) {

    }

    @Override
    public void onCancelTabsDialog(String targetTranslationId) {

    }

    @Override
    public void onConfirmTabsDialog(String targetTranslationId) {
        // TODO: make sure we have at least one tab and then redirect back to previous mode
    }
}
