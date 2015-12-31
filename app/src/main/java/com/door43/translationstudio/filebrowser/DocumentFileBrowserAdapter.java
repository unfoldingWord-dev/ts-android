package com.door43.translationstudio.filebrowser;

import android.content.Context;
import android.support.v4.provider.DocumentFile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ArchiveDetails;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.Util;
import com.door43.util.tasks.ThreadableUI;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Handles the rendering of the file browser activity
 */
public class DocumentFileBrowserAdapter extends BaseAdapter {

    private List<DocumentFileItem> mFiles = new ArrayList<>();

    public void loadFiles(ListView listView, List<DocumentFileItem> files) {
        final Library library = AppContext.getLibrary();
        mFiles = files;
        new ThreadableUI(listView.getContext()) {

            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                for(DocumentFileItem item:mFiles) {
                    if(item.isTranslationArchive()) {
                        item.inspect(Locale.getDefault().getLanguage(), library);
                    }
                }
            }

            @Override
            public void onPostExecute() {
                sortFiles(mFiles);
                notifyDataSetChanged();
            }
        }.start();
    }

    @Override
    public int getCount() {
        return mFiles.size();
    }

    @Override
    public DocumentFileItem getItem(int i) {
        return mFiles.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        if(convertView == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_file_item, null);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        holder.icon.setImageResource(getItem(position).getIconResource());
        if(getItem(position).isBackupsDir()) {
            holder.title.setText(parent.getContext().getResources().getString(R.string.automatic_backups));
        } else {
            holder.title.setText(getItem(position).getTitle());
        }
        holder.archiveDetails.setVisibility(View.GONE);
        holder.title.setTextColor(parent.getContext().getResources().getColor(R.color.dark_secondary_text));
        if(holder.archiveDetails.getChildCount() > 0) {
            holder.archiveDetails.removeAllViews();
        }

        if(getItem(position).isTranslationArchive() && getItem(position).getArchiveDetails() != null) {
            ArchiveDetails details = getItem(position).getArchiveDetails();
            holder.archiveDetails.setVisibility(View.VISIBLE);
            DateFormat format = DateFormat.getDateTimeInstance();
            Date date = Util.dateFromUnixTime(details.createdAt);
            holder.title.setText(format.format(date));
            // display complex archive info
            for (ArchiveDetails.TargetTranslationDetails td : details.targetTranslationDetails) {
                TextView targetTranslationTextView = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_file_sub_item, null);
                targetTranslationTextView.setText(td.projectName + " - " + td.targetLanguageName);
                holder.archiveDetails.addView(targetTranslationTextView);
            }
        }

        if(getItem(position).isBackupsDir()) {
            holder.title.setTextColor(parent.getContext().getResources().getColor(R.color.dark_primary_text));
        }

        return v;
    }

    private static class ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final LinearLayout archiveDetails;
//        public ThreadableUI inspectThread;

        public ViewHolder(View v) {
            this.title = (TextView)v.findViewById(R.id.title);
            this.icon = (ImageView)v.findViewById(R.id.icon);
            this.archiveDetails = (LinearLayout)v.findViewById(R.id.archive_details);
            v.setTag(this);
        }
    }

    /**
     * Sorts target languages by id
     * @param files
     */
    private static void sortFiles(List<DocumentFileItem> files) {
        Collections.sort(files, new Comparator<DocumentFileItem>() {
            @Override
            public int compare(DocumentFileItem lhs, DocumentFileItem rhs) {
                int sort = 0;
                if (lhs.isUpButton || rhs.isUpButton) {
                    // up button is always first
                    sort = lhs.isUpButton ? -1 : 1;
                } else if (lhs.isBackupsDir()) {
                    // backup dir is after archives
                    if (rhs.isTranslationArchive()) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (rhs.isBackupsDir()) {
                    // backup dir is after archives
                    if (lhs.isTranslationArchive()) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else if (lhs.isTranslationArchive() && rhs.isTranslationArchive()) {
                    // sort by date (if the archive has been inspected)
                    if (lhs.getArchiveDetails() != null && rhs.getArchiveDetails() != null) {
                        long lhsCreated = lhs.getArchiveDetails().createdAt;
                        long rhsCreated = rhs.getArchiveDetails().createdAt;
                        if (lhsCreated > rhsCreated) {
                            sort = -1;
                        } else if (lhsCreated < rhsCreated) {
                            sort = 1;
                        }
                    }
                } else if (!lhs.isTranslationArchive() && !rhs.isTranslationArchive()) {
                    // sort by name
                    sort = lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                } else {
                    // archives are before regular files
                    sort = lhs.isTranslationArchive() ? -1 : 1;
                }
                return sort;
            }
        });
    }
}

