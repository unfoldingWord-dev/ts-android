package com.door43.translationstudio.device2device;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.service.PeerStatusKeys;

import java.util.ArrayList;

/**
 * Created by joel on 12/11/2014.
 */
public class PeerAdapter extends BaseAdapter {
    private final boolean mIsServer;
    private ArrayList<Peer> mPeerList;
    private final Context mContext;

    public PeerAdapter(boolean isServer, Context context) {
        mPeerList = new ArrayList<>();
        mContext = context;
        mIsServer = isServer;
    }

    public PeerAdapter(ArrayList<Peer> peerList, boolean isServer, Context context) {
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
        TextView ipAddressView = (TextView)v.findViewById(R.id.ipAddressText);
        TextView instructionsText = (TextView)v.findViewById(R.id.instructionsText);
        Button browseButton = (Button)v.findViewById(R.id.browseButton);
        ProgressBar progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
        ImageView peerIcon = (ImageView)v.findViewById(R.id.peerIcon);

        // ip address
        ipAddressView.setText(p.getName());
        instructionsText.setText(p.getIpAddress());

        // button
//        Button button = (Button)v.findViewById(R.id.button);
//        if(p.isSecure()) {
//            button.setText(R.string.import_project);
//            button.setBackgroundResource(R.color.blue);
//        } else {
//            button.setText(R.string.label_connect);
//            button.setBackgroundResource(R.color.gray);
//        }
        // TODO: handle click events on the button

        // instructions

//        if(p.isSecure()) {
//            instructionsText.setText(R.string.connected);
            if(p.getDevice().equals("tablet")) {
                peerIcon.setBackgroundResource(R.drawable.ic_tablet_android_black_24dp);
            } else if(p.getDevice().equals("phone")) {
                peerIcon.setBackgroundResource(R.drawable.ic_phone_android_black_24dp);
            } else {
                peerIcon.setBackgroundResource(R.drawable.ic_devices_other_black_24dp);
            }
//        } else {
//            if(mIsServer) {
//                peerIcon.setBackgroundResource(R.drawable.icon_update_nearby_dark);
//            } else {
//                peerIcon.setBackgroundResource(R.drawable.icon_library_dark);
//            }
//            instructionsText.setText(R.string.click_to_connect);
//        }

        browseButton.setVisibility(View.GONE);

        // progress bar

        boolean isWaiting = p.keyStore.getBool(PeerStatusKeys.WAITING);
        int progress = p.keyStore.getInt(PeerStatusKeys.PROGRESS);
        progressBar.setIndeterminate(isWaiting);
        progressBar.setProgress(progress);
        if(!isWaiting && progress == 0) {
            progressBar.setVisibility(View.GONE);
            peerIcon.setVisibility(View.VISIBLE);
            String controlText = p.keyStore.getString(PeerStatusKeys.CONTROL_TEXT);
            if(controlText != null && !controlText.isEmpty()) {
                browseButton.setVisibility(View.VISIBLE);
                browseButton.setText(controlText);
            } else {
                browseButton.setText(R.string.browse);
            }
        } else {
            progressBar.setVisibility(View.VISIBLE);
            peerIcon.setVisibility(View.GONE);
        }
        if(isWaiting) {
            instructionsText.setText(R.string.waiting_for_device);
        } else if(progress > 0) {
            instructionsText.setText(R.string.downloading);
        }

        return v;
    }
}
