package com.ggx.editor.utils;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;

public class HTMLTagParser {

    private static final String TAG="<[^>]+>";


    /**
     * 通过正则表达式提取html标签中的文本
     * @param html html标签
     * @return 标签文本
     */
    public static String getTextByRegex(String html){
        return html.replaceAll(TAG,"")
                .replaceAll("[\t\r\n]","");

    }

    public static String getTextByHTMLParser(String html){
        StringBuilder sb=new StringBuilder();
        HTMLEditorKit.ParserCallback callback=new HTMLEditorKit.ParserCallback(){

            @Override
            public void handleText(char[] data, int pos) {
                sb.append(data);
            }

        };
        ParserDelegator delegator=new ParserDelegator();
        try {
            delegator.parse(new StringReader(html),callback,true);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }

}
