package com.door43.translationstudio.ui.translate.review;

import com.door43.translationstudio.App;
import com.door43.translationstudio.core.ContainerCache;
import com.door43.translationstudio.ui.translate.ReviewModeAdapter;
import com.door43.translationstudio.ui.translate.TranslationHelp;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.Link;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.logger.Logger;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 3/3/17.
 */
public class RenderHelpsTask extends ManagedTask {

    private static final String TAG = "RenderHelpsTask";
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
        setThreadPriority(Thread.MAX_PRIORITY);

        // init default values
        Map<String, Object> result = new HashMap<>();
        result.put("words", new ArrayList<>());
        result.put("questions", new ArrayList<>());
        result.put("notes", new ArrayList<>());
        setResult(null);

        if(interrupted()) return;
        Map<String, List<String>> config = item.getChunkConfig();

        if(interrupted()) return;
        if (config.containsKey("words")) {
            List<Link> links = ContainerCache.cacheFromLinks(library, config.get("words"), item.getSource().language);
            Pattern titlePattern = Pattern.compile("#(.*)");
            for (int i = 0; i < links.size(); i ++) {
                Link link = links.get(i);
                if (interrupted()) return;
                try {
                    // TRICKY: links may not have a language
                    ResourceContainer rc = ContainerCache.cacheClosest(App.getLibrary(), link.language != null ? link.language : item.getSource().language.slug, link.project, link.resource);
                    if (interrupted()) return;
                    if (rc != null) {
                        // re-build link with proper language
                        if(link.language == null || !link.language.equals(rc.language.slug)) {
                            link = Link.parseLink("/" + rc.language.slug + "/" + link.project + "/" + link.resource + "/" + link.arguments);
                        }
                        // TODO: 10/12/16 the words need to have their title placed into a "title" file instead of being inline in the chunk
                        String word = rc.readChunk(link.chapter, "01");
                        Matcher match = titlePattern.matcher(word.trim());
                        if (match.find()) {
                            link.title = match.group(1);
                        }

                        // update link
                        links.set(i, link);
                    } else {
                        Logger.w(TAG, "could not find resource container for words " + link.language + "-" + link.project + "-" + link.resource);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, e.getMessage(), e);
                }
            }
//                    if(links.size() > 0) Logger.i("Resource Card", getTaskId() + " found words at position " + position);
            if(links.size() > 0) {
                result.put("words", links);
            }
        }

        if (interrupted()) return;
        List<TranslationHelp> translationQuestions = new ArrayList<>();

        if (item.getSource() != null) {
            List<Translation> questionTranslations = library.index.findTranslations(item.getSource().language.slug, item.getSource().project.slug, "tq", "help", null, 0, -1);
            if (questionTranslations.size() > 0) {
                try {
                    ResourceContainer rc = ContainerCache.cache(library, questionTranslations.get(0).resourceContainerSlug);
                    if (interrupted()) return;
                    if(rc != null) {
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
                    } else {
                        Logger.w(TAG, "could not find resource container for questions " + questionTranslations.get(0).resourceContainerSlug);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, e.getMessage(), e);
                }
                if(translationQuestions.size() > 0) {
                    result.put("questions", translationQuestions);
                }
            }
        }

        if (interrupted()) return;
        List<TranslationHelp> translationNotes = new ArrayList<>();

        if (item.getSource() != null) {
            List<Translation> noteTranslations = library.index.findTranslations(item.getSource().language.slug, item.getSource().project.slug, "tn", "help", null, 0, -1);
            if (noteTranslations.size() > 0) {
                try {
                    ResourceContainer rc = ContainerCache.cache(library, noteTranslations.get(0).resourceContainerSlug);
                    if (interrupted()) return;
                    if(rc != null) {
                        String rawNotes = rc.readChunk(item.chapterSlug, item.chunkSlug);
                        if (!rawNotes.isEmpty()) {
                            List<TranslationHelp> helps = parseHelps(rawNotes);
                            translationNotes.addAll(helps);
                        }
                    } else {
                        Logger.w(TAG, "could not find resource container for notes " + noteTranslations.get(0).resourceContainerSlug);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, e.getMessage(), e);
                }
                if(translationNotes.size() > 0) {
                    result.put("notes", translationNotes);
                }
            }
        }

        if(interrupted()) return;
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

        String[] helpTextArray = rawText.split("\n\n");
        for(int i = 0; i < helpTextArray.length; i ++) {
            if(helpTextArray[i].trim().isEmpty()) continue;

            String title = helpTextArray[i].trim().replaceAll("^#+", "");
            String body;

            // TRICKY: for badly structured help data use the title as the body.
            if(helpTextArray.length > i + 1) {
                body = helpTextArray[i + 1].trim();
            } else {
                body = title;
            }

            // limit title length
            int maxTitleLength = 100;
            if(title.length() > maxTitleLength) {
                title = title.substring(0, maxTitleLength - 3) + "...";
            }

            // TRICKY: avoid duplicates. e.g. if a question appears in verses 1 and 2 while the chunk spans both verses.
            if(!foundTitles.contains(title)) {
                foundTitles.add(title);
                helps.add(new TranslationHelp(title, body));
            }
            i ++;
        }
        return helps;
    }

    public ReviewListItem getItem() {
        return item;
    }
}
