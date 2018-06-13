package com.ggx.editor.editor.preview;

import com.ggx.editor.utils.Range;
import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.aside.AsideExtension;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.issues.GfmIssuesExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.util.ArrayList;
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
        List<Extension> extensions=new ArrayList<>();
        extensions.add(TablesExtension.create());
        extensions.add(FootnoteExtension.create());
        extensions.add(AnchorLinkExtension.create());
        extensions.add(AttributesExtension.create());
        extensions.add(GfmIssuesExtension.create());
        extensions.add(StrikethroughSubscriptExtension.create());
        extensions.add(TaskListExtension.create());
        extensions.add(JekyllTagExtension.create());
        extensions.add(AsideExtension.create());
        options.set(HtmlRenderer.GENERATE_HEADER_ID,true);
        options.set(Parser.EXTENSIONS, extensions);

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
