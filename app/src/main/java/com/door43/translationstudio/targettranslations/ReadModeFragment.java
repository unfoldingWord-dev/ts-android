package com.door43.translationstudio.targettranslations;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.util.AppContext;

import java.security.InvalidParameterException;

/**
 * Created by joel on 9/8/2015.
 */
public class ReadModeFragment extends Fragment implements TargetTranslationDetailMode {

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ReadAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_read_mode, container, false);

        Bundle args = getArguments();
        String targetTranslationId = args.getString(TargetTranslationDetailActivity.EXTRA_TARGET_TRANSLATION_ID, null);
        TargetTranslation translation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);
        if(translation == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        // TODO: retreive source translation id of selected tab
        String projectId = translation.getProjectId();
        SourceLanguage[] sourceLanguages = AppContext.getLibrary().getSourceLanguages(projectId);
        String sourceLanguageId = sourceLanguages[0].getId();
        Resource[] resources = AppContext.getLibrary().getResources(projectId, sourceLanguageId);
        String resourceId = resources[0].getId();
        SourceTranslation sourceTranslation = AppContext.getLibrary().getSourceTranslation(projectId, sourceLanguageId, resourceId);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ReadAdapter(this.getActivity(), targetTranslationId, sourceTranslation.getId());
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onScrollProgressUpdate(float scrollProgress) {
        float position = mAdapter.getItemCount() * scrollProgress;
        int scrollPosition = (int)Math.floor(position);
        if(scrollPosition <= 0) {
            scrollPosition = 1;
        }
        mRecyclerView.smoothScrollToPosition(scrollPosition);
    }
}
