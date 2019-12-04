package org.tastefuljava.flipit.server;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ServerConnection {
    private static final String BASE_URI = "https://perry.ch/flipit-server/api/";

    private final String username;
    private final String password;
    private User currentUser;

    public static ServerConnection open(String username, String password) throws IOException {
        ServerConnection cnt = new ServerConnection(username,password);
        cnt.connect();
        return cnt;
    }

    private ServerConnection(String username, String password) {
        this.username = username;
        this.password = password;
    }

    private void connect() throws IOException {
        currentUser = fetchUser();
    }

    private User fetchUser() throws IOException {
        HttpURLConnection cnt = openConnection("user");
        cnt.setRequestMethod("GET");
        int st = cnt.getResponseCode();
        if (st != 200) {
            throw new IOException("Error " + st + " returned by the server");
        }
        String json = readResponse(cnt);
        try {
            JSONObject obj = new JSONObject(json);
            User user = new User();
            user.setEmail(obj.getString("email"));
            user.setDisplayName(obj.getString("displayName"));
            JSONArray facetArray = obj.getJSONArray("facets");
            for (int i = 0; i < facetArray.length(); ++i) {
                JSONObject f = facetArray.getJSONObject(i);
                user.addFacet(new Facet(f.getString("symbol"), f.getString("label")));
            }
            return user;
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
    }

    private String readResponse(HttpURLConnection cnt) throws IOException {
        try (InputStream stream = cnt.getInputStream();
             Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(reader)) {
            StringBuilder buf = new StringBuilder();
            String s = in.readLine();
            while (s != null) {
                buf.append(s);
                buf.append('\n');
                s = in.readLine();
            }
            return buf.toString();
        }
    }

    private HttpURLConnection openConnection(String path) throws IOException {
        URL url = new URL(BASE_URI + path);
        HttpURLConnection cnt = (HttpURLConnection) url.openConnection();
        cnt.setRequestProperty("Authorize", "Basic " + base64(username + ":" + password));
        return cnt;
    }

    private String base64(String s) {
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
    }
}
