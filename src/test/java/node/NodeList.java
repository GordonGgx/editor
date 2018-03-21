package node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NodeList{

    private Node root;
    public List<Node> nodes=new ArrayList<>();

    public void add(String data){
        Node node=new Node(data);
        nodes.add(node);
        root=node;
    }

    public void addLeft(String data) {
        root.addLeft(new Node(data));
    }

    public void addRight(String data){
        root.addRight(new Node(data));
    }

    public void addTop(String data){
        root.addTop(new Node(data));
    }

    public void addBottom(String data){
        root.addBottom(new Node(data));
    }

    public Node getRoot() {
        return root;
    }

    public void print(){
        root.print();
    }

}
