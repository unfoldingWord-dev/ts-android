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
import com.door43.translationstudio.util.AnimationUtilities;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

/**
 * Created by joel on 8/29/2014.
 */
public class FrameItemAdapter extends BaseAdapter {

    private final MainApplication mContext;
    private final float mImageWidth;

    /**
     * Creates a new Frame adapter
     * @param c The application context
     */
    public FrameItemAdapter(MainApplication c) {
        mContext = c;
        mImageWidth = mContext.getResources().getDimension(R.dimen.list_item_image_width);
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
    public Frame getItem(int i) {
        return mContext.getSharedProjectManager().getSelectedProject().getSelectedChapter().getFrame(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        View v = convertView;
        ViewHolder holder = new ViewHolder();
        Frame f = getItem(position);
        String imageUri = "assets://"+ f.getImagePath();

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_pane_left_frames_item, null);
            holder.icon = (ImageView)v.findViewById(R.id.frameIcon);
            holder.bodyLayout = (LinearLayout)v.findViewById(R.id.bodyLayout);
            holder.title = (TextView)v.findViewById(R.id.frameId);
            holder.description = (TextView)v.findViewById(R.id.frameDescription);
            holder.translationIcon = (ImageView)v.findViewById(R.id.translationStatusIcon);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.icon.clearAnimation();
        holder.bodyLayout.clearAnimation();

        holder.title.setText(f.getId());
        holder.description.setText(f.getText());

        // translation in progress
        if(f.isTranslating()) {
            holder.translationIcon.setVisibility(View.VISIBLE);
        } else {
            holder.translationIcon.setVisibility(View.GONE);
        }

        // highlight the selected frame
        if(mContext.getSharedProjectManager().getSelectedProject().getSelectedChapter().getSelectedFrame().getId() == f.getId()) {
            v.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
            holder.description.setTextColor(Color.WHITE);
            holder.title.setTextColor(Color.WHITE);
            holder.translationIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_pencil));
        } else {
            v.setBackgroundColor(Color.TRANSPARENT);
            holder.description.setTextColor(mContext.getResources().getColor(R.color.gray));
            holder.title.setTextColor(mContext.getResources().getColor(R.color.black));
            holder.translationIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_pencil_dark));
        }

        // image
        holder.icon.setVisibility(View.GONE);
        ViewGroup.LayoutParams params = holder.bodyLayout.getLayoutParams();
        params.width = parent.getWidth();
        holder.bodyLayout.setLayoutParams(params);

        final ViewHolder staticHolder = holder;

        // load image
        mContext.getImageLoader().loadImage(imageUri, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                // load values
                staticHolder.icon.setImageBitmap(loadedImage);
                staticHolder.icon.setVisibility(View.VISIBLE);

                // animate views
                AnimationUtilities.slideInLeft(staticHolder.icon);
                AnimationUtilities.resizeWidth(staticHolder.bodyLayout, parent.getWidth(), parent.getWidth() - mImageWidth);
            }
        });

        return v;
    }

    /**
     * Improves performance
     */
    private static class ViewHolder {
        public ImageView icon;
        public LinearLayout bodyLayout;
        public TextView title;
        public TextView description;
        public ImageView translationIcon;
    }
}
