package com.door43.translationstudio.ui.dialogs;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.tasks.DownloadResourceContainersTask;
import com.door43.translationstudio.tasks.GetAvailableSourcesTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by blm on 12/1/16.
 */

public class DownloadSourcesDialog extends DialogFragment implements ManagedTask.OnFinishedListener, ManagedTask.OnProgressListener {
    public static final String TAG = DownloadSourcesDialog.class.getSimpleName();
    public static final String STATE_SEARCH_STRING = "state_search_string";
    private static final String TASK_DOWNLOAD_SOURCES = "download-sources";
    public static final String STATE_FILTER_STEPS = "state_filter_steps";
    public static final String STATE_BY_LANGUAGE_FLAG = "state_by_language_flag";
    public static final String STATE_SELECTED_LIST = "state_selected_list";
    public static final String STATE_DOWNLOADED_LIST = "state_downloaded_list";
    public static final String STATE_DOWNLOADED_ERRORS_LIST = "state_downloaded_errors_list";
    private Door43Client mLibrary;
    private ProgressDialog mProgressDialog = null;
    private DownloadSourcesAdapter mAdapter;
    private List<DownloadSourcesAdapter.FilterStep> mSteps;
    private View v;
    private LinearLayout mSelectionBar;
    private CheckBox mSelectAllButton;
    private CheckBox mUnSelectAllButton;
    private Button mDownloadButton;
    private ImageView mSearchIcon;
    private EditText mSearchText;
    private String mSearchString;
    private LinearLayout mSearchTextBorder;
    private RadioButton mByLanguageButton;
    private RadioButton mByBookButton;
    private List<String> mSelected;
    private TextWatcher searchTextWatcher;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        v = inflater.inflate(R.layout.dialog_download_sources, container, false);

        mLibrary = App.getLibrary();
        mSteps = new ArrayList<>();

        ManagedTask task = new GetAvailableSourcesTask();
        ((GetAvailableSourcesTask)task).setPrefix(this.getResources().getString(R.string.loading_sources));
        task.addOnProgressListener(this);
        task.addOnFinishedListener(this);
        TaskManager.addTask(task, GetAvailableSourcesTask.TASK_ID);

        ImageButton backButton = (ImageButton) v.findViewById(R.id.search_back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSteps.size() > 1) {
                    removeLastStep();
                    setFilter();
                } else {
                    dismiss();
                }
            }
        });

        mSelectionBar = (LinearLayout) v.findViewById(R.id.selection_bar);

        mSelectAllButton = (CheckBox) v.findViewById(R.id.select_all);
        mSelectAllButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    if(mAdapter != null) {
                        mAdapter.forceSelection( true, false);
                        onSelectionChanged();
                    }
                }
            }
        });
        mUnSelectAllButton = (CheckBox) v.findViewById(R.id.unselect_all);
        mUnSelectAllButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    if(mAdapter != null) {
                        mAdapter.forceSelection( false, true);
                        onSelectionChanged();
                    }
                }
            }
        });
        mDownloadButton = (Button) v.findViewById(R.id.download_button);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAdapter != null) {
                    List<String> selected = mAdapter.getSelected();
                    if((selected != null) && (selected.size() > 0)) {
                        DownloadResourceContainersTask task = new DownloadResourceContainersTask(selected);
                        task.addOnFinishedListener(DownloadSourcesDialog.this);
                        task.addOnProgressListener(DownloadSourcesDialog.this);
                        TaskManager.addTask(task, TASK_DOWNLOAD_SOURCES);
                    }
                }
            }
        });

        mSearchIcon = (ImageView) v.findViewById(R.id.search_mag_icon);
        mSearchText = (EditText) v.findViewById(R.id.search_text);
        mSearchTextBorder = (LinearLayout) v.findViewById(R.id.search_text_border);
        mSearchString = null;

        mAdapter = new DownloadSourcesAdapter(getActivity());

        ListView listView = (ListView) v.findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                if((mAdapter != null) && (mSteps != null) && (mSteps.size() > 0)) {
                    mSearchString = null;
                    DownloadSourcesAdapter.FilterStep currentStep = mSteps.get(mSteps.size() - 1);
                    DownloadSourcesAdapter.ViewItem item = mAdapter.getItem(position);
                    currentStep.old_label = currentStep.label;
                    currentStep.label = item.title.toString();
                    currentStep.filter = item.filter;

                    if(mSteps.size() < 2) { // if we haven't set up last step
                        switch (currentStep.selection) {
                            default:
                            case language:
                                addStep(DownloadSourcesAdapter.SelectionType.book_type, R.string.choose_category);
                                break;

                            case oldTestament:
                            case newTestament:
                            case translationAcademy:
                            case other_book:
                                addStep(DownloadSourcesAdapter.SelectionType.language, R.string.choose_language);
                                break;

                            case book_type:
                                DownloadSourcesAdapter.SelectionType category = mAdapter.getCategoryForFilter(currentStep.filter);
                                switch (category) {
                                    case oldTestament:
                                        addStep(DownloadSourcesAdapter.SelectionType.oldTestament, R.string.choose_book);
                                        break;
                                    case newTestament:
                                        addStep(DownloadSourcesAdapter.SelectionType.newTestament, R.string.choose_book);
                                        break;
                                    case translationAcademy:
                                        addStep(DownloadSourcesAdapter.SelectionType.translationAcademy, R.string.choose_book);
                                        break;
                                    default:
                                    case other_book:
                                        addStep(DownloadSourcesAdapter.SelectionType.other_book, R.string.choose_book);
                                        break;
                                }
                                break;
                        }
                    } else if(mSteps.size() < 3) { // set up last step
                        DownloadSourcesAdapter.FilterStep firstStep = mSteps.get(0);
                        switch (firstStep.selection) {
                            case language:
                                addStep(DownloadSourcesAdapter.SelectionType.source_filtered_by_language, R.string.choose_sources);
                                break;
                            default:
                                addStep(DownloadSourcesAdapter.SelectionType.source_filtered_by_book, R.string.choose_sources);
                                break;
                        }
                    } else { // at last step, do toggling
                        mAdapter.toggleSelection(position);
                        DownloadSourcesAdapter.SelectedState selectedState = mAdapter.getSelectedState();
                        switch (selectedState) {
                            case all:
                                mSelectAllButton.setChecked(true);
                                break;

                            case none:
                                mUnSelectAllButton.setChecked(true);
                                break;

                            default:
                                onSelectionChanged();
                                break;
                        }
                        return;
                    }
                    setFilter();
                }
            }
        });

        mByLanguageButton = (RadioButton) v.findViewById(R.id.byLanguage);
        mByBookButton = (RadioButton) v.findViewById(R.id.byBook);

        if(savedInstanceState != null) {
            String stepsArrayJson = savedInstanceState.getString(STATE_FILTER_STEPS, null);
            try {
                JSONArray stepsArray = new JSONArray(stepsArrayJson);
                for (int i = 0; i < stepsArray.length(); i++) {
                    JSONObject jsonObject = (JSONObject) stepsArray.get(i);
                    DownloadSourcesAdapter.FilterStep step = DownloadSourcesAdapter.FilterStep.generate(jsonObject);
                    mSteps.add(step);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSearchString = savedInstanceState.getString(STATE_SEARCH_STRING, null);
            boolean byLanguage = savedInstanceState.getBoolean(STATE_BY_LANGUAGE_FLAG, true);
            if(byLanguage) {
                mByLanguageButton.setChecked(true);
            } else {
                mByBookButton.setChecked(true);
            }

            mSelected = savedInstanceState.getStringArrayList(STATE_SELECTED_LIST);
            mAdapter.setSelected(mSelected);
            List<String> downloaded = savedInstanceState.getStringArrayList(STATE_DOWNLOADED_LIST);
            mAdapter.setDownloaded(downloaded);
            List<String> downloadError = savedInstanceState.getStringArrayList(STATE_DOWNLOADED_ERRORS_LIST);
            mAdapter.setDownloadError(downloadError);
            setFilter();
        }

        mByLanguageButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    mSearchString = null;
                    mSteps = new ArrayList<>(); // clear existing filter and start over
                    addStep(DownloadSourcesAdapter.SelectionType.language, R.string.choose_language);
                    setFilter();
                }
            }
        });
        mByBookButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    mSteps = new ArrayList<>(); // clear existing filter and start over
                    addStep(DownloadSourcesAdapter.SelectionType.book_type, R.string.choose_category);
                    setFilter();
                }
            }
        });

        if(savedInstanceState == null) {
            mByLanguageButton.setChecked(true); // setup initial state
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onStart();

        // widen dialog to accommodate more text
        int height = getResources().getDisplayMetrics().heightPixels;
        int width = getResources().getDisplayMetrics().widthPixels;
        float screenWidthFactor = 0.5f; // landscape mode
        if(height > width) { // if portrait mode
            screenWidthFactor = 0.85f;
        }
        getDialog().getWindow().setLayout((int) (width * screenWidthFactor), WindowManager.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {

        JSONArray stepsArray = new JSONArray();
        for (DownloadSourcesAdapter.FilterStep step : mSteps) {
            JSONObject jsonObject = step.toJson();
            stepsArray.put(jsonObject);
        }
        out.putString(STATE_FILTER_STEPS, stepsArray.toString());
        out.putString(STATE_SEARCH_STRING, mSearchString);
        out.putBoolean(STATE_BY_LANGUAGE_FLAG, mByLanguageButton.isChecked());
        if(mAdapter != null) {
            out.putStringArrayList(STATE_SELECTED_LIST, (ArrayList) mAdapter.getSelected());
            out.putStringArrayList(STATE_DOWNLOADED_LIST, (ArrayList) mAdapter.getDownloaded());
            out.putStringArrayList(STATE_DOWNLOADED_ERRORS_LIST, (ArrayList) mAdapter.getDownloadError());
        }

        super.onSaveInstanceState(out);
    }

    /**
     * update controls for selection state
     */
    public void onSelectionChanged() {
        if(mAdapter != null) {
            DownloadSourcesAdapter.SelectedState selectedState = mAdapter.getSelectedState();
            boolean allSelected = (selectedState == DownloadSourcesAdapter.SelectedState.all);
            mSelectAllButton.setEnabled(!allSelected);
            if(!allSelected) {
                mSelectAllButton.setChecked(false);
            }
            boolean noneSelected = (selectedState == DownloadSourcesAdapter.SelectedState.none);
            mUnSelectAllButton.setEnabled(!noneSelected);
            if(!noneSelected) {
                mUnSelectAllButton.setChecked(false);
            }

            boolean downloadSelect = !noneSelected;
            mDownloadButton.setEnabled(downloadSelect);
            int backgroundColor = downloadSelect ? R.color.accent : R.color.light_gray;
            mDownloadButton.setBackgroundColor(getResources().getColor(backgroundColor));
        }
    }

    /**
     * remove the last step from stack
     */
    private void removeLastStep() {
        DownloadSourcesAdapter.FilterStep lastStep = mSteps.get(mSteps.size() - 1);
        mSteps.remove(lastStep);
        lastStep = mSteps.get(mSteps.size() - 1);
        lastStep.filter = null;
        lastStep.label = lastStep.old_label;
    }

    /**
     * display the nav label and show choices to user
     *
     */
    private void setFilter() {
        for (int step = 0; step < 3; step++) {
            setNavBarStep(step);
        }

        // setup selection bar
        boolean selectDownloads = (mSteps.size() == 3);
        mSelectionBar.setVisibility(selectDownloads ? View.VISIBLE : View.GONE);
        mAdapter.setFilterSteps(mSteps, mSearchString);
        if(selectDownloads) {
            onSelectionChanged();
        }

        //set up nav/search bar
        boolean enable_language_search = (mSteps.size() == 1)
                                        && (mSteps.get(0).selection == DownloadSourcesAdapter.SelectionType.language);
        if(enable_language_search) {
            setupLanguageSearch();
        } else {
            mSearchIcon.setVisibility(View.GONE);
            showNavbar(true);
        }

        mSearchTextBorder.setVisibility(View.GONE);
        mSearchText.setVisibility(View.GONE);
        mSearchText.setEnabled(false);
        if(searchTextWatcher != null) {
            mSearchText.removeTextChangedListener(searchTextWatcher);
            searchTextWatcher = null;
        }

        TextView nav1 = getTextView(1);
        nav1.setOnClickListener(null);
    }

    /**
     * setup UI for doing language search
     */
    private void setupLanguageSearch() {
        mSearchIcon.setVisibility(View.VISIBLE);
        mSearchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNavbar(false);
                mSearchTextBorder.setVisibility(View.VISIBLE);
                mSearchText.setVisibility(View.VISIBLE);
                mSearchText.setEnabled(true);
                mSearchText.requestFocus();
                App.showKeyboard(getActivity(), mSearchText, false);
                mSearchText.setText("");

                if(searchTextWatcher != null) {
                    mSearchText.removeTextChangedListener(searchTextWatcher);
                }

                searchTextWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (mAdapter != null) {
                            mSearchString = s.toString();
                            mAdapter.setSearch(mSearchString);
                        }
                    }
                };
                mSearchText.addTextChangedListener(searchTextWatcher);
            }
        });
    }

    /**
     * set text at nav bar position
     * @param stepIndex
     */
    private void setNavBarStep(int stepIndex) {
        CharSequence navText = getTextForStep(stepIndex);

        int viewPosition = stepIndex * 2 + 1;

        setNavPosition(navText, viewPosition);

        CharSequence sep = null;
        if (navText != null) { // if we have something at this position, then add separator
            sep = ">";
        }
        setNavPosition(sep, viewPosition - 1);
    }

    /**
     * get text for position
     * @param stepIndex
     * @return
     */
    private CharSequence getTextForStep(int stepIndex) {
        CharSequence navText = null;
        boolean enable = stepIndex < mSteps.size();
        if (enable) {
            navText = null;

            DownloadSourcesAdapter.FilterStep step = mSteps.get(stepIndex);

            SpannableStringBuilder span = new SpannableStringBuilder(step.label);
            boolean lastItem = (stepIndex >= (mSteps.size() - 1));
            if (!lastItem) {

                // insert a clickable span
                span.setSpan(new SpannableStringBuilder(step.label), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                final int resetToStep = stepIndex;
                ClickableSpan clickSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Logger.i(TAG, "clicked on item: " + resetToStep);
                        while (mSteps.size() > resetToStep + 1) {
                            removeLastStep();
                        }
                        setFilter();
                    }
                };
                span.setSpan(clickSpan, 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            navText = span;
        }
        return navText;
    }

    /**
     * set text at position, or hide view if text is null
     * @param text
     * @param position
     */
    private void setNavPosition(CharSequence text, int position) {
        TextView view = getTextView( position);
        if(view != null) {
            if(text != null) {
                view.setText(text);
                view.setVisibility(View.VISIBLE);
                view.setMovementMethod(LinkMovementMethod.getInstance()); // enable clicking on TextView
            } else {
                view.setText("");
                view.setVisibility(View.GONE);
            }
        }
    }

    /**
     * show/hide navbar items
     * @param enable
     */
    private void showNavbar(boolean enable) {
        int visibility = enable ? View.VISIBLE : View.GONE;
        for (int i = 1; i <= 5; i++) {
            TextView v = getTextView(i);
            if(v != null) {
                v.setVisibility(visibility);
            }
        }
    }
    /**
     * find text view for position
     * @param position
     * @return
     */
    private TextView getTextView(int position) {

        int resID = 0;
        switch (position) {
            case 1:
                resID = R.id.nav_text1;
                break;

            case 2:
                resID = R.id.nav_text2;
                break;

            case 3:
                resID = R.id.nav_text3;
                break;

            case 4:
                resID = R.id.nav_text4;
                break;

            case 5:
                resID = R.id.nav_text5;
                break;

            default:
                return null;
        }
        TextView searchView = (TextView) v.findViewById(resID);
        return searchView;
    }

    /**
     * add step to sequence
     * @param selection
     * @param prompt
     */
    private void addStep(DownloadSourcesAdapter.SelectionType selection, int prompt) {
        String promptStr = getResources().getString(prompt);
        DownloadSourcesAdapter.FilterStep step = new DownloadSourcesAdapter.FilterStep(selection, promptStr);
        mSteps.add(step);
    }

    @Override
    public void onTaskFinished(final ManagedTask task) {
        TaskManager.clearTask(task);

        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                if(task instanceof GetAvailableSourcesTask) {
                    GetAvailableSourcesTask availableSourcesTask = (GetAvailableSourcesTask) task;
                    mAdapter.setData(availableSourcesTask);
                    if(mSelected != null) {
                        mAdapter.setSelected(mSelected);
                        mAdapter.initializeSelections();
                        onSelectionChanged();
                    }

                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                } else if(task instanceof DownloadResourceContainersTask) {
                    DownloadResourceContainersTask downloadSourcesTask = (DownloadResourceContainersTask) task;
                    List<ResourceContainer> downloadedContainers = downloadSourcesTask.getDownloadedContainers();

                    List<DownloadSourcesAdapter.ViewItem> items = mAdapter.getItems();

                    for (ResourceContainer container : downloadedContainers) {
                        Logger.i(TAG, "Received: " + container.slug);

                        int pos = mAdapter.findPosition(container.slug);
                        if(pos >= 0) {
                            mAdapter.markItemDownloaded(pos);
                        }
                    }

                    List<Translation> failed = downloadSourcesTask.getFailedDownloads();
                    for (Translation translation : failed) {
                        Logger.e(TAG, "Download failed: " + translation.resourceContainerSlug);
                        int pos = mAdapter.findPosition(translation.resourceContainerSlug);
                        if(pos >= 0) {
                            mAdapter.markItemError(pos);
                        }
                    }

                    boolean canceled = downloadSourcesTask.isCanceled();
                    String downloads = getActivity().getResources().getString(R.string.downloads_success,downloadedContainers.size());
                    String errors = "";
                    if((failed.size() > 0) && !canceled) {
                        errors = "\n" + getActivity().getResources().getString(R.string.downloads_fail, failed.size());
                    }

                    mAdapter.notifyDataSetChanged();
                    onSelectionChanged();

                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }

                    int title = canceled ? R.string.download_cancelled : R.string.download_complete;

                    new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                            .setTitle(title)
                            .setMessage(downloads + errors)
                            .setPositiveButton(R.string.label_close, null)
                            .show();
                }
            }
        });
    }

    @Override
    public void onTaskProgress(final ManagedTask task, final double progress, final String message, boolean secondary) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                // init dialog
                if(mProgressDialog == null) {
                    mProgressDialog = new ProgressDialog(getActivity());
                    mProgressDialog.setCancelable(true);
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    mProgressDialog.setOnCancelListener(DownloadSourcesDialog.this);
                    mProgressDialog.setIcon(R.drawable.ic_cloud_download_black_24dp);
                    mProgressDialog.setTitle(R.string.updating);
                    mProgressDialog.setMessage("");

                    mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            TaskManager.cancelTask(task);
                        }
                    });
                }

                // dismiss if finished or cancelled
                if(task.isFinished() || task.isCanceled()) {
                    mProgressDialog.dismiss();
                    return;
                }

                // progress
                mProgressDialog.setMax(task.maxProgress());
                mProgressDialog.setMessage(message);
                if(progress > 0) {
                    mProgressDialog.setIndeterminate(false);
                    mProgressDialog.setProgress((int)(progress * mProgressDialog.getMax()));
                    mProgressDialog.setProgressNumberFormat("%1d/%2d");
                    mProgressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
                } else {
                    mProgressDialog.setIndeterminate(true);
                    mProgressDialog.setProgress(mProgressDialog.getMax());
                    mProgressDialog.setProgressNumberFormat(null);
                    mProgressDialog.setProgressPercentFormat(null);
                }

                // show
                if(task.isFinished()) {
                    mProgressDialog.dismiss();
                } else if(!mProgressDialog.isShowing()) {
                    mProgressDialog.show();
                }
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ManagedTask task = TaskManager.getTask(GetAvailableSourcesTask.TASK_ID);
        if(task != null) TaskManager.cancelTask(task);
    }

    @Override
    public void onDestroy() {
        ManagedTask task = TaskManager.getTask(GetAvailableSourcesTask.TASK_ID);
        if(task != null) {
            task.removeOnProgressListener(this);
            task.removeOnFinishedListener(this);
        }

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        super.onDestroy();
    }

}
