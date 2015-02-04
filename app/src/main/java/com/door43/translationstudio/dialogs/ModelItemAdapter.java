package com.door43.translationstudio.dialogs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.util.AnimationUtilities;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.ThreadableUI;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;

/**
 * Created by joel on 8/29/2014.
 */
public class ModelItemAdapter extends BaseAdapter {

    private final MainApplication mContext;
    private final float mImageWidth;
    private final boolean mIndicateSelected;
    private Model[] mModels;

    /**
     *
     * @param c
     * @param models
     */
    public ModelItemAdapter(MainApplication c, Model[] models) {
        mContext = c;
        mModels = models;
        mImageWidth = mContext.getResources().getDimension(R.dimen.model_list_item_image_width);
        mIndicateSelected = true;
    }

    /**
     *
     * @param c
     * @param models
     * @param indicatSelection if true the adapter will highlight models that are selected.
     */
    public ModelItemAdapter(MainApplication c, Model[] models, boolean indicatSelection) {
        mContext = c;
        mModels = models;
        mImageWidth = mContext.getResources().getDimension(R.dimen.model_list_item_image_width);
        mIndicateSelected = indicatSelection;
    }

    @Override
    public int getCount() {
        return mModels.length;
    }

    @Override
    public Model getItem(int i) {
        return mModels[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        View v = convertView;
        ViewHolder holder = new ViewHolder();
        Model m = getItem(position);
        String imageUri;
        File img = MainContext.getContext().getAssetAsFile(m.getImagePath());
        File defaultImg = MainContext.getContext().getAssetAsFile(m.getDefaultImagePath());
        if(img != null && img.exists() && img.isFile()) {
            imageUri = "assets://"+ m.getImagePath();
        } else if(defaultImg != null && defaultImg.exists() && defaultImg.isFile()){
            imageUri = "assets://"+ m.getDefaultImagePath();
        } else {
            imageUri = null;
        }

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_model_list_item, null);
            // layout
            holder.bodyLayout = (RelativeLayout)v.findViewById(R.id.bodyLayout);
            holder.image = (ImageView)v.findViewById(R.id.modelImage);
            holder.title = (TextView)v.findViewById(R.id.modelTitle);
            holder.description = (TextView)v.findViewById(R.id.modelDescription);

            // alternate layout
            holder.altBodyLayout = (RelativeLayout)v.findViewById(R.id.bodyLayoutAlt);
            holder.altTitle = (TextView)v.findViewById(R.id.modelTitleAlt);
            holder.altDescription = (TextView)v.findViewById(R.id.modelDescriptionAlt);

            // icons
            holder.audioIcon = (ImageView)v.findViewById(R.id.audioIcon);
            holder.languagesIcon = (ImageView)v.findViewById(R.id.languagesIcon);
            holder.translationIcon = (ImageView)v.findViewById(R.id.translationIcon);
            holder.iconGroup = (LinearLayout)v.findViewById(R.id.iconGroupLayout);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.image.clearAnimation();
        holder.bodyLayout.clearAnimation();

        holder.title.setText(m.getTitle());
        holder.altTitle.setText(m.getTitle());
        holder.description.setText(m.getDescription());
        holder.altDescription.setText(m.getDescription());

        // display the correct layout
        if(imageUri != null) {
            // default layout
            holder.bodyLayout.setVisibility(View.VISIBLE);
            holder.altBodyLayout.setVisibility(View.GONE);
        } else {
            // alternate layout
            holder.bodyLayout.setVisibility(View.GONE);
            holder.altBodyLayout.setVisibility(View.VISIBLE);
        }

        // icons
        holder.translationIcon.setBackgroundResource(R.drawable.ic_project_status_blank);
        holder.languagesIcon.setBackgroundResource(R.drawable.ic_project_status_blank);
        holder.audioIcon.setBackgroundResource(R.drawable.ic_project_status_blank);
        holder.iconGroup.setVisibility(View.GONE);

        // highlight selected item
        boolean isSelected = false;
        if(getItem(position).isSelected() && mIndicateSelected) {
            isSelected = true;
            v.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
            holder.title.setTextColor(Color.WHITE);
            holder.altTitle.setTextColor(Color.WHITE);
            holder.description.setTextColor(Color.WHITE);
            holder.altDescription.setTextColor(Color.WHITE);
            holder.iconGroup.setBackgroundColor(mContext.getResources().getColor(R.color.medium_blue));
        } else {
            v.setBackgroundColor(Color.TRANSPARENT);
            holder.title.setTextColor(mContext.getResources().getColor(R.color.dark_gray));
            holder.altTitle.setTextColor(mContext.getResources().getColor(R.color.dark_gray));
            holder.description.setTextColor(mContext.getResources().getColor(R.color.gray));
            holder.altDescription.setTextColor(mContext.getResources().getColor(R.color.gray));
            holder.iconGroup.setBackgroundColor(mContext.getResources().getColor(R.color.lighter_gray));
        }

        // prepare image
        holder.image.setVisibility(View.GONE);
        ViewGroup.LayoutParams params = holder.bodyLayout.getLayoutParams();
        params.width = parent.getWidth();
        holder.bodyLayout.setLayoutParams(params);

        final ViewHolder staticHolder = holder;
        final Model staticModel = m;
        final boolean staticIsSelected = isSelected;


        // load image
        if(imageUri != null) {
            mContext.getImageLoader().loadImage(imageUri, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    // load values
                    staticHolder.image.setImageBitmap(loadedImage);
                    staticHolder.image.setVisibility(View.VISIBLE);

                    // animate views
                    AnimationUtilities.slideInLeft(staticHolder.image);
                    AnimationUtilities.resizeWidth(staticHolder.bodyLayout, parent.getWidth(), parent.getWidth() - mImageWidth);
                }
            });
        }

        // enable icons
        new ThreadableUI(mContext) {
            private boolean isTranslating;
            private boolean isTranslatingGlobal;
            private boolean hasAudio;

            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                isTranslating = staticModel.isTranslating();
                isTranslatingGlobal = staticModel.isTranslatingGlobal();
                hasAudio = false;
            }

            @Override
            public void onPostExecute() {
                if(staticIsSelected) {
                    if(isTranslating) staticHolder.translationIcon.setBackgroundResource(R.drawable.ic_project_status_translating);
                    if(isTranslatingGlobal) staticHolder.languagesIcon.setBackgroundResource(R.drawable.ic_project_status_global);
                    if(hasAudio) staticHolder.audioIcon.setBackgroundResource(R.drawable.ic_project_status_audio);
                } else {
                    if(isTranslating) staticHolder.translationIcon.setBackgroundResource(R.drawable.ic_project_status_translating_light);
                    if(isTranslatingGlobal) staticHolder.languagesIcon.setBackgroundResource(R.drawable.ic_project_status_global_light);
                    if(hasAudio) staticHolder.audioIcon.setBackgroundResource(R.drawable.ic_project_status_audio_light);
                }
                if(isTranslating || isTranslatingGlobal || hasAudio) {
                    staticHolder.iconGroup.setVisibility(View.VISIBLE);
                    AnimationUtilities.fadeIn(staticHolder.iconGroup, 100);
                }
            }
        }.start();

        return v;
    }

    /**
     * Changes the dataset
     * @param models
     */
    public void changeDataSet(Model[] models) {
        mModels = models;
        notifyDataSetChanged();
    }

    /**
     * Improves performance
     */
    private static class ViewHolder {
        public ImageView image;
        public RelativeLayout bodyLayout;
        public TextView title;
        public TextView description;

        public RelativeLayout altBodyLayout;
        public TextView altTitle;
        public TextView altDescription;

        public ImageView translationIcon;
        public ImageView languagesIcon;
        public ImageView audioIcon;
        public LinearLayout iconGroup;
    }
}
