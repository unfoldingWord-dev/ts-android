package com.door43.translationstudio.newui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.door43.tools.reporting.GlobalExceptionHandler;
import com.door43.translationstudio.CrashReporterActivity;
import com.door43.translationstudio.SplashScreenActivity;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.TermsOfUseActivity;
import com.door43.translationstudio.newui.legal.LegalDocumentActivity;

import java.io.File;

/**
 * This should be extended by all activities in the app so that we can perform verification on
 * activities such as recovery from crashes.
 *
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void onResume() {
        super.onResume();

        if(this instanceof TermsOfUseActivity == false
                && this instanceof LegalDocumentActivity == false
                && this instanceof SplashScreenActivity == false
                && this instanceof CrashReporterActivity == false) {
            // check if we crashed or if we need to reload
            File dir = new File(getExternalCacheDir(), AppContext.context().STACKTRACE_DIR);
            String[] crashFiles = GlobalExceptionHandler.getStacktraces(dir);
            if (crashFiles.length > 0 || !AppContext.getLibrary().exists() || AppContext.getLibrary().getTargetLanguagesLength() == 0) {
                // restart
                Intent intent = new Intent(this, SplashScreenActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }
}
