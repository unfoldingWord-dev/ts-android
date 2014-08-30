package com.door43.translationstudio.util;


import android.app.Fragment;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.MainApplication;

/**
 * Created by joel on 8/27/2014.
 */
public class TranslatorBaseFragment extends Fragment {

    /**
     * Returns the context of the fragment
     * @return
     */
    protected MainApplication app() {
        return (MainApplication)getActivity().getApplicationContext();
    }
}
