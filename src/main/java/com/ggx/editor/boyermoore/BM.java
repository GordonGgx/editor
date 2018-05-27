package com.ggx.editor.boyermoore;

public class BM {

    /**
     * @param c 主串（源串）中的字符
     * @param T 模式串（目标串）字符数组
     * @return 滑动距离
     */
    private static int dist(char c, char T[]) {
        int n = T.length;
        if (c == T[n - 1]) {
            return n;// c出现在模式串最后一位时
        }
        for (int i = n; i >= 1; i--) {
            if (T[i - 1] == c)
                return n - i;// i=max{i|t[i-1]且0<=i<=n-2}
        }
        return n;// c不出现在模式中时
    }

    /**
     * @param p_s
     * @param p_t
     * @return -2错误，-1匹配不到，[0,p_s.length-p_t.length]表示t在s中位置,下标从0开始
     */
    public static int index(final String p_s, final String p_t) {
        if (p_s == null || p_t == null) {
            return -2;
        }
        char[] s = p_s.toCharArray();
        char[] t = p_t.toCharArray();
        int slen = s.length, tlen = t.length;

        if (slen < tlen) {
            return -1;
        }

        int i = tlen, j;
        while (i <= slen) {
            j = tlen;
            while (j > 0 && s[i - 1] == t[j - 1]) {// S[i-1]与T[j-1]若匹配，则进行下一组比较；反之离开循环。
                i--;
                j--;
            }
            if (0 == j) {// j=0时，表示完美匹配，返回其开始匹配的位置
                int pos=i;
                System.out.println("匹配成功：位置="+pos);
                i = i+tlen+1;
                //return i;//如果要匹配多个，这里改为：int pos=i;i = i+tlen+1; --其中每次这个pos就是位置
            } else {
                //System.out.println(dist(s[i - 1], t));
                i = i + dist(s[i - 1], t);// 把主串和模式串均向右滑动一段距离dist(s[i-1]).即跳过dist(s[i-1])个字符无需比较
            }
        }

        return -1;// 模式串与主串无法匹配

    }
}
