package com.door43.translationstudio.core;

import android.app.Activity;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.rendering.USXtoUSFMConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by blm on 3/7/16.
 */
public class TranslationConvertFromUSXtoUSFMformt {

    public static boolean convertProject(String targetTranslationId) {

        Library mLibrary = AppContext.getLibrary();
        Translator mTranslator = AppContext.getTranslator();
        TargetTranslation mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);

        TranslationFormat format = mTargetTranslation.getFormat();

        if(TranslationFormat.USFM == format) {
            return true; // nothing to do

        } else if(TranslationFormat.USX == format) {
            ChapterTranslation[] chapters = mTargetTranslation.getChapterTranslations();
            for (ChapterTranslation c : chapters) {
                FrameTranslation[] frameTranslations = mTargetTranslation.getFrameTranslations(c.getId(), format);
                for (FrameTranslation frameTranslation : frameTranslations) {

                    String text = frameTranslation.body;
                    String usfm = USXtoUSFMConverter.doConversion(text).toString();
                    mTargetTranslation.applyFrameTranslation(frameTranslation,usfm);
                }
            }

            mTargetTranslation.applyFormat(TranslationFormat.USFM);
            return true;
        } else {
            return false; // format not supported
        }
    }

}
