package com.jeka8833.tntserver.util;

import com.google.gson.Gson;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class Util {
    private static final Logger LOGGER = LogManager.getLogger(Util.class);
    public static final Gson GSON = new Gson();
    public static final OkHttpClient HTTP_CLIENT = createSocket();

    public static String getParam(final String[] args, final String key) {
        for (int i = 0; i < args.length - 1; i++)
            if (args[i].equalsIgnoreCase(key))
                return args[i + 1];
        return System.getenv(key.substring(1).toUpperCase());
    }

    @NotNull
    public static UUID fromString(@NotNull String input) {
        return UUID.fromString(
                input.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }

    @NotNull
    @Contract("_ -> new")
    public static <V> ArrayList<V> pollAll(@NotNull Queue<V> queue) {
        ArrayList<V> temp = new ArrayList<>();

        V polled;
        while ((polled = queue.poll()) != null) temp.add(polled);
        temp.trimToSize();

        return temp;
    }

    @NotNull
    @Contract(pure = true, value = "_, _, _ -> new")
    public static ThreadFactory getThreadFactory(@NotNull String name,
                                                 @Range(from = Thread.MIN_PRIORITY, to = Thread.MAX_PRIORITY)
                                                 int priority, boolean daemon) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setPriority(priority);
            t.setDaemon(daemon);
            return t;
        };
    }

    private static OkHttpClient createSocket() {
        OkHttpClient.Builder newBuilder = new OkHttpClient.Builder();
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            newBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
            newBuilder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            LOGGER.error("Fail create noSSL socket", e);
        }

        newBuilder.fastFallback(true);

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(20);
        newBuilder.dispatcher(dispatcher);
        newBuilder.connectionPool(new ConnectionPool(5, 60, TimeUnit.SECONDS));
        return newBuilder.build();
    }
}
