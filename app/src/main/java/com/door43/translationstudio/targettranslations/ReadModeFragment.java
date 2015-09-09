package com.door43.translationstudio.targettranslations;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;

/**
 * Created by joel on 9/8/2015.
 */
public class ReadModeFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ReadAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_read_mode, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        String[] dataset = new String[6];
        dataset[0] = "this is the first card";
        dataset[1] = "this is the second card";
        dataset[2] = "this is the third card";
        dataset[3] = "this is the forth card";
        dataset[4] = "this is the fifth card";
        dataset[5] = "this is the sixth card";
        mAdapter = new ReadAdapter(dataset);
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }
}
