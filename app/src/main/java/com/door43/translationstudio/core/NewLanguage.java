package com.door43.translationstudio.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by blm on 3/9/16.
 */
public class NewLanguage {
    List<NewLanguageQuestion> mList;

    NewLanguage() {
        mList = new ArrayList<>();
    }

    void addQuestion(NewLanguageQuestion question) {
        mList.add(question.id, question);
    }


}
