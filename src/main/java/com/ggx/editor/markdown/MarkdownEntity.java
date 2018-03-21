package com.ggx.editor.markdown;

import com.ggx.editor.utils.FileReadUtil;

import java.io.IOException;

public class MarkdownEntity {

    private static String TAG_WIDTH = "<style type=\"text/css\"> %s { width:85%%} </style>";

    // css 样式
    private String css;
    private StringBuilder script;


    // 转换后的html文档
    private String html;


    public MarkdownEntity(String html) {
        this.html = html;
        script=new StringBuilder();
        addScript("<script type=\"text/javascript\" src=\"http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=default\"></script>");
        addScript("<script>window.onload=function(){window.scrollTo(0,document.body.scrollHeight);}</script>");
        try {
            css = FileReadUtil.readAll(ClassLoader.getSystemResourceAsStream("md/huimarkdown"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder("<!DOCTYPE html>\n");
        sb.append("<html>").append("\n");
        sb.append("<head>").append("\n");
        sb.append("<style>").append("\n");
        sb.append(css);
        sb.append("</style>").append("\n");
        sb.append(script.toString());
        sb.append("</head>").append("\n");
        sb.append("<body class=\"markdown-body\">").append("\n");
        sb.append(html);
        sb.append("</body>").append("\n");
        sb.append("</html>").append("\n");
        return sb.toString();
    }



    public void addWidthCss(String tag) {
        String wcss = String.format(TAG_WIDTH, tag);
        css += wcss;
    }

    public void addScript(String stript){
        this.script.append(stript).append("\n");
    }
}
