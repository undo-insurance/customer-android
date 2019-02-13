package com.kustomer.kustomersdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


class KustomerTestUtils {

    static boolean mapsAreEqual(@NonNull Map<String, Object> map1, @NonNull Map<String, Object> map2) {
        for (String key : map1.keySet()) {
            if (!KustomerTestUtils.objectsAreEqual(map1.get(key), map2.get(key))) {
                return false;
            }
        }

        for (String key : map1.keySet()) {
            if (!map2.containsKey(key)) {
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

        try {
            Object obj1Converted = convertJsonToObjectIfNecessary(obj1);
            Object obj2Converted = convertJsonToObjectIfNecessary(obj2);
            return obj1Converted.equals(obj2Converted);
        } catch (JSONException e) {
            return false;
        }

    }

    @NonNull
    private static Object convertJsonToObjectIfNecessary(@NonNull Object obj) throws JSONException {
        if (obj instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) obj;
            Map<String, Object> jsonMap = new HashMap<>();

            Iterator<String> keys = jsonObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                jsonMap.put(key, convertJsonToObjectIfNecessary(jsonObj.get(key)));
            }

            return jsonMap;
        } else {
            return obj;
        }
    }

    @NonNull
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

}
