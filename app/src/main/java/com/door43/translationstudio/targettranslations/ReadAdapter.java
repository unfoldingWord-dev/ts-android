package com.door43.translationstudio.targettranslations;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.door43.translationstudio.R;

/**
 * Created by joel on 9/9/2015.
 */
public class ReadAdapter extends RecyclerView.Adapter<ReadAdapter.ViewHolder> {

    private final String[] mDataset;

    public ReadAdapter(String[] dataset) {
        mDataset = dataset;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_read_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mBody.setText(mDataset[position]);
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitle;
        public TextView mBody;
        public ViewHolder(View v) {
            super(v);
            mTitle = (TextView)v.findViewById(R.id.source_translation_title);
            mBody = (TextView)v.findViewById(R.id.source_translation_body);
        }
    }
}
