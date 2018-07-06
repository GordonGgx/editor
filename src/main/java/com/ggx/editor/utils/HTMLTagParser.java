package com.ggx.editor.utils;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static List<String> getImgSrc(String content){

        List<String> list = new ArrayList<>();
        //目前img标签标示有3种表达式
        //<img alt="" src="1.jpg"/>   <img alt="" src="1.jpg"></img>     <img alt="" src="1.jpg">
        //开始匹配content中的<img />标签
        Pattern p_img = Pattern.compile("<(img|IMG)(.*?)(/>|></img>|>)");
        Matcher m_img = p_img.matcher(content);
        boolean result_img = m_img.find();
        if (result_img) {
            while (result_img) {
                //获取到匹配的<img />标签中的内容
                String str_img = m_img.group(2);
                //开始匹配<img />标签中的src
                Pattern p_src = Pattern.compile("(src|SRC)=(\"|\')(.*?)(\"|\')");
                Matcher m_src = p_src.matcher(str_img);
                if (m_src.find()) {
                    String str_src = m_src.group(3);
                    list.add(str_src);
                }
                //结束匹配<img />标签中的src
                //匹配content中是否存在下一个<img />标签，有则继续以上步骤匹配<img />标签中的src
                result_img = m_img.find();
            }
        }
        return list;
    }

    public static List<String> getImageTag(String html){
        List<String> list = new ArrayList<>();
        //开始匹配content中的<img />标签
        Pattern p_img = Pattern.compile("<(img|IMG)(.*?)(/>|></img>|>)");
        Matcher m_img = p_img.matcher(html);
        boolean result_img = m_img.find();
        if (result_img) {
            while (result_img) {
                //获取到匹配的<img />标签中的内容
                String str_img = m_img.group(2);
                list.add(str_img);
                //结束匹配<img />标签中的src
                //匹配content中是否存在下一个<img />标签，有则继续以上步骤匹配<img />标签中的src
                result_img = m_img.find();
            }
        }
        return list;
    }

    public static String getSingleImageSrc(String imgTag){
        Pattern p_src = Pattern.compile("(src|SRC)=(\"|\')(.*?)(\"|\')");
        Matcher m_src = p_src.matcher(imgTag);
        if (m_src.find()) {
            return  m_src.group(3);
        }
        return "";
    }

    public static void main(String[] args) {
        String src="<img src=\"test.png\" alt=\" hahaha\"/>";
        System.out.println(getSingleImageSrc(src));
    }

}
