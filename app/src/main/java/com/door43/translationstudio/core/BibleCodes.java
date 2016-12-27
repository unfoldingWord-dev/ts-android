package com.door43.translationstudio.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by blm on 12/21/16.
 */

public class BibleCodes {

    private static final List<String> ntBookList =  Arrays.asList(
            "mat" , "mrk", "luk", "jhn", "act", "rom", "1co", "2co",
            "gal", "eph", "php", "col", "1th", "2th", "1ti", "2ti",
            "tit", "phm", "heb", "jas", "1pe", "2pe", "1jn", "2jn",
            "3jn", "jud", "rev");
    private static final List<String> otBookList = Arrays.asList(
            "gen" , "exo", "lev", "num", "deu", "jos", "jdg", "rut",
            "1sa", "2sa", "1ki", "2ki", "1ch", "2ch", "ezr", "neh",
            "est", "job", "psa", "pro", "ecc", "sng", "isa", "jer",
            "lam", "ezk", "dan", "hos", "jol", "amo", "oba", "jon",
            "mic", "nam", "hab", "zep", "hag", "zec", "mal");

    static public String[] getNtBooks() {
        return ntBookList.toArray(new String[ntBookList.size()]);
    }

    static public String[] getOtBooks() {
        return otBookList.toArray(new String[otBookList.size()]);
    }

    static public String[] getBibleBooks() {
        List<String> bible = new ArrayList<>();
        bible.addAll(otBookList);
        bible.addAll(ntBookList);
        return bible.toArray(new String[bible.size()]);
    }
}
