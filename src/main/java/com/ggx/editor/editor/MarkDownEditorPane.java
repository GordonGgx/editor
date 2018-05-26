package com.ggx.editor.editor;

import com.ggx.editor.Main;
import com.ggx.editor.markdown.MarkDownKeyWord;
import com.ggx.editor.options.Options;
import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataSet;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.IndexRange;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.EventStreams;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MarkDownEditorPane {

    private CodeArea textArea;
    private VirtualizedScrollPane<CodeArea> scrollPane;
    private Parser parser;
    private String lineSeparator = getLineSeparatorOrDefault();
    private SimpleIntegerProperty textSize=new SimpleIntegerProperty();
    private ReadOnlyDoubleWrapper scrollY=new ReadOnlyDoubleWrapper();
    private ReadOnlyObjectWrapper<Node> markDownAST=new ReadOnlyObjectWrapper<>();
    private ReadOnlyStringWrapper markDownText=new ReadOnlyStringWrapper();

    public MarkDownEditorPane() {
        textArea=new CodeArea();
        textArea.setWrapText(true);
        textArea.setUseInitialStyleForInsertion(true);
//        textArea.getStyleClass().add("markdown-editor");
//        textArea.getStylesheets().add("css/MarkdownEditor.css");
        textArea.getStylesheets().add("css/prism.css");
        textArea.getStylesheets().add("css/md-keywords.css");
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
        //listener textArea scroll
        textArea.estimatedScrollYProperty().addListener(scrollYListener);
        //keyworlds lighheith
        textArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .successionEnds(Duration.ofMillis(500))
                .supplyTask(()-> MarkDownKeyWord.computeHighlightingAsync(Main.getExecutor(),textArea))
                .awaitLatest(textArea.richChanges())
                .filterMap(t -> {
                    if(t.isSuccess()) {
                        return Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(styleSpans -> {
                    //应用高亮样式
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
    }

    private Node parseMarkDown(String text){
        if(parser==null){
//            DataHolder options=PegdownOptionsAdapter.flexmarkOptions(true,Extensions.ALL);
            MutableDataSet options = new MutableDataSet();
            options.setFrom(ParserEmulationProfile.MARKDOWN);
            // enable table parse!
            List<Extension> extensions=new ArrayList<>();
            extensions.add(TablesExtension.create());
            extensions.add(AnchorLinkExtension.create());
            extensions.add(FootnoteExtension.create());
            options.set(Parser.EXTENSIONS, extensions);
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

    public void saveFile(File file){
        String text=textArea.getText();
        try (BufferedWriter fos=new BufferedWriter(new FileWriter(file,false))){
            fos.write(text);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
