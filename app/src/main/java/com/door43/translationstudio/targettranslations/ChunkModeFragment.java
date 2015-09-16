package com.door43.translationstudio.targettranslations;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.util.AppContext;

import java.security.InvalidParameterException;

/**
 * Created by joel on 9/8/2015.
 * TODO: the only difference between this fragment and the ReadModeFragment is the adapter.
 * we could probably abstract most of this away to make it easier to maintain
 */
public class ChunkModeFragment extends Fragment implements TargetTranslationDetailActivityListener {

    private RecyclerView mRecyclerView;
    private TargetTranslationDetailFragmentListener mListener;
    private boolean mFingerScroll = false;
    private LinearLayoutManager mLayoutManager;
    private ChunkAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stacked_card_list, container, false);

        Bundle args = getArguments();
        String targetTranslationId = args.getString(TargetTranslationDetailActivity.EXTRA_TARGET_TRANSLATION_ID, null);
        TargetTranslation targetTranslation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);
        if(targetTranslation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        // open first tab
        String[] sourceTranslationIds = AppContext.getTranslator().getSourceTranslations(targetTranslation.getId());
        String sourceTranslationId = null;
        if(sourceTranslationIds.length > 0) {
            SourceTranslation sourceTranslation = AppContext.getLibrary().getSourceTranslation(sourceTranslationIds[0]);
            if(sourceTranslation != null) {
                sourceTranslationId = sourceTranslation.getId();
            }
        }

        if(sourceTranslationId == null) {
            mListener.onNoSourceTranslations(targetTranslationId);
        } else {
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
            mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());

            mAdapter = new ChunkAdapter(this.getActivity(), targetTranslationId, sourceTranslationId);
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
        }

        return rootView;
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
}
