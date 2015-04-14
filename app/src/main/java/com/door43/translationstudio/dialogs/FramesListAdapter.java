package com.door43.translationstudio.dialogs;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.door43.translationstudio.MainApplication;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.PseudoProject;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.util.AnimationUtilities;
import com.door43.translationstudio.util.AppContext;

import java.util.List;

/**
 * Created by jaicksninan on 4/10/15.
 */
public class FramesListAdapter  extends BaseAdapter {

    private MainApplication mContext;
    private int id;
    private List<String> items ;
    private  float mImageWidth;
    private  boolean mIndicateSelected;
    private  boolean mIndicateStatus;
    private Model[] mModels;
    private Activity aContext;


    public FramesListAdapter(MainApplication c, Model[] models) {
        mContext = c;
        mModels = models;
        mImageWidth = mContext.getResources().getDimension(R.dimen.model_list_item_image_width);
        mIndicateSelected = true;
        mIndicateStatus = true;
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


        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.dialogfragment, null);
            // layout
            holder.bodyLayout = (RelativeLayout)v.findViewById(R.id.layoutBody);
            holder.description = (TextView)v.findViewById(R.id.frameDescription);

            // alternate layout
            holder.altBodyLayout = (RelativeLayout)v.findViewById(R.id.layoutBodyAlt);
            holder.altDescription = (TextView)v.findViewById(R.id.frameDescriptionAlt);


            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.bodyLayout.clearAnimation();

        Frame f=AppContext.projectManager().getSelectedProject().getSelectedChapter().getFrame(position);

        holder.description.setText(new USXRenderer().render(f.getText()));
        holder.altDescription.setText(new USXRenderer().render(f.getText()));

        // set graphite fontface
        Typeface typeface;
        if(getItem(position).getSelectedSourceLanguage() != null) {
            typeface = AppContext.graphiteTypeface(getItem(position).getSelectedSourceLanguage());
        } else {
            // use english as default
            typeface = AppContext.graphiteTypeface(AppContext.projectManager().getLanguage("en"));
        }

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

        holder.description.setTextSize((fontsize));
        holder.altDescription.setTextSize((fontsize));

        // highlight selected item
        boolean isSelected = false;
        if(getItem(position).isSelected() && mIndicateSelected) {
            isSelected = true;
            v.setBackgroundColor(mContext.getResources().getColor(R.color.blue));

            holder.description.setTextColor(Color.WHITE);
            holder.altDescription.setTextColor(Color.WHITE);

        } else {
            v.setBackgroundColor(Color.TRANSPARENT);
            holder.description.setTextColor(mContext.getResources().getColor(R.color.black));
            holder.altDescription.setTextColor(mContext.getResources().getColor(R.color.black));

        }


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


    private static class ViewHolder {

        public RelativeLayout bodyLayout;
        public TextView description;
        public RelativeLayout altBodyLayout;
        public TextView altDescription;

    }
}
