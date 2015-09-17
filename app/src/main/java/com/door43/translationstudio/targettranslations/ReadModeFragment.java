package com.door43.translationstudio.targettranslations;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;

import java.security.InvalidParameterException;

/**
 * Created by joel on 9/8/2015.
 */
public class ReadModeFragment extends Fragment implements TargetTranslationDetailActivityListener, ReadAdapter.OnClickListener, ChooseSourceTranslationDialog.OnClickListener {

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ReadAdapter mAdapter;
    private boolean mFingerScroll = false;
    private TargetTranslationDetailFragmentListener mListener;
    private TargetTranslation mTargetTranslation;
    private Translator mTranslator;
    private Library mLibrary;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stacked_card_list, container, false);

        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();

        Bundle args = getArguments();
        String targetTranslationId = args.getString(TargetTranslationDetailActivity.EXTRA_TARGET_TRANSLATION_ID, null);
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        if(mTargetTranslation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        // open selected tab
        String sourceTranslationId = mTranslator.getSelectedSourceTranslationId(targetTranslationId);

        if(sourceTranslationId == null) {
            mListener.onNoSourceTranslations(targetTranslationId);
        } else {
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
            mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());

            mAdapter = new ReadAdapter(this.getActivity(), targetTranslationId, sourceTranslationId);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    mFingerScroll = true;
                    super.onScrollStateChanged(recyclerView, newState);
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (mFingerScroll) {
                        int position = mLayoutManager.findFirstVisibleItemPosition();
                        mListener.onScrollProgress(position);
                    }
                }
            });

            mListener.onItemCountChanged(mAdapter.getItemCount(), 0);

            mAdapter.setOnClickListener(this);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.rebuild();
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (TargetTranslationDetailFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement TargetTranslationDetailFragmentListener");
        }
    }

    @Override
    public void onScrollProgressUpdate(int scrollProgress) {
        mFingerScroll = false;
        mRecyclerView.scrollToPosition(scrollProgress);
    }

    @Override
    public void onTabClick(String sourceTranslationId) {
        mTranslator.setSelectedSourceTranslation(mTargetTranslation.getId(), sourceTranslationId);
        mAdapter.setSourceTranslation(sourceTranslationId);
    }

    @Override
    public void onNewTabClick() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("tabsDialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        ChooseSourceTranslationDialog dialog = new ChooseSourceTranslationDialog();
        Bundle args = new Bundle();
        args.putString(ChooseSourceTranslationDialog.ARG_TARGET_TRANSLATION_ID, mTargetTranslation.getId());
        dialog.setOnClickListener(this);
        dialog.setArguments(args);
        dialog.show(ft, "tabsDialog");
    }

    @Override
    public void onCancelTabsDialog(String targetTranslationId) {

    }

    @Override
    public void onConfirmTabsDialog(String targetTranslationId, String[] sourceTranslationIds) {
        String[] oldSourceTranslationIds = mTranslator.getSourceTranslations(targetTranslationId);
        for(String id:oldSourceTranslationIds) {
            mTranslator.removeSourceTranslation(targetTranslationId, id);
        }

        if(sourceTranslationIds.length > 0) {
            // save open source language tabs
            for(String id:sourceTranslationIds) {
                SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(id);
                mTranslator.addSourceTranslation(targetTranslationId, sourceTranslation);
            }
            String selectedSourceTranslationId = mTranslator.getSelectedSourceTranslationId(targetTranslationId);
            mAdapter.setSourceTranslation(selectedSourceTranslationId);
        } else {
            mListener.onNoSourceTranslations(targetTranslationId);
        }
    }
}
