package com.door43.translationstudio.newui.newlanguage;

import android.test.InstrumentationTestCase;

import com.door43.translationstudio.core.QuestionnaireQuestion;

import org.json.JSONObject;

/**
 * Created by blm on 5/2/16.
 */
public class QuestionnaireQuestionTest extends InstrumentationTestCase {

    public void test01ToJson()  {
        //given
        boolean required = true;
        QuestionnaireQuestion.InputType type = QuestionnaireQuestion.InputType.Boolean;
        long conditionalID = -1;
        String questionText = "QQ";
        String hintText = "HH";
        int id = 111;
        QuestionnaireQuestion question = new QuestionnaireQuestion(id, questionText, hintText,
                type, required, conditionalID, 0);

        //when
        JSONObject json = question.toJson();

        //then
        QuestionnaireQuestion actual = QuestionnaireQuestion.generate(json);
        assertQuestionsEqual(question, actual);
    }

    public void test02ToJson()  {
        //given
        boolean required = false;
        QuestionnaireQuestion.InputType type = QuestionnaireQuestion.InputType.String;
        long conditionalID = 15;
        String questionText = "Q";
        String hintText = "H";
        int id = 11111;
        QuestionnaireQuestion question = new QuestionnaireQuestion(id, questionText, hintText,
                type, required, conditionalID, 0);

        //when
        JSONObject json = question.toJson();

        //then
        QuestionnaireQuestion actual = QuestionnaireQuestion.generate(json);
        assertQuestionsEqual(question, actual);
    }

    private void assertQuestionsEqual(QuestionnaireQuestion expected, QuestionnaireQuestion actual) {
        assertEquals("id", expected.id, actual.id);
        assertEquals("question", expected.question, actual.question);
        assertEquals("helpText", expected.helpText, actual.helpText);
//        assertEquals("answer", expected.answer, actual.answer);
        assertEquals("required", expected.required, actual.required);
        assertEquals("type", expected.type, actual.type);
        assertEquals("reliantQuestionId", expected.reliantQuestionId, actual.reliantQuestionId);
    }

}
