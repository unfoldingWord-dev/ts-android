package com.door43.translationstudio.uploadwizard;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.AppContext;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class ReviewFragment extends WizardFragment {
    private Button mContinueBtn;
    private ValidateUploadTask mValidator;
    private ArrayList<UploadValidationItem> mValidationItems = new ArrayList<UploadValidationItem>();
    private UploadValidationAdapter mAdapter;
    private boolean mHasErrors = false;
    private boolean mHasWarnings = false;

    public ReviewFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_upload_overview_old, container, false);

        Button cancelBtn = (Button)rootView.findViewById(R.id.upload_wizard_cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopValidation();
                onCancel();
            }
        });
        mContinueBtn = (Button)rootView.findViewById(R.id.upload_wizard_continue_btn);
        mContinueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mHasErrors) {
                    app().showMessageDialog(R.string.dialog_validation_errors, R.string.validation_errors);
                } else if(mHasWarnings) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    DialogInterface.OnClickListener cancel = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    };
                    DialogInterface.OnClickListener ok = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            onContinue();
                        }
                    };
                    builder.setTitle(R.string.dialog_validation_warnings).setMessage(R.string.validation_warnings)
                            .setPositiveButton(R.string.label_continue, ok)
                            .setNegativeButton(R.string.title_cancel, cancel).show();
                } else {
                    onContinue();
                }
            }
        });

        ListView list = (ListView)rootView.findViewById(R.id.validationListView);
        mAdapter = new UploadValidationAdapter(mValidationItems, getActivity());
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                UploadValidationItem item = mAdapter.getItem(i);
                if(!item.getDescription().isEmpty()) {
                    app().showMessageDialog(item.getTitle(), item.getDescription());
                }
            }
        });

        performValidation();
        return rootView;
    }

    /**
     * begins performing the upload validation
     */
    private void performValidation() {
        // TODO: display progressbar or spinning wheel durring validation.
        mContinueBtn.setBackgroundColor(getResources().getColor(R.color.lighter_gray));
        mContinueBtn.setEnabled(false);
        mValidator = new ValidateUploadTask(new OnValidateProgress() {
            @Override
            public void onProgress(UploadValidationItem item) {
                // TODO: populate validation item
                mValidationItems.add(item);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onSuccess() {
                onValidationComplete();
            }
        });
        mValidator.execute();
    }

    /**
     * stops performing the upload validation
     */
    private void stopValidation() {
        // TODO: stop the async validation
        mValidator.cancel(true);
        onCancel();
    }

    private void onValidationComplete() {
        mContinueBtn.setBackgroundColor(getResources().getColor(R.color.blue));
        mContinueBtn.setEnabled(true);
        // TODO: allow the user to fix things
    }

    private class ValidateUploadTask extends AsyncTask<Void, UploadValidationItem, Void> {
        private OnValidateProgress mCallback;

        public ValidateUploadTask(OnValidateProgress callback) {
            mCallback = callback;
        }

        @Override
        protected Void doInBackground(Void[] voids) {
            // make sure the project is configured correctly
            Project p = AppContext.projectManager().getSelectedProject();
            if(p.getSelectedSourceLanguage().equals(p.getSelectedTargetLanguage())) {
                // source and target language should not be the same
                publishProgress(new UploadValidationItem(getResources().getString(R.string.title_project_settings), getResources().getString(R.string.error_target_and_source_are_same), UploadValidationItem.Status.ERROR));
                mHasErrors = true;
            } else {
                publishProgress(new UploadValidationItem(getResources().getString(R.string.title_project_settings), UploadValidationItem.Status.SUCCESS));
            }

            // make sure all the chapter titles and references have been set
            int numChaptersTranslated = 0;
            boolean chapterHasWarnings = false;
            for(int i = 0; i < p.numChapters(); i ++) {
                Chapter c = p.getChapter(i);
                String description = "";
                UploadValidationItem.Status status = UploadValidationItem.Status.SUCCESS;
                // check title
                if(c.getTitleTranslation().getText().isEmpty()) {
                    description = getResources().getString(R.string.error_chapter_title_missing);
                    status = UploadValidationItem.Status.WARNING;
                    chapterHasWarnings = true;
                }
                // check reference
                if(c.getReferenceTranslation().getText().isEmpty()) {
                    description = description + "\n"+getResources().getString(R.string.error_chapter_reference_missing);
                    status = UploadValidationItem.Status.WARNING;
                    chapterHasWarnings = true;
                }
                // check frames
                int numFramesNotTranslated = 0;
                for(int j = 0; j < c.numFrames(); j ++) {
                    Frame f = c.getFrame(j);
                    if(f.getTranslation().getText().isEmpty()) {
                        numFramesNotTranslated ++;
                    }
                }
                if(numFramesNotTranslated > 0) {
                    description += "\n"+String.format(getResources().getString(R.string.error_frames_not_translated), numFramesNotTranslated);
                    status = UploadValidationItem.Status.WARNING;
                    chapterHasWarnings = true;
                }

                // only display warnings for chapters that have at least some frames translated
                if(numFramesNotTranslated != c.numFrames()) {
                    numChaptersTranslated ++;
                    publishProgress(new UploadValidationItem(String.format(getResources().getString(R.string.label_chapter_title_detailed), c.getTitle()), description, status));
                } else {
                    // ignore
                    chapterHasWarnings = false;
                }
                mHasWarnings = chapterHasWarnings || mHasWarnings;
            }

            // ensure at least one chapter has been translated
            if(numChaptersTranslated == 0) {
                publishProgress(new UploadValidationItem(getResources().getString(R.string.title_chapters), getResources().getString(R.string.no_translated_chapters), UploadValidationItem.Status.ERROR));
                mHasErrors = true;
            }
            return null;
        }

        protected void onProgressUpdate(UploadValidationItem... items) {
            mCallback.onProgress(items[0]);
        }

        protected void onPostExecute(Void result) {
            mCallback.onSuccess();
        }
    }

    private interface OnValidateProgress {
        void onProgress(UploadValidationItem item);
        void onSuccess();
    }
}
