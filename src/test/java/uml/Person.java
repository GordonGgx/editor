package uml;

public class Person {

    private String name;
    private int age;


    public void say(String str){
        FangYan fangYan=new FangYan("四川话");
        System.out.println("用"+fangYan.getName()+"说："+str);
    }

}
