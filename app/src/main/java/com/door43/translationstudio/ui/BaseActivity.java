package com.door43.translationstudio.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.door43.translationstudio.App;
import com.door43.translationstudio.services.BackupService;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.tools.foreground.Foreground;
import org.unfoldingword.tools.logger.Logger;

import java.io.File;

/**
 * This should be extended by all activities in the app so that we can perform verification on
 * activities such as recovery from crashes.
 *
 */
public abstract class BaseActivity extends AppCompatActivity implements Foreground.Listener {

    private static final int PERMISSIONS_REQUEST_WRITE_EXT_STORAGE = 0;
    private static final String KEY_WAITING_FOR_PERMISSIONS = "waiting_for_permissions";
    private boolean waitingForPermissions = false;
    private Foreground foreground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            this.waitingForPermissions = savedInstanceState.getBoolean(KEY_WAITING_FOR_PERMISSIONS);
        }
        try {
            this.foreground = Foreground.get();
            this.foreground.addListener(this);
        } catch (IllegalStateException e) {
            Logger.i(this.getClass().getName(), "Foreground was not initialized");
        }
    }

    /**
     * Indicates if the activity is waiting for the user to grant manifest permissions
     * @return
     */
    public boolean waitingForPermissions() {
       return this.waitingForPermissions;
    }

    @Override
    public void onResume() {
        super.onResume();

        // request permissions
        if(!this.waitingForPermissions) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXT_STORAGE);
                this.waitingForPermissions = true;
            }
        }

        // check if we crashed or if we need to reload
        if(!waitingForPermissions) {
            if (!isBootActivity()) {
                File[] crashFiles = Logger.listStacktraces();;
                if (crashFiles.length > 0) {
                    // restart
                    Intent intent = new Intent(this, SplashScreenActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }
    }

    private boolean isBootActivity() {
        return this instanceof TermsOfUseActivity
                || this instanceof SplashScreenActivity
                || this instanceof CrashReporterActivity;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch(requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXT_STORAGE: {
                if(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.waitingForPermissions = false;
                    onResume();
                } else {
                    // The app cannot function without needed permissions
                    finish();
                }
                return;
            }
        }
    }

    @Override
    public void onDestroy() {
        if(this.foreground != null) {
            this.foreground.removeListener(this);
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_WAITING_FOR_PERMISSIONS, this.waitingForPermissions);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBecameForeground() {
        // check if the index had been loaded
        if(!isBootActivity() && !App.isLibraryDeployed()) {
            Logger.w(this.getClass().getName(), "The library was not deployed.");
            // restart
            Intent intent = new Intent(this, SplashScreenActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBecameBackground() {

    }
}
