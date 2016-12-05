package com.door43.translationstudio.ui.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Util;
import com.door43.translationstudio.tasks.GetAvailableSourcesTask;
import com.door43.widget.ViewUtil;

import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.tools.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


/**
 * Created by blm on 12/1/16.
 */

public class DownloadSourcesAdapter  extends BaseAdapter {

    public static final String TAG = DownloadSourcesAdapter.class.getSimpleName();
    public static final int TYPE_ITEM_FILTER_SELECTION = 0;
    public static final int TYPE_ITEM_SOURCE_SELECTION = 1;
    private final Context mContext;
    private List<String> mSelected = new ArrayList<>();
    private List<ViewItem> mItems = new ArrayList<>();
    private List<Translation> mAvailableSources;
    private Map<String,List<Integer>> mByLanguage;
    private Map<String,List<Integer>> mOtBooks;
    private Map<String,List<Integer>> mNtBooks;
    private Map<String,List<Integer>> mOtherBooks;

    private static int[] BookTypeNameList = { R.string.old_testament_label, R.string.new_testament_label, R.string.other_label};
    private static int[] BookTypeIconList = { R.drawable.ic_library_books_black_24dp, R.drawable.ic_library_books_black_24dp, R.drawable.ic_local_library_black_24dp };

    private SelectionType mSelectionType = SelectionType.language;
    private List<DownloadSourcesAdapter.FilterStep> mSteps;
    private String mLanguageFilter;
    private String mBookFilter;

    public DownloadSourcesAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    /**
     * Loads source lists from task results
a     * @param task
     */
    public void setData(GetAvailableSourcesTask task) {
        if(task != null) {
            mAvailableSources = task.getSources();
            Logger.i(TAG, "Found " + mAvailableSources.size() + " sources");

            mByLanguage = task.getByLanguage();
            mOtBooks = task.getOtBooks();
            mNtBooks = task.getNtBooks();
            mOtherBooks = task.getOther();
            initializeSelections();
        }
    }

    /**
     * loads the filter stages (e.g. filter by language, and then by category)
     * @param steps
     */
    public void setFilterSteps(List<DownloadSourcesAdapter.FilterStep> steps) {
        mSteps = steps;
        initializeSelections();
    }

    @Override
    public ViewItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        int type = TYPE_ITEM_FILTER_SELECTION;
        switch (mSelectionType) {
            case language:
            case newTestament:
            case oldTestament:
            case other:
                type = TYPE_ITEM_FILTER_SELECTION;
                break;

            case source_filtered_by_language:
            case source_filtered_by_book:
                type = TYPE_ITEM_SOURCE_SELECTION;
                break;
        }
        return type;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public List<String> getSelected() {
        return mSelected;
    }

    public List<ViewItem> getItems() {
        return mItems;
    }

    /**
     * Resorts the data
     */
    public void initializeSelections() {

        mSelected = new ArrayList<>(); // clear selections

        if((mSteps == null) // make sure we have data to sort
            || (mSteps.size() <= 0)
            || (mAvailableSources == null)
            || (mAvailableSources.size() <= 0) ){
            return;
        }

        mSelectionType = mSteps.get(mSteps.size()-1).selection;

        for (int i = 0; i < mSteps.size() - 1; i++) { // iterate through previous steps to extract filters
            FilterStep step = mSteps.get(i);
            switch (step.selection) {
                case language:
                    mLanguageFilter = step.filter;
                    break;
                case oldTestament:
                case newTestament:
                case other:
                case book_type:
                    mBookFilter = step.filter;
                    break;
            }
        }

        switch (mSelectionType) {
            case source_filtered_by_language:
                getSourcesForLanguageAndCategory();
                break;

            case source_filtered_by_book:
                getSourcesForBook();
                break;

            case oldTestament:
                getBooksInCategory(mOtBooks);
                break;

            case newTestament:
                getBooksInCategory(mNtBooks);
                break;

            case other:
                getBooksInCategory(mOtherBooks);
                break;

            case book_type:
                getCategories();
                break;

            case language:
            default:
                getLanguages();
                break;
        }
        notifyDataSetChanged();
    }

    /**
     * create list of languages available in sources
     */
    private void getLanguages() {
        mItems = new ArrayList<>();
        for (String key : mByLanguage.keySet()) {
            List<Integer> items = mByLanguage.get(key);
            if((items != null)  && (items.size() > 0)) {
                int index = items.get(0);
                if((index >= 0) && (index < mAvailableSources.size())) {
                    Translation sourceTranslation = mAvailableSources.get(index);
                    String title = sourceTranslation.language.name + "  (" + sourceTranslation.language.slug + ")";
                    ViewItem newItem = new ViewItem(title, sourceTranslation.language.slug, sourceTranslation, false, false);
                    mItems.add(newItem);
                }
            }
        }
    }

    /**
     * get list of categories (OT, NT, other).  If language has been selected, only
     *      return categories that contain the language.
     */
    private void getCategories() {
        mItems = new ArrayList<>();
        for(int i = 0; i < BookTypeNameList.length; i++) {
            Integer id = BookTypeNameList[i];
            if(mLanguageFilter != null) {
                boolean found = false;
                switch (id) {
                    case R.string.old_testament_label:
                        found = isLanguageInCategory(mByLanguage, mOtBooks);
                        break;
                    case R.string.new_testament_label:
                        found = isLanguageInCategory(mByLanguage, mNtBooks);
                        break;
                    default:
                    case R.string.other_label:
                        found = isLanguageInCategory(mByLanguage, mOtherBooks);
                        break;
                }
                if(!found) { // if category is not found, skip
                    continue;
                }
            }
            String title = mContext.getResources().getString(id);
            ViewItem newItem = new ViewItem(title, id.toString(), null, false, false);
            newItem.icon = BookTypeIconList[i];
            mItems.add(newItem);
        }
    }

    /**
     * check if category (OT, NT, other) contains the selected language
     * @param sortSet
     * @param category
     * @return
     */
    private boolean isLanguageInCategory(Map<String, List<Integer>> sortSet, Map<String, List<Integer>> category) {
        boolean found = false;
        if(sortSet.containsKey(mLanguageFilter)) {
            List<Integer> items = sortSet.get(mLanguageFilter);
            for (Integer index : items) {
                if ((index >= 0) && (index < mAvailableSources.size())) {
                    Translation sourceTranslation = mAvailableSources.get(index);
                    if (category.containsKey(sourceTranslation.project.slug)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }

    /**
     * create list of source selections that match book
     */
    private void getSourcesForBook() {
        List<Integer> sourceList = null;
        mItems = new ArrayList<>();

        // first get book list for selected book type
        if(mNtBooks.containsKey(mBookFilter)) {
            sourceList = mNtBooks.get(mBookFilter);
        }
        if(sourceList == null) {
            if(mOtBooks.containsKey(mBookFilter)) {
                sourceList = mOtBooks.get(mBookFilter);
            }
        }
        if(sourceList == null) {
            if(mOtherBooks.containsKey(mBookFilter)) {
                sourceList = mOtherBooks.get(mBookFilter);
            }
        }

        for (Integer index : sourceList) {
            if ((index >= 0) && (index < mAvailableSources.size())) {
                Translation sourceTranslation = mAvailableSources.get(index);

                String filter = sourceTranslation.resourceContainerSlug;
                String language = sourceTranslation.language.name + "  (" + sourceTranslation.language.slug + ")";
                String project = sourceTranslation.project.name + "  (" + sourceTranslation.project.slug + ")";

                ViewItem newItem = new ViewItem(language, project, filter, sourceTranslation, false, false);
                mItems.add(newItem);
            }
        }

        // sort by language code
        Collections.sort(mItems, new Comparator<ViewItem>() { // do numeric sort
            @Override
            public int compare(ViewItem lhs, ViewItem rhs) {
                return lhs.filter.compareTo(rhs.filter);
            }
        });
    }

    /**
     * create list of source selections that match language and category
     */
    private void getSourcesForLanguageAndCategory() {
        Map<String, List<Integer>> sortSet;
        mItems = new ArrayList<>();

        //get book list for category
        int category = Util.strToInt(mBookFilter,0);
        switch(category) {
            case R.string.old_testament_label:
                sortSet = mOtBooks;
                break;
            case R.string.new_testament_label:
                sortSet = mNtBooks;
                break;
            case R.string.other_label:
            default:
                sortSet = mOtherBooks;
                break;
        }

        for (String key : mByLanguage.keySet()) {
            if(mLanguageFilter != null) {
                if(!key.equals(mLanguageFilter)) { // skip over language if not matching filter
                    continue;
                }
            }
            List<Integer> items = mByLanguage.get(key);
            if((items != null)  && (items.size() > 0)) {
                for (Integer index : items) {
                    if ((index >= 0) && (index < mAvailableSources.size())) {
                        Translation sourceTranslation = mAvailableSources.get(index);

                        if(sortSet != null) {
                            if(!sortSet.containsKey(sourceTranslation.project.slug)) { // if not in right category then skip
                                continue;
                            }
                        }

                        String filter = sourceTranslation.resourceContainerSlug;
                        String project = sourceTranslation.project.name + "  (" + sourceTranslation.project.slug + ")";
                        String resource = sourceTranslation.resource.name + "  (" + sourceTranslation.language.slug + ")";

                        ViewItem newItem = new ViewItem(project, resource, filter, sourceTranslation, false, false);
                        mItems.add(newItem);
                    }
                }
            }
            if(mLanguageFilter != null) { // if filtering by specific language, then done
                break;
            }
        }

        if(sortSet != null) {
            List<ViewItem> unOrdered = mItems;
            mItems = new ArrayList<>();

            for (String book : sortSet.keySet() ) {
                for (int i = 0; i < unOrdered.size(); i++) {
                    ViewItem viewItem = unOrdered.get(i);
                    Translation sourceTranslation = viewItem.sourceTranslation;
                    if(book.equals(sourceTranslation.project.slug)) {
                        mItems.add(viewItem);
                        unOrdered.remove(viewItem);
                        i--;
                    }
                }
            }
        }
    }

    /**
     * create ordered list based on category
     * @param bookType
     */
    private void getBooksInCategory(Map<String, List<Integer>> bookType) {
        mItems = new ArrayList<>();
        for (String key : bookType.keySet()) {
            List<Integer> items = bookType.get(key);
            if((items != null)  && (items.size() > 0)) {
                int index = 0;
                Translation sourceTranslation = null;
                String title = null;
                String filter = null;
                for (int i = 0; i < items.size(); i++) {
                    index = items.get(i);
                    sourceTranslation = mAvailableSources.get(index);
                    filter = sourceTranslation.project.slug;
                    title = sourceTranslation.project.name + "  (" + filter + ")";
                    if(sourceTranslation.language.slug.equals("en")) {
                        break;
                    }
                }

                if(sourceTranslation != null) {
                    ViewItem newItem = new ViewItem(title, filter, sourceTranslation, false, false);
                    mItems.add(newItem);
                }
            }
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder = null;
        int rowType = getItemViewType(position);
        final ViewItem item = getItem(position);

        if(convertView == null) {
            holder = new ViewHolder();
            switch (rowType) {
                case TYPE_ITEM_FILTER_SELECTION:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_filter_item, null);
                    break;
                case TYPE_ITEM_SOURCE_SELECTION:
                default:
                    v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_select_download_source_item, null);
                    holder.titleView2 = (TextView)v.findViewById(R.id.title2);
                    break;
            }
            holder.titleView = (TextView)v.findViewById(R.id.title);
            holder.imageView = (ImageView) v.findViewById(R.id.item_icon);

            v.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.titleView.setText(item.title);

        if(holder.titleView2 != null) {
            holder.titleView2.setText((item.title2 != null) ? item.title2 : "");
        }

        if(rowType == TYPE_ITEM_SOURCE_SELECTION) {
            if (item.selected) {
                holder.imageView.setBackgroundResource(R.drawable.ic_check_box_black_24dp);
                ViewUtil.tintViewDrawable(holder.imageView, parent.getContext().getResources().getColor(R.color.accent));
                // display checked
            } else {
                holder.imageView.setBackgroundResource(R.drawable.ic_check_box_outline_blank_black_24dp);
                ViewUtil.tintViewDrawable(holder.imageView, parent.getContext().getResources().getColor(R.color.dark_primary_text));
                // display unchecked
            }
        } else {
            if(mSelectionType == SelectionType.book_type) {
                holder.imageView.setImageDrawable(mContext.getResources().getDrawable(item.icon));
            } else {
                holder.imageView.setVisibility(View.GONE);
            }
        }

        return v;
    }

    /**
     * toggle selection state for item
     * @param position
     */
    public void toggleSelection(int position) {
        if(getItem(position).selected) {
            deselect(position);
        } else {
            select(position);
        }
        notifyDataSetChanged();
    }

    public void select(int position) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.selected = true;
            mSelected.add(item.containerSlug);
        }
    }

    public void deselect(int position) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.selected = false;
            mSelected.remove(item.containerSlug);
        }
    }

    /**
     * marks an item as downloaded
     * @param position
     */
    public void markItemDownloaded(int position) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.downloaded = true;
            item.error = false;
        }
    }

    /**
     * marks an item as error
     * @param position
     */
    public void markItemError(int position) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.error = true;
        }
    }

    public static class ViewHolder {
        public TextView titleView;
        public TextView titleView2;
        public ImageView imageView;
        public Object currentTaskId;
        public int currentPosition;
    }

    public static class ViewItem {
        public final CharSequence title;
        public final CharSequence title2;
        public final String containerSlug;
        public final Translation sourceTranslation;
        public boolean selected;
        public boolean downloaded;
        public boolean error;
        public int icon;
        public String filter;

        // two text field version
        public ViewItem(CharSequence title, CharSequence title2, String filter, Translation sourceTranslation, boolean selected, boolean downloaded) {
            this.title = title;
            this.title2 = title2;
            this.selected = selected;
            this.sourceTranslation = sourceTranslation;
            if (sourceTranslation != null) {
                this.containerSlug = sourceTranslation.resourceContainerSlug;
            } else {
                this.containerSlug = null;
            }
            this.downloaded = downloaded;
            this.filter = filter;
            error = false;
            icon = 0;
        }

        // single text field version
        public ViewItem(CharSequence title, String filter, Translation sourceTranslation, boolean selected, boolean downloaded) {
            this(title, null, filter, sourceTranslation, selected, downloaded);
        }
    }

    public static class FilterStep {
        public final SelectionType selection;
        public String label;
        public String old_label;
        public String filter;

        public FilterStep(SelectionType selection, String label) {
            this.selection = selection;
            this.label = label;
            filter = null;
            old_label = null;
        }
    }

    /**
     * enum that keeps track of current state of USFM import
     */
    public enum SelectionType {
        language(0),
        oldTestament(1),
        newTestament(2),
        other(3),
        book_type(4),
        source_filtered_by_language(5),
        source_filtered_by_book(6);

        private int _value;

        SelectionType(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static SelectionType fromInt(int i) {
            for (SelectionType b : SelectionType.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }
}
