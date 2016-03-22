package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by blm on 3/17/16.
 */
public class NewLanguagePackage {
    public static final String REQUEST_ID = "request_id";
    public static final String TEMP_CODE = "temp_code";
    public static final String QUESTIONAIRE_ID = "questionaire_id";
    public static final String ANSWERS = "answers";

    public static final String QUESTION_ANSWER = "answer";
    public static final String QUESTION_ID = "question_id";
    public static final String TAG = NewLanguagePackage.class.getSimpleName();
    public static final String REGION = "region";

    public static final String TRUE_STR = "YES";
    public static final String FALSE_STR = "NO";

    public static int NEW_LANGUAGE_NAME_ID = 100;

    final public long questionaireID;
    final public String tempLanguageCode;
    final public String region;
    final public String requestID;
    final public JSONArray answersJson;

    NewLanguagePackage(long questionaireID, String tempLanguageCode, String requestID, String region, JSONArray answersJson) {
        this.questionaireID = questionaireID;
        this.tempLanguageCode = tempLanguageCode;
        this.requestID = requestID;
        this.answersJson = answersJson;
        this.region = region;
    }

    /**
     * create a new language instance
     * @param questions
     * @param region
     * @return
     * @throws JSONException
     */
    public static NewLanguagePackage generateNew(long questionaireID, List<NewLanguageQuestion> questions, String region) throws JSONException {

        JSONArray answers = questionsToJsonAnswers(questions);
        String requestID = UUID.randomUUID().toString();
        String tempLanguageCode = getNewLanguageCode();

        return new NewLanguagePackage(questionaireID,tempLanguageCode, requestID, region, answers);
    }

    /**
     * parse JSON string data into new object.  Returns null if error.
     * @param jsonStr
     * @return
     */
    public static NewLanguagePackage parse(String jsonStr) {
        try {
            JSONObject newLanguageData = new JSONObject(jsonStr);

            String requestID = newLanguageData.getString(REQUEST_ID);
            String tempLanguageCode = newLanguageData.getString(TEMP_CODE);
            long questionaireID = newLanguageData.getLong(QUESTIONAIRE_ID);
            JSONArray answers = newLanguageData.getJSONArray(ANSWERS);

            String region = "uncertain";
            if(newLanguageData.has(REGION)) {
                region = newLanguageData.getString(REGION);
            }

            return new NewLanguagePackage(questionaireID,tempLanguageCode, requestID, region, answers);

        } catch (Exception e) {
            Logger.e(TAG, "Failed to parse data",e);
        }
        return null;
    }

    /**
     * return json data for object
     * @return
     * @throws JSONException
     */
    public JSONObject toJson() throws JSONException{
        JSONObject newLanguageData = new JSONObject();
        newLanguageData.put(REQUEST_ID, requestID);
        newLanguageData.put(TEMP_CODE, tempLanguageCode);
        newLanguageData.put(QUESTIONAIRE_ID, questionaireID);
        newLanguageData.put(ANSWERS, answersJson);
        newLanguageData.put(REGION, region);
        return newLanguageData;
    }

    /**
     * save object json data to new_language.json in folder
     * @param destinationFolder
     * @return
     */
    public boolean commit(File destinationFolder) {

        File newLanguageFile = new File(destinationFolder,"new_language.json");
        return commitToFile(newLanguageFile);
    }

    /**
     * save object json data to destination file
     * @param destinationFile
     * @return
     */
    public boolean commitToFile(File destinationFile) {
        String path = "(null)";
        try {
            String outData = toJson().toString();
            path = destinationFile.toString();
            FileUtils.write(destinationFile, outData);

        } catch (Exception e) {
            Logger.e(TAG, "Could not write to: " + path,e);
            return false;
        }

        return true;
    }

    /**
     * read json data from new_language.json in folder and parse into object
     * @param sourceFolder
     * @return
     */
    public static NewLanguagePackage open(File sourceFolder) {

        File newLanguageFile = new File(sourceFolder,"new_language.json");
        return openFile(newLanguageFile);
    }

    /**
     * read json data from destination file and parse into object
     * @param sourceFile
     * @return
     */
    public static NewLanguagePackage openFile(File sourceFile) {
        String path = "(null)";
        try {
            path = sourceFile.toString();
            String jsonData = FileUtils.readFileToString(sourceFile);
            return parse(jsonData);

        } catch (Exception e) {
            Logger.e(TAG, "Could not write to: " + path,e);
        }

        return null;
    }

    /**
     * look up specific question in answers by question id
     * @param id
     * @return
     * @throws JSONException
     */
    public JSONObject getAnswerForID(long id) throws JSONException {

        for (int i = 0; i < answersJson.length(); i++) {
            JSONObject answer = answersJson.getJSONObject(i);
            long qid = answer.getLong(QUESTION_ID);
            if (qid == id) {
                return answer;
            }
        }

        return null;
    }

    /**
     * extracts answer data from list of questions.  This is in new_language.json format
     * @param questions
     * @return
     * @throws JSONException
     */
    private static JSONArray questionsToJsonAnswers(List<NewLanguageQuestion> questions) throws JSONException {
        JSONArray questionsJson = new JSONArray();

        for (NewLanguageQuestion question : questions) {
            JSONObject answer = new JSONObject();
            answer.put(QUESTION_ID,question.id);
            answer.put(QUESTION_ANSWER,question.answer);
            answer.put("question",question.question); // useful for debugging
            questionsJson.put(answer);
        }

        return questionsJson;
    }

    /**
     * test if asnwer to question is empty
     * @param question
     * @return
     */
    static public boolean isAnswerEmpty(NewLanguageQuestion question) {
        return (null == question.answer ) || question.answer.isEmpty();
    }

    /**
     * test to see if checkbox is true (checked), null answer is false
     * @param question
     * @return
     */
    static public boolean isCheckBoxAnswerTrue(NewLanguageQuestion question) {
        if(question != null) {
            return TRUE_STR.equals(question.answer);
        }
        return false;
    }

    /**
     * get answer string for checkbox state
     * @param isChecked
     * @return
     */
    static public String getCheckBoxAnswer(boolean isChecked) {
        return isChecked ? TRUE_STR : FALSE_STR;
    }

    /**
     * return true if this dependency is met.
     * If editText then answer must be not null or empty.
     * If checkbos then answer must be true.
     * @param dependency
     * @return
     */
    static public boolean isDependencyMet(NewLanguageQuestion dependency) {
        boolean enable = true;
        if(dependency != null) {
            if(!isAnswerEmpty(dependency)) {

                if(dependency.type == NewLanguageQuestion.QuestionType.INPUT_TYPE_BOOLEAN) {
                    enable = NewLanguagePackage.isCheckBoxAnswerTrue(dependency);
                } else {
                    enable = true;
                }
            } else {
                enable = false;
            }
        }
        return enable;
    }

    /**
     * generate a new language code
     * @return
     */
    public static String getNewLanguageCode() {
        String languageCode;
        languageCode= "qaa-x-";

        String androidId = AppContext.udid();

        long ms = (new Date()).getTime();
        String uniqueString = androidId + ms;
        String sha1Value = getSha1Hex(uniqueString);
        languageCode += sha1Value.substring(0, 6);
        return languageCode.toLowerCase();
    }

    /**
     * generate sha1 for string
     * @param clearString
     * @return
     */
    public static String getSha1Hex(String clearString)
    {
        try
        {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(clearString.getBytes("UTF-8"));
            byte[] bytes = messageDigest.digest();
            StringBuilder buffer = new StringBuilder();
            for (byte b : bytes)
            {
                buffer.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return buffer.toString();
        }
        catch (Exception ignored)
        {
            ignored.printStackTrace();
            return null;
        }
    }
}
