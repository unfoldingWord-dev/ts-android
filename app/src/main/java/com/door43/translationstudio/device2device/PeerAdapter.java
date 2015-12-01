package com.door43.translationstudio.device2device;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.service.PeerNotice;
import com.door43.translationstudio.service.PeerStatusKeys;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joel on 12/11/2014.
 */
public class PeerAdapter extends BaseAdapter {
    private final boolean mIsServer;
    private ArrayList<Peer> mPeerList;
    private final Context mContext;
    private PeerNotice[] noticies;
    private Map<String, List<PeerNotice>> peerNoticeMap = new HashMap<>();
    private boolean[] animateNotification;

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
    public void setPeers(ArrayList<Peer> peerList) {
        mPeerList = peerList;
        animateNotification = new boolean[peerList.size()];
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
    public View getView(int position, View view, ViewGroup viewGroup) {
        LinearLayout v;
        Peer p = getItem(position);

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

        // ip address
        deviceNameView.setText(p.getName());
        ipAddressView.setText(p.getIpAddress());


        if(p.getDevice().equals("tablet")) {
            deviceIcon.setBackgroundResource(R.drawable.ic_tablet_android_black_24dp);
        } else if(p.getDevice().equals("phone")) {
            deviceIcon.setBackgroundResource(R.drawable.ic_phone_android_black_24dp);
        } else {
            deviceIcon.setBackgroundResource(R.drawable.ic_devices_other_black_24dp);
        }

        // progress bar

        boolean isWaiting = p.keyStore.getBool(PeerStatusKeys.WAITING);
        int progress = p.keyStore.getInt(PeerStatusKeys.PROGRESS);
        progressBar.setIndeterminate(isWaiting);
        progressBar.setProgress(progress);
        if(!isWaiting && progress == 0) {
            progressBar.setVisibility(View.GONE);
            deviceIcon.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            deviceIcon.setVisibility(View.GONE);
        }
        if(isWaiting) {
            ipAddressView.setText(R.string.waiting_for_device);
        } else if(progress > 0) {
            ipAddressView.setText(R.string.downloading);
        }

        // noticies
        favoriteIcon.setVisibility(View.GONE);
        if(peerNoticeMap.containsKey(p.getId())) {
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
////            Animation pulse = AnimationUtils.loadAnimation(mContext, R.anim.pulse);
//            AnimationSet pulse = new AnimationSet(true);
//            pulse.setInterpolator(new AnticipateOvershootInterpolator(2.0f));
//            pulse.setDuration(500);
//            pulse.addAnimation(new ScaleAnimation(.5f, 1f, .5f, 1f, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f));
//            notificationIcon.startAnimation(pulse);
        }

        return v;
    }

    /**
     * Adds a single peer notice
     * @param notice
     */
    public void addNotice(PeerNotice notice) {
        // TODO: 12/1/2015 we need to animate the view item
        if(!peerNoticeMap.containsKey(notice.peer.getId())) {
            peerNoticeMap.put(notice.peer.getId(), new ArrayList<PeerNotice>());
        }
        peerNoticeMap.get(notice.peer.getId()).add(notice);

        // schedual an animation for the notification icon
        int index = mPeerList.indexOf(notice.peer);
        if(index >= 0 && index < animateNotification.length) {
            animateNotification[index]=true;
        }
        notifyDataSetChanged();
    }

    /**
     * Sets the noticies to be displayed
     * @param noticies
     */
    public void setNoticies(PeerNotice[] noticies) {
        // build map of notices for each peer
        peerNoticeMap.clear();
        for(PeerNotice notice:noticies) {
            if(!peerNoticeMap.containsKey(notice.peer.getId())) {
                peerNoticeMap.put(notice.peer.getId(), new ArrayList<PeerNotice>());
            }
            peerNoticeMap.get(notice.peer.getId()).add(notice);
        }
        notifyDataSetChanged();
    }
}
