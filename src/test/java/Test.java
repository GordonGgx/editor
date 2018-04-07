import uml.Car;
import uml.Driver;
import uml.FangYan;
import uml.Person;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class Test {


    public static void main(String[] args) {
//        ArrayBlockingQueue<String> queue=new ArrayBlockingQueue<>(1);
//        Action action=str -> {
//            try {
//                queue.put(str);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        };
//        new Thread(new Ansy(action),"ggx").start();
//        try {
//            String str=queue.take();
//            System.out.println(str+" "+Thread.currentThread().getName());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        Driver driver=Driver.getInstance();

    }
}
