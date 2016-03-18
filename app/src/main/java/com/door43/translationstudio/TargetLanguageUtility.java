package com.door43.translationstudio;

import com.door43.translationstudio.core.NewLanguagePackage;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;

/**
 * Created by blm on 3/17/16.
 */
public class TargetLanguageUtility {

    /**
     * get target language from database with fallback to creating it directly if new language
     * @param targetLanguageID
     * @return
     */
    // TODO: 3/17/16 this is temporary implementation until we determine a long term solution
    public static TargetLanguage getTargetLanguageWithFallback(String targetLanguageID, String targetTranslationID) {
        TargetLanguage targetLanguage = AppContext.getLibrary().getTargetLanguage(targetLanguageID);
        if(null == targetLanguage) { // may be new language, create directly using new language data
            Translator translator = AppContext.getTranslator();
            TargetTranslation targetTranslation = translator.getTargetTranslation(targetTranslationID);
            if(targetTranslation != null) {
                NewLanguagePackage newlang = NewLanguagePackage.open(targetTranslation.getPath());
                if (newlang != null) {
                    TargetLanguage targetNewLanguage = new TargetLanguage(targetTranslation.getId(), targetTranslation.getTargetLanguageName(), newlang.region, targetTranslation.getTargetLanguageDirection());
                    return targetNewLanguage;
                }
            }
        }
        return targetLanguage;
    }
}
