package com.jeka8833.tntserver.util;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

public class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);
    public static final OkHttpClient HTTP_CLIENT = createSocket();

    public static boolean isDJFix(long moduleList) {
        return (moduleList & (1L << 6L)) == (1L << 6L);
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
