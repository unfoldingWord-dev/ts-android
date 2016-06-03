package com.door43.translationstudio.newui.publish;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
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

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.core.Project;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationViewMode;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.newui.Door43LoginDialog;
import com.door43.translationstudio.newui.FeedbackDialog;
import com.door43.translationstudio.newui.MergeConflictsDialog;
import com.door43.translationstudio.newui.translate.TargetTranslationActivity;
import com.door43.translationstudio.tasks.CreateRepositoryTask;
import com.door43.translationstudio.tasks.PullTargetTranslationTask;
import com.door43.translationstudio.tasks.PushTargetTranslationTask;
import com.door43.translationstudio.tasks.RegisterSSHKeysTask;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.GenericTaskWatcher;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;
import com.door43.widget.ViewUtil;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.merge.MergeStrategy;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Map;


/**
 * Created by joel on 9/20/2015.
 */
public class PublishFragment extends PublishStepFragment implements GenericTaskWatcher.OnFinishedListener {

    private static final String STATE_UPLOADED = "state_uploaded";
    private boolean mUploaded = false;
    private Button mUploadButton;
    private GenericTaskWatcher taskWatcher;
    private LinearLayout mUploadSuccess;
    private TargetTranslation targetTranslation;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_publish_publish, container, false);

        HtmlTextView explanationView = (HtmlTextView)v.findViewById(R.id.explanation);
        explanationView.setHtmlFromString(getResources().getString(R.string.publishing_explanation), true);

        if(savedInstanceState != null) {
            mUploaded = savedInstanceState.getBoolean(STATE_UPLOADED, false);
        }

        Bundle args = getArguments();
        final String targetTranslationId = args.getString(PublishActivity.EXTRA_TARGET_TRANSLATION_ID);
        if (targetTranslationId == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }

        taskWatcher = new GenericTaskWatcher(getActivity(), R.string.uploading);
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

                    // TODO: 5/26/16 this would be a lot easier if we tried to clone instead of pulling.  Then we could merge manually
                    PullTargetTranslationTask task = new PullTargetTranslationTask(targetTranslation);
                    taskWatcher.watch(task);
                    TaskManager.addTask(task, PullTargetTranslationTask.TASK_ID);
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
        PullTargetTranslationTask pullTask = (PullTargetTranslationTask)TaskManager.getTask(PullTargetTranslationTask.TASK_ID);
        RegisterSSHKeysTask keysTask = (RegisterSSHKeysTask)TaskManager.getTask(RegisterSSHKeysTask.TASK_ID);
        CreateRepositoryTask repoTask = (CreateRepositoryTask)TaskManager.getTask(CreateRepositoryTask.TASK_ID);
        PushTargetTranslationTask pushTask = (PushTargetTranslationTask)TaskManager.getTask(PushTargetTranslationTask.TASK_ID);

        if(pullTask != null) {
            taskWatcher.watch(pullTask);
        } else if (keysTask != null) {
            taskWatcher.watch(keysTask);
        } else if(repoTask != null) {
            taskWatcher.watch(repoTask);
        } else if(pushTask != null) {
            taskWatcher.watch(pushTask);
        }

        // attach to dialogs
        MergeConflictsDialog mergeConflictsDialog = (MergeConflictsDialog)getFragmentManager().findFragmentByTag(MergeConflictsDialog.TAG);
        if(mergeConflictsDialog != null) {
            attachMergeConflictListener(mergeConflictsDialog);
        }

        return v;
    }

    /**
     * The publishing tasks are quite complicated so here's an overview in order:
     * 1. Pull - retreives any outstanding changes from the server. Also checks authentication (goto 2) , and existence of repo (goto 3)
     * 2. Register Keys - generates ssh keys and registers them with the gogs account. Then tries to pull again.
     * 3. Create Repo - creates a new repository in gogs. Then tries to pull again.
     * 4. Push - pushes the target translation to the gogs repo. If authentication fails goto 2
     * User intervention is required if there are merge conflicts.
     * @param task
     */
    @Override
    public void onFinished(final ManagedTask task) {
        taskWatcher.stop();
        if(task instanceof PullTargetTranslationTask) {
            PullTargetTranslationTask.Status status = ((PullTargetTranslationTask)task).getStatus();
            //  TRICKY: we continue to push for unknown status in case the repo was just created (the missing branch is an error)
            // the pull task will catch any errors
            if(status == PullTargetTranslationTask.Status.UP_TO_DATE
                    || status == PullTargetTranslationTask.Status.UNKNOWN) {
                Logger.i(this.getClass().getName(), "Changes on the server were synced with " + targetTranslation.getId());
                try {
                    final Handler hand = new Handler(Looper.getMainLooper());
                    targetTranslation.setPublished(new TargetTranslation.OnPublishedListener() {
                        @Override
                        public void onSuccess() {
                            // begin upload
                            PushTargetTranslationTask task = new PushTargetTranslationTask(targetTranslation, true);
                            taskWatcher.watch(task);
                            TaskManager.addTask(task, PushTargetTranslationTask.TASK_ID);
                        }

                        @Override
                        public void onFailed(Exception e) {
                            hand.post(new Runnable() {
                                @Override
                                public void run() {
                                    notifyPublishFailed(targetTranslation);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    Logger.e(PublishFragment.class.getName(), "Failed to mark target translation " + targetTranslation.getId() + " as publishable", e);
                    notifyPublishFailed(targetTranslation);
                    return;
                }
            } else if(status == PullTargetTranslationTask.Status.AUTH_FAILURE) {
                Logger.i(this.getClass().getName(), "Authentication failed");
                // if we have already tried ask the user if they would like to try again
                if(AppContext.context().hasSSHKeys()) {
                    showAuthFailure();
                    return;
                }

                RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(false);
                taskWatcher.watch(keyTask);
                TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
            } else if(status == PullTargetTranslationTask.Status.NO_REMOTE_REPO) {
                Logger.i(this.getClass().getName(), "The repository " + targetTranslation.getId() + " could not be found");
                // create missing repo
                CreateRepositoryTask repoTask = new CreateRepositoryTask(targetTranslation);
                taskWatcher.watch(repoTask);
                TaskManager.addTask(repoTask, CreateRepositoryTask.TASK_ID);
            } else if(status == PullTargetTranslationTask.Status.MERGE_CONFLICTS) {
                Logger.i(this.getClass().getName(), "The server contains conflicting changes for " + targetTranslation.getId());
                notifyMergeConflicts(((PullTargetTranslationTask)task).getConflicts());
            } else {
                notifyPublishFailed(targetTranslation);
            }
        } else if(task instanceof RegisterSSHKeysTask) {
            if(((RegisterSSHKeysTask)task).isSuccess()) {
                Logger.i(this.getClass().getName(), "SSH keys were registered with the server");
                // try to pull again
                PullTargetTranslationTask pullTask = new PullTargetTranslationTask(targetTranslation);
                taskWatcher.watch(pullTask);
                TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
            } else {
                notifyPublishFailed(targetTranslation);
            }
        } else if(task instanceof CreateRepositoryTask) {
            if(((CreateRepositoryTask)task).isSuccess()) {
                Logger.i(this.getClass().getName(), "A new repository " + targetTranslation.getId() + " was created on the server");
                PullTargetTranslationTask pullTask = new PullTargetTranslationTask(targetTranslation);
                taskWatcher.watch(pullTask);
                TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
            } else {
                notifyPublishFailed(targetTranslation);
            }
        } else if(task instanceof PushTargetTranslationTask) {
            PushTargetTranslationTask.Status status =((PushTargetTranslationTask)task).getStatus();
            final String uploadDetails = ((PushTargetTranslationTask)task).getMessage();

            if(status == PushTargetTranslationTask.Status.OK) {
                Logger.i(this.getClass().getName(), "The target translation " + targetTranslation.getId() + " was pushed to the server");
                getListener().finishPublishing();
                Handler hand = new Handler(Looper.getMainLooper());
                hand.post(new Runnable() {
                    @Override
                    public void run() {
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

                        final CustomAlertDialog dlg = CustomAlertDialog.Create(getActivity());
                        dlg.setTitle(R.string.success)
                                .setMessage(clickableDestinationMessage)
                                .setAutoDismiss(false)
                                .setPositiveButton(R.string.dismiss, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        dlg.dismiss();
                                    }
                                })
                                .setNeutralButton(R.string.label_details, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        showDetails(uploadDetails);
                                    }
                                })
                                .show("publish-finished");

                        applyClickableMessageToDialog(clickableDestinationMessage, dlg);
                    }
                });
            } else if(status == PushTargetTranslationTask.Status.AUTH_FAILURE) {
                Logger.i(this.getClass().getName(), "Authentication failed");
                showAuthFailure();
            } else {
                notifyPublishFailed(targetTranslation);
            }

        }
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

    private void notifyMergeConflicts(Map<String, int[][]> conflicts) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        MergeConflictsDialog dialog = new MergeConflictsDialog();
        attachMergeConflictListener(dialog);
        dialog.show(ft, MergeConflictsDialog.TAG);
    }

    private void attachMergeConflictListener(MergeConflictsDialog dialog) {
        dialog.setOnClickListener(new MergeConflictsDialog.OnClickListener() {
            @Override
            public void onReview() {
                // ask parent activity to navigate to a new activity
                Intent intent = new Intent(getActivity(), TargetTranslationActivity.class);
                Bundle args = new Bundle();
                args.putString(AppContext.EXTRA_TARGET_TRANSLATION_ID, targetTranslation.getId());
                // TODO: 4/20/16 it woulid be nice to navigate directly to the first conflict
//                args.putString(AppContext.EXTRA_CHAPTER_ID, chapterId);
//                args.putString(AppContext.EXTRA_FRAME_ID, frameId);
                args.putString(AppContext.EXTRA_VIEW_MODE, TranslationViewMode.REVIEW.toString());
                intent.putExtras(args);
                startActivity(intent);
                getActivity().finish();
            }

            @Override
            public void onKeepServer() {
                try {
                    Git git = targetTranslation.getRepo().getGit();
                    ResetCommand resetCommand = git.reset();
                    resetCommand.setMode(ResetCommand.ResetType.HARD)
                            .setRef("backup-master")
                            .call();

                    targetTranslation.commitSync();

                    // try to pull again
                    PullTargetTranslationTask pullTask = new PullTargetTranslationTask(targetTranslation, MergeStrategy.THEIRS);
                    taskWatcher.watch(pullTask);
                    TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to keep server changes durring publish", e);
                    notifyPublishFailed(targetTranslation);
                }
            }

            @Override
            public void onKeepLocal() {
                try {
                    Git git = targetTranslation.getRepo().getGit();
                    ResetCommand resetCommand = git.reset();
                    resetCommand.setMode(ResetCommand.ResetType.HARD)
                            .setRef("backup-master")
                            .call();

                    targetTranslation.commitSync();

                    // try to pull again
                    PullTargetTranslationTask pullTask = new PullTargetTranslationTask(targetTranslation, MergeStrategy.OURS);
                    taskWatcher.watch(pullTask);
                    TaskManager.addTask(pullTask, PullTargetTranslationTask.TASK_ID);
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to keep local changes durring publish", e);
                    notifyPublishFailed(targetTranslation);
                }
            }

            @Override
            public void onCancel() {
                try {
                    Git git = targetTranslation.getRepo().getGit();
                    ResetCommand resetCommand = git.reset();
                    resetCommand.setMode(ResetCommand.ResetType.HARD)
                            .setRef("backup-master")
                            .call();
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "Failed to restore local changes", e);
                }
                // TODO: 4/20/16 notify canceled
            }
        });
    }

    public void showAuthFailure() {
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.error).setMessage(R.string.auth_failure_retry)
                .setPositiveButton(R.string.yes, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RegisterSSHKeysTask keyTask = new RegisterSSHKeysTask(true);
                        taskWatcher.watch(keyTask);
                        TaskManager.addTask(keyTask, RegisterSSHKeysTask.TASK_ID);
                    }
                })
                .setNegativeButton(R.string.no, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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
        final Project project = AppContext.getLibrary().getProject(targetTranslation.getProjectId(), "en");
        CustomAlertDialog.Create(getActivity())
                .setTitle(R.string.error)
                .setMessage(R.string.upload_failed)
                .setPositiveButton(R.string.dismiss, null)
                .setNeutralButton(R.string.menu_bug, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

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
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroy() {
        if(taskWatcher != null) {
            taskWatcher.stop();
        }
        super.onDestroy();
    }
}
