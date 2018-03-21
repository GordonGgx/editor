public class Ansy implements Runnable{

    private Action action;

    public Ansy(Action action){
        this.action=action;
    }
    @Override
    public void run() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        action.get("haha");
    }
}
