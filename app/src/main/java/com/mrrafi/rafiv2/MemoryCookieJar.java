package com.mrrafi.rafiv2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * In-memory cookie store for OkHttp.
 * Keeps session cookies across login + submit calls.
 */
public final class MemoryCookieJar implements CookieJar {

    private final Map<String, List<Cookie>> store = new HashMap<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String host = url.host();
        List<Cookie> current = store.getOrDefault(host, new ArrayList<>());
        for (Cookie incoming : cookies) {
            // Replace existing cookie with same name, or add new
            current.removeIf(c -> c.name().equals(incoming.name()));
            current.add(incoming);
        }
        store.put(host, current);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> result = new ArrayList<>();
        for (Map.Entry<String, List<Cookie>> entry : store.entrySet()) {
            for (Cookie cookie : entry.getValue()) {
                if (cookie.matches(url)) {
                    result.add(cookie);
                }
            }
        }
        return result;
    }
}
