package com.door43.translationstudio.library;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.SourceLanguage;

import javax.xml.transform.Source;

/**
 * This adpater handles the display of source languages in the server library
 */
public class LibraryLanguageAdapter extends BaseAdapter {
    private final Context mContext;
    private SourceLanguage[] mLanguages = new SourceLanguage[]{};

    public LibraryLanguageAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mLanguages.length;
    }

    @Override
    public SourceLanguage getItem(int i) {
        return mLanguages[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = view;
        ViewHolder holder = new ViewHolder();

        if(view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_project_library_languages_item, null);
            // layout
            holder.name = (TextView)v.findViewById(R.id.languageNameTextView);

            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.name.setText(getItem(i).getName());
        // TODO: indicate if the language has been downloaded yet.

        return v;
    }

    /**
     * Changes the dataset
     * @param languages
     */
    public void changeDataSet(SourceLanguage[] languages) {
        mLanguages = languages;
        notifyDataSetChanged();
    }

    private class ViewHolder {

        public TextView name;
    }
}
