package com.ggx.editor.preview;

import com.ggx.editor.utils.Range;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.Visitor;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommonmarkPreviewRenderer implements MarkDownPreviewPane.Renderer{

    private String markDownText;
    private String html;
    private Node astRoot;

    @Override
    public void update(String markdownText, Node astRoot) {
        markDownText=markdownText;
        this.astRoot=astRoot;

    }

    @Override
    public String getHtml() {
        MutableDataSet options=new MutableDataSet();
        options.setFrom(ParserEmulationProfile.MARKDOWN);
        // enable table parse!
        options.set(Parser.EXTENSIONS, Collections.singletonList(TablesExtension.create()));

        Parser parser=Parser.builder(options).build();
        Node astNode=parser.parse(markDownText);
        if(astNode==null)return "";
        html=HtmlRenderer.builder(options).build().render(astNode);

        return html;
    }

    @Override
    public String getAST() {
        return null;
    }

    @Override
    public List<Range> findSequences(int startOffset, int endOffset) {
        ArrayList<Range> sequences = new ArrayList<>();
        return sequences;
    }
}
