package node;

import java.util.LinkedList;

public class Test {

    public static void main(String[] args) {
        NodeList list=new NodeList();
        list.add("root");
        list.addLeft("aa");
        list.addRight("bb");
        list.addTop("cc");
        list.addBottom("dd");

        list.add("root1");
        list.addLeft("aa1");
        list.addRight("bb1");
        list.addTop("cc1");
        list.addBottom("dd1");

        list.add("root2");
        list.addLeft("aa2");
        list.addRight("bb2");
        list.addTop("cc2");
        list.addBottom("dd2");

        list.add("root3");
        list.addLeft("aa3");
        list.addRight("bb3");
        list.addTop("cc3");
        list.addBottom("dd3");

        list.add("root4");
        list.addLeft("aa4");
        list.addRight("bb4");
        list.addTop("cc4");
        list.addBottom("dd4");

        for (int i=0;i<list.nodes.size();i++){
            Node node=list.nodes.get(i).next();
            if(node!=null){
                System.out.println(node);
            }
        }

    }
}
