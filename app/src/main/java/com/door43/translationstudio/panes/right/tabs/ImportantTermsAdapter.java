package com.door43.translationstudio.panes.right.tabs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;

import java.util.ArrayList;

/**
 * Created by joel on 2/17/2015.
 */
public class ImportantTermsAdapter extends BaseAdapter {

    private final Context mContext;
    private ArrayList<String> mTerms;

    public ImportantTermsAdapter(Context context, ArrayList<String> terms) {
        mContext = context;
        mTerms = terms;
    }

    /**
     * Update the terms list
     * @param terms
     */
    public void setTermsList(ArrayList<String> terms) {
        mTerms = terms;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mTerms.size();
    }

    @Override
    public String getItem(int i) {
        return mTerms.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout v;
        String term = getItem(i);

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = (LinearLayout)inflater.inflate(R.layout.fragment_pane_right_term_item, null);
        } else {
            v = (LinearLayout)view;
        }

        TextView titleText = (TextView)v.findViewById(R.id.keyTermTitleText);
        titleText.setText(term);

        return v;
    }
}
