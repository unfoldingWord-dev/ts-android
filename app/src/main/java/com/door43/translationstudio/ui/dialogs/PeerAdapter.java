package com.door43.translationstudio.ui.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.services.PeerStatusKeys;
import com.door43.translationstudio.services.Request;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

import java.util.ArrayList;

/**
 * Created by joel on 12/11/2014.
 */
public class PeerAdapter extends BaseAdapter {
    private ArrayList<Peer> peers;
    private final Context mContext;
    private boolean[] animateNotification;

    public PeerAdapter(Context context) {
        peers = new ArrayList<>();
        mContext = context;
    }

    /**
     * Updates the peer list
     * @param peerList
     */
    public void setPeers(ArrayList<Peer> peerList) {
        peers = peerList;
        animateNotification = new boolean[peerList.size()];
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return peers.size();
    }

    @Override
    public Peer getItem(int i) {
        return peers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        LinearLayout v;
        Peer peer = getItem(position);

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = (LinearLayout)inflater.inflate(R.layout.fragment_device_peer_list_item, null);
        } else {
            v = (LinearLayout)view;
        }
        TextView deviceNameView = (TextView)v.findViewById(R.id.ipAddressText);
        TextView ipAddressView = (TextView)v.findViewById(R.id.ip_address);
        ImageView favoriteIcon = (ImageView)v.findViewById(R.id.favorite);
        final ImageView notificationIcon = (ImageView)v.findViewById(R.id.notification);
        ProgressBar progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
        ImageView deviceIcon = (ImageView)v.findViewById(R.id.peerIcon);

        // name
        deviceNameView.setText(peer.getName());
        ipAddressView.setText(peer.getIpAddress());

        // device type
        if(peer.getDevice().equals("tablet")) {
            deviceIcon.setBackgroundResource(R.drawable.ic_tablet_android_black_24dp);
        } else if(peer.getDevice().equals("phone")) {
            deviceIcon.setBackgroundResource(R.drawable.ic_phone_android_black_24dp);
        } else {
            deviceIcon.setBackgroundResource(R.drawable.ic_devices_other_black_24dp);
        }

        // progress bar
        boolean isWaiting = peer.keyStore.getBool(PeerStatusKeys.WAITING);
        int progress = peer.keyStore.getInt(PeerStatusKeys.PROGRESS);
        progressBar.setIndeterminate(isWaiting);
        progressBar.setProgress(progress);
        if(!isWaiting && progress == 0) {
            progressBar.setVisibility(View.GONE);
            deviceIcon.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            deviceIcon.setVisibility(View.GONE);
        }

        // requests
        favoriteIcon.setVisibility(View.GONE);
        if(peer.getRequests().length > 0) {
            notificationIcon.setVisibility(View.VISIBLE);
        } else {
            notificationIcon.setVisibility(View.GONE);
        }

        // animate notification
        if(animateNotification[position]) {
            animateNotification[position] = false;
            SpringSystem springSystem = SpringSystem.create();
            Spring spring = springSystem.createSpring();
            SpringConfig springConfig = spring.getSpringConfig();
            springConfig.friction = 10;
            springConfig.tension = 440;
            spring.addListener(new SimpleSpringListener() {
                @Override
                public void onSpringUpdate(Spring spring) {
                    float value = (float) spring.getCurrentValue();
                    float scale = 1f - (value * 0.5f);
                    notificationIcon.setScaleX(scale);
                    notificationIcon.setScaleY(scale);
                }
            });
            spring.setCurrentValue(-1, true);
            spring.setEndValue(0);
        }

        return v;
    }

    /**
     * Indicates that a request from the peer is awaiting approval
     * @param peer
     * @param request
     */
    public void newRequestAlert(Peer peer, Request request) {
        // schedule an animation for the notification icon
        int index = peers.indexOf(peer);
        if(index >= 0 && index < animateNotification.length) {
            animateNotification[index] = true;
        }
        notifyDataSetChanged();
    }
}
