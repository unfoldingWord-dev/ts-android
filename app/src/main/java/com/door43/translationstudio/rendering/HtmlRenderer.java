package com.door43.translationstudio.rendering;

import android.text.Html;
import android.text.TextUtils;

import com.door43.translationstudio.ui.spannables.ArticleLinkSpan;
import com.door43.translationstudio.ui.spannables.MarkdownLinkSpan;
import com.door43.translationstudio.ui.spannables.MarkdownTitledLinkSpan;
import com.door43.translationstudio.ui.spannables.PassageLinkSpan;
import com.door43.translationstudio.ui.spannables.ShortReferenceSpan;
import com.door43.translationstudio.ui.spannables.Span;
import com.door43.translationstudio.ui.spannables.TranslationWordLinkSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 12/2/2015.
 */
public class HtmlRenderer extends RenderingEngine {

    private final Span.OnClickListener mLinkListener;
    private final OnPreprocessLink preprocessCallback;

    public HtmlRenderer(OnPreprocessLink preprocessor, Span.OnClickListener linkListener) {
        mLinkListener = linkListener;
        preprocessCallback = preprocessor;
    }

    @Override
    public CharSequence render(CharSequence in) {
        CharSequence out = in;
        out = renderTranslationAcademyAddress(out);
        if(isStopped()) return in;
        out = renderTranslationAcademyRCAddress(out);
        if(isStopped()) return in;
        out = renderTranslationAcademyOddRCAddress(out);
        if(isStopped()) return in;
        out = renderPassageLink(out);
        if(isStopped()) return in;
        out = renderShortReferenceLink(out);
        if(isStopped()) return in;
        out = renderMarkdownLink(out);
        if(isStopped()) return in;
        out = renderTranslationWordLink(out);
        if(isStopped()) return in;
        out = renderTranslationWordHTMLLink(out);
        if(isStopped()) return in;
        // TODO: 12/15/2015 it would be nice if we could pass in a private click listener and interpret the link types before calling the supplied listener.
        // this will allow calling code to use instance of rather than comparing strings.
        out = Html.fromHtml(out.toString(), null, new HtmlTagHandler(mLinkListener));
        if(isStopped()) return in;
        return out;
    }

    private CharSequence renderTranslationWordHTMLLink(CharSequence in) {
        return renderLink(in, Pattern.compile("<a\\s+href=\"..\\/([a-z]+)\\/([0-1a-z]+).md\"\\s*>([^<]+)<\\/a>"), "tw", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                try {
                    String id = matcher.group(2).trim().toLowerCase();
                    String title = matcher.group(3).trim();
                    return new TranslationWordLinkSpan(title, id);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

    private CharSequence renderTranslationWordLink(CharSequence in) {
        return renderLink(in, MarkdownLinkSpan.PATTERN, "tw", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                String address = matcher.group(1).replaceAll("^:", "").trim().toLowerCase();

                // cut off title e.g. en:obe:other:stuff|title
                String[] addressName = address.split("\\|");
                address = addressName[0];

                String[] chunks = address.split(":");
                if(chunks.length > 2) {
                    String id = null;
                    // check for tw links
                    if(chunks[1].equals("obe")) {
                        id = chunks[chunks.length-1];
                    }
                    // TODO: if there are other forms of tw links we can check for them here.

                    if(id != null) {
                        return new TranslationWordLinkSpan(id, id);
                    }
                }
                return null;
            }
        });
    }

    private CharSequence renderMarkdownLink(CharSequence in) {
        return renderLink(in, MarkdownTitledLinkSpan.PATTERN, "m", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                return new MarkdownTitledLinkSpan(matcher.group(1), matcher.group(3));
            }
        });
    }

    /**
     * Renders links to other passages in the project
     * @param in
     * @return
     */
    private CharSequence renderPassageLink(CharSequence in) {
        return renderLink(in, PassageLinkSpan.PATTERN, "p", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                PassageLinkSpan span = new PassageLinkSpan(matcher.group(3), matcher.group(1));
                if(span.isValid()) {
                    return span;
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * Renders short references. that is references without a book label.
     * e.g. 1:1 indicates chapter 1 verse 1 of the current book.
     *
     * @param in
     * @return
     */
    private CharSequence renderShortReferenceLink(CharSequence in) {
        return renderLink(in, ShortReferenceSpan.PATTERN, "sr", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                return new ShortReferenceSpan(matcher.group(0));
            }
        });
    }

    /**
     * Renders addresses to translation academy pages as html
     * Example [[en:ta:vol1:translate:translate_unknown|How to Translate Unknowns]]
     * @param in
     * @return
     */
    public CharSequence renderTranslationAcademyAddress(CharSequence in) {
        return renderLink(in, ArticleLinkSpan.ADDRESS_PATTERN, "ta", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                String title = matcher.group(4);
                if(title == null) {
                    title = matcher.group(0);
                }
                return ArticleLinkSpan.parse(title, matcher.group(2));
            }
        });
    }

    /**
     * Renders resource container links to translation academy pages.
     * Example [[rc://hi/ta/man/translate/figs-you]]
     * @param in
     * @return
     */
    public CharSequence renderTranslationAcademyRCAddress(CharSequence in) {
        return renderLink(in, ArticleLinkSpan.RC_ADDRESS_PATTERN, "ta", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                String manual = matcher.group(2);
                String article = matcher.group(3).replace("_", "-");
                // TRICKY: volumes are deprecated, but because the index is using the old format we have to use it.
                String volume = getArticleVolume(article);
                // TRICKY: only english articles are supported
                return new ArticleLinkSpan(article, "en", volume, manual, article);
            }
        });
    }

    /**
     * strangely formatted links
     * @param in
     * @return
     */
    public CharSequence renderTranslationAcademyOddRCAddress(CharSequence in) {
        return renderLink(in, ArticleLinkSpan.ODD_RC_ADDRESS_PATTERN, "ta", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                String manual = matcher.group(2);
                String article = matcher.group(3).replace("_", "-");
                // TRICKY: volumes are deprecated, but because the index is using the old format we have to use it.
                String volume = getArticleVolume(article);
                // TRICKY: only english articles are supported
                return new ArticleLinkSpan(article, "en", volume, manual, article);
            }
        });
    }

    /**
     * Renders links to translation academy pages as html
     * Example <a href="/en/ta/vol1/translate/figs_intro" title="en:ta:vol1:translate:figs_intro">Figures of Speech</a>
     * @param in
     * @return
     */
    public CharSequence renderTranslationAcademyLink(CharSequence in) {
        return renderLink(in, ArticleLinkSpan.LINK_PATTERN, "ta", new OnCreateLink() {
            @Override
            public Span onCreate(Matcher matcher) {
                String title = matcher.group(6);
                if(title == null) {
                    title = matcher.group(0);
                }
                return ArticleLinkSpan.parse(title, matcher.group(3).replace("/", ":"));
            }
        });
    }

    /**
     * A generic rendering method for rendering content links as html
     *
     * @param in
     *@param pattern
     * @param callback   @return
     */
    private CharSequence renderLink(CharSequence in, Pattern pattern, String linkType, OnCreateLink callback) {
        CharSequence out = "";
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            Span link = callback.onCreate(matcher);
            if(link != null) {
                link.setOnClickListener(mLinkListener);
                if (preprocessCallback == null || preprocessCallback.onPreprocess(link)) {
                    // render clickable link
                    CharSequence title = link.getHumanReadable();
                    if(title == null || title.toString().isEmpty()) {
                        title = link.getMachineReadable();
                    }
                    String htmlLink = "<app-link href=\"" + link.getMachineReadable() + "\" type=\"" + linkType + "\" >" + title + "</app-link>";
                    out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), htmlLink);
                } else {
                    // render as plain text
                    out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), link.getHumanReadable());
                }
            } else {
                // ignore link
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.end()));
            }
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    private interface OnCreateLink {
        Span onCreate(Matcher matcher);
    }

    /**
     * Used to identify which links to render
     */
    public interface OnPreprocessLink {
        boolean onPreprocess(Span span);
    }

    /**
     * Returns the appropriate volume id for the article
     * @param article
     * @return
     */
    private String getArticleVolume(String article) {
        switch(article) {
            case "acceptable":
            case "accuracy-check":
            case "accurate":
            case "authority-level1":
            case "authority-level2":
            case "authority-level3":
            case "authority-process":
            case "church-leader-check":
            case "clear":
            case "community-evaluation":
            case "complete":
            case "goal-checking":
            case "good":
            case "important-term-check":
            case "intro-check":
            case "intro-checking":
            case "intro-levels":
            case "language-community-check":
            case "level1":
            case "level1-affirm":
            case "level2":
            case "level3":
            case "level3-approval":
            case "level3-questions":
            case "natural":
            case "other-methods":
            case "peer-check":
            case "self-assessment":
            case "self-check":
            case "finding-answers":
            case "gl-strategy":
            case "open-license":
            case "statement-of-faith":
            case "ta-intro":
            case "translation-guidelines":
            case "uw-intro":
            case "door43-translation":
            case "getting-started":
            case "intro-publishing":
            case "intro-share":
            case "platforms":
            case "prechecking-training":
            case "pretranslation-training":
            case "process-manual":
            case "publishing-prereqs":
            case "publishing-process":
            case "required-checking":
            case "setup-door43":
            case "setup-team":
            case "setup-tsandroid":
            case "setup-tsdesktop":
            case "setup-word":
            case "share-published":
            case "share-unpublished":
            case "tsandroid-translation":
            case "tsdesktop-translation":
            case "upload-merge":
            case "word-translation":
            case "tk-create":
            case "tk-enable":
            case "tk-find":
            case "tk-install":
            case "tk-intro":
            case "tk-start":
            case "tk-update":
            case "tk-use":
            case "translate-helpts":
            case "ts-create":
            case "ts-first":
            case "ts-install":
            case "ts-intro":
            case "ts-markverses":
            case "ts-navigate":
            case "ts-open":
            case "ts-problem":
            case "ts-publish":
            case "ts-request":
            case "ts-resources":
            case "ts-select":
            case "ts-settings":
            case "ts-share":
            case "ts-translate":
            case "ts-update":
            case "ts-upload":
            case "ts-useresources":
            case "uw-app":
            case "uw-audio":
            case "uw-checking":
            case "uw-first":
            case "uw-install":
            case "uw-language":
            case "uw-select":
            case "uw-update-content":
            case "choose-team":
            case "figs-events":
            case "figs-explicit":
            case "figs-explicitinfo":
            case "figs-hypo":
            case "figs-idiom":
            case "figs-intro":
            case "figs-irony":
            case "figs-metaphor":
            case "figs-order":
            case "figs-parables":
            case "figs-rquestion":
            case "figs-simile":
            case "figs-you":
            case "figs-youdual":
            case "figs-yousingular":
            case "file-formats":
            case "first-draft":
            case "guidelines-accurate":
            case "guidelines-church-approved":
            case "guidelines-clear":
            case "guidelines-intro":
            case "guidelines-natural":
            case "mast":
            case "qualifications":
            case "resources-intro":
            case "resources-links":
            case "resources-porp":
            case "resources-types":
            case "resources-words":
            case "translate-alphabet":
            case "translate-discover":
            case "translate-dynamic":
            case "translate-fandm":
            case "translate-form":
            case "translate-help":
            case "translate-levels":
            case "translate-literal":
            case "translate-manual":
            case "translate-names":
            case "translate-problem":
            case "translate-process":
            case "translate-retell":
            case "translate-source-licensing":
            case "translate-source-text":
            case "translate-source-version":
            case "translate-terms":
            case "translate-tform":
            case "translate-transliterate":
            case "translate-unknown":
            case "translate-wforw":
            case "translate-whatis":
            case "translate-why":
            case "translation-difficulty":
            case "writing-decisions":
                return "vol1";
            case "about-audio-recording":
            case "approach-to-audio":
            case "audio-acoustic-principles":
            case "audio-acoustical-treatments":
            case "audio-assessing-recording-space":
            case "audio-best-practices":
            case "audio-checklist-preparing-project":
            case "audio-checklist-recording-process":
            case "audio-checklists":
            case "audio-creating-new-file":
            case "audio-digital-recording-devices":
            case "audio-distribution":
            case "audio-distribution-amplification-recharging":
            case "audio-distribution-audio-player":
            case "audio-distribution-best-solutions":
            case "audio-distribution-door43":
            case "audio-distribution-license":
            case "audio-distribution-local":
            case "audio-distribution-microsd":
            case "audio-distribution-mobile-phone":
            case "audio-distribution-offline":
            case "audio-distribution-preparing-content":
            case "audio-distribution-radio":
            case "audio-distribution-wifi-hotspot":
            case "audio-editing":
            case "audio-editing-common-procedures":
            case "audio-editing-corrections":
            case "audio-editing-decisions-edit-rerecord":
            case "audio-editing-decisions-objective-subjective":
            case "audio-editing-finalizing":
            case "audio-editing-measuring-selection-length":
            case "audio-editing-modifying-pauses":
            case "audio-editing-navigating-timeline":
            case "audio-editing-using-your-ears":
            case "audio-equipment-overview":
            case "audio-equipment-setup":
            case "audio-field-environment":
            case "audio-guides":
            case "audio-guides-conversion-batch":
            case "audio-guides-normalizing":
            case "audio-guides-rename-batch":
            case "audio-interfaces":
            case "audio-introduction":
            case "audio-logistics":
            case "audio-managing-data":
            case "audio-managing-files":
            case "audio-managing-folders":
            case "audio-markers":
            case "audio-mic-activation":
            case "audio-mic-fine-tuning":
            case "audio-mic-gain-level":
            case "audio-mic-position":
            case "audio-mic-setup":
            case "audio-microphone":
            case "audio-noise-floor":
            case "audio-optimize-laptop":
            case "audio-playback-monitoring":
            case "audio-project-setup":
            case "audio-publishing-unfoldingword":
            case "audio-quality-standards":
            case "audio-recommended-accessories":
            case "audio-recommended-cables":
            case "audio-recommended-equipment":
            case "audio-recommended-headphones":
            case "audio-recommended-laptops":
            case "audio-recommended-mic-stands":
            case "audio-recommended-monitors":
            case "audio-recommended-playback-equipment":
            case "audio-recommended-pop-filters":
            case "audio-recommended-portable-recorders":
            case "audio-recommended-recording-devices":
            case "audio-recommended-tablets":
            case "audio-recording":
            case "audio-recording-environment":
            case "audio-recording-further-considerations":
            case "audio-recording-process":
            case "audio-setup-content":
            case "audio-setup-h2n":
            case "audio-setup-keyboard-shortcuts-audacity":
            case "audio-setup-keyboard-shortcuts-ocenaudio":
            case "audio-setup-ocenaudio":
            case "audio-setup-team":
            case "audio-signal-path":
            case "audio-signal-to-noise":
            case "audio-software":
            case "audio-software-file-renaming":
            case "audio-software-file-sharing":
            case "audio-software-format-conversion":
            case "audio-software-metadata-encoding":
            case "audio-software-recording-editing":
            case "audio-software-workspace":
            case "audio-standard-characteristics":
            case "audio-standard-file-naming":
            case "audio-standard-format":
            case "audio-standard-license":
            case "audio-standard-style":
            case "audio-studio-environment":
            case "audio-the-checker":
            case "audio-the-coordinator":
            case "audio-the-narrator":
            case "audio-the-recordist":
            case "audio-vision-purpose":
            case "audio-waveform-editor":
            case "audio-workspace-layout":
            case "excellence-in-audio":
            case "simplicity-in-audio":
            case "skills-training-in-audio":
            case "alphabet":
            case "formatting":
            case "headings":
            case "punctuation":
            case "spelling":
            case "verses":
            case "vol2-backtranslation":
            case "vol2-backtranslation-guidelines":
            case "vol2-backtranslation-kinds":
            case "vol2-backtranslation-purpose":
            case "vol2-backtranslation-who":
            case "vol2-backtranslation-written":
            case "vol2-intro":
            case "vol2-steps":
            case "vol2-things-to-check":
            case "check-notes":
            case "check-udb":
            case "check-ulb":
            case "gl-adaptulb":
            case "gl-done-checking":
            case "gl-notes":
            case "gl-questions":
            case "gl-translate":
            case "gl-udb":
            case "gl-ulb":
            case "gl-words":
            case "figs-123person":
            case "figs-abstractnouns":
            case "figs-activepassive":
            case "figs-apostrophe":
            case "figs-distinguish":
            case "figs-doublenegatives":
            case "figs-doublet":
            case "figs-ellipsis":
            case "figs-euphemism":
            case "figs-exclusive":
            case "figs-gendernotations":
            case "figs-genericnoun":
            case "figs-genitivecase":
            case "figs-go":
            case "figs-grammar":
            case "figs-hendiadys":
            case "figs-hyperbole":
            case "figs-inclusive":
            case "figs-informremind":
            case "figs-litotes":
            case "figs-merism":
            case "figs-metonymy":
            case "figs-parallelism":
            case "figs-partsofspeech":
            case "figs-personification":
            case "figs-pluralpronouns":
            case "figs-quotations":
            case "figs-rpronouns":
            case "figs-sentences":
            case "figs-singularpronouns":
            case "figs-synecdoche":
            case "figs-synonparallelism":
            case "figs-verbs":
            case "figs-youformal":
            case "guidelines-authoritative":
            case "guidelines-collaborative":
            case "guidelines-equal":
            case "guidelines-faithful":
            case "guidelines-historical":
            case "guidelines-ongoing":
            case "translate-bibleorg":
            case "translate-chapverse":
            case "translate-fraction":
            case "translate-manuscripts":
            case "translate-numbers":
            case "translate-ordinal":
            case "translate-original":
            case "translate-symaction":
            case "translate-textvariants":
            case "translate-versebridge":
            case "writing-background":
            case "writing-connectingwords":
            case "writing-intro":
            case "writing-newevent":
            case "writing-participants":
            case "writing-poetry":
            case "writing-proverbs":
            case "writing-quotations":
            case "writing-symlanguage":
            default:
                return "vol2";
        }
    }
}
