package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * finds available sources.  Then organizes the sources into languages, and book category (i.e. OT, NT, other)
 */


public class GetAvailableSourcesTask extends ManagedTask {
    public static final String TASK_ID = "get_available_sources_task";
    public static final String TAG = GetAvailableSourcesTask.class.getName();
    private int maxProgress = 0;
    private boolean success = false;
    private List<Translation> availableTranslations;
    private Map<String,List<Integer>> byLanguage;
    private Map<String,List<Integer>> otBooks;
    private Map<String,List<Integer>> ntBooks;
    private Map<String,List<Integer>> otherBooks;

    private static String[] ntBookList = { "mat" , "mrk", "luk", "jhn", "act", "rom", "1co", "2co",
                                            "gal", "eph", "php", "col", "1th", "2th", "1ti", "2ti",
                                            "tit", "phm", "heb", "jas", "1pe", "2pe", "1jn", "2jn",
                                            "3jn", "jud", "rev"};
    private static String[] otBookList = { "gen" , "exo", "lev", "num", "deu", "jos", "jdg", "rut",
                                            "1sa", "2sa", "1ki", "2ki", "1ch", "2ch", "ezr", "neh",
                                            "est", "job", "psa", "pro", "ecc", "sng", "isa", "jer",
                                            "lam", "ezk", "dan", "hos", "jol", "amo", "oba", "jon",
                                            "mic", "nam", "hab", "zep", "hag", "zec", "mal"};
    private String prefix;

    @Override
    public void start() {
        success = false;

        publishProgress(-1, "");

        Door43Client library = App.getLibrary();
        availableTranslations = library.index.findTranslations(null, null, null, "book", null, App.MIN_CHECKING_LEVEL, -1);
        byLanguage = new TreeMap<>();
        maxProgress = availableTranslations.size();

        // initialize NT book list
        ntBooks = new LinkedHashMap<>();
        for (String book : ntBookList) {
            List<Integer> books = new ArrayList<>();
            ntBooks.put(book, books);
        }

        // initialize OT book list
        otBooks = new LinkedHashMap<>();
        for (String book : otBookList) {
            List<Integer> books = new ArrayList<>();
            otBooks.put(book, books);
        }
        otherBooks = new LinkedHashMap<>();

        for (int i = 0; i < maxProgress; i++) {
            Translation t = availableTranslations.get(i);

            if( i % 16 == 0) {
                publishProgress((float)i/maxProgress, prefix);

                if(GetAvailableSourcesTask.this.isCanceled()) {
                    success = false;
                    return;
                }
            }

            String id = t.resourceContainerSlug;
            String language = t.language.slug;

            //add to language
            List<Integer> translations;
            if(byLanguage.containsKey(language)) {
                translations =  byLanguage.get(language);
            } else {
                translations = new ArrayList<>();
                byLanguage.put(language, translations);
            }

            translations.add(i);

            //add to book list
            String book = t.project.slug;
            if(ntBooks.containsKey(book)) { // if NT book
                List<Integer> books = ntBooks.get(book);
                books.add(i);
            }
            else if(otBooks.containsKey(book)) { // if OT book
                List<Integer> books = otBooks.get(book);
                books.add(i);
            } else { // other
                List<Integer> books;
                if(otherBooks.containsKey(book)) { // if book already present, add source to it
                   books = otherBooks.get(book);
                } else {
                    books = new ArrayList<>();  // if book not present, create new book entry
                    otherBooks.put(book, books);
                }
                books.add(i);
            }
        }

        success = true;
    }

    @Override
    public int maxProgress() {
        return maxProgress;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<Translation> getSources() {
        return availableTranslations;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix + "  ";
    }

    public Map<String, List<Integer>> getOther() {
        return otherBooks;
    }

    public Map<String, List<Integer>> getNtBooks() {
        return ntBooks;
    }

    public Map<String, List<Integer>> getOtBooks() {
        return otBooks;
    }

    public Map<String, List<Integer>> getByLanguage() {
        return byLanguage;
    }

}
