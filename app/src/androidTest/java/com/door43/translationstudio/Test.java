package com.door43.translationstudio;

import android.test.InstrumentationTestCase;
import android.util.Log;

/**
 * Created by Andrew on 6/28/2016.
 */
public class Test extends InstrumentationTestCase {
//    private Library mLibrary;
    private String TAG="TEST";

    protected void setUp() throws Exception {
//        mLibrary = App.getLibrary();
    }

    public void test02a() throws Exception{
        int num=7;
        assertTrue(num==8);
        Log.e(TAG,"test results: ");
    }

}
