package com.example.techwatch.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

public class UrlSafetyPolicy {
    public void validate(URI uri) throws IOException {
        if (uri == null || !uri.isAbsolute()) throw new IOException("絶対URLではありません");
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IOException("HTTP(S)以外のURLは取得できません");
        }
        if (uri.getUserInfo() != null) throw new IOException("認証情報を含むURLは取得できません");
        String host = uri.getHost();
        if (host == null || host.isBlank()) throw new IOException("URLのホストが不正です");
        InetAddress[] addresses;
        try { addresses = InetAddress.getAllByName(host); }
        catch (UnknownHostException error) { throw new IOException("ホストを解決できません: " + host, error); }
        if (addresses.length == 0) throw new IOException("ホストを解決できません: " + host);
        for (InetAddress address : addresses) {
            if (isBlocked(address)) throw new IOException("内部ネットワークのURLは取得できません: " + host);
        }
    }

    boolean isBlocked(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return true;
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first == 0 || first >= 224 || (first == 100 && second >= 64 && second <= 127);
        }
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }
}
