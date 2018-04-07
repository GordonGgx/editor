package uml;

/**
 * 这是一个演示类
 *
 * @author ggx
 * @version v1.0
 */
public class Driver {

    private Driver(){}

    /**
     * 这是一个单列获取方法
     * @return 唯一Driver实例
     */
    public static Driver getInstance(){
        return INSTANCE.driver;
    }


    /**
     * 这是一个java doc 演示的方法
     *
     * @param str 这是该方法的一个字符串类型的参数
     * @return 该方法返回一个字符串类新的值
     */
    public String say(String str){
        return str;
    }

     private static class INSTANCE{
        private static Driver driver=new Driver();
    }

}
