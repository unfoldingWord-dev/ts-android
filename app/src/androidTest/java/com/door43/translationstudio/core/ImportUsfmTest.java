package com.door43.translationstudio.core;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.AppContext;
import android.content.Context;

import junit.framework.TestCase;
import junit.textui.ResultPrinter;


/**
 * Created by blm on 4/19/16.
 */
public class ImportUsfmTest extends InstrumentationTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {

    }

//    mDeveloperTools.add(new ToolItem("Test USFM Import", "Test USFM Import", 0, new ToolItem.ToolAction() {
//        @Override
//        public void run() { //// TODO: 4/16/16 remove
//            boolean success = false;
////                String file = "mrk.usfm.txt";
//            String file = "usfm.zip";
//            ImportUsfmActivity.startActivityForResourceImport(DeveloperToolsActivity.this,file);
//        }
//    }));


    public void testValidImportMark() throws Exception {
        //given
        String source = "mrk.usfm.txt";
        Library library = AppContext.getLibrary();
        TargetLanguage targetLanguage = library.getTargetLanguage("es");
        ImportUsfm usfm = new ImportUsfm(getInstrumentation().getContext(), targetLanguage);

        //when
        boolean success = usfm.readResourceFile(source);

        //then
        assertEquals(success,true);
        String results = usfm.getResultsString();
        assertTrue(!results.isEmpty());
    }

}