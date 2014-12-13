package com.door43.translationstudio.device2device;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
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
        LinearLayout peerItemView;

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            peerItemView = (LinearLayout)inflater.inflate(R.layout.fragment_device_peer_list_item, null);
        } else {
            peerItemView = (LinearLayout)view;
        }

        // ip address
        TextView ipAddressView = (TextView)peerItemView.findViewById(R.id.ipAddressText);
        ipAddressView.setText(getItem(i).getIpAddress());

        TextView instructionsText = (TextView)peerItemView.findViewById(R.id.instructionsText);
        if(mIsServer) {
            instructionsText.setText(R.string.click_to_share);
        } else {
            instructionsText.setText(R.string.click_to_import);
        }

        return peerItemView;
    }
}
