package com.ggx.editor.editor;

import com.ggx.editor.markdown.MarkDownHtmlWrapper;
import com.ggx.editor.markdown.MarkDownKeyWord;
import com.ggx.editor.options.Options;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataSet;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.IndexRange;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Change;
import org.reactfx.EventStreams;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class MarkDownEditorPane {

    private CodeArea textArea;
    private VirtualizedScrollPane<CodeArea> scrollPane;
    private Parser parser;
    private String lineSeparator = getLineSeparatorOrDefault();
    private SimpleIntegerProperty textSize=new SimpleIntegerProperty();
    private ReadOnlyDoubleWrapper scrollY=new ReadOnlyDoubleWrapper();
    private ReadOnlyObjectWrapper<Node> markDownAST=new ReadOnlyObjectWrapper<>();
    private ReadOnlyStringWrapper markDownText=new ReadOnlyStringWrapper();

    private ExecutorService executor;

    public void setExecutor(ExecutorService executor){
        this.executor=executor;
    }

    public MarkDownEditorPane() {
        textArea=new CodeArea();
        textArea.setWrapText(true);
        textArea.setUseInitialStyleForInsertion(true);
//        textArea.getStyleClass().add("markdown-editor");
//        textArea.getStylesheets().add("css/MarkdownEditor.css");
//        textArea.getStylesheets().add("css/prism.css");
        textArea.getStylesheets().add("css/java-keywords.css");
        textArea.setParagraphGraphicFactory(LineNumberFactory.get(textArea));

        textSize.addListener((observable, oldValue, newValue) -> textArea.setStyle("-fx-font-size:"+newValue));
        textSize.setValue(16);
        EventStreams.changesOf(textArea.textProperty())
                .reduceSuccessions((stringChange, stringChange2) -> stringChange2,
                        Duration.ofMillis(500))
                .subscribe(stringChange -> textChanged(stringChange.getNewValue()));
        ChangeListener<Double> scrollYListener=(observable, oldValue, newValue) -> {
            double value=textArea.estimatedScrollYProperty().getValue();
            double maxValue=textArea.totalHeightEstimateProperty()
                    .getOrElse(0.)-textArea.getHeight();
            scrollY.set((maxValue>0)?Math.min(Math.max(value/maxValue,0),1):0);
        };
        EventStreams.changesOf(textArea.totalHeightEstimateProperty().orElseConst(0.))
                .filter(doubleChange -> doubleChange.getNewValue()<doubleChange.getOldValue())
                .subscribe(doubleChange ->{
                    double value=textArea.estimatedScrollYProperty().getValue();
                    double maxValue=textArea.totalHeightEstimateProperty()
                            .getOrElse(0.)-textArea.getHeight();
                    scrollY.set((maxValue>0)?Math.min(Math.max(value/maxValue,0),1):0);
                } );
        //监听textArea滚动
        textArea.estimatedScrollYProperty().addListener(scrollYListener);
        //设置关键词高亮
        textArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .successionEnds(Duration.ofMillis(1000))
                .supplyTask(()-> MarkDownKeyWord.computeHighlightingAsync(executor,textArea))
                .awaitLatest(textArea.richChanges())
                .filterMap(t -> {
                    if(t.isSuccess()) {
                        System.out.println("成功");
                        return Optional.of(t.get());
                    } else {
                        System.out.println("失败");
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(styleSpans -> {
                    //应用高亮样式
                    System.out.println(styleSpans);
                    textArea.setStyleSpans(0, styleSpans);
                });

        //添加VirtualScrollPane
        scrollPane=new VirtualizedScrollPane<>(textArea);
        markDownText.set("");
        markDownAST.set(parseMarkDown(""));

    }

    private String getLineSeparatorOrDefault() {
        String lineSeparator = Options.getLineSeparator();
        return (lineSeparator != null) ? lineSeparator : System.getProperty( "line.separator", "\n" );
    }

    private void textChanged(String newText) {
        Node node=parseMarkDown(newText);
        markDownText.set(newText);
        markDownAST.set(node);
        System.out.println(node);
    }

    private Node parseMarkDown(String text){
        if(parser==null){
            MutableDataSet options = new MutableDataSet();
            options.setFrom(ParserEmulationProfile.MARKDOWN);
            // enable table parse!
            options.set(Parser.EXTENSIONS, Collections.singletonList(TablesExtension.create()));
            parser = Parser.builder(options).build();
        }
        return parser.parse(text);
    }

    public void setNewFileContent(String text){
        textArea.clear();
        textArea.replaceText(0,0,text);
    }

    public int getTextSize() {
        return textSize.get();
    }
    public SimpleIntegerProperty textSizeProperty() {
        return textSize;
    }

    public double getScrollY() {
        return scrollY.get();
    }

    public ReadOnlyDoubleWrapper scrollYProperty() {
        return scrollY;
    }

    public Node getMarkDownAST() {
        return markDownAST.get();
    }

    public ReadOnlyObjectWrapper<Node> markDownASTProperty() {
        return markDownAST;
    }

    public String getMarkDownText() {
        return markDownText.get();
    }

    public ReadOnlyStringWrapper markDownTextProperty() {
        return markDownText;
    }

    public ObservableValue<IndexRange> selectionProperty() { return textArea.selectionProperty(); }

    public VirtualizedScrollPane<CodeArea> getScrollPane() {
        return scrollPane;
    }
}
