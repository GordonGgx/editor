package node;

import java.util.Iterator;

public class Node {

    private Node left;
    private Node top;
    private Node right;
    private Node bottom;

    private String data;

    public Node(String data) {
        this.data = data;
    }

    public void addLeft(Node node){
        if(left==null){
            left=node;
        }else {
            left.addLeft(node);
        }
    }

    public void addRight(Node node){
        if(right==null){
            right=node;
        }else {
            right.addRight(node);
        }
    }

    public void addTop(Node node){
        if(top==null){
            top=node;
        }else {
            top.addTop(node);
        }
    }

    public void addBottom(Node node){
        if(bottom==null){
            bottom=node;
        }else {
            bottom.addBottom(node);
        }
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Node getLeft() {
        return left;
    }

    public Node getTop() {
        return top;
    }

    public Node getRight() {
        return right;
    }

    public Node getBottom() {
        return bottom;
    }

    public void print(){
        System.out.println("Node{"+data+"}");
        if(left!=null){
            left.print();
        }
        if(top!=null){
            top.print();
        }
        if(right!=null){
            right.print();
        }
        if(bottom!=null){
            bottom.print();
        }
//        System.out.println("Node{"+left.data+","+top.data+","+right.data+","+bottom.data+"}");
    }

    @Override
    public String toString() {
        return data;
    }


    public Node next() {
        Node node;
        if(right!=null){
            node= right;
        }else if(bottom!=null){
            node= bottom;
        }else if(left!=null){
            node= left;
        }else{
            node= top;
        }
        return node;
    }

}
