package com.door43.translationstudio.newui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.door43.tools.reporting.GlobalExceptionHandler;
import com.door43.translationstudio.CrashReporterActivity;
import com.door43.translationstudio.SplashScreenActivity;
import com.door43.translationstudio.util.AppContext;

import java.io.File;

/**
 * This should be extended by all activities in the app so that we can perform verification on
 * activities such as recovery from crashes.
 *
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void onRestart() {
        super.onRestart();

        // check if we crashed
        File dir = new File(getExternalCacheDir(), AppContext.context().STACKTRACE_DIR);
        String[] files = GlobalExceptionHandler.getStacktraces(dir);
        if (files.length > 0) {
            // restart
            Intent intent = new Intent(this, SplashScreenActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
