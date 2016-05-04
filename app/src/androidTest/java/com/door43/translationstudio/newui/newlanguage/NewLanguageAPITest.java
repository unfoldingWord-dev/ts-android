package com.door43.translationstudio.newui.newlanguage;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.door43.translationstudio.core.NewLanguagePackage;
import com.door43.translationstudio.core.NewLanguageQuestion;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.unfoldingword.gogsclient.Response;

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

    @Before
    public void setup() {
        mApi = new NewLanguageAPI();
        mUrl = "http://td-demo.unfoldingword.org/api/questionnaire/";
        mApi.setNewLangUrl(mUrl);
        mSourceLangID = "en-x-demo2";

    }

    @Test
    public void postQuestionnaireLowLevel() throws JSONException, UnsupportedEncodingException, InterruptedException {
        //given
        final CountDownLatch signal = new CountDownLatch(1);
        NewLanguagePackage newLang = getQuestionaireAndFillAnswers(mSourceLangID);
        final JSONObject uploadSuccess = new JSONObject();

        //when
        mApi.postToApiIfNeeded(newLang, new NewLanguageAPI.OnRequestFinished() {
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
        JSONObject response = mApi.readQuestionnaireFromServer();

        //then
        assertNotNull(response);
        assertTrue(response.getJSONArray("languages").getJSONObject(0).getInt("questionnaire_id") > 0);
    }

    @Test
    public void getQuestionnaireRegularUrl() throws JSONException {
        //given
        mApi.setNewLangUrl(NewLanguageAPI.NEW_LANGUAGE_URL);

        //when
        JSONObject response = mApi.readQuestionnaireFromServer();

        //then
        assertNotNull(response);
        assertTrue(response.getJSONArray("languages").getJSONObject(0).getInt("questionnaire_id") > 0);
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
        JSONObject response = mApi.readQuestionnaireFromServer();
        return response.toString();
    }
}