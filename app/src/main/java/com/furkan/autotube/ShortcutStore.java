package com.furkan.autotube;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

final class ShortcutStore {
    private static final String PREFS = "autotube_shortcuts";
    private static final String KEY = "items";
    private ShortcutStore() {}

    static synchronized List<Item> read(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ArrayList<Item> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;
                String id=o.optString("id", ""), name=o.optString("name", "");
                String value=o.optString("value", ""), type=o.optString("type", "");
                if (id.isEmpty() || name.isEmpty() || value.isEmpty()) continue;
                result.add(new Item(id, name, value, "app".equals(type)));
            }
        } catch (JSONException ignored) {}
        return Collections.unmodifiableList(result);
    }

    static synchronized void removeBuiltinShortcuts(Context context) {
        ArrayList<Item> items=new ArrayList<>();
        for(Item item:read(context))if(!item.id.equals("builtin-youtube")&&!item.id.equals("builtin-browser"))items.add(item);
        write(context,items);
    }

    static synchronized void add(Context context, String name, String value, boolean app) {
        JSONArray array = new JSONArray();
        for (Item item : read(context)) array.put(toJson(item));
        array.put(toJson(new Item(Long.toString(System.nanoTime()), name.trim(), value.trim(), app)));
        prefs(context).edit().putString(KEY, array.toString()).apply();
    }

    static synchronized void remove(Context context, String id) {
        JSONArray array = new JSONArray();
        for (Item item : read(context)) if (!item.id.equals(id)) array.put(toJson(item));
        prefs(context).edit().putString(KEY, array.toString()).apply();
    }

    static synchronized void update(Context context, String id, String name, String value) {
        JSONArray array = new JSONArray();
        for (Item item : read(context)) {
            if (item.id.equals(id)) array.put(toJson(new Item(item.id, name.trim(), value.trim(), item.app)));
            else array.put(toJson(item));
        }
        prefs(context).edit().putString(KEY, array.toString()).apply();
    }

    static synchronized void removeAppItems(Context context) {
        ArrayList<Item> items=new ArrayList<>();for(Item item:read(context))if(!item.app)items.add(item);write(context,items);
    }

    static synchronized void move(Context context,String id,int targetIndex){
        ArrayList<Item> items=new ArrayList<>(read(context));int from=-1;
        for(int i=0;i<items.size();i++)if(items.get(i).id.equals(id)){from=i;break;}
        if(from<0)return;Item item=items.remove(from);int to=Math.max(0,Math.min(targetIndex,items.size()));items.add(to,item);write(context,items);
    }

    private static boolean hasId(List<Item> items,String id){for(Item item:items)if(item.id.equals(id))return true;return false;}
    private static void write(Context context,List<Item> items){JSONArray array=new JSONArray();for(Item item:items)array.put(toJson(item));prefs(context).edit().putString(KEY,array.toString()).apply();}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    private static JSONObject toJson(Item item) {
        JSONObject o = new JSONObject();
        try { o.put("id",item.id); o.put("name",item.name); o.put("value",item.value); o.put("type",item.app?"app":"web"); }
        catch (JSONException ignored) {}
        return o;
    }
    static String normalizeUrl(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return "";
        if (!s.matches("(?i)^https?://.*")) s = "https://" + s;
        Uri u = Uri.parse(s);
        return (u.getHost() == null) ? "" : u.toString();
    }

    static final class Item {
        final String id, name, value;
        final boolean app;
        Item(String id, String name, String value, boolean app) { this.id=id; this.name=name; this.value=value; this.app=app; }
    }
}
