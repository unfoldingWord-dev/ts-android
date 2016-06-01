package com.door43.translationstudio.newui.newlanguage;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.core.NewLanguageQuestion;

import org.json.JSONObject;

/**
 * Created by blm on 5/2/16.
 */
public class NewLanguageQuestionTest  extends InstrumentationTestCase {

    public void test01ToJson()  {
        //given
        boolean required = true;
        NewLanguageQuestion.QuestionType type = NewLanguageQuestion.QuestionType.INPUT_TYPE_BOOLEAN;
        long conditionalID = NewLanguageQuestion.NO_DEPENDENCY;
        String questionText = "QQ";
        String hintText = "HH";
        String answerText = "AA";
        int id = 111;
        String query = "query";
        NewLanguageQuestion question = new NewLanguageQuestion(id, questionText, hintText, answerText,
                type, required, query, conditionalID);

        //when
        JSONObject json = question.toJson();

        //then
        NewLanguageQuestion actual = NewLanguageQuestion.parse(json);
        assertQuestionsEqual(question, actual);
    }

    public void test02ToJson()  {
        //given
        boolean required = false;
        NewLanguageQuestion.QuestionType type = NewLanguageQuestion.QuestionType.INPUT_TYPE_STRING;
        long conditionalID = 15;
        String questionText = "Q";
        String hintText = "H";
        String answerText = "A";
        int id = 11111;
        String query = "query2";
        NewLanguageQuestion question = new NewLanguageQuestion(id, questionText, hintText, answerText,
                type, required, query, conditionalID);

        //when
        JSONObject json = question.toJson();

        //then
        NewLanguageQuestion actual = NewLanguageQuestion.parse(json);
        assertQuestionsEqual(question, actual);
    }

    private void assertQuestionsEqual(NewLanguageQuestion expected, NewLanguageQuestion actual) {
        assertEquals("id", expected.id, actual.id);
        assertEquals("question", expected.question, actual.question);
        assertEquals("helpText", expected.helpText, actual.helpText);
        assertEquals("answer", expected.answer, actual.answer);
        assertEquals("required", expected.required, actual.required);
        assertEquals("type", expected.type, actual.type);
        assertEquals("query", expected.query, actual.query);
        assertEquals("reliantQuestionId", expected.reliantQuestionId, actual.reliantQuestionId);
    }

}
