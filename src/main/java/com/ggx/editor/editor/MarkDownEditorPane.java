package com.ggx.editor.editor;

import com.ggx.editor.keyword.MarkdownSyntaxHighlighter;
import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.issues.GfmIssuesExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataSet;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.control.IndexRange;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.reactfx.EventStreams;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MarkDownEditorPane {

    private CodeArea textArea;
    private FindReplacePane findReplacePane;
    private VirtualizedScrollPane<CodeArea> scrollPane;
    private final FindReplacePane.HitsChangeListener hitsChangeListener;
    private Parser parser;
    private SimpleIntegerProperty textSize=new SimpleIntegerProperty();
    private ReadOnlyDoubleWrapper scrollY=new ReadOnlyDoubleWrapper();
    private ReadOnlyObjectWrapper<Node> markDownAST=new ReadOnlyObjectWrapper<>();
    private ReadOnlyStringWrapper markDownText=new ReadOnlyStringWrapper();
    private ReadOnlyStringWrapper title=new ReadOnlyStringWrapper();
    private boolean isTextChange=false;

    public MarkDownEditorPane(BorderPane container) {
        textArea=new CodeArea();
        textArea.setWrapText(true);
        textArea.setUseInitialStyleForInsertion(true);
        textArea.getStyleClass().add("keyword-editor");
        textArea.getStylesheets().add("css/MarkdownEditor.css");
        textArea.getStylesheets().add("css/prism.css");
        textArea.setParagraphGraphicFactory(LineNumberFactory.get(textArea));

        textSize.addListener((observable, oldValue, newValue) -> textArea.setStyle("-fx-font-size:"+newValue));
        textSize.setValue(16);
        EventStreams.changesOf(textArea.textProperty())
                .filter(stringChange -> stringChange.getNewValue().length()!=0)
                .subscribe(stringChange -> {
                    isTextChange=true;
                    textChanged(stringChange.getNewValue());
                });
        EventStreams.changesOf(textArea.currentParagraphProperty())
                .filter(integerChange -> isTextChange)
                .map(integerChange -> {
                    isTextChange=false;
                    if(textArea.getText().length()==0){
                        return null;
                    }
                    return textArea.getParagraph(integerChange.getOldValue()).getText();
                })
                .filter(s ->
                        s!=null&&(
                        s.startsWith("# ")
                        ||s.startsWith("## ")
                        ||s.startsWith("### ")
                        ||s.startsWith("#### ")
                        ||s.startsWith("##### ")
                        ||s.startsWith("###### ")))
        .subscribe(s -> {
            title.setValue(textArea.getText());
        });

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
        Nodes.addInputMap(textArea,InputMap.sequence(
                InputMap.consume(EventPattern.keyPressed(KeyCode.TAB),keyEvent -> {
                    textArea.replaceSelection("    ");
                    textArea.requestFollowCaret();
                })
        ) );
        textArea.setInputMethodRequests(new InputMethodRequestsObject());
        textArea.setOnInputMethodTextChanged(event -> {
            if(!event.getCommitted().equals("")){
                textArea.insertText(textArea.getCaretPosition(), event.getCommitted());
            }
        });

        //findReplace
        findReplacePane=new FindReplacePane(container,textArea);
        hitsChangeListener=this::findHitsChanged;
        findReplacePane.addListener(hitsChangeListener);

        //添加VirtualScrollPane
        scrollPane=new VirtualizedScrollPane<>(textArea);
        markDownText.set("");
        markDownAST.set(parseMarkDown(""));
    }


    private void findHitsChanged() {
        applyHighlighting(markDownAST.get());
    }

    private void applyHighlighting(Node astRoot){
        List<MarkdownSyntaxHighlighter.ExtraStyledRanges> extraStyledRanges = findReplacePane.hasHits()
                ? Arrays.asList(
                new MarkdownSyntaxHighlighter.ExtraStyledRanges("hit", findReplacePane.getHits()),
                new MarkdownSyntaxHighlighter.ExtraStyledRanges("hit-active", Collections.singletonList(findReplacePane.getActiveHit())))
                : null;
        MarkdownSyntaxHighlighter.highlight(textArea, astRoot, extraStyledRanges);
    }

    private void textChanged(String newText) {

        Node node=parseMarkDown(newText);
        applyHighlighting(node);
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
            extensions.add(GfmIssuesExtension.create());

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

    public ReadOnlyStringWrapper titleProperty(){return title;}

    public CodeArea getTextArea() {
        return textArea;
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

    public void showFindPane(){
        findReplacePane.showFindPane();
    }

    public void jumpToLine(int lineNum){
//        textArea.showParagraphAtTop(lineNum);
        Timeline tl=new Timeline();
        SimpleDoubleProperty property=new SimpleDoubleProperty();
        property.set(textArea.getEstimatedScrollY());
        property.addListener((observable, oldValue, newValue) -> {
            textArea.scrollYToPixel(newValue.doubleValue());
        });
        KeyValue kv=new KeyValue(property,textArea.getAbsolutePosition(lineNum,0));
        KeyFrame kf=new KeyFrame(Duration.millis(500),kv);
        tl.getKeyFrames().add(kf);
        tl.play();
    }


    class InputMethodRequestsObject implements InputMethodRequests {

        @Override
        public Point2D getTextLocation(int offset) {
            return new Point2D(0,0);
        }

        @Override
        public int getLocationOffset(int x, int y) {
            return 0;
        }

        @Override
        public void cancelLatestCommittedText() {

        }

        @Override
        public String getSelectedText() {
            return "";
        }
    }
}
