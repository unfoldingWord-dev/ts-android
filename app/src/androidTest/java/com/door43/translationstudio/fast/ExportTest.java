package com.door43.translationstudio.fast;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.door43.translationstudio.core.Translator;

import java.io.File;

/**
 * Created by joel on 10/27/2015.
 */
public class ExportTest extends InstrumentationTestCase {

    private Context context;
    private Translator translator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.context = getInstrumentation().getContext();
        this.translator = new Translator(this.context, new File(this.context.getCacheDir(), "export_test"));
    }

}
