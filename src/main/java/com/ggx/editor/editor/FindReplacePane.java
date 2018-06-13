package com.ggx.editor.editor;

import com.ggx.editor.boyermoore.BM;
import com.ggx.editor.utils.Range;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.MultiChangeBuilder;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.TwoDimensional;
import org.reactfx.Change;
import org.reactfx.EventStreams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FindReplacePane {

    private BorderPane container;
    private StackPane findRoot;
    private TextField findField;
    private TextField replaceField;


    public void showFindPane(){
        if(container.getTop()==null){
            container.setTop(findRoot);
            visible.set(true);
        }
        String selectedText = codeArea.getSelectedText();
        if (!selectedText.isEmpty() && selectedText.indexOf('\n') < 0)
            findField.setText(selectedText);
        findField.requestFocus();
        textChanged();
    }

    public interface HitsChangeListener{
        void hitsChanged();
    }

    private final SimpleBooleanProperty visible=new SimpleBooleanProperty(false);
    private final List<HitsChangeListener> listeners=new ArrayList<>();
    private final CodeArea codeArea;
    private List<Range> hits=new ArrayList<>();
    private int activeHitIndex=-1;


    public boolean isVisible(){return visible.get();}

    public FindReplacePane(BorderPane container,CodeArea area){
        this.container=container;
        codeArea=area;
        FXMLLoader loader=new FXMLLoader(ClassLoader.getSystemResource("fxml/findReplace.fxml"));
        try {
            findRoot=loader.load();
            findRoot.setPrefWidth(container.getWidth());
            findField= (TextField) findRoot.lookup("#tfSearch");
            replaceField= (TextField) findRoot.lookup("#tfReplace");
            Node close =findRoot.lookup("#findClose");
            close.setOnMouseClicked(event -> {
                visible.set(false);
                setActiveHitIndex(-1,false);
                FindReplacePane.this.container.setTop(null);
            });
            findRoot.lookup("#searchDown").setOnMouseClicked(event -> findNext());
            findRoot.lookup("#searchUp").setOnMouseClicked(event -> findPrevious());
            EventStreams.changesOf(findField.textProperty()).map(Change::getNewValue)
                    .subscribe(s -> textChanged());
            Button replace= (Button) findRoot.lookup("#replaceBtn");
            EventStreams.eventsOf(replace,MouseEvent.MOUSE_CLICKED)
                    .subscribe(mouseEvent -> replaceAll(replaceField.getText()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void addListener(HitsChangeListener listener){
        listeners.add(listener);
    }
    public void removeListener(HitsChangeListener listener){
        listeners.remove(listener);
    }

    private void activeHitsChanged(){
        for (HitsChangeListener listener:listeners)listener.hitsChanged();
    }

    public List<Range> getHits(){return hits;}
    public Range getActiveHit(){
        if(activeHitIndex>=0&&activeHitIndex<hits.size()){
            return hits.get(activeHitIndex);
        }
        return null;
    }
    public boolean hasHits(){return activeHitIndex>=0;}

    public void textChanged(){
        findAll(false);
    }
    private void findAll(boolean selectActiveHit) {
        findAll(codeArea.getText(), findField.getText(), selectActiveHit);
    }
    private void findAll(String text,String find,boolean selectActivity){
        if(find.isEmpty()){
            clearHits();
            return;
        }
        hits.clear();
        //使用BM算法获取
        BM.index(hits,find,text);
        if(hits.isEmpty()){
            setActiveHitIndex(-1,selectActivity);
            updateOverviewRuler();
            return;
        }
        int anchor=codeArea.getAnchor();
        int index=Collections.binarySearch(hits,new Range(anchor,anchor),(r1,r2)->r1.end-r2.start);
        if (index < 0) {
            index = -index - 1;
            if (index >= hits.size()) index = 0; // wrap
        }
        setActiveHitIndex(index,selectActivity);
        updateOverviewRuler();
    }

    private void findPrevious(){
        if (hits.size() == 0)
            return;

        int previous = activeHitIndex - 1;
        if (previous < 0)
            previous = hits.size() - 1;
        setActiveHitIndex(previous, true);
        updateOverviewRuler();
    }

    private void findNext(){
        if (hits.size() ==0)
            return;

        int next = activeHitIndex + 1;
        if (next >= hits.size())
            next = 0;

        setActiveHitIndex(next, true);
        updateOverviewRuler();
    }

    private void clearHits() {
        hits.clear();
        setActiveHitIndex(-1, false);
        updateOverviewRuler();
    }

    private void setActiveHitIndex(int index, boolean selectActiveHit) {
        int oldActiveHitIndex=activeHitIndex;
        activeHitIndex = index;

        if (selectActiveHit)
            selectActiveHit();

        if (oldActiveHitIndex<0&&activeHitIndex < 0)
            return; // 不需要激活
        activeHitsChanged();
    }

    private void selectActiveHit() {
        if (activeHitIndex < 0)
            return;

        Range activeHit = getActiveHit();
        if(activeHit!=null){
            codeArea.selectRange(activeHit.start, activeHit.end);
        }
    }

    private void updateOverviewRuler() {
        if (hits.isEmpty())
            return;
        Range hit=getActiveHit();
        if(hit!=null){
            int line = codeArea.offsetToPosition(hit.start, TwoDimensional.Bias.Backward).getMajor();
            codeArea.showParagraphInViewport(line);
        }
    }

    private void replaceAll(String replace){
        if(hits.isEmpty())return;

        MultiChangeBuilder<Collection<String>, String, Collection<String>> multiChange =
                codeArea.createMultiChange(hits.size());
        for (Range hit : hits) {
            multiChange.replaceText(hit.start, hit.end, replace);
        }
        hits.clear();
        //撤销保存
        codeArea.getUndoManager().preventMerge();
        multiChange.commit();
        codeArea.getUndoManager().preventMerge();
        codeArea.requestFocus();
    }
}
