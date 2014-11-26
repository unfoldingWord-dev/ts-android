package com.door43.translationstudio.panes.left.tabs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Frame;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

/**
 * Created by joel on 8/29/2014.
 */
public class FrameItemAdapter extends BaseAdapter {

    private final MainApplication mContext;

    /**
     * Creates a new Frame adapter
     * @param c The application context
     */
    public FrameItemAdapter(MainApplication c) {
        mContext = c;
    }

    @Override
    public int getCount() {
        try {
            return mContext.getSharedProjectManager().getSelectedProject().getSelectedChapter().numFrames();
        } catch(Exception e) {
            return 0;
        }
    }

    @Override
    public Object getItem(int i) {
        return getFrameItem(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        RelativeLayout frameItemView;

        // if it's not recycled, initialize some attributes
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            frameItemView = (RelativeLayout)inflater.inflate(R.layout.fragment_pane_left_frames_item, null);
        } else {
            frameItemView = (RelativeLayout)view;
        }

        // image
        final ImageView frameIcon = (ImageView)frameItemView.findViewById(R.id.frameIcon);
        String imageUri = "assets://"+getFrameItem(i).getImagePath();
        mContext.getImageLoader().loadImage(imageUri, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                frameIcon.setImageBitmap(loadedImage);
            }
        });

        // title
        TextView frameTitle = (TextView)frameItemView.findViewById(R.id.frameId);
        frameTitle.setText(getFrameItem(i).getChapterFrameId());

        // description
        TextView frameDescription = (TextView)frameItemView.findViewById(R.id.frameDescription);
        frameDescription.setText(getFrameItem(i).getText());

        // translation in progress
        ImageView translationIcon = (ImageView)frameItemView.findViewById(R.id.translationStatusIcon);
        if(getFrameItem(i).isTranslating()) {
            translationIcon.setVisibility(View.VISIBLE);
        } else {
            translationIcon.setVisibility(View.GONE);
        }

        // highlight selected frame
        if(mContext.getSharedProjectManager().getSelectedProject().getSelectedChapter().getSelectedFrame().getChapterFrameId() == getFrameItem(i).getChapterFrameId()) {
            frameItemView.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
            frameDescription.setTextColor(Color.WHITE);
            frameTitle.setTextColor(Color.WHITE);
            translationIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_wrench));
        } else {
            frameItemView.setBackgroundColor(Color.TRANSPARENT);
            frameDescription.setTextColor(mContext.getResources().getColor(R.color.gray));
            frameTitle.setTextColor(mContext.getResources().getColor(R.color.black));
            translationIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_wrench_dark));
        }

        return frameItemView;
    }

    private Frame getFrameItem(int i) {
        return mContext.getSharedProjectManager().getSelectedProject().getSelectedChapter().getFrame(i);
    }
}
