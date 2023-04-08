package com.Jeka8833.TNTServer.util;

import com.Jeka8833.TNTServer.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class Util {

    public static final int GOOD_AUTH = 0;
    public static final int FAIL_AUTH = 1;
    public static final int FAIL_PARSE = 2;
    public static final int FAIL_CONNECTION = 3;
    public static final int KEY_THROTTLING = 4;

    private static final Logger logger = LogManager.getLogger(Util.class);
    public static final HttpClient client = HttpClient.newHttpClient();

    public static String getParam(final String[] args, final String key) {
        for (int i = 0; i < args.length - 1; i++)
            if (args[i].equalsIgnoreCase(key))
                return args[i + 1];
        return System.getenv(key.substring(1).toUpperCase());
    }

    public static int checkKey(final UUID user, final UUID key) {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.hypixel.net/key?key=" + key)).GET().build();
        try {
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ParseUser parseUser = Main.GSON.fromJson(response.body(), ParseUser.class);
                if (parseUser == null || parseUser.record == null || parseUser.record.owner == null) {
                    return FAIL_CONNECTION;
                } else {
                    if (parseUser.record.owner.equals(user))
                        return GOOD_AUTH;
                    else
                        return FAIL_AUTH;
                }
            } else if (response.statusCode() == 403) {
                return FAIL_AUTH;
            } else if (response.statusCode() == 429) {
                return KEY_THROTTLING;
            }
        } catch (Exception ex) {
            logger.warn("Hypixel API is down", ex);
        }
        return FAIL_CONNECTION;
    }

    private static class ParseUser {
        private Record record;
    }

    private static class Record {
        private UUID owner;
    }
}
