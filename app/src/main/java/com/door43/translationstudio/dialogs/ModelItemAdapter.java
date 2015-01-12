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
import android.widget.TextView;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.util.AnimationUtilities;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

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
        String imageUri = "assets://"+ m.getImagePath();

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_model_list_item, null);
            holder.icon = (ImageView)v.findViewById(R.id.modelImage);
            holder.bodyLayout = (LinearLayout)v.findViewById(R.id.bodyLayout);
            holder.title = (TextView)v.findViewById(R.id.modelTitle);
            holder.description = (TextView)v.findViewById(R.id.modelDescription);
            holder.translationIcon = (ImageView)v.findViewById(R.id.translationStatusIcon);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.icon.clearAnimation();
        holder.bodyLayout.clearAnimation();

        holder.title.setText(m.getTitle());
        holder.description.setText(m.getDescription());

        // translation in progress
        boolean isTranslating = m.isTranslating();
        boolean isTranslatingGlobal = m.isTranslatingGlobal();
        if(isTranslating || isTranslatingGlobal) {
            holder.translationIcon.setVisibility(View.VISIBLE);
        } else {
            holder.translationIcon.setVisibility(View.GONE);
        }

        // highlight selected project
        if(getItem(position).isSelected() && mIndicateSelected) {
            v.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
            holder.description.setTextColor(Color.WHITE);
            holder.title.setTextColor(Color.WHITE);
            if(isTranslating) {
                holder.translationIcon.setBackgroundResource(R.drawable.ic_pencil);
//                holder.translationIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_pencil));
            } else {
                holder.translationIcon.setBackgroundResource(R.drawable.ic_translation_small);
//                holder.translationIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_translation_small));
            }
        } else {
            v.setBackgroundColor(Color.TRANSPARENT);
            holder.description.setTextColor(mContext.getResources().getColor(R.color.gray));
            holder.title.setTextColor(mContext.getResources().getColor(R.color.black));
            if(isTranslating) {
                holder.translationIcon.setBackgroundResource(R.drawable.ic_pencil_dark);
//                holder.translationIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_pencil_dark));
            } else {
                holder.translationIcon.setBackgroundResource(R.drawable.ic_translation_small_dark);
//                holder.translationIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_translation_small_dark));
            }
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
        public ImageView icon;
        public LinearLayout bodyLayout;
        public TextView title;
        public TextView description;
        public ImageView translationIcon;
        public ImageView translationGlobalIcon;
    }
}
