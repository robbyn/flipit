package org.tastefuljava.flipit.server;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerConnection {
    private static final String TAG = ServerConnection.class.getSimpleName();
    private static final String BASE_URI = "https://perry.ch/flipit-server/api/";

    private final Context context;
    private final String username;
    private final String password;
    private static final ExecutorService exec = Executors.newFixedThreadPool(1);

    public static ServerConnection open(Context context, String username, String password) {
        ServerConnection cnt = new ServerConnection(context, username,password);
        cnt.connect();
        return cnt;
    }

    private ServerConnection(Context context, String username, String password) {
        this.context = context;
        this.username = username;
        this.password = password;
    }

    public void getLastActivity() {
        exec.submit(() -> {
            try {
                fetchLastActivity();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        });
    }

    public void sendFacet(final int facetNumber) {
        exec.submit(() -> {
            try {
                postFacet(facetNumber);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
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

    private void connect() {
        exec.submit(() -> {
            try {
                fetchUser();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        });
    }

    private void publishCurrentUser(String user) {
        Log.i(TAG, "Current user: " + user);
        Intent intent = new Intent("current_user");
        intent.putExtra("user", user);
        context.sendBroadcast(intent);
    }

    private void fetchUser() throws IOException {
        HttpURLConnection cnt = openConnection("user");
        cnt.setRequestMethod("GET");
        int st = cnt.getResponseCode();
        if (st != 200) {
            throw new IOException("Error " + st + " returned by the server");
        }
        String json = readResponse(cnt);
        publishCurrentUser(json);
    }

    private void fetchLastActivity() throws IOException {
        HttpURLConnection cnt = openConnection("activity/last");
        cnt.setRequestMethod("GET");
        int st = cnt.getResponseCode();
        if (st != 200) {
            throw new IOException("Error " + st + " returned by the server");
        }
        String json = readResponse(cnt);
        publishLastActivity(json);
    }

    private void publishLastActivity(String json) {
        Log.i(TAG, "Activity: " + json);
        Intent intent = new Intent("last_activity");
        intent.putExtra("activity", json);
        context.sendBroadcast(intent);
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
