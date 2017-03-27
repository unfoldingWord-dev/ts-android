package com.door43.translationstudio.tasks;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.BibleCodes;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.Resource;
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
    private Map<String,List<Integer>> taBooks;
    private Map<String,List<Integer>> otherBooks;
    private String prefix;

    private static final String[] ntBookList = BibleCodes.getNtBooks();
    private static final String[] otBookList = BibleCodes.getOtBooks();

    @Override
    public void start() {
        success = false;

        publishProgress(-1, "");

        Door43Client library = App.getLibrary();
        availableTranslations = library.index.findTranslations(null, null, null, "book", null, App.MIN_CHECKING_LEVEL, -1);

        if(GetAvailableSourcesTask.this.isCanceled()) {
            success = false;
            return;
        }

        List<Translation> tw = library.index.findTranslations(null, null, null, "dict", null, App.MIN_CHECKING_LEVEL, -1);
        availableTranslations.addAll(tw);

        if(GetAvailableSourcesTask.this.isCanceled()) {
            success = false;
            return;
        }

        // 02/20/2017 - for now we are disabling updating of TA since a major change coming up could break the app
//        List<Translation> man = library.index.findTranslations(null, null, null, "man", null, App.MIN_CHECKING_LEVEL, -1);
//        availableTranslations.addAll(man);

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
        taBooks = new LinkedHashMap<>();

        if(GetAvailableSourcesTask.this.isCanceled()) {
            success = false;
            return;
        }

        for (int i = 0; i < maxProgress; i++) {
            Translation t = availableTranslations.get(i);

            if( i % 16 == 0) {
                if(GetAvailableSourcesTask.this.isCanceled()) {
                    success = false;
                    return;
                }

                publishProgress((float)i/maxProgress, prefix);
            }

            if(GetAvailableSourcesTask.this.isCanceled()) {
                success = false;
                return;
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
                if("ta-".equals(book.substring(0,3))) {
                    addToOtherBook(taBooks, i, book);
                } else {
                    addToOtherBook(otherBooks, i, book);
                }
            }
        }

        success = true;
    }

    /**
     * add index to book list
     * @param booksList
     * @param i
     * @param book
     */
    private void addToOtherBook(Map<String,List<Integer>> booksList, int i, String book) {
        List<Integer> books;
        if(booksList.containsKey(book)) { // if book already present, add source to it
           books = booksList.get(book);
        } else {
            books = new ArrayList<>();  // if book not present, create new book entry
            booksList.put(book, books);
        }
        books.add(i);
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

    public Map<String, List<Integer>> getTaBooks() {
        return taBooks;
    }

    public Map<String, List<Integer>> getByLanguage() {
        return byLanguage;
    }

}
