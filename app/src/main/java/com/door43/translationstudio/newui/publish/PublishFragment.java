package com.door43.translationstudio.newui.publish;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.Door43LoginDialog;
import com.door43.translationstudio.newui.FeedbackDialog;
import com.door43.translationstudio.tasks.CreateRepositoryTask;
import com.door43.translationstudio.tasks.PushTargetTranslationTask;
import com.door43.translationstudio.tasks.RegisterSSHKeysTask;
import com.door43.translationstudio.AppContext;
import org.unfoldingword.tools.taskmanager.SimpleTaskWatcher;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;
import com.door43.widget.ViewUtil;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.io.File;
import java.security.InvalidParameterException;


/**
 * Created by joel on 9/20/2015.
 */
public class PublishFragment extends PublishStepFragment implements SimpleTaskWatcher.OnFinishedListener {

    private static final String STATE_UPLOADED = "state_uploaded";
    public static final String STATE_DIALOG_SHOWN = "state_dialog_shown";
    public static final String STATE_UPLOAD_DETAILS = "state_upload_details";
    public static final String TAG = PublishFragment.class.getSimpleName();
    private boolean mUploaded = false;
    private Button mUploadButton;
    private SimpleTaskWatcher taskWatcher;
    private LinearLayout mUploadSuccess;
    private TargetTranslation targetTranslation;
    private eDialogShown mDialogShown = eDialogShown.NONE;
    private String mUploadDetails;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_publish_publish, container, false);

        HtmlTextView explanationView = (HtmlTextView)v.findViewById(R.id.explanation);
        explanationView.setHtmlFromString(getResources().getString(R.string.publishing_explanation), true);

        if(savedInstanceState != null) {
            mUploaded = savedInstanceState.getBoolean(STATE_UPLOADED, false);
            mDialogShown = eDialogShown.fromInt(savedInstanceState.getInt(STATE_DIALOG_SHOWN, eDialogShown.NONE.getValue()));
            mUploadDetails = savedInstanceState.getString(STATE_UPLOAD_DETAILS, null);
        }

        Bundle args = getArguments();
        final String targetTranslationId = args.getString(PublishActivity.EXTRA_TARGET_TRANSLATION_ID);
        if (targetTranslationId == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        taskWatcher = new SimpleTaskWatcher(getActivity(), R.string.uploading);
        taskWatcher.setOnFinishedListener(this);

        // receive uploaded status from activity (overrides save state from fragment)
        if(savedInstanceState == null) {
            mUploaded = args.getBoolean(ARG_PUBLISH_FINISHED, mUploaded);
        }

        this.targetTranslation = AppContext.getTranslator().getTargetTranslation(targetTranslationId);

        mUploadSuccess = (LinearLayout)v.findViewById(R.id.upload_success);
        mUploadButton = (Button)v.findViewById(R.id.upload_button);

        if(mUploaded) {
            mUploadButton.setVisibility(View.GONE);
            mUploadSuccess.setVisibility(View.VISIBLE);
        } else {
            mUploadButton.setVisibility(View.VISIBLE);
            mUploadSuccess.setVisibility(View.GONE);
        }

        // give the user some happy feedback in case they feel like clicking again
        mUploadSuccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.success, Snackbar.LENGTH_SHORT);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(AppContext.context().isNetworkAvailable()) {
                    // make sure we have a gogs user
                    if(AppContext.getProfile().gogsUser == null) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Door43LoginDialog dialog = new Door43LoginDialog();
                        dialog.show(ft, Door43LoginDialog.TAG);
                        return;
                    }

                    PushTargetTranslationTask task = new PushTargetTranslationTask(targetTranslation, true);
                    taskWatcher.watch(task);
                    TaskManager.addTask(task, PushTargetTranslationTask.TASK_ID);
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.internet_not_available, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });

        ImageView wifiIcon = (ImageView)v.findViewById(R.id.wifi_icon);
        ViewUtil.tintViewDrawable(wifiIcon, getResources().getColor(R.color.dark_secondary_text));

        final String filename = targetTranslation.getId() + ".zip";

        // export buttons
        Button exportToApp = (Button)v.findViewById(R.id.backup_to_app);
        exportToApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File exportFile = new File(AppContext.getSharingDir(), filename);
                try {
                    AppContext.getTranslator().exportDokuWiki(targetTranslation, exportFile);
                } catch (Exception e) {
                    Logger.e(PublishFragment.class.getName(), "Failed to export the target translation " + targetTranslation.getId(), e);
                }
                if(exportFile.exists()) {
                    Uri u = FileProvider.getUriForFile(getActivity(), "com.door43.translationstudio.fileprovider", exportFile);
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("application/zip");
                    i.putExtra(Intent.EXTRA_STREAM, u);
                    startActivity(Intent.createChooser(i, "Email:"));
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.translation_export_failed, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });
        Button exportToSD = (Button)v.findViewById(R.id.export_to_sdcard);
        exportToSD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 10/27/2015 have the user choose where to save the file
                File exportFile = new File(AppContext.getPublicDownloadsDirectory(), System.currentTimeMillis() / 1000L + "_" + filename);
                try {
                    AppContext.getTranslator().exportDokuWiki(targetTranslation, exportFile);
                } catch (Exception e) {
                    Logger.e(PublishFragment.class.getName(), "Failed to export the target translation " + targetTranslation.getId(), e);
                }
                if(exportFile.exists()) {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.success, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                } else {
                    Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.translation_export_failed, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });
        Button exportToDevice = (Button)v.findViewById(R.id.backup_to_device);
        exportToDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar snack = Snackbar.make(getActivity().findViewById(android.R.id.content), "Coming soon", Snackbar.LENGTH_SHORT);
                ViewUtil.setSnackBarTextColor(snack, getResources().getColor(R.color.light_primary_text));
                snack.show();
            }
        });

        // Connect to existing tasks
        RegisterSSHKeysTask keysTask = (RegisterSSHKeysTask) TaskManager.getTask(RegisterSSHKeysTask.TASK_ID);
        CreateRepositoryTask repoTask = (CreateRepositoryTask) TaskManager.getTask(CreateRepositoryTask.TASK_ID);
        PushTargetTranslationTask pushTask = (PushTargetTranslationTask) TaskManager.getTask(PushTargetTranslationTask.TASK_ID);

        if (keysTask != null) {
            taskWatcher.watch(keysTask);
        } else if (repoTask != null) {
            taskWatcher.watch(repoTask);
        } else if (pushTask != null) {
            taskWatcher.watch(pushTask);
        }

        restoreDialogs();
        return v;
    }

    /**
     * recreate the dialogs that were displayed before rotation
     */
    private void restoreDialogs() {
        switch(mDialogShown) {
            case PUBLISH_FAILED:
                notifyPublishFailed(targetTranslation);
                break;

            case AUTH_FAILURE:
                showAuthFailure();
                break;

            case PUSH_FAILURE:
                showPushFailure();
                break;

            case PUBLISH_SUCCESS:
                showPublishSuccessDialog(mUploadDetails);
                break;

            case NONE:
                break;

            default:
                Logger.e(TAG,"Unsupported restore dialog: " + mDialogShown.toString());
                break;
        }
    }

    /**
     * The publishing tasks are quite complicated so here's an overview in order:
     * 1. Push - pushes the target translation to the gogs repo. Also checks authentication (goto 2) , and existence of repo (goto 3)
     * 2. Register Keys - generates ssh keys and registers them with the gogs account. Then tries to push again.
     * 3. Create Repo - creates a new repository in gogs. Then tries to push again.
     * User is warned that they will need to merge if push fails.
     *
     * @param task
     */
    @Override
    public void onFinished(final ManagedTask task) {
        taskWatcher.stop();

        if (task instanceof PushTargetTranslationTask) {
            PushTargetTranslationTask.Status status = ((PushTargetTranslationTask) task).getStatus();
            mUploadDetails = ((PushTargetTranslationTask) task).getMessage();

            if (status == PushTargetTranslationTask.Status.OK) {
                Logger.i(this.getClass().getName(), "The target translation " + targetTranslation.getId() + " was pushed to the server");
                getListener().finishPublishing();
                Handler hand = new Handler(Looper.getMainLooper());
                hand.post(new Runnable() {
                    @Override
                    public void run() {
                        showPublishSuccessDialog(mUploadDetails);
                    }
                });
            } else if (status == PushTargetTranslationTask.Status.AUTH_FAILURE) {
                Logger.i(this.getClass().getName(), "Authentication failed");
                // if we have already tried ask the user if they would like to try again
                if(AppContext.context().hasSSHKeys()) {
                    showAuthFailure();
                    return;
                }

                RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(false);
                taskWatcher.watch(keyTask);
                TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);

            } else if (status == PushTargetTranslationTask.Status.NO_REMOTE_REPO) {
                Logger.i(this.getClass().getName(), "The repository " + targetTranslation.getId() + " could not be found");
                // create missing repo
                CreateRepositoryTask repoTask = new CreateRepositoryTask(targetTranslation);
                taskWatcher.watch(repoTask);
                TaskManager.addTask(repoTask, CreateRepositoryTask.TASK_ID);
            } else if (status == PushTargetTranslationTask.Status.REJECTED) {
                showPushFailure();
            } else {
                notifyPublishFailed(targetTranslation);
            }
        } else if (task instanceof RegisterSSHKeysTask) {
            if (((RegisterSSHKeysTask) task).isSuccess()) {
                Logger.i(this.getClass().getName(), "SSH keys were registered with the server");
                // try to push again

                PushTargetTranslationTask pushTask = new PushTargetTranslationTask(targetTranslation, true);
                taskWatcher.watch(pushTask);
                TaskManager.addTask(pushTask, PushTargetTranslationTask.TASK_ID);
            } else {
                notifyPublishFailed(targetTranslation);
            }
        } else if (task instanceof CreateRepositoryTask) {
            if (((CreateRepositoryTask) task).isSuccess()) {
                Logger.i(this.getClass().getName(), "A new repository " + targetTranslation.getId() + " was created on the server");
                PushTargetTranslationTask pushTask = new PushTargetTranslationTask(targetTranslation, true);
                taskWatcher.watch(pushTask);
                TaskManager.addTask(pushTask, PushTargetTranslationTask.TASK_ID);
            } else {
                notifyPublishFailed(targetTranslation);
            }
        }
    }

    private void showPublishSuccessDialog(final String uploadDetails) {
        mDialogShown = eDialogShown.PUBLISH_SUCCESS;
        mUploadButton.setVisibility(View.GONE);
        mUploadSuccess.setVisibility(View.VISIBLE);

        final String publishedUrl = getPublishedUrl(targetTranslation);
        String format = getActivity().getResources().getString(R.string.project_uploaded_to);
        final String destinationMessage = String.format(format, publishedUrl);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Uri uri = Uri.parse(publishedUrl);
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        };

        final SpannableString clickableDestinationMessage = getClickableText(publishedUrl, destinationMessage, clickableSpan);

        final CustomAlertDialog dlg = CustomAlertDialog.Builder(getActivity());
        dlg.setTitle(R.string.success)
                .setMessage(clickableDestinationMessage)
                .setAutoDismiss(false)
                .setPositiveButton(R.string.dismiss, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialogShown = eDialogShown.NONE;
                        dlg.dismiss();
                    }
                })
                .setNeutralButton(R.string.label_details, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialogShown = eDialogShown.NONE;
                        showDetails(uploadDetails);
                    }
                })
                .show("publish-finished");

        applyClickableMessageToDialog(clickableDestinationMessage, dlg);
    }

    /**
     * display the upload details
     * @param destinationMessage
     */
    private void showDetails(String destinationMessage) {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.project_uploaded)
                .setMessage(destinationMessage)
                .setPositiveButton(R.string.dismiss, null)
                .show();
        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
        textView.setMaxLines(5);
        textView.setScroller(new Scroller(getActivity()));
        textView.setVerticalScrollBarEnabled(true);
        textView.setMovementMethod(new ScrollingMovementMethod());
    }

    /**
     * put clickable message in dialog
     * @param clickableMessage
     * @param dlg
     */
    private void applyClickableMessageToDialog(final SpannableString clickableMessage, final CustomAlertDialog dlg) {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() { // wait until dialog has been created
            @Override
            public void run() {
                try {
                    Dialog dialog = dlg.getDialog();
                    TextView textView = (TextView) dialog.findViewById(R.id.dialog_content);
                    textView.setText(clickableMessage);
                    textView.setMovementMethod(LinkMovementMethod.getInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * make selected text clickable
     * @param textToClick
     * @param entireText
     * @param clickableSpan
     * @return
     */
    private SpannableString getClickableText(String textToClick, String entireText, ClickableSpan clickableSpan) {
        int startIndex = entireText.indexOf(textToClick);
        int lastIndex;
        if(startIndex < 0) { // if not found
            startIndex = 0;
            lastIndex = textToClick.length();
        } else {
            lastIndex = startIndex + textToClick.length();
        }

        SpannableString clickable = new SpannableString(entireText);
        clickable.setSpan(clickableSpan,startIndex,lastIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // set click
        clickable.setSpan(new UnderlineSpan(),startIndex,lastIndex,0); // underline
        clickable.setSpan(new StyleSpan(Typeface.BOLD),startIndex,lastIndex,0); // make bold
        return clickable;
    }

    /**
     * generate the url where the user can see that the published target is stored
     * @param targetTranslation
     * @return
     */
    public static String getPublishedUrl(TargetTranslation targetTranslation) {
        String userName = "";
        Profile profile = AppContext.getProfile();
        if(profile != null && profile.gogsUser != null) {
            userName = profile.gogsUser.getUsername();
        }

        String server = "";
        try {
            server = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, AppContext.context().getResources().getString(R.string.pref_default_git_server));
        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] parts = server.split("git@");
        if(parts.length == 2) {
            server = parts[1];
        }

        return "https://" + server + "/" + userName + "/" + targetTranslation.getId();
    }

    public void showPushFailure() {
        mDialogShown = eDialogShown.PUSH_FAILURE;
        CustomAlertDialog.Builder(getActivity())
                .setTitle(R.string.error).setMessage(R.string.upload_push_failure)
                .setPositiveButton(R.string.label_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialogShown = eDialogShown.NONE;
                        getListener().pushFailure();
                    }
                })
                .show("push_failure");
    }

    public void showAuthFailure() {
        mDialogShown = eDialogShown.AUTH_FAILURE;
        CustomAlertDialog.Builder(getActivity())
                .setTitle(R.string.error).setMessage(R.string.auth_failure_retry)
                .setPositiveButton(R.string.yes, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialogShown = eDialogShown.NONE;
                        RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(true);
                        taskWatcher.watch(keyTask);
                        TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
                    }
                })
                .setNegativeButton(R.string.no, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialogShown = eDialogShown.NONE;
                        notifyPublishFailed(targetTranslation);
                    }
                })
                .show("auth-failed");
    }

    /**
     * Displays a dialog to the user indicating the publish failed.
     * Includes an option to submit a bug report
     * @param targetTranslation
     */
    private void notifyPublishFailed(final TargetTranslation targetTranslation) {
        mDialogShown = eDialogShown.PUBLISH_FAILED;
        final Project project = AppContext.getLibrary().getProject(targetTranslation.getProjectId(), "en");
        CustomAlertDialog.Builder(getActivity())
                .setTitle(R.string.error)
                .setMessage(R.string.upload_failed)
                .setPositiveButton(R.string.dismiss, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialogShown = eDialogShown.NONE;
                    }
                })
                .setNeutralButton(R.string.menu_bug, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialogShown = eDialogShown.NONE;

                        // open bug report dialog
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag("bugDialog");
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ft.addToBackStack(null);

                        FeedbackDialog dialog = new FeedbackDialog();
                        Bundle args = new Bundle();
                        String message = "Failed to publish the translation of " +
                                project.name + " into " +
                                targetTranslation.getTargetLanguageName()
                                + ".\ntargetTranslation: " + targetTranslation.getId() +
                                "\n--------\n\n";
                        args.putString(FeedbackDialog.ARG_MESSAGE, message);
                        dialog.setArguments(args);
                        dialog.show(ft, "bugDialog");
                    }
                }).show("publish-failed");
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_UPLOADED, mUploaded);
        out.putInt(STATE_DIALOG_SHOWN, mDialogShown.getValue());
        if(mUploadDetails != null) {
            out.putString(STATE_UPLOAD_DETAILS, mUploadDetails);
        }
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        if(taskWatcher != null) {
            taskWatcher.stop();
        }
        super.onDestroy();
    }

    /**
     * for keeping track if dialog is being shown for orientation changes
     */
    public enum eDialogShown {
        NONE(0),
        PUBLISH_FAILED(1),
        AUTH_FAILURE(2),
        PUSH_FAILURE(3),
        PUBLISH_SUCCESS(4);

        private int _value;

        eDialogShown(int Value) {
            this._value = Value;
        }

        public int getValue() {
            return _value;
        }

        public static eDialogShown fromInt(int i) {
            for (eDialogShown b : eDialogShown.values()) {
                if (b.getValue() == i) {
                    return b;
                }
            }
            return null;
        }
    }
}
