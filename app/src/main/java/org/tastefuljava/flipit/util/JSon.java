package org.tastefuljava.flipit.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tastefuljava.flipit.domain.Activity;
import org.tastefuljava.flipit.domain.Facet;
import org.tastefuljava.flipit.domain.User;

import java.util.List;

public class JSon {
    public static String stringify(Facet facet) throws JSONException {
        JSONObject obj = objectify(facet);
        return obj.toString();
    }

    public static Facet parseFacet(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return toFacet(obj);
    }

    public static String stringify(User user) throws JSONException {
        JSONObject obj = objectify(user);
        return obj.toString();
    }

    public static User parseUser(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return toUser(obj);
    }

    public static String stringify(Activity activity) throws JSONException {
        JSONObject obj = objectify(activity);
        return obj.toString();
    }

    public static Activity parseActivity(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return toActivity(obj);
    }

    private static JSONObject objectify(Facet facet) throws JSONException {
        JSONObject obj = new JSONObject();
        if (facet.getSymbol() != null) {
            obj.put("symbol", facet.getSymbol());
        }
        if (facet.getLabel() != null) {
            obj.put("label", facet.getLabel());
        }
        return obj;
    }

    private static Facet toFacet(JSONObject obj) throws JSONException {
        String symbol = obj.has("symbol") ? obj.getString("symbol") : null;
        String label = obj.has("label") ? obj.getString("label") : null;
        return new Facet(symbol, label);
    }

    private static JSONObject objectify(User user) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("email", user.getEmail());
        if (user.getDisplayName() != null) {
            obj.put("displayName", user.getDisplayName());
        }
        JSONArray facetArray = new JSONArray();
        List<Facet> facets = user.getFacets();
        for (int i = 0; i < facets.size(); ++i) {
            facetArray.put(i, objectify(facets.get(i)));
        }
        obj.put("facets", facetArray);
        return obj;
    }

    private static User toUser(JSONObject obj) throws JSONException {
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
    }

    private static JSONObject objectify(Activity activity) throws JSONException {
        JSONObject obj = new JSONObject();
        if (activity.getFacetNumber() != null) {
            obj.put("facetNumber", activity.getFacetNumber());
        }
        obj.put("startTime", activity.getStartTime());
        if (activity.getComment() != null) {
            obj.put("comment", activity.getComment());
        }
        return obj;
    }

    private static Activity toActivity(JSONObject obj) throws JSONException {
        Activity activity = new Activity();
        if (obj.has("facetNumber")) {
            activity.setFacetNumber(obj.getInt("facetNumber"));
        }
        activity.setStartTime(obj.getString("startTime"));
        if (obj.has("comment")) {
            activity.setComment(obj.getString("comment"));
        }
        return activity;
    }
}
