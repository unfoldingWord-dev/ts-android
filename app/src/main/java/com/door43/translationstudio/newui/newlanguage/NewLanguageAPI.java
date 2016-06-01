package com.door43.translationstudio.newui.newlanguage;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.view.View;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguagePackage;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.dialogs.CustomAlertDialog;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unfoldingword.gogsclient.Response;
import org.unfoldingword.gogsclient.User;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by blm on 3/21/16.
 */
public class NewLanguageAPI {
    public static final String INPUT_TYPE_STRING = "str";
    public static final String INPUT_TYPE_BOOLEAN = "boolean";

    public static final String QUESTIONNAIRE_ID_KEY = "id";
    public static final String QUESTIONNAIRE_META_KEY = "meta";
    public static final String QUESTIONAIRE_DATA_KEY = "data";
    public static final String QUESTIONNAIRE_ORDER_KEY = "order";

    public static final String TAG = NewLanguageAPI.class.getSimpleName();
    public static final int QUESTIONS_PER_PAGE = 3;

    public static final String API_READ_LANGUAGES = "languages";
    public static final String API_READ_QUESTIONS_LANGUAGE_SLUG = "slug";
    public static final String API_READ_QUESTION_ID = "id";
    public static final String API_READ_QUESTION_TEXT = "text";
    public static final String API_READ_HELP = "help";
    public static final String API_READ_INPUT_TYPE = "input_type";
    public static final String API_READ_SORT = "sort";
    public static final String API_READ_DEPENDS_ON = "depends_on";
    public static final String API_READ_QUESTIONS = "questions";
    public static final String API_READ_REQUIRED = "required";
    public static final String NEW_LANGUAGE_URL  = "http://td.unfoldingword.org/api/questionnaire/";
    public static final String NEW_LANGUAGE_URL_DEBUG = "http://td-demo.unfoldingword.org/api/questionnaire/";
    public static final String DUPLICATE_KEY_ERROR = "duplicate key value violates unique constraint \\\"td_templanguage_code_key\\\"";

    private int readTimeout = 5000;
    private int connectionTimeout = 5000;
    private Response lastResponse = null;

    private JSONArray mPageData;
    private JSONArray mPageMeta;

    private JSONArray mData;
    private JSONArray mMeta;

    private String mNewLangUrl = NEW_LANGUAGE_URL;


    public NewLanguageAPI() {

    }

    public void setNewLangUrl(String mNewLangUrl) {
        this.mNewLangUrl = mNewLangUrl;
    }

    /**
     * read and parse the questionnaire into JSONObject of pages of questsion
     * @param context
     * @param questionsJsonStr - optional questions data - if null then data is fetched from API
     * @param sourceLangID
     * @return
     */
    public JSONObject readQuestionnaireIntoPages(Context context, String questionsJsonStr, String sourceLangID) {
        try {
            if(questionsJsonStr == null) {
                InputStream is = context.getResources().getAssets().open("newLanguageQuestionaire.json"); // // TODO: 4/28/16 replace with library call
                if (is != null) {
                    questionsJsonStr = IOUtils.toString(is);
                }
            }
            return parseQuestionnaireIntoPages(questionsJsonStr, sourceLangID);

        } catch (Exception e) {
            Logger.e(TAG,"Error getting questionnaire",e);
        }

        return null;
    }

    /**
     * read and parse the questionnaire
     * @return
     */
    public Response readQuestionnaireFromServer() {

        return doRequest(mNewLangUrl, null, null, "GET");
    }

    /**
     * extract questionnaire from response.data
     * @param response
     * @return
     */
    public JSONObject parseServerFetchResponse(Response response) {
        if(response != null) {
            try {
                JSONObject questionnaire = new JSONObject(response.data);

                //make sure it has list of languages
                JSONArray languages = questionnaire.getJSONArray(API_READ_LANGUAGES);

                return questionnaire;

            } catch (Exception e) {
                Logger.e(TAG, "Error parsing questionnaire", e);
            }
        }
        return null;
    }

    /**
     * generate the data to be posted to new language API
     * @param pkg
     * @return
     */
    public static HashMap<String, String> getPostData(NewLanguagePackage pkg)  {
        try {
            HashMap<String, String> apis = new HashMap<>();

            apis.put(NewLanguagePackage.API_REQUEST_ID, AppContext.udid());
            apis.put(NewLanguagePackage.API_TEMP_CODE, pkg.tempLanguageCode);
            apis.put(NewLanguagePackage.API_QUESTIONNAIRE_ID, pkg.questionaireID+"");
            apis.put(NewLanguagePackage.API_APP, pkg.app);
            apis.put(NewLanguagePackage.API_REQUESTER, pkg.requester);
            JSONArray answers = new JSONArray();
            for (int i = 0; i < pkg.answersJson.length(); i++) {
                JSONObject answer = pkg.answersJson.getJSONObject(i);
                JSONObject obj = new JSONObject();
                obj.put(NewLanguagePackage.API_QUESTION_ID, answer.getInt(NewLanguagePackage.API_QUESTION_ID));
                obj.put(NewLanguagePackage.API_ANSWER, answer.optString(NewLanguagePackage.QUESTION_ANSWER,""));
                answers.put(obj);
            }
            apis.put(NewLanguagePackage.API_ANSWERS, answers.toString());
            return apis;

        } catch (Exception e) {
            Logger.e(TAG, "Could not create API post data", e);
        }

        return null;
    }

    /**
     * parse API new language data into JSON object of pages.  Returns null if error.
     *
     * @param jsonStr
     * @param sourceLangID
     * @return
     */
    private JSONObject parseQuestionnaireIntoPages(String jsonStr, String sourceLangID) {

        try {
            JSONObject apiData = new JSONObject(jsonStr);
            JSONArray languages = apiData.getJSONArray(API_READ_LANGUAGES);

            if(languages != null) {
                for (int i = 0; i < languages.length(); i++) {
                    JSONObject language = languages.getJSONObject(i);
                    String langSlug = language.getString(API_READ_QUESTIONS_LANGUAGE_SLUG);
                    String questionaireID = language.getString(NewLanguagePackage.API_QUESTIONNAIRE_ID);
                    if(sourceLangID.equals(langSlug)) {
                        JSONArray questionsStr = language.getJSONArray(API_READ_QUESTIONS);
                        return splitQuestionsIntoPages(Integer.valueOf(questionaireID), questionsStr);
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to parse data", e);
        }
        return null;
    }

    /**
     * take the long list of questions and split into small pages
     *
     * @param questionaireID
     * @param questions
     * @return
     */
    private JSONObject splitQuestionsIntoPages(int questionaireID, JSONArray questions) {
        JSONObject questionnaire = new JSONObject();
        mData = new JSONArray();
        mMeta = new JSONArray();
        
        try {
            final boolean required = true;
            final boolean not_required = false;
            
            mPageData = new JSONArray();
            mPageMeta = new JSONArray();

            // group dependencies
            List<JSONObject> questionList = new ArrayList<>();
            for (int j = 0; j < questions.length(); j++) {
                JSONObject question = questions.getJSONObject(j);
                int sort = Integer.valueOf(question.getString(API_READ_SORT));

                while(questionList.size() <= sort) {  // make sure item will fit
                    questionList.add(null);
                }

                questionList.set(sort, question);
            }

            // remove missing items
            int j = 0;
            while ( j < questionList.size() ) {
                JSONObject question = questionList.get(j);
                if( null == question) {
                    questionList.remove(j);
                } else {
                    j++;
                }
            }

            for ( j = 0; j < questionList.size(); j++) {
                JSONObject question = questionList.get(j);

                int id = Integer.valueOf(question.getString(API_READ_QUESTION_ID));

                int k = j + 1;
                JSONObject nextQuestion = getDependentQuestion( questionList, id, k);
                if(nextQuestion != null) {

                    if(mPageData.length() > 1) {
                        pushPage();
                    }

                    addQuestion(question);
                    addQuestion(nextQuestion);
                    int lastPos = k;
                    lastPos = getDependentQuestions(questionList, id, k+1, lastPos);
                    j = lastPos; //skip over the dependent questions
                    continue;
                }

                addQuestion(question);

                if(mPageData.length() >= QUESTIONS_PER_PAGE) {
                    pushPage();
                }
            }

            if(mPageData.length() > 0) {
                pushPage();
            }

            questionnaire.put(QUESTIONNAIRE_ID_KEY, questionaireID);
            JSONObject meta = new JSONObject();
            meta.put(QUESTIONNAIRE_ORDER_KEY, mMeta);
            questionnaire.put(QUESTIONNAIRE_META_KEY, meta);
            questionnaire.put(QUESTIONAIRE_DATA_KEY, mData);
            return questionnaire;

        } catch (Exception e) {
            Logger.e(TAG,"Error creating questionnaire",e);
        }

        return null;
    }

    /**
     * find the dependent questions that follow question id
     *
     * @param questionList
     * @param id
     * @param k
     * @param lastPos
     * @return
     * @throws Exception
     */
    private int getDependentQuestions(List<JSONObject> questionList, int id, int k, int lastPos) throws Exception {
        for (; k < questionList.size(); k++) {
            JSONObject nextQuestion = getDependentQuestion( questionList, id, k);
            if(nextQuestion != null) {
                lastPos = k;
                addQuestion(nextQuestion);
            } else {
                break;
            }
        }

        pushPage();
        return lastPos;
    }

    /**
     * get the question if it is dependent on question id.  Otherwise return null.
     *
     * @param questionList
     * @param id
     * @param pos
     * @return
     * @throws Exception
     */
    private JSONObject getDependentQuestion(List<JSONObject> questionList, int id, int pos) throws Exception {
        if(pos >= questionList.size()) {
            return null;
        }

        JSONObject questionNext = questionList.get(pos);

        // find adjacent dependencies
        long dependsOn = questionNext.optLong(API_READ_DEPENDS_ON, -1);
        if (id != dependsOn) {
            return null;
        }

        return questionNext;
    }

    /**
     * add question in json to list
     * @param question
     * @throws JSONException
     */
    private void addQuestion(JSONObject question) throws JSONException {
        String id = question.getString(API_READ_QUESTION_ID);
        String questionString = question.getString(API_READ_QUESTION_TEXT);
        String helpText = question.getString(API_READ_HELP);
        boolean required = question.getBoolean(API_READ_REQUIRED);
        String inputType = question.getString(API_READ_INPUT_TYPE);
        long dependsOn = question.optLong(API_READ_DEPENDS_ON, -1);
        addQuestion(Long.valueOf(id), questionString, helpText, inputType, required, dependsOn);
    }

    /**
     * add question to list
     * @param id
     * @param question
     * @param helpText
     * @param inputType
     * @param required
     * @param dependsOn
     * @throws JSONException
     */
    private void addQuestion(long id, String question, String helpText, String inputType, boolean required, long dependsOn) throws JSONException {
        addQuestion( id, question, helpText, inputType, required, dependsOn, Long.toString(id));
    }

    /**
     * add question to list
     *
     * @param id
     * @param question
     * @param helpText
     * @param inputType
     * @param required
     * @param dependsOn
     * @param query
     * @throws JSONException
     */
    private void addQuestion(long id, String question, String helpText, String inputType, boolean required, long dependsOn, String query) throws JSONException {
        JSONObject questionData = new JSONObject();
        questionData.put("id",id);
        questionData.put("text",question);
        questionData.put("help",helpText);
        questionData.put("input_type",inputType);
        questionData.put("required",required);
        questionData.put("query",query);
        questionData.put("depends_on",dependsOn);
        mPageData.put(questionData);
        mPageMeta.put(id);
    }

    /**
     * move current question list into a new page
     * @throws JSONException
     */
    private void pushPage( ) throws JSONException {
        for(int i = 0; i <mPageData.length(); i++) {
            mData.put(mPageData.getJSONObject(i));
        }
        mMeta.put(mPageMeta);

        mPageData = new JSONArray();
        mPageMeta = new JSONArray();
    }

    /**
     * upload new language answers to API, if listener is null, then default will be to put up warning dialog if error
     * @param targetTranslation
     * @param listener
     */
    public void uploadAnswersToAPI(final Activity activity, TargetTranslation targetTranslation, final OnRequestFinished listener) {

        final File folder = targetTranslation.getPath();
        final NewLanguagePackage pkg = NewLanguagePackage.open(folder);
        uploadAnswersToAPI(pkg, new OnRequestFinished() {
            @Override
            public void onRequestFinished(boolean success, Response response) {
                if(success) {
                    pkg.commit(folder); // save changes
                    updateListener(listener, success, response);
                } else { // failed
                    if((listener == null) && (activity != null)) {
                        Handler mHand = new Handler(Looper.getMainLooper());
                        mHand.post(new Runnable() {
                            @Override
                            public void run() {
                                final CustomAlertDialog dialog = CustomAlertDialog.Create(activity);
                                dialog.setTitle(R.string.new_lang_api_upload_failure_title)
                                        .setMessageHtml(R.string.new_lang_api_upload_failure)
                                        .setPositiveButton(R.string.confirm, null)
                                        .show("new_language_upload_failed");
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * will upload answers to API if they haven't been uploaded
     * @param nlPackage
     * @param listener - optional
     */
    public void uploadAnswersToAPI(final NewLanguagePackage nlPackage, final OnRequestFinished listener) {

        if(nlPackage.isUploaded())  { // check if already done
            updateListener(listener, true, null);
            return;
        }

        Thread postThread = new Thread() {
            @Override
            public void run() {
                Response response = null;
                try {
                    HashMap<String, String> postData = getPostData( nlPackage);
                    NewLanguageAPI api = new NewLanguageAPI();
                    NewLanguageAPI here = NewLanguageAPI.this;
                    response = here.doRequest(here.mNewLangUrl, null, postData, "POST");
                    if(response.exception != null) { // reflect errors
                        throw response.exception;
                    }

                    String responseData = getResponseData(response);
                    JSONObject jsonResponse = new JSONObject(responseData);
                    String status = jsonResponse.getString("status");
                    boolean success = "success".equalsIgnoreCase(status);
                    if(!success) {
                        if(responseData.indexOf(DUPLICATE_KEY_ERROR) >= 0) {
                            success = true; // if already uploaded, then we don't need to keep trying
                        }
                    }

                    if(success) {
                        nlPackage.setUploaded(true);
                    }
                    updateListener(listener, success, response);
                } catch (Exception e) {
                    Logger.e(TAG, "Error sending to New Language API", e);
                    updateListener(listener, false, response);
                }
            }
        };
        postThread.start();
    }


    private void updateListener(OnRequestFinished listener, boolean success, Response response) {
        if(!success) {
            String data = getResponseData(response);
            Logger.e(TAG, "new language upload failed response: " + data, response.exception);
        }

        if(listener != null) {
            listener.onRequestFinished(success,response);
        }
    }

    private String getResponseData(Response response) {
        String data = "(null)";
        if((response != null) && (response.data != null)) {
            data = response.data;
        }
        return data;
    }

    /**
     * Performs a request against the api
     * @param urlStr the api command
     * @param user the user authenticating this request. Requires token or username and pasword
     * @param postData if not null the request will POST the data (key,value) otherwise it will be a GET request
     * @param requestMethod if null the request method will default to POST or GET
     * @return
     */
    public Response doRequest(String urlStr, User user, HashMap<String, String> postData, String requestMethod) {
        int responseCode = -1;
        String responseData = null;
        Exception exception = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn;
            if(url.getProtocol() == "https") {
                conn = (HttpsURLConnection)url.openConnection();
            } else {
                conn = (HttpURLConnection)url.openConnection();
            }
            if(user != null) {
                String auth = encodeUserAuth(user);
                if(auth != null) {
                    conn.addRequestProperty("Authorization", auth);
                }
            }
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setReadTimeout(this.readTimeout);
            conn.setConnectTimeout(this.connectionTimeout);

            // custom request method
            if(requestMethod != null) {
                conn.setRequestMethod(requestMethod.toUpperCase());
            }

            if(postData != null) {
                // post
                if(requestMethod == null) {
                    conn.setRequestMethod("POST");
                }

                // build multi-part post

                String lineEnd = "\r\n";
                String hyphens = "--";
                String boundary = AppContext.udid() ;
                String postString = "";
                for(Map.Entry<String, String> entry : postData.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    String parameterStr = hyphens + boundary + lineEnd;
                    parameterStr += "Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd;
                    parameterStr += lineEnd;
                    parameterStr += value;
                    parameterStr += lineEnd;

                    postString += parameterStr;
                }

                postString += hyphens + boundary + lineEnd;

//                Logger.i(TAG, "post data:\n" + postString);

                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);

                conn.setDoOutput(true);
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(postString);
                dos.flush();
                dos.close();
            }

            responseCode = conn.getResponseCode();

            if(isRequestMethodReadable(conn.getRequestMethod())) {
                try {
                    // read response
                    InputStream is = conn.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int current;
                    while ((current = bis.read()) != -1) {
                        baos.write((byte) current);
                    }
                    responseData = baos.toString("UTF-8");
                } catch (Exception e) {
                    //no response data
                }
            }
        } catch (Exception e) {
            exception = e;
        }
        this.lastResponse = new Response(responseCode, responseData, exception);
        return this.lastResponse;
    }

    /**
     * Checks if the request method is one that will return content
     * @param method
     * @return
     */
    private boolean isRequestMethodReadable(String method) {
        switch(method.toUpperCase()) {
            case "DELETE":
            case "PUT":
                return false;
            default:
                return true;
        }
    }

    /**
     * Generates the authentication parameter for the user
     * Preference will be given to the token if it exists
     * @param user
     * @return
     */
    private String encodeUserAuth(User user) {
        if(user != null) {
            if(user.token != null) {
                return "token " + user.token;
            } else if(!user.getUsername().isEmpty() && !user.getPassword().isEmpty()) {
                String credentials = user.getUsername() + ":" + user.getPassword();
                try {
                    return "Basic " + Base64.encodeToString(credentials.getBytes("UTF-8"), Base64.NO_WRAP);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public interface OnRequestFinished {

        /**
         * called when the API request has completed
         * @param success
         */
        void onRequestFinished(boolean success, Response response);
    }
}
