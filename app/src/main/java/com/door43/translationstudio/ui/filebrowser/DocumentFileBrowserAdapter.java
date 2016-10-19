package com.door43.translationstudio.ui.filebrowser;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.ArchiveDetails;
import com.door43.translationstudio.core.Util;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.tools.taskmanager.ThreadableUI;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Handles the rendering of the file browser activity
 */
public class DocumentFileBrowserAdapter extends BaseAdapter {

    private List<DocumentFileItem> mFiles = new ArrayList<>();
    private int mSelectedPosition = -1;

    public void loadFiles(Context context, List<DocumentFileItem> files) {
        final Door43Client library = App.getLibrary();
        mFiles = files;
        new ThreadableUI(context) {

            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                for(DocumentFileItem item:mFiles) {
                    if(item.isTranslationArchive()) {
                        item.inspect(App.getDeviceLanguageCode(), library);
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

        if(mSelectedPosition == position) {
            v.setBackgroundColor(parent.getContext().getResources().getColor(R.color.accent_light));
        } else {
            Drawable currentBackground = v.getBackground();
            if(currentBackground != null) {
                v.setBackgroundDrawable(null); // clear background
            }
        }

        DocumentFileItem item = getItem(position);
        holder.icon.setImageResource(item.getIconResource());
        boolean isBackupsDir = item.isBackupsDir();
        if(isBackupsDir) {
            holder.title.setText(parent.getContext().getResources().getString(R.string.automatic_backups));
        } else {
            holder.title.setText(item.getTitle());
        }
        holder.archiveDetails.setVisibility(View.GONE);
        holder.title.setTextColor(parent.getContext().getResources().getColor(R.color.dark_secondary_text));
        if(holder.archiveDetails.getChildCount() > 0) {
            holder.archiveDetails.removeAllViews();
        }

        if(item.isTranslationArchive() && item.getArchiveDetails() != null) {
            ArchiveDetails details = item.getArchiveDetails();
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

        if(isBackupsDir) {
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

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public void setSelectedPosition(int mSelectedPosition) {
        this.mSelectedPosition = mSelectedPosition;
    }
}

