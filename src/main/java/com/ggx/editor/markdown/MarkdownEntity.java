package com.ggx.editor.markdown;

import com.ggx.editor.utils.FileReadUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

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
        try {

            css = FileReadUtil.readAll(ClassLoader.getSystemResourceAsStream("md/huimarkdown"));
            addScript("<script>window.onload=function(){window.scrollTo(0,document.body.scrollHeight);}</script>");
            URL url=ClassLoader.getSystemResource("js/MathJax.js");
            addScript("<script type=\"text/javascript\" src=\""+url.toExternalForm()+"?config=TeX-AMS_HTML\"></script>");
            addScript("<script type=\"text/x-mathjax-config\">\n" +
                        " MathJax.Hub.Config({\n" +
                        "    extensions: [\"tex2jax.js\"],\n" +
                        "    jax: [\"input/TeX\", \"output/HTML-CSS\"],\n" +
                        "    tex2jax: {\n" +
                        "        inlineMath:  [ [\"$\", \"$\"] ],\n" +
                        "        displayMath: [ [\"$$\",\"$$\"] ],\n" +
                        "        skipTags: ['script', 'noscript', 'style', 'textarea', 'pre','code','a'],\n" +
                        "        ignoreClass:\"class1\"\n" +
                        "    },\n" +
                        "    \"HTML-CSS\": {\n" +
                        "\t\tshowMathMenu: false\n" +
                        "    },\n" +
                        "\tshowProcessingMessages: false,\n" +
                        "    messageStyle: \"none\"\n" +
                        "});\n" +
                        "MathJax.Hub.Queue([\"Typeset\",MathJax.Hub]);"+
                        "</script>");
        } catch (IOException  e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder("<!DOCTYPE html>\n");
        sb.append("<html>").append("\n");
        sb.append("<head>").append("\n");
        sb.append("<meta charset=\"UTF-8\">").append("\n");
        sb.append("<style>").append("\n");
        sb.append(css);
        sb.append("</style>").append("\n");
        sb.append(script.toString());
        sb.append("</head>").append("\n");
        sb.append("<body class=\"markdown-body\">").append("\n");
        sb.append(html);
        sb.append("</body>").append("\n");
        sb.append("</html>").append("\n");
        System.out.println(sb.toString());
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
