package com.door43.translationstudio.dialogs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.util.AnimationUtilities;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.ThreadableUI;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.io.IOException;

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
        String imageUri;

        if(AppContext.assetExists(getItem(position).getImagePath())) {
            imageUri = "assets://"+ getItem(position).getImagePath();
        } else if(AppContext.assetExists(getItem(position).getDefaultImagePath())){
            imageUri = "assets://"+ getItem(position).getDefaultImagePath();
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
            holder.iconGroup.setVisibility(View.INVISIBLE);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.image.clearAnimation();
        holder.bodyLayout.clearAnimation();

        // set title and description
        holder.title.setText(getItem(position).getTitle());
        holder.altTitle.setText(getItem(position).getTitle());
        holder.description.setText(getItem(position).getDescription());
        holder.altDescription.setText(getItem(position).getDescription());

        // set graphite fontface
        Typeface typeface = AppContext.graphiteTypeface(getItem(position).getSelectedSourceLanguage());
        holder.title.setTypeface(typeface, 0);
        holder.altTitle.setTypeface(typeface, 0);
        holder.description.setTypeface(typeface, 0);
        holder.altDescription.setTypeface(typeface, 0);

        // use selected project language in pseudo project description fontface
        if(mIndicateSelected && getItem(position).getClass().getName().equals(PseudoProject.class.getName())) {
            if(getItem(position).isSelected() && AppContext.projectManager().getSelectedProject() != null) {
                holder.altDescription.setTypeface(AppContext.graphiteTypeface(AppContext.projectManager().getSelectedProject().getSelectedSourceLanguage()), 0);
            }
        }

        // set font size
        float fontsize = AppContext.typefaceSize();
        holder.title.setTextSize(fontsize);
        holder.altTitle.setTextSize(fontsize);
        holder.description.setTextSize((float)(fontsize*0.7));
        holder.altDescription.setTextSize((float)(fontsize*0.7));

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
        holder.iconGroup.setVisibility(View.INVISIBLE);

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
        holder.image.setVisibility(View.INVISIBLE);

        final ViewHolder staticHolder = holder;
        final Model staticModel = getItem(position);
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
                    AnimationUtilities.fadeIn(staticHolder.image, 100);
                }
            });
        }

        // enable icons
        if(holder.statusThread != null) {
            holder.statusThread.stop();
        }
        holder.statusThread = new ThreadableUI(mContext) {
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
                } else {
                    staticHolder.iconGroup.setVisibility(View.INVISIBLE);
                }
            }
        };
        holder.statusThread.start();

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

        public ThreadableUI statusThread;
    }
}
