package com.door43.translationstudio.targettranslations;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.util.AppContext;

/**
 * Displays a list of target translations
 */
public class TargetTranslationListFragment extends Fragment implements TargetTranslationInfoDialog.OnDeleteListener {

    private TargetTranslationAdapter mAdapter;
    private OnItemClickListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_target_translation_list, container, false);

        ListView list = (ListView) rootView.findViewById(R.id.translationsList);
        mAdapter = new TargetTranslationAdapter(AppContext.getTranslator().getTargetTranslations());
        mAdapter.setOnInfoClickListener(new TargetTranslationAdapter.OnInfoClickListener() {
            @Override
            public void onClick(String targetTranslationId) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("infoDialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);

                TargetTranslation translation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);
                if(translation != null) {
                    TargetTranslationInfoDialog dialog = new TargetTranslationInfoDialog();
                    Bundle args = new Bundle();
                    args.putString(TargetTranslationInfoDialog.ARG_TARGET_TRANSLATION_ID, targetTranslationId);
                    dialog.setOnDeleteListener(TargetTranslationListFragment.this);
                    dialog.setArguments(args);
                    dialog.show(ft, "infoDialog");
                } else {
                    reloadList();
                }
            }
        });
        list.setAdapter(mAdapter);

        // open target translation
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.onItemClick(mAdapter.getItem(position));
            }
        });

        // attach to info dialogs
        if(savedInstanceState != null) {
            TargetTranslationInfoDialog dialog = (TargetTranslationInfoDialog) getFragmentManager().findFragmentByTag("infoDialog");
            if(dialog != null) {
                dialog.setOnDeleteListener(this);
            }
        }

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnItemClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnItemClickListener");
        }
    }

    /**
     * Reloads the list of target translations
     */
    public void reloadList() {
        mAdapter.changeData(AppContext.getTranslator().getTargetTranslations());
    }

    @Override
    public void onDeleteTargetTranslation(String targetTranslationId) {
        mListener.onItemDeleted(targetTranslationId);
    }

    public interface OnItemClickListener {
        void onItemDeleted(String targetTranslationId);
        void onItemClick(TargetTranslation targetTranslation);
    }
}
