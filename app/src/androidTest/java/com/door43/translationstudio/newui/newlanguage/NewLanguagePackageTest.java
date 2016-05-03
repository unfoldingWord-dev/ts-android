package com.door43.translationstudio.newui.newlanguage;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.core.NewLanguagePackage;
import com.door43.translationstudio.core.NewLanguageQuestion;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by blm on 5/2/16.
 */
public class NewLanguagePackageTest extends InstrumentationTestCase {

    List<NewLanguageQuestion> mQuestions;

    public void test01ToJson() throws Exception {
        //given
        int questionaireID = 1001;
        int numQuestions = 15;
        mQuestions = generateQuestions(numQuestions);
        NewLanguagePackage newLanguagePackage = NewLanguagePackage.newInstance(questionaireID, mQuestions);

        //when
        String jsonStr = newLanguagePackage.toJson().toString();

        //then
        NewLanguagePackage restored = NewLanguagePackage.parse(jsonStr);
        assertPackagesEqual(newLanguagePackage, restored);
    }

    private void assertPackagesEqual(NewLanguagePackage expected, NewLanguagePackage actual) throws JSONException {
        assertEquals("questionaireID", expected.questionaireID,actual.questionaireID);
        assertEquals("tempLanguageCode", expected.tempLanguageCode,actual.tempLanguageCode);
        assertEquals("languageName", expected.languageName,actual.languageName);
        assertEquals("requestID", expected.requestID,actual.requestID);
        assertEquals("requester", expected.requester,actual.requester);
        assertEquals("app", expected.app,actual.app);
        assertEquals("same number of answers", expected.answersJson.length(),actual.answersJson.length());
        for (int i = 0; i < expected.answersJson.length(); i++) {
            JSONObject expectedAnswer = expected.answersJson.getJSONObject(i);
            JSONObject actualAnswer = actual.answersJson.getJSONObject(i);
            assertEquals("ID", expectedAnswer.getInt(NewLanguagePackage.API_QUESTION_ID), actualAnswer.getInt(NewLanguagePackage.API_QUESTION_ID));
            assertEquals("answer", expectedAnswer.getString(NewLanguagePackage.QUESTION_ANSWER), actualAnswer.getString(NewLanguagePackage.QUESTION_ANSWER));
        }
    }

    private List<NewLanguageQuestion>  generateQuestions(int numQuestions) {
        List<NewLanguageQuestion> questions = new ArrayList<>();
        for (int i = 0; i < numQuestions; i++) {
            boolean required = i % 3 == 0;
            NewLanguageQuestion.QuestionType type = (i % 4 == 0) ? NewLanguageQuestion.QuestionType.INPUT_TYPE_BOOLEAN : NewLanguageQuestion.QuestionType.INPUT_TYPE_STRING;
            long conditionalID = i > 0 ? i - 1 : NewLanguageQuestion.NO_DEPENDENCY;
            NewLanguageQuestion question = new NewLanguageQuestion(i, "Q-" + i, "H-" + i, "A-" + i,
                    type, required, "", conditionalID);
            questions.add(question);
        }
        return questions;
    }


}
