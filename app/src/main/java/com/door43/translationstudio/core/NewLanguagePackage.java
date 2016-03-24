package com.door43.translationstudio.core;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.util.Security;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
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
    public static final String LANGUAGE_NAME = "language_name";

    public static final String QUESTION_ANSWER = "answer";
    public static final String QUESTION_ID = "question_id";
    public static final String TAG = NewLanguagePackage.class.getSimpleName();
    public static final String REGION = "region";

    private static final String TEMP_LANGUAGE_PREFIX = "qaa-x-";
    public static final String NEW_LANGUAGES_FOLDER = "new_languages";
    public static final String NEW_LANGUAGE_FILE_EXTENSION = ".json";
    private static int NEW_LANGUAGE_NAME_ID = 1;

    final public long questionaireID;
    final public String tempLanguageCode;
    final public String languageName;
    final public String region;
    final public String requestID;
    final public JSONArray answersJson;

    NewLanguagePackage(long questionaireID, String tempLanguageCode, String languageName, String requestID, String region, JSONArray answersJson) {
        this.questionaireID = questionaireID;
        this.tempLanguageCode = tempLanguageCode;
        this.requestID = requestID;
        this.answersJson = answersJson;
        this.region = region;
        this.languageName = languageName;
    }

    /**
     * create a new language instance
     *
     * @param questions
     * @param region
     * @return
     * @throws JSONException
     */
    public static NewLanguagePackage newInstance(long questionaireID, List<NewLanguageQuestion> questions, String region) throws JSONException {

        JSONArray answers = questionsToJsonAnswers(questions);
        String requestID = UUID.randomUUID().toString();
        String tempLanguageCode = getNewLanguageCode();

        JSONObject nameAnswer = getQuestionForID(answers, NewLanguagePackage.NEW_LANGUAGE_NAME_ID);
        if(nameAnswer != null) {
            String nLangName = nameAnswer.getString(NewLanguagePackage.QUESTION_ANSWER);
            if(!nLangName.isEmpty()) {
                NewLanguagePackage newLang = new NewLanguagePackage(questionaireID, tempLanguageCode, nLangName, requestID, region, answers);
                return newLang;
            }
        }

        return null;
    }

    /**
     * parse JSON string data into new object.  Returns null if error.
     *
     * @param jsonStr
     * @return
     */
    public static NewLanguagePackage parse(String jsonStr) {
        try {
            JSONObject newLanguageData = new JSONObject(jsonStr);

            String requestID = newLanguageData.getString(REQUEST_ID);
            String tempLanguageCode = newLanguageData.getString(TEMP_CODE);
            String languageName = newLanguageData.getString(LANGUAGE_NAME);
            long questionaireID = newLanguageData.getLong(QUESTIONAIRE_ID);
            JSONArray answers = newLanguageData.getJSONArray(ANSWERS);

            String region = "uncertain";
            if (newLanguageData.has(REGION)) {
                region = newLanguageData.getString(REGION);
            }

            return new NewLanguagePackage(questionaireID, tempLanguageCode, languageName, requestID, region, answers);

        } catch (Exception e) {
            Logger.e(TAG, "Failed to parse data", e);
        }
        return null;
    }

    /**
     * return json data for object
     *
     * @return
     * @throws JSONException
     */
    public JSONObject toJson() throws JSONException {
        JSONObject newLanguageData = new JSONObject();
        newLanguageData.put(REQUEST_ID, requestID);
        newLanguageData.put(TEMP_CODE, tempLanguageCode);
        newLanguageData.put(LANGUAGE_NAME, languageName);
        newLanguageData.put(QUESTIONAIRE_ID, questionaireID);
        newLanguageData.put(ANSWERS, answersJson);
        newLanguageData.put(REGION, region);
        return newLanguageData;
    }

    /**
     * save object json data to new_language.json in folder
     *
     * @param destinationFolder
     * @return
     */
    public boolean commit(File destinationFolder) {

        File newLanguageFile = new File(destinationFolder, "new_language.json");
        return commitToFile(newLanguageFile);
    }

    /**
     * save object json data to destination file
     *
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
            Logger.e(TAG, "Could not write to: " + path, e);
            return false;
        }

        return true;
    }

    /**
     * read json data from new_language.json in folder and parse into object
     *
     * @param sourceFolder
     * @return
     */
    public static NewLanguagePackage open(File sourceFolder) {

        File newLanguageFile = new File(sourceFolder, "new_language.json");
        return openFile(newLanguageFile);
    }

    /**
     * read json data from destination file and parse into object
     *
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
            Logger.e(TAG, "Could not open file: " + path, e);
        }

        return null;
    }

    /**
     * look up specific question in answers by question id
     *
     * @param id
     * @return
     * @throws JSONException
     */
    public JSONObject getQuestionForID(long id) throws JSONException {

        return getQuestionForID(answersJson, id);
    }

    /**
     * look up specific question in answers by question id
     *
     * @param id
     * @return
     * @throws JSONException
     */
    public static JSONObject getQuestionForID(JSONArray answersJson, long id) throws JSONException {

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
     *
     * @param questions
     * @return
     * @throws JSONException
     */
    private static JSONArray questionsToJsonAnswers(List<NewLanguageQuestion> questions) throws JSONException {
        JSONArray questionsJson = new JSONArray();

        for (NewLanguageQuestion question : questions) {
            JSONObject answer = new JSONObject();
            answer.put(QUESTION_ID, question.id);
            answer.put(QUESTION_ANSWER, question.answer);
            answer.put("question", question.question); // useful for debugging
            questionsJson.put(answer);
        }

        return questionsJson;
    }

    /**
     * return true if this dependency is met.
     * If editText then answer must be not null or empty.
     * If checkbos then answer must be true.
     *
     * @param dependency
     * @return
     */
    static public boolean isDependencyMet(NewLanguageQuestion dependency) {
        boolean enable = true;
        if (dependency != null) {
            if (!dependency.isAnswerEmpty()) {

                if (dependency.type == NewLanguageQuestion.QuestionType.INPUT_TYPE_BOOLEAN) {
                    enable = dependency.isBooleanAnswerTrue();
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
     * get new language name from answers
     * @return
     */
    public static File getNewLanguageFolder() {
        File folder = AppContext.getTranslator().getPath();
        File dataPath = new File(folder, "../" + NEW_LANGUAGES_FOLDER);
        return dataPath;
    }

    /**
     * check new_language folder to see if we have this language saved
     * @param tempLanguageCode
     * @return
     */
    public static NewLanguagePackage getNewLanguageFromFileSystem(final String tempLanguageCode) {
        NewLanguagePackage newLang = null;
        final List<File> matches = new ArrayList<>();
        File folder = getNewLanguageFolder();
        folder.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                String fileNameLC = filename.toLowerCase();
                int pos = fileNameLC.indexOf(NEW_LANGUAGE_FILE_EXTENSION);
                if(pos > 0) {
                    pos = fileNameLC.indexOf(tempLanguageCode);
                    if(pos > 0) {
                        File path = new File(dir, filename);
                        matches.add(path);
                    }
                }

                return false;
            }
        });

        if(matches.size() > 0) {
            File path = matches.get(0);
            newLang = openFile(path);
        }

        return newLang;
    }

    /**
     * generate a new language code
     *
     * @return
     */
    public static String getNewLanguageCode() {
        String languageCode;
        languageCode = TEMP_LANGUAGE_PREFIX;

        String androidId = AppContext.udid();

        long ms = (new Date()).getTime();
        String uniqueString = androidId + ms;
        String sha1Value = Security.sha1(uniqueString);
        languageCode += sha1Value.substring(0, 6);
        return languageCode.toLowerCase();
    }

    /**
     * looks to see if this language code is a temp code
     * @param languageCode
     * @return
     */
    public static boolean isNewLanguageCode(String languageCode) {
        if (null == languageCode) {
            return false;
        }

        int pos = languageCode.toLowerCase().indexOf(TEMP_LANGUAGE_PREFIX.toLowerCase());
        return (0 == pos); // language code most start with temp prefix
    }
}