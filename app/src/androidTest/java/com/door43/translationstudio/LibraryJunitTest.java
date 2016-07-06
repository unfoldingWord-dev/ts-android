package com.door43.translationstudio;


import android.util.Log;

import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.ProjectCategory;
import com.door43.translationstudio.core.Questionnaire;
import com.door43.translationstudio.core.TargetLanguage;

import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Andrew on 6/29/2016.
 */
public class LibraryJunitTest {
    private Library mLibrary;
    private String TAG="LibraryJunitTest";
    @Before
    public void setup(){
        mLibrary = App.getLibrary();
        assertTrue(mLibrary.exists());
//        try {
//            App.deployDefaultLibrary();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        mLibrary = App.getLibrary();

//        // NOTE: this will fail when first updating the db version
//        assertTrue(mLibrary.exists());

    }
    @After
    public void tearDown(){

    }
    @Test
    public void getQuestionaire(){
        Questionnaire [] questionnaires=mLibrary.getQuestionnaires();
        assertTrue(questionnaires.length>0);
    }

    @Test
    public void checker(){
        int num=7;
        assertThat(num, is(8));
//        assertEquals();
//        Log.e(TAG,"test results: ");
    }

    @Test
    public void getLanguages(){
        TargetLanguage[] targetLanguages=mLibrary.getTargetLanguages();

        Log.e(TAG,"getTargetLanguages: "+targetLanguages.length);

        for(TargetLanguage tl:targetLanguages){
            Log.e(TAG,"Target Language id: "+tl.getId()+", region: "+tl.region+", name: "+tl.name+", code: "+tl.code);
        }
    }
    @Test
    public void getProjectCategory(){
        ProjectCategory [] pC= mLibrary.getProjectCategories("");
        assertTrue(pC.length>0);
        for(ProjectCategory projCat:pC){
            Log.e(TAG,"projectCategory: "+projCat.getId());
        }
//        Log.e(TAG,"Project category: "+pC[0].getId());
    }
}
