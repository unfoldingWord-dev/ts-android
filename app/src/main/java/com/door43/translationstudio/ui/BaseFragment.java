package com.door43.translationstudio.ui;

import android.app.Fragment;
import android.content.Intent;

import org.unfoldingword.tools.logger.Logger;

import java.io.File;

/**
 * This should be extended by all activities in the app so that we can perform verification on
 * activities such as recovery from crashes.
 *
 */
public abstract class BaseFragment extends Fragment {

    @Override
    public void onResume() {
        super.onResume();

        if(getActivity() instanceof TermsOfUseActivity == false
                && getActivity() instanceof  SplashScreenActivity == false
                && getActivity() instanceof CrashReporterActivity == false) {
            // check if we crashed or if we need to reload
            File[] crashFiles = Logger.listStacktraces();
            if (crashFiles.length > 0) {
                // restart
                Intent intent = new Intent(getActivity(), SplashScreenActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        }
    }
}
