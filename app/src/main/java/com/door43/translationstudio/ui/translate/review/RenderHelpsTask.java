package com.door43.translationstudio.ui.translate.review;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.ContainerCache;
import com.door43.translationstudio.core.Util;
import com.door43.translationstudio.ui.translate.ReviewModeAdapter;
import com.door43.translationstudio.ui.translate.TranslationHelp;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.Link;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 3/3/17.
 */
public class RenderHelpsTask extends ManagedTask {

    private final Door43Client library;
    private final ReviewListItem item;
    private final Map<String, String[]> sortedChunks;

    public RenderHelpsTask (Door43Client library, ReviewListItem item, Map<String, String[]> sortedChunks) {
        this.library = library;
        this.item = item;
        this.sortedChunks = sortedChunks;
    }

    @Override
    public void start() {
        setThreadPriority(Thread.MIN_PRIORITY);

        // be lazy so quickly starting and stopping this task will not pile up.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // don't consume the interrupt
            Thread.currentThread().interrupt();
        }

        // init default values
        Map<String, Object> result = new HashMap<>();
        result.put("words", new ArrayList<>());
        result.put("questions", new ArrayList<>());
        result.put("notes", new ArrayList<>());
        setResult(result);

        if(interrupted()) return;
        Map<String, List<String>> config = item.getChunkConfig();

        if(config.containsKey("words")) {
            List<Link> links = ContainerCache.cacheClosestFromLinks(library, config.get("words"));
            Pattern titlePattern = Pattern.compile("#(.*)");
            for(Link link:links) {
                if(interrupted()) return;
                ResourceContainer rc = ContainerCache.cacheClosest(App.getLibrary(), link.language, link.project, link.resource);
                if(interrupted()) return;
                // TODO: 10/12/16 the words need to have their title placed into a "title" file instead of being inline in the chunk
                String word = rc.readChunk(link.chapter, "01");
                Matcher match = titlePattern.matcher(word.trim());
                if(match.find()) {
                    link.title = match.group(1);
                }
            }
//                    if(links.size() > 0) Logger.i("Resource Card", getTaskId() + " found words at position " + position);
            result.put("words", links);
        }

        if(interrupted()) return;
        List<TranslationHelp> translationQuestions = new ArrayList<>();
        if(item.getSource() != null) {
            List<Translation> questionTranslations = library.index.findTranslations(item.getSource().language.slug, item.getSource().project.slug, "tq", "help", null, 0, -1);
            if (questionTranslations.size() > 0) {
                try {
                    ResourceContainer rc = ContainerCache.cache(library, questionTranslations.get(0).resourceContainerSlug);
                    if(interrupted()) return;
                    // TRICKY: questions are id'd by verse not chunk
                    String[] verses = rc.chunks(item.chapterSlug);
                    String rawQuestions = "";
                    // TODO: 2/21/17 this is very inefficient. We should only have to map chunk id's once, not for every chunk.
                    for (String verse : verses) {
                        if (interrupted()) return;
                        String chunk = ReviewModeAdapter.mapVerseToChunk(item.chapterSlug, verse, sortedChunks, item.getSource());
                        if (chunk.equals(item.chunkSlug)) {
                            rawQuestions += "\n\n" + rc.readChunk(item.chapterSlug, verse);
                        }
                    }
                    List<TranslationHelp> helps = parseHelps(rawQuestions.trim());
                    translationQuestions.addAll(helps);
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                    if(translationQuestions.size() > 0) Logger.i("Resource Card", getTaskId() + " found questions at position " + position);
                result.put("questions", translationQuestions);
            }
        }

        if(interrupted()) return;
        List<TranslationHelp> translationNotes = new ArrayList<>();
        if(item.getSource() != null) {
            List<Translation> noteTranslations = library.index.findTranslations(item.getSource().language.slug, item.getSource().project.slug, "tn", "help", null, 0, -1);
            if (noteTranslations.size() > 0) {
                try {
                    ResourceContainer rc = ContainerCache.cache(library, noteTranslations.get(0).resourceContainerSlug);
                    if(interrupted()) return;
                    String rawNotes = rc.readChunk(item.chapterSlug, item.chunkSlug);
                    if (!rawNotes.isEmpty()) {
                        List<TranslationHelp> helps = parseHelps(rawNotes);
                        translationNotes.addAll(helps);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                    if(translationNotes.size() > 0) Logger.i("Resource Card", getTaskId() + " found notes at position " + position);
                result.put("notes", translationNotes);
            }
        }

        // TODO: 10/17/16 if there are no results then look in the english version of this container
        setResult(result);
    }

    /**
     * Generates a tag for this task
     *
     * @param chapter
     * @param chunk
     * @return
     */
    public static String makeTag(String chapter, String chunk) {
        return "render_helps_" + chapter + "_" + chunk + "_task";
    }

    /**
     * Splits some raw help text into translation helps
     * @param rawText the help text
     * @return
     */
    private List<TranslationHelp> parseHelps(String rawText) {
        List<TranslationHelp> helps = new ArrayList<>();
        List<String> foundTitles = new ArrayList<>();

        // split up multiple helps
        String[] helpTextArray = rawText.split("#");
        for(String helpText:helpTextArray) {
            if(helpText.trim().isEmpty()) continue;

            // split help title and body
            String[] parts = helpText.trim().split("\n", 2);
            String title = parts[0].trim();
            String body = parts.length > 1 ? parts[1].trim() : null;

            // prepare snippets (has no title)
            int maxSnippetLength = 50;
            if(body == null) {
                body = title;
                if (title.length() > maxSnippetLength) {
                    title = title.substring(0, maxSnippetLength) + "...";
                }
            }
            // TRICKY: avoid duplicates. e.g. if a question appears in verses 1 and 2 while the chunk spans both verses.
            if(!foundTitles.contains(title)) {
                foundTitles.add(title);
                helps.add(new TranslationHelp(title, body));
            }
        }
        return helps;
    }

    public ReviewListItem getItem() {
        return item;
    }
}
