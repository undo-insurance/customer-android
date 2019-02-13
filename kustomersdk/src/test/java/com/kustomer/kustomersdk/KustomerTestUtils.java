package com.kustomer.kustomersdk;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


class KustomerTestUtils {

    static boolean mapsAreEqual(Map<String, Object> map1, Map<String, Object> map2) {
        for (String k : map1.keySet()) {
            if (!KustomerTestUtils.objectsAreEqual(map1.get(k), map2.get(k))) {
                return false;
            }
        }
        for (String y : map1.keySet()) {
            if (!map2.containsKey(y)) {
                return false;
            }
        }
        return true;
    }

    private static boolean objectsAreEqual(@Nullable Object obj1, @Nullable Object obj2) {
        if (obj1 == null && obj2 == null)
            return true;

        if (obj1 == null || obj2 == null)
            return false;

        if (obj1 instanceof JSONObject && obj2 instanceof JSONObject) {
            try {
                return areEqual(obj1, obj2);
            } catch (JSONException e) {
                return false;
            }
        }

        return obj1.equals(obj2);
    }

    static JSONObject combineJsonObjects(@Nullable JSONObject jsonObj1, @Nullable JSONObject jsonObj2) {
        JSONObject resultJsonObject = new JSONObject();
        if (jsonObj1 != null) {
            Iterator iterator = jsonObj1.keys();
            while (iterator.hasNext()) {
                String key = iterator.next().toString();
                try {
                    resultJsonObject.put(key, jsonObj1.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if (jsonObj2 != null) {
            Iterator iterator = jsonObj2.keys();
            while (iterator.hasNext()) {
                String key = iterator.next().toString();
                try {
                    resultJsonObject.put(key, jsonObj2.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return resultJsonObject;
    }

    private static boolean areEqual(Object ob1, Object ob2) throws JSONException {
        Object obj1Converted = convertJsonElement(ob1);
        Object obj2Converted = convertJsonElement(ob2);
        return obj1Converted.equals(obj2Converted);
    }

    private static Object convertJsonElement(Object elem) throws JSONException {
        if (elem instanceof JSONObject) {
            JSONObject obj = (JSONObject) elem;
            Iterator<String> keys = obj.keys();
            Map<String, Object> jsonMap = new HashMap<>();
            while (keys.hasNext()) {
                String key = keys.next();
                jsonMap.put(key, convertJsonElement(obj.get(key)));
            }
            return jsonMap;
        } else if (elem instanceof JSONArray) {
            JSONArray arr = (JSONArray) elem;
            Set<Object> jsonSet = new HashSet<>();
            for (int i = 0; i < arr.length(); i++) {
                jsonSet.add(convertJsonElement(arr.get(i)));
            }
            return jsonSet;
        } else {
            return elem;
        }
    }
}
