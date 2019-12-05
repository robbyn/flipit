package org.tastefuljava.flipit.server;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ServerConnection {
    private static final String TAG = ServerConnection.class.getSimpleName();
    private static final String BASE_URI = "https://perry.ch/flipit-server/api/";

    private final String username;
    private final String password;
    private Future<User> currentUser;
    private static final ExecutorService exec = Executors.newFixedThreadPool(1);

    public static ServerConnection open(String username, String password) {
        ServerConnection cnt = new ServerConnection(username,password);
        cnt.connect();
        return cnt;
    }

    public User currentUser() {
        try {
            return currentUser.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public void sendFacet(final int facetNumber) {
        exec.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    postFacet(facetNumber);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void postFacet(int facetNumber) throws IOException {
        HttpURLConnection cnt = openConnection("activity/log");
        cnt.setRequestMethod("POST");
        cnt.setDoOutput(true);
        cnt.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        cnt.setRequestMethod("POST");
        try (Writer writer = new OutputStreamWriter(cnt.getOutputStream(),
                StandardCharsets.UTF_8)) {
            String parms = "facet=" + facetNumber;
            Log.i(TAG, parms);
            writer.write(parms);
        }
        int st = cnt.getResponseCode();
        if (st >= 200 && st <= 299) {
            Log.i(TAG, "Request sent");
        } else {
            Log.e(TAG, "Request error: " + st);
        }
    }

    private ServerConnection(String username, String password) {
        this.username = username;
        this.password = password;
    }

    private void connect() {
        currentUser = exec.submit(new Callable<User>() {
            @Override
            public User call() throws Exception {
                return fetchUser();
            }
        });
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
            if (obj.has("displayName")) {
                user.setDisplayName(obj.getString("displayName"));
            }
            JSONArray facetArray = obj.getJSONArray("facets");
            for (int i = 0; i < facetArray.length(); ++i) {
                JSONObject f = facetArray.getJSONObject(i);
                String symbol = f.has("symbol") ? f.getString("symbol") : null;
                String label = f.has("label") ? f.getString("label") : null;
                user.addFacet(new Facet(symbol, label));
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
        cnt.setRequestProperty("Authorization", "Basic " + base64(username + ":" + password));
        return cnt;
    }

    private String base64(String s) {
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
    }
}
