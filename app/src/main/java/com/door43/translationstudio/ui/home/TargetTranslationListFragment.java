package com.door43.translationstudio.ui.home;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.ui.BaseFragment;

import org.unfoldingword.tools.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of target translations
 */
public class TargetTranslationListFragment extends BaseFragment implements TargetTranslationInfoDialog.OnDeleteListener {

    public static final String TAG = TargetTranslationListFragment.class.getSimpleName();
    private TargetTranslationAdapter mAdapter;
    private OnItemClickListener mListener;
    private SortProjectColumnType mSortProjectColumn;
    private SortByColumnType mSortByColumn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_target_translation_list, container, false);

        ListView list = (ListView) rootView.findViewById(R.id.translationsList);
        mAdapter = new TargetTranslationAdapter(getActivity());
        mAdapter.setOnInfoClickListener(new TargetTranslationAdapter.OnInfoClickListener() {
            @Override
            public void onClick(String targetTranslationId) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("infoDialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);

                final TargetTranslation translation = App.getTranslator().getTargetTranslation(targetTranslationId);
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

        Spinner sortColumnSpinner = (Spinner) rootView.findViewById(R.id.sort_column);
        if(sortColumnSpinner != null) {
            List<String> types = new ArrayList<String>();
            types.add(this.getResources().getString(R.string.sort_project_then_language));
            types.add(this.getResources().getString(R.string.sort_language_then_project));
            types.add(this.getResources().getString(R.string.sort_progress_then_project));
            ArrayAdapter<String> typesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, types);
            typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sortColumnSpinner.setAdapter(typesAdapter);

            sortColumnSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Logger.i(TAG, "Sort column item selected: " + position);
                    mSortByColumn = SortByColumnType.fromInt(position);
                    if(mAdapter != null) {
                        mAdapter.sort(mSortByColumn, mSortProjectColumn);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        Spinner sortProjectSpinner = (Spinner) rootView.findViewById(R.id.sort_projects);
        if(sortProjectSpinner != null) {
            List<String> types = new ArrayList<String>();
            types.add(this.getResources().getString(R.string.sort_bible_order));
            types.add(this.getResources().getString(R.string.sort_alphabetical_order));
            ArrayAdapter<String> typesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, types);
            typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sortProjectSpinner.setAdapter(typesAdapter);

            sortProjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Logger.i(TAG, "Sort project column item selected: " + position);
                    mSortProjectColumn = SortProjectColumnType.fromInt(position);
                    if(mAdapter != null) {
                        mAdapter.sort(mSortByColumn, mSortProjectColumn);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

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
        mAdapter.changeData(App.getTranslator().getTargetTranslations());
    }

    @Override
    public void onDeleteTargetTranslation(String targetTranslationId) {
        mListener.onItemDeleted(targetTranslationId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public interface OnItemClickListener {
        void onItemDeleted(String targetTranslationId);
        void onItemClick(TargetTranslation targetTranslation);
    }

    /**
     * enum that keeps track of current state of USFM import
     */
    public enum SortByColumnType {
        projectThenLanguage(0),
        languageThenProject(1),
        progressThenProject(2);

        private int _value;

        SortByColumnType(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static SortByColumnType fromInt(int i) {
            for (SortByColumnType b : SortByColumnType.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }

    /**
     * enum that keeps track of current state of USFM import
     */
    public enum SortProjectColumnType {
        bibleOrder(0),
        alphabetical(1);

        private int _value;

        SortProjectColumnType(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static SortProjectColumnType fromInt(int i) {
            for (SortProjectColumnType b : SortProjectColumnType.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }

}
