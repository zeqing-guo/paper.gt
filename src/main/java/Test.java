import java.io.IOException;

/**
 * Created by lss on 11/24/16.
 */
public class Test {

    public static void main(String[] args) throws IOException, InterruptedException {
//        Main.initialize(null, null, 2000);
//        System.out.println(Main.getProxy().toString());
        ProxyFetcher pf = ProxyFetcher.getInstance(null, 0);
        Thread.sleep(1000 * 30);
        System.out.println("start calling proxy");
        for (int i = 0; i < 5; i++) {
            HelloProxy p = pf.getProxy();
            System.out.println(p);
            Thread.sleep(1000 * 30);
        }
        Thread.sleep(1000 * 60 * 5);
        System.out.println("start calling proxy");
        for (int i = 0; i < 5; i++) {
            HelloProxy p = pf.getProxy();
            System.out.println(p);
            Thread.sleep(1000 * 30);
        }
        Thread.sleep(1000 * 60 * 5);
        pf.close();
    }
}
