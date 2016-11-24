import com.sun.javaws.exceptions.InvalidArgumentException;

import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lss on 11/24/16.
 */
public class ProxyFetcher {
    private Proxy fetchingProxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 1080));
    private int timeout = 2000;

    private static ProxyFetcher main = null;
    private static Logger logger = Logger.getLogger("test");

    private static final int CANDIDATE_SIZE = 200, MAX_WAITING_ROUND = 20;
    private static boolean doubleCheck = true;
    private static Deque<HelloProxy> stack = new ArrayDeque<>();
    private static ArrayBlockingQueue<HelloProxy> candidateQueue = new ArrayBlockingQueue<>(CANDIDATE_SIZE);

    private FetcherThread fetcher = new FetcherThread();
    private CheckerThread checker1, checker2, checker3;
    private String checkURL1 = "http://www.baidu.com/", checkURL2 = "https://www.microsoft.com/en-us/";
    private int waitingRound = MAX_WAITING_ROUND;

    private ProxyFetcher(Proxy ipv6proxy, int checkingTimeOut){
        fetchingProxy = (ipv6proxy != null) ? ipv6proxy : fetchingProxy;
        timeout = (checkingTimeOut > 0) ? checkingTimeOut : timeout;
        fetcher.start();
    }

    /**
     * A proxy fetcher object.
     * It will be better to call this method as earlier as possible since it needs some time to check the connectivity of the scraped proxy
     * @param ipv6proxy  This fetcher requires an IPv6 proxy to fetch proxy lists - maybe it will be fixed later
     * @param checkingTimeOut The timeout for checking the connection which would affect accuracy but may reduce latency
     * @return a ProxyFetcher
     */
    public static ProxyFetcher getInstance(Proxy ipv6proxy, int checkingTimeOut){
        if (main != null)
            return main;
        main = new ProxyFetcher(ipv6proxy, checkingTimeOut);
        return main;
    }

    /**
     *
     * @return proxy: a HelloProxy type which can extract Proxy.Type and InetSocketAddress
     */
    public HelloProxy getProxy() {
        if (stack.isEmpty()) {
            while (stack.isEmpty() && waitingRound > 0) {
                try {
                    logger.log(Level.WARNING, "waiting for proxy ...");
                    Thread.sleep(5 * timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                waitingRound--;
                System.out.println("stack size: " + stack.size());
            }
            if (stack.isEmpty())
                throw new RuntimeException("something wrong with the code or internet connection");
        } else if (stack.size() < 3) {
            // wake up fetcher
            fetcher.interrupt();
        }
        HelloProxy p = stack.pop();
        if (checkConnection(p, checkURL1)) {
            waitingRound = MAX_WAITING_ROUND;
            return p;
        }else
            return getProxy();
    }

    /**
     * Close the fetcher - kill all threads
     */
    public void close() {
        fetcher.stopExecuting();
        fetcher.interrupt();
        checker1.stopExecuting();
        checker2.stopExecuting();
        checker3.stopExecuting();
    }

    private class FetcherThread extends Thread {
        private boolean running = true;
        /**
         *  fetching candidate proxy lists
         *  fill in the candidate queue
         *  wake up checkers
         */
        @Override
        public void run() {
            while (running) {
                // fetching proxy and add to candidate queue
                logger.log(Level.INFO, "fetcher running ...");
                fetchingSocksProxy();
                startCheckers();
                fetchingHttpProxy();
                try {
                    logger.log(Level.INFO, "fetcher go to sleep ...");
                    sleep(1000 * 600);
                } catch (InterruptedException ignored) {
                }
            }
        }

        void stopExecuting() {
            running = false;
            logger.log(Level.INFO, "fetcher stopping ...");
        }
    }

    private class CheckerThread extends Thread {
        private boolean running = true;
        @Override
        public void run() {
            // pick up proxy to check from the queue
            // check the connection of the proxy
            // if unreachable and allow double check
            // put the proxy back again (only once)
            logger.log(Level.INFO, "checker running ...");
            try {
                HelloProxy p = candidateQueue.poll();
                while (p != null && running) {
                    // check the connectivity of the proxy p
                    if (p.isCheckAgain()) {
                        if (checkConnection(p, checkURL2)) {
                            stack.push(p);
                        }
                    } else {
                        if (checkConnection(p, checkURL1)) {
                            stack.push(p);
                        } else if (doubleCheck){
                            p.setCheckAgain();
                            candidateQueue.put(p);
                        }
                    }

                    p = candidateQueue.poll(timeout * 2, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException ignored) {
            }
            logger.log(Level.INFO, "checker stopping ...   stack size: " + stack.size());
        }

        void stopExecuting() {
            running = false;
        }
    }

    private void startCheckers() {
        checker1 = new CheckerThread();
        checker2 = new CheckerThread();
        checker3 = new CheckerThread();
        checker1.start();
        checker2.start();
        checker3.start();
    }

    private void fetchingSocksProxy() {
        try {
            // fetching from the website and put them into the candidate queue
            URL url = new URL("http://www.socks-proxy.net/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(fetchingProxy);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36");
            conn.usingProxy();
            BufferedReader br;
            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 304) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher matcher = Pattern.compile("[tboydr<>]+([0-9.]+)[tdr<>/]+([0-9]+).*").matcher(line);
                    if (matcher.find()) {
                        String[] tmp = line.split("</td><td>");
                        try {
                            candidateQueue.put(new HelloProxy(tmp[0].replaceAll("[tbodyr<>]+", ""), tmp[1], "SOCKS"));
                        } catch (InvalidArgumentException e) {
                            logger.warning("something wrong with your code");
                            e.printStackTrace();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                br.close();
            }
            logger.log(Level.INFO, "size of candidate queue: " + candidateQueue.size());
        }  catch (IOException e) {
            System.out.println("Connection to free-proxy-list site failed");
            e.printStackTrace();
        }
    }

    private void fetchingHttpProxy() {
        try {
            URL url = new URL("http://free-proxy-list.net/anonymous-proxy.html");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(fetchingProxy);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36");
            conn.usingProxy();
            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 304) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher matcher = Pattern.compile("[tboydr<>]+([0-9.]+)[tdr<>/]+([0-9]+).*").matcher(line);
                    if (matcher.find()) {
                        String[] tmp = line.split("</td><td>");
                        try {
                            candidateQueue.put(new HelloProxy(tmp[0].replaceAll("[tbodyr<>]+", ""), tmp[1], (tmp[6].equals("yes") ? "HTTPS" : "HTTP")));
                        } catch (InvalidArgumentException e) {
                            e.printStackTrace();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                br.close();
            }
            logger.log(Level.INFO, "size of candidate queue: " + candidateQueue.size());
        } catch (IOException e) {
            System.out.println("Connection to free-proxy-list site failed");
            e.printStackTrace();
        }
    }

    private boolean checkConnection(HelloProxy p, String checkURL) {
        try {
            HttpURLConnection conn = (HttpURLConnection) (new URL(checkURL)).openConnection(new Proxy(p.getProxyType(), p.getAddress()));
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36");
            conn.usingProxy();
            int rc = conn.getResponseCode();
            if (rc == 200 || rc == 340 || rc == 302) {
                logger.log(Level.INFO, "proxy available: " + p.toString());
                return true;
            } else {
                logger.log(Level.INFO, p.toString() + "  response code: " + rc);
                return false;
            }
        }catch (SocketTimeoutException e){
            logger.log(Level.FINE, p.toString() + " - " + e.getMessage());
            return false;
        } catch (SocketException e) {
            logger.log(Level.FINE, p.toString() + " - " + e.getMessage());
            return false;
        } catch (RuntimeException e) {
            logger.log(Level.FINE, p.toString() + " - " + e.getMessage());
            return false;
        } catch (SSLHandshakeException e){
            logger.log(Level.WARNING, p.toString() + " - " + e.getMessage());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
