import com.sun.javaws.exceptions.InvalidArgumentException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;

/**
 * Created by lss on 11/24/16.
 */
public class HelloProxy {
    public enum Type{
        HTTP, HTTPS, SOCKS;
    }

    private String ip, port;
    private Type type;
    private boolean checkAgain = false;

    public HelloProxy(String ip, String port, String s) throws InvalidArgumentException {
        this.ip = ip;
        this.port = port;
        this.type = parseType(s);
        if (this.type == null) {
            String[] args = {ip, port, s};
            throw new InvalidArgumentException(args);
        }
    }

    private Type parseType(String s) {
        if (s.equals("HTTP"))
            return Type.HTTP;
        else if (s.equals("HTTPS"))
            return Type.HTTPS;
        else if (s.equals("SOCKS"))
            return Type.SOCKS;
        return null;
    }

    public HelloProxy(String addr) throws InvalidArgumentException {
        String[] s = addr.split(":");
        if (s.length > 1) {
            this.ip = s[0];
            this.port = s[1];
            this.type = parseType(s[2]);
        } else {
            String[] args = {addr};
            throw new InvalidArgumentException(args);
        }
    }

    void setCheckAgain() {
        checkAgain = true;
    }

    boolean isCheckAgain() {
        return checkAgain;
    }

    /**
     * The InetSocketAddress field in setting a proxy for web connection
     */
    public InetSocketAddress getAddress() {
        return new InetSocketAddress(ip, Integer.parseInt(port));
    }

    InetAddress getInetAddress() {
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    Type getType() {
        return type;
    }

    public Proxy.Type getProxyType() {
        return (type == Type.SOCKS) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
    }

    @Override
    public String toString() {
        return ip + ":" + port + ":" + type;
    }
}
