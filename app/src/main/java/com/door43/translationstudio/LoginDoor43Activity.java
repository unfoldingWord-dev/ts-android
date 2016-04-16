package com.door43.translationstudio;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.door43.translationstudio.core.Profile;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.tasks.LoginDoor43Task;
import com.door43.util.tasks.ManagedTask;
import com.door43.util.tasks.TaskManager;

import org.unfoldingword.gogsclient.User;

public class LoginDoor43Activity extends AppCompatActivity implements ManagedTask.OnFinishedListener {

    private ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_door43);

        final EditText usernameText = (EditText)findViewById(R.id.username);
        final EditText passwordText = (EditText)findViewById(R.id.password);

        Button cancelButton = (Button)findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        Button continueButton = (Button)findViewById(R.id.ok_button);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppContext.closeKeyboard(LoginDoor43Activity.this);
                String username = usernameText.getText().toString();
                String password = passwordText.getText().toString();
                LoginDoor43Task task = new LoginDoor43Task(username, password);
                showProgressDialog();
                task.addOnFinishedListener(LoginDoor43Activity.this);
                TaskManager.addTask(task, LoginDoor43Task.TASK_ID);
            }
        });

        LoginDoor43Task task = (LoginDoor43Task) TaskManager.getTask(LoginDoor43Task.TASK_ID);
        if(task != null) {
            showProgressDialog();
            task.addOnFinishedListener(this);
        }
    }

    @Override
    public void onFinished(ManagedTask task) {
        TaskManager.clearTask(task);

        if(progressDialog != null) {
            progressDialog.dismiss();
        }

        User user = ((LoginDoor43Task)task).getUser();
        if(user != null) {
            // save gogs user to profile
            // TODO: 4/15/16 if the fullname has not been set we need to ask for it
            String fullName = user.fullName == null || user.fullName.isEmpty() ? user.getUsername() : user.fullName;
            Profile profile = new Profile(fullName);
            profile.gogsUser = user;
            AppContext.setProfile(profile);
            finish();
        } else {
            // login failed
            CustomAlertDialog.Create(this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.double_check_credentials)
                    .setPositiveButton(R.string.label_ok, null)
                    .show("login_failed");
        }
    }

    public void showProgressDialog() {
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }

        progressDialog.setTitle(getResources().getString(R.string.logging_in));
        progressDialog.setMessage(getResources().getString(R.string.please_wait));
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }
}
