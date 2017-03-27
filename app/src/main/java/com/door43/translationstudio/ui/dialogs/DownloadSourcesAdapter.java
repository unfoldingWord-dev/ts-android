package com.door43.translationstudio.ui.dialogs;

import android.content.Context;
import android.support.v7.app.AlertDialog;
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

import org.json.JSONObject;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.tools.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
    private List<String> mDownloaded = new ArrayList<>();
    private List<ViewItem> mItems = new ArrayList<>();
    private List<Translation> mAvailableSources;
    private Map<String,List<Integer>> mByLanguage;
    private Map<String,List<Integer>> mOtBooks;
    private Map<String,List<Integer>> mNtBooks;
    private Map<String,List<Integer>> mTaBooks;
    private Map<String,List<Integer>> mOtherBooks;

    // 02/20/2017 - for now we are disabling updating of TA since a major change coming up could break the app
    private static int[] BookTypeNameList = { R.string.old_testament_label, R.string.new_testament_label, R.string.other_label}; // removed R.string.ta_label to disable updating TA
    private static int[] BookTypeIconList = { R.drawable.ic_library_books_black_24dp, R.drawable.ic_library_books_black_24dp, R.drawable.ic_local_library_black_24dp };

    private SelectionType mSelectionType = SelectionType.language;
    private List<DownloadSourcesAdapter.FilterStep> mSteps;
    private String mLanguageFilter;
    private String mBookFilter;
    private String mSearch = null;
    private Map<String, String> mDownloadErrors = new HashMap<>();

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
            mTaBooks = task.getTaBooks();
            mSelected = new ArrayList<>(); // clear selections
            initializeSelections();
        }
    }

    /**
     * loads the filter stages (e.g. filter by language, and then by category)
     * @param steps
     * @param search - string to search for
     * @param restore - if true then don't reset selection list
     */
    public void setFilterSteps(List<DownloadSourcesAdapter.FilterStep> steps, String search, boolean restore) {
        mSteps = steps;
        mSearch = search;
        if(!restore) {
            mSelected = new ArrayList<>(); // clear selections
        }
        initializeSelections();
    }

    /**
     * loads the filter stages (e.g. filter by language, and then by category)
     * @param search - string to search for
     */
    public void setSearch(String search) {
        mSearch = search;
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
            case other_book:
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

    public List<String> getDownloaded() {
        return mDownloaded;
    }

    public void setSelected(List<String> mSelected) {
        this.mSelected = mSelected;
    }

    public void setDownloaded(List<String> mDownloaded) {
        this.mDownloaded = mDownloaded;
    }

    public JSONObject getDownloadErrorMessages() {
        return new JSONObject(mDownloadErrors);
    }

    public void setDownloadErrorMessages(String jsonDownloadErrorMessagesStr) {
        mDownloadErrors.clear();
        try {
            JSONObject jsonMessages = new JSONObject(jsonDownloadErrorMessagesStr);
            Iterator<?> keyset = jsonMessages.keys();
            while (keyset.hasNext()) {
                String key = (String) keyset.next();
                Object value = jsonMessages.get(key);
                if(value != null) {
                    mDownloadErrors.put(key, value.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SelectedState getSelectedState() {
        boolean allSelected = true;
        boolean noneSelected = true;
        for (ViewItem item : mItems) {
            if(!item.downloaded) { // ignore items already downloaded
                if (item.selected) {
                    noneSelected = false;
                } else {
                    allSelected = false;
                }
            }
        }

        if(noneSelected) {
            return SelectedState.none;
        } else if(allSelected) {
            return SelectedState.all;
        }
        return SelectedState.not_empty;
    }

    public List<ViewItem> getItems() {
        return mItems;
    }

    /**
     * Resorts the data
     */
    public void initializeSelections() {

        mBookFilter = null; // clear filters
        mLanguageFilter = null;

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
                case other_book:
                case translationAcademy:
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
                getBooksInCategory(mOtBooks, false);
                break;

            case newTestament:
                getBooksInCategory(mNtBooks, false);
                break;

            case translationAcademy:
                getBooksInCategory(mTaBooks, true);
                break;

            case other_book:
                getBooksInCategory(mOtherBooks, true);
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
        if((mSearch != null) && !mSearch.isEmpty()) {
            List<ViewItem> filteredItems = new ArrayList<>();

            // filter by language code
            for (ViewItem item : mItems) {
                String code = item.sourceTranslation.language.slug;
                if(code.length() >= mSearch.length()) {
                    if (code.substring(0, mSearch.length()).equalsIgnoreCase(mSearch)) {
                        filteredItems.add(item);
                    }
                }
            }

            // filter by language name
            for (ViewItem item : mItems) {
                String name = item.sourceTranslation.language.name;
                if(name.length() >= mSearch.length()) {
                    if (name.substring(0, mSearch.length()).equalsIgnoreCase(mSearch)) {
                        if (!filteredItems.contains(item)) { // prevent duplicates
                            filteredItems.add(item);
                        }
                    }
                }
            }

            mItems = filteredItems;
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
                    case R.string.ta_label:
                        found = isLanguageInCategory(mByLanguage, mTaBooks);
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
            if(mTaBooks.containsKey(mBookFilter)) {
                sourceList = mTaBooks.get(mBookFilter);
            }
        }
        if(sourceList == null) {
            if(mOtherBooks.containsKey(mBookFilter)) {
                sourceList = mOtherBooks.get(mBookFilter);
            }
        }

        if(sourceList == null) {
            return;
        }

        for (Integer index : sourceList) {
            if ((index >= 0) && (index < mAvailableSources.size())) {
                Translation source = mAvailableSources.get(index);
                String filter = source.resourceContainerSlug;
                String language = source.language.name + "  (" + source.language.slug + ")";
                String project = source.resource.name + "  (" + source.resource.slug + ")";
                addNewViewItem(language, project, filter, source);
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
     * create new view item, apply previous state info, and add to list
     * @param title1
     * @param title2
     * @param filter
     * @param source
     */
    private void addNewViewItem(String title1, String title2, String filter, Translation source) {
        ViewItem newItem = new ViewItem(title1, title2, filter, source, false, false);

        if(mSelected.contains(newItem.containerSlug)) {
            newItem.selected = true;
        }
        if(mDownloaded.contains(newItem.containerSlug)) {
            newItem.downloaded = true;
        }
        if(mDownloadErrors.containsKey(newItem.containerSlug)) {
            newItem.error = true;
            newItem.errorMessage = mDownloadErrors.get(newItem.containerSlug);
        }
        mItems.add(newItem);
    }

    /**
     * gets the selection type based on filter
     * @param categoryFilter
     * @return
     */
    public SelectionType getCategoryForFilter(String categoryFilter) {
        int bookTypeSelected = Util.strToInt(categoryFilter, R.string.other_label);
        switch (bookTypeSelected) {
            case R.string.old_testament_label:
                return SelectionType.oldTestament;
            case R.string.new_testament_label:
                return  SelectionType.newTestament;
            case R.string.ta_label:
                return SelectionType.translationAcademy;
            default:
            case R.string.other_label:
                break;
        }
        return SelectionType.other_book;
    }

    /**
     * create list of source selections that match language and category
     */
    private void getSourcesForLanguageAndCategory() {
        Map<String, List<Integer>> sortSet;
        mItems = new ArrayList<>();

        //get book list for category
        SelectionType category = getCategoryForFilter(mBookFilter);
        switch(category) {
            case oldTestament:
                sortSet = mOtBooks;
                break;
            case newTestament:
                sortSet = mNtBooks;
                break;
            case translationAcademy:
                sortSet = mTaBooks;
                break;
            case other_book:
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
                        Translation source = mAvailableSources.get(index);

                        if(sortSet != null) {
                            if(!sortSet.containsKey(source.project.slug)) { // if not in right category then skip
                                continue;
                            }
                        }

                        String filter = source.resourceContainerSlug;
                        String project = source.project.name + "  (" + source.project.slug + ")";
                        String resource = source.resource.name + "  (" + source.resource.slug + ")";
                        addNewViewItem(project, resource, filter, source);
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
     * create ordered list based on category, optionally sort
     * @param bookType
     * @param sort
     */
    private void getBooksInCategory(Map<String, List<Integer>> bookType, boolean sort) {
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

        if(sort) {
            Collections.sort(mItems, new Comparator<ViewItem>() { // do numeric sort
                @Override
                public int compare(ViewItem lhs, ViewItem rhs) {
                    return lhs.title.toString().compareTo(rhs.title.toString());
                }
            });
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
                    holder.errorView = (ImageView) v.findViewById(R.id.error_icon);
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
            holder.errorView.setVisibility(item.error ? View.VISIBLE : View.GONE);
            if(item.error) {
                holder.errorView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                                .setTitle(R.string.download_failed)
                                .setMessage(R.string.check_network_connection)
                                .setPositiveButton(R.string.label_close, null);
//                                .show();
                        if(item.errorMessage != null) {
                            builder.setMessage(item.errorMessage);
                        }
                        builder.show();
                    }
                });
            }
            if(item.downloaded) { // display with a green check
                holder.imageView.setBackgroundResource(R.drawable.ic_done_black_24dp);
                ViewUtil.tintViewDrawable(holder.imageView, parent.getContext().getResources().getColor(R.color.green));
            } else if (item.selected) { // display checked box
                holder.imageView.setBackgroundResource(R.drawable.ic_check_box_black_24dp);
                ViewUtil.tintViewDrawable(holder.imageView, parent.getContext().getResources().getColor(R.color.accent));
            } else { // display unchecked box
                holder.imageView.setBackgroundResource(R.drawable.ic_check_box_outline_blank_black_24dp);
                ViewUtil.tintViewDrawable(holder.imageView, parent.getContext().getResources().getColor(R.color.dark_primary_text));
            }
        } else {
            if(mSelectionType == SelectionType.book_type) {
                holder.imageView.setImageDrawable(mContext.getResources().getDrawable(item.icon));
                holder.imageView.setVisibility(View.VISIBLE);
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
            if(!item.downloaded) {
                item.selected = true;
                if (!mSelected.contains(item.containerSlug)) { // make sure we don't add entry twice (particularly during select all)
                    mSelected.add(item.containerSlug);
                }
            }
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
     * search items for position that matches slug
     * @param slug
     * @return
     */
    public int findPosition(String slug) {
        for (int i = 0; i < mItems.size(); i++) {
            ViewItem item = mItems.get(i);
            if(item.containerSlug.equals(slug)) {
                return i;
            }
        }
        return -1;
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
            deselect(position);

            if(!mDownloaded.contains(item.containerSlug)) {
                mDownloaded.add(item.containerSlug);
            }
            mDownloadErrors.remove(item.containerSlug);
        }
    }

    /**
     * marks an item as error
     * @param position
     */
    public void markItemError(int position, String message) {
        ViewItem item = getItem(position);
        if(item != null) {
            item.error = true;
            item.errorMessage = message;

            mDownloadErrors.put(item.containerSlug, message);
            mDownloaded.remove(item.containerSlug);
        }
    }

    /**
     * used to force selection of all or none of the items
     * @param selectAll
     * @param selectNone
     */
    public void forceSelection(boolean selectAll, boolean selectNone) {
        if(selectAll) {
            for (int i = 0; i < mItems.size(); i++) {
                select(i);
            }
        }
        if(selectNone) {
            for (int i = 0; i < mItems.size(); i++) {
                deselect(i);
            }
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder {
        public TextView titleView;
        public TextView titleView2;
        public ImageView imageView;
        public ImageView errorView;
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
        public String errorMessage;

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

        private FilterStep(SelectionType selection, String label, String filter, String old_label) {
            this.selection = selection;
            this.label = label;
            this.filter = filter;
            this.old_label = old_label;
        }

        public JSONObject toJson() {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.putOpt("selection", selection.getValue());
                jsonObject.putOpt("label", label);
                jsonObject.putOpt("old_label", old_label);
                jsonObject.putOpt("filter", filter);

                return jsonObject;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        static FilterStep generate(JSONObject jsonObject) {
            try {
                SelectionType selection = SelectionType.fromInt((int) getOpt(jsonObject,"selection"));
                String label = (String) getOpt(jsonObject,"label");
                String old_label = (String) getOpt(jsonObject,"old_label");
                String filter = (String) getOpt(jsonObject,"filter");
                return new FilterStep( selection, label, filter, old_label);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        static Object getOpt(JSONObject json, String key) {
            try {
                if(json.has(key)) {
                    return json.get(key);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * enum that keeps track of current state of USFM import
     */
    public enum SelectionType {
        language(0),
        oldTestament(1),
        newTestament(2),
        translationAcademy(3),
        other_book(4),
        book_type(5),
        source_filtered_by_language(6),
        source_filtered_by_book(7);

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

    public enum SelectedState {
        all,
        none,
        not_empty;
    }
}
