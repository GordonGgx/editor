package com.ggx.editor.markdown;

import javafx.concurrent.Task;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkDownKeyWord {

    private static final String WELL_PATTERN="^\\n?#{1,6} ";
    private static final String UNORDERED_LIST_PATTERN ="^\\s*\\* ";
    private static final String ORDERED_LIST_PATTERN ="^\\s*\\d+\\. ";
    private static final String PARAGRAPH_PATTERN ="^\\n?(>\\s*)+";
    private static final String CODE_LINE_PATTERN ="[`]|[`]{3}";
    private static final String BOLD_PATTERN ="\\*\\*\\w+\\*\\*";
    private static final String ITALIC_PATTERN="\\*\\w+\\*";
    private static final String PAREN_PATTERN = "[()]";
    private static final String BRACE_PATTERN = "[{}]";
    private static final String BRACKET_PATTERN = "[\\[\\]]";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";

    private static final Pattern PATTERN = Pattern.compile(
               "(?<WELL>" + WELL_PATTERN + ")"
                       + "|(?<ULP>" + UNORDERED_LIST_PATTERN + ")"
                       + "|(?<OLP>" + ORDERED_LIST_PATTERN + ")"
                       + "|(?<PARAGRAPH>" + PARAGRAPH_PATTERN + ")"
                       + "|(?<CODELINE>" + CODE_LINE_PATTERN + ")"
                       + "|(?<BOLD>" + BOLD_PATTERN + ")"
                       + "|(?<ITALIC>" + ITALIC_PATTERN + ")"
                       + "|(?<PAREN>" + PAREN_PATTERN + ")"
                       + "|(?<BRACE>" + BRACE_PATTERN + ")"
                       + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                       + "|(?<STRING>" + STRING_PATTERN + ")"
    ,Pattern.MULTILINE);


    public static Task<StyleSpans<Collection<String>>> computeHighlightingAsync(ExecutorService executor,CodeArea area) {
        String text = area.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() {
                return computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("WELL")!=null?"keyword":
                    matcher.group("ULP")!=null?"keyword":
                    matcher.group("BOLD")!=null?"keyword":
                    matcher.group("ITALIC")!=null?"keyword":
                    matcher.group("OLP")!=null?"ol":
                    matcher.group("PARAGRAPH")!=null?"ol":
                    matcher.group("CODELINE")!=null?"code_line":
                    matcher.group("PAREN") != null ? "paren" :
                    matcher.group("BRACE") != null ? "brace" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("BRACKET") != null ? "bracket" :
                    null;
            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        StyleSpans<Collection<String>> ss=spansBuilder.create();
        return ss;
    }
}
