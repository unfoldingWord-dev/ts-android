package com.door43.translationstudio.device2device;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.network.Peer;

import java.util.ArrayList;

/**
 * Created by joel on 12/11/2014.
 */
public class DevicePeerAdapter extends BaseAdapter {
    private final boolean mIsServer;
    private ArrayList<Peer> mPeerList;
    private final Context mContext;

    public DevicePeerAdapter(boolean isServer, Context context) {
        mPeerList = new ArrayList<>();
        mContext = context;
        mIsServer = isServer;
    }

    public DevicePeerAdapter(ArrayList<Peer> peerList, boolean isServer, Context context) {
        mPeerList = peerList;
        mContext = context;
        mIsServer = isServer;
    }

    /**
     * Updates the peer list
     * @param peerList
     */
    public void setPeerList(ArrayList<Peer> peerList) {
        mPeerList = peerList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mPeerList.size();
    }

    @Override
    public Peer getItem(int i) {
        return mPeerList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout v;
        Peer p = getItem(i);

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = (LinearLayout)inflater.inflate(R.layout.fragment_device_peer_list_item, null);
        } else {
            v = (LinearLayout)view;
        }

        // ip address
        TextView ipAddressView = (TextView)v.findViewById(R.id.ipAddressText);
        ipAddressView.setText(p.getIpAddress());

        // button
//        Button button = (Button)v.findViewById(R.id.button);
//        if(p.isConnected()) {
//            button.setText(R.string.import_project);
//            button.setBackgroundResource(R.color.blue);
//        } else {
//            button.setText(R.string.label_connect);
//            button.setBackgroundResource(R.color.gray);
//        }
        // TODO: handle click events on the button

        // instructions
        TextView instructionsText = (TextView)v.findViewById(R.id.instructionsText);
        if(p.isConnected()) {
            instructionsText.setText(R.string.connected);
        } else {
            instructionsText.setText(R.string.click_to_connect);
        }

        TextView controlTextView = (TextView)v.findViewById(R.id.controlTextView);
        controlTextView.setVisibility(View.GONE);

        // progress bar
        ProgressBar progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
        boolean isWaiting = p.keyStore.getBool(PeerStatusKeys.WAITING);
        int progress = p.keyStore.getInt(PeerStatusKeys.PROGRESS);
        progressBar.setIndeterminate(isWaiting);
        progressBar.setProgress(progress);
        if(!isWaiting && progress == 0) {
            progressBar.setVisibility(View.GONE);
            String controlText = p.keyStore.getString(PeerStatusKeys.CONTROL_TEXT);
            if(controlText != null && !controlText.isEmpty()) {
                controlTextView.setVisibility(View.VISIBLE);
                controlTextView.setText(controlText);
            }
        } else {
            progressBar.setVisibility(View.VISIBLE);
        }
        if(isWaiting) {
            instructionsText.setText(R.string.waiting_for_device);
        } else if(progress > 0) {
            instructionsText.setText(R.string.downloading);
        }

        return v;
    }
}
