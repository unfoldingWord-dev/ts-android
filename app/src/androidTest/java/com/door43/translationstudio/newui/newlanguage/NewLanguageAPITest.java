package com.door43.translationstudio.newui.newlanguage;

import android.content.pm.PackageInfo;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.core.LanguageDirection;
import com.door43.translationstudio.core.NativeSpeaker;
import com.door43.translationstudio.core.NewLanguagePackage;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.core.Resource;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationType;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.unfoldingword.gogsclient.Response;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NewLanguageAPITest {

    private NewLanguageAPI mApi;
    private String mUrl;
    private String mSourceLangID;
    private File mTempDir;

    @Before
    public void setup() {
        mApi = new NewLanguageAPI();
        mUrl = "http://td-demo.unfoldingword.org/api/questionnaire/";
        mApi.setNewLangUrl(mUrl);
        mSourceLangID = "en-x-demo2";
    }

    @After
    public void tearDown() {
        if(mTempDir != null) {
            FileUtils.deleteQuietly(mTempDir);
            mTempDir = null;
        }
    }

    @Test
    public void postQuestionnaireFromTargetDuplicateCode() throws Exception {
        //given
        final CountDownLatch signal = new CountDownLatch(1);
        NewLanguagePackage newLang = getQuestionaireAndFillAnswers(mSourceLangID);
        final JSONObject uploadSuccess = new JSONObject();
        TargetTranslation targetTranslation = createDummyTargetTranslationPackage(newLang);
        mApi.uploadAnswersToAPI(null, targetTranslation, null); // send once
        newLang.setUploaded(false); // reset to force upload next backup
        newLang.commit(mTempDir);

        //when
        mApi.uploadAnswersToAPI(null, targetTranslation, new NewLanguageAPI.OnRequestFinished() { // now send duplicate
            @Override
            public void onRequestFinished(boolean success, Response response) {
                try {
                    uploadSuccess.put("success", success);
                    if(response != null) {
                        uploadSuccess.putOpt("responseData", response.data);
                        uploadSuccess.putOpt("responseException", response.exception);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                signal.countDown();
            }
        });

        //then
        signal.await(30, TimeUnit.SECONDS);
        assertTrue(uploadSuccess.getBoolean("success"));
        assertNull(uploadSuccess.opt("responseException"));
        assertNotNull(uploadSuccess.optString("responseData"));
        assertTrue(uploadSuccess.optString("responseData").indexOf(NewLanguageAPI.DUPLICATE_KEY_ERROR) >= 0);
    }

    @Test
    public void postQuestionnaireFromTarget() throws Exception {
        //given
        final CountDownLatch signal = new CountDownLatch(1);
        NewLanguagePackage newLang = getQuestionaireAndFillAnswers(mSourceLangID);
        final JSONObject uploadSuccess = new JSONObject();
        TargetTranslation targetTranslation = createDummyTargetTranslationPackage(newLang);

        //when
        mApi.uploadAnswersToAPI(null, targetTranslation, new NewLanguageAPI.OnRequestFinished() {
            @Override
            public void onRequestFinished(boolean success, Response response) {
                try {
                    uploadSuccess.put("success", success);
                    if(response != null) {
                        uploadSuccess.putOpt("responseData", response.data);
                        uploadSuccess.putOpt("responseException", response.exception);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                signal.countDown();
            }
        });

        //then
        signal.await(30, TimeUnit.SECONDS);
        assertTrue(uploadSuccess.getBoolean("success"));
        assertNull(uploadSuccess.opt("responseException"));
        assertNotNull(uploadSuccess.optString("responseData"));
    }

    private TargetTranslation createDummyTargetTranslationPackage(NewLanguagePackage newLang) throws Exception {
        mTempDir = new File(AppContext.context().getCacheDir(), System.currentTimeMillis() + "");
        mTempDir.mkdirs();

        PackageInfo pinfo = AppContext.context().getPackageManager().getPackageInfo(AppContext.context().getPackageName(), 0);
        TargetLanguage targetLanguage = new TargetLanguage(newLang.tempLanguageCode, newLang.languageName, "uncertain", LanguageDirection.LeftToRight);
        NativeSpeaker speaker = new NativeSpeaker("testing");
        TargetTranslation targetTranslation = TargetTranslation.create(AppContext.context(),
                speaker, TranslationFormat.USFM, targetLanguage, "aae_obs_text_obs",
                TranslationType.TEXT, Resource.REGULAR_SLUG, pinfo, mTempDir);
        targetTranslation.commit();
        newLang.commit(mTempDir);
        return targetTranslation;
    }


    @Test
    public void postQuestionnaire() throws JSONException, UnsupportedEncodingException, InterruptedException {
        //given
        final CountDownLatch signal = new CountDownLatch(1);
        NewLanguagePackage newLang = getQuestionaireAndFillAnswers(mSourceLangID);
        final JSONObject uploadSuccess = new JSONObject();

        //when
        mApi.uploadAnswersToAPI(newLang, new NewLanguageAPI.OnRequestFinished() {
            @Override
            public void onRequestFinished(boolean success, Response response) {
                try {
                    uploadSuccess.put("success", success);
                    if(response != null) {
                        uploadSuccess.putOpt("responseData", response.data);
                        uploadSuccess.putOpt("responseException", response.exception);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                signal.countDown();
            }
        });

        //then
        signal.await(30, TimeUnit.SECONDS);
        assertTrue(uploadSuccess.getBoolean("success"));
        assertNull(uploadSuccess.opt("responseException"));
        assertNotNull(uploadSuccess.optString("responseData"));
    }

    @Test
    public void getQuestionnaireDemoUrl() throws JSONException {
        //given

        //when
        Response response = mApi.readQuestionnaireFromServer();

        //then
        assertNotNull(response);
        verifyQuestionnaireID(response);
    }

    private void verifyQuestionnaireID(Response response) throws JSONException {
        JSONObject questionnaire = mApi.parseServerFetchResponse(response);
        assertTrue(questionnaire.getJSONArray("languages").getJSONObject(0).getInt("questionnaire_id") > 0);
    }

    @Test
    public void getQuestionnaireInvalidURL() throws JSONException {
        //given
        mApi.setNewLangUrl(NewLanguageAPI.NEW_LANGUAGE_URL_DEBUG + "dummy/");

        //when
        Response response = mApi.readQuestionnaireFromServer();

        //then
        assertNotNull(response);
        assertNull(mApi.parseServerFetchResponse(response));
    }

    @Test
    public void getQuestionnaireRegularUrl() throws JSONException {
        //given
        mApi.setNewLangUrl(NewLanguageAPI.NEW_LANGUAGE_URL);

        //when
        Response response = mApi.readQuestionnaireFromServer();

        //then
        assertNotNull(response);
        verifyQuestionnaireID(response);
    }

    private NewLanguagePackage getQuestionaireAndFillAnswers(String sourceLangID) throws JSONException {

        String questionnaire = getQuestions();
        JSONObject questions = mApi.readQuestionnaireIntoPages(null,questionnaire, sourceLangID);
        assertNotNull("could not find questions for " + sourceLangID, questions);
        long id = NewLanguageActivity.getQuestionnaireID(questions);
        List<List<NewLanguageQuestion>> questionPages = new ArrayList<>();
        NewLanguageActivity.getQuestionPages(questionPages, questions);
        List<NewLanguageQuestion> mergedQuestions = NewLanguageActivity.mergePagesOfNewLanguageQuestions(questionPages);
        for (NewLanguageQuestion newLanguageQuestion : mergedQuestions) {
            long qid = newLanguageQuestion.id;
            newLanguageQuestion.answer = "Answer-" + qid;
        }
        return NewLanguagePackage.newInstance(id, mergedQuestions);
    }

    private String getQuestions() throws JSONException {
        Response response = mApi.readQuestionnaireFromServer();
        return response.toString();
    }
}