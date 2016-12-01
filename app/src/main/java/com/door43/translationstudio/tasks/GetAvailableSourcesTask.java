package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * searches resources to find new and updated resources.
 */


public class GetAvailableSourcesTask extends ManagedTask {
    public static final String TASK_ID = "get_available_sources_task";
    public static final String TAG = GetAvailableSourcesTask.class.getName();
    private int maxProgress = 0;
    private boolean success = false;
    private List<Translation> availableTranslations;
    private Map<String,Set<Integer>> byLanguage;
    private Map<String,Set<Integer>> otBooks;
    private Map<String,Set<Integer>> ntBooks;
    private Map<String,Set<Integer>> other;

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

        ntBooks = new LinkedHashMap<>();
        for (String book : ntBookList) {
            Set<Integer> books = new HashSet<>();
            ntBooks.put(book, books);
        }

        otBooks = new LinkedHashMap<>();
        for (String book : otBookList) {
            Set<Integer> books = new HashSet<>();
            otBooks.put(book, books);
        }
        other = new LinkedHashMap<>();

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
            Set<Integer> translations;
            if(byLanguage.containsKey(language)) {
                translations =  byLanguage.get(language);
            } else {
                translations = new HashSet<>();
                byLanguage.put(language, translations);
            }

            translations.add(i);

            //add to book list
            String book = t.project.slug;
            if(ntBooks.containsKey(book)) { // if NT book
                Set<Integer> books = ntBooks.get(book);
                books.add(i);
            }
            else if(otBooks.containsKey(book)) { // if OT book
                Set<Integer> books = otBooks.get(book);
                books.add(i);
            } else { // other
                Set<Integer> books;
                if(other.containsKey(book)) {
                   books = other.get(book);
                } else {
                    books = new HashSet<>();
                    other.put(book, books);
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

    public Map<String, Set<Integer>> getOther() {
        return other;
    }

    public Map<String, Set<Integer>> getNtBooks() {
        return ntBooks;
    }

    public Map<String, Set<Integer>> getOtBooks() {
        return otBooks;
    }

    public Map<String, Set<Integer>> getByLanguage() {
        return byLanguage;
    }

}
