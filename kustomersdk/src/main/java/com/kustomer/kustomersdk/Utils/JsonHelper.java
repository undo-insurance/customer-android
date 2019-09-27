package com.kustomer.kustomersdk.Utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kustomer.kustomersdk.Enums.KUSChatMessageType;
import com.kustomer.kustomersdk.Helpers.KUSDate;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Helpers.KUSLog;
import com.kustomer.kustomersdk.Models.KUSChatMessage;
import com.kustomer.kustomersdk.Models.KUSModel;
import com.kustomer.kustomersdk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Junaid on 1/23/2018.
 */

public class JsonHelper {

    public static int getErrorStatus(@Nullable Error error) {
        if (error == null)
            return 0;
        try {
            return new JSONObject(error.getMessage()).getJSONArray("errors")
                    .getJSONObject(0).getInt("status");
        } catch (Exception e) {
            return 0;
        }
    }

    @NonNull
    public static JSONObject jsonObjectFromHashMap(@NonNull HashMap<String, Object> dataMap) {
        JSONObject jsonObject = new JSONObject();
        try {
            for (String key : dataMap.keySet())
                jsonObject.put(key, dataMap.get(key));

        } catch (JSONException e) {
            KUSLog.KUSLogError(e.getMessage());
        }

        return jsonObject;
    }

    public static URL urlFromKeyPath(JSONObject jsonObject, String keyPath) {
        String value = stringFromKeyPath(jsonObject, keyPath);

        if (value != null && !value.isEmpty())
            try {
                return new URL(value);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }


        return null;
    }

    public static String stringFromKeyPath(JSONObject jsonObject, String keyPath) {
        try {
            String[] keys = keyPath.split("[.]");
            for (int i = 0; i < keys.length - 1; i++) {
                jsonObject = jsonObject.getJSONObject(keys[i]);
            }

            String value = keys.length > 0 ? jsonObject.getString(keys[keys.length - 1]) : jsonObject.getString(keyPath);
            return value.equals("null") ? null : value;
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONArray arrayFromKeyPath(JSONObject jsonObject, String keyPath) {
        try {
            String[] keys = keyPath.split("[.]");
            for (int i = 0; i < keys.length - 1; i++) {
                jsonObject = jsonObject.getJSONObject(keys[i]);
            }
            return keys.length > 0 ? jsonObject.getJSONArray(keys[keys.length - 1]) : jsonObject.getJSONArray(keyPath);
        } catch (Exception e) {
            return null;
        }
    }

    public static ArrayList<String> arrayListFromKeyPath(JSONObject jsonObject, String keyPath) {
        try {
            Gson googleJson = new Gson();
            String[] keys = keyPath.split("[.]");
            for (int i = 0; i < keys.length - 1; i++) {
                jsonObject = jsonObject.getJSONObject(keys[i]);
            }
            JSONArray jsonArray = keys.length > 0 ? jsonObject.getJSONArray(keys[keys.length - 1]) : jsonObject.getJSONArray(keyPath);

            Type listType = new TypeToken<ArrayList<String>>() {
            }.getType();
            return googleJson.fromJson(jsonArray.toString(), listType);
        } catch (Exception e) {
            return null;
        }
    }

    public static HashMap<String, String> hashMapFromKeyPath(JSONObject jsonObject, String keyPath) {
        try {
            Gson googleJson = new Gson();
            String[] keys = keyPath.split("[.]");
            for (int i = 0; i < keys.length - 1; i++) {
                jsonObject = jsonObject.getJSONObject(keys[i]);
            }
            JSONObject jsonObject1 = keys.length > 0 ? jsonObject.getJSONObject(keys[keys.length - 1]) : jsonObject.getJSONObject(keyPath);

            Type hashMapType = new TypeToken<HashMap<String, String>>() {
            }.getType();
            return googleJson.fromJson(jsonObject1.toString(), hashMapType);
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean boolFromKeyPath(JSONObject jsonObject, String keyPath) {
        try {
            String[] keys = keyPath.split("[.]");
            for (int i = 0; i < keys.length - 1; i++) {
                jsonObject = jsonObject.getJSONObject(keys[i]);
            }
            return keys.length > 0 ? jsonObject.getBoolean(keys[keys.length - 1]) : jsonObject.getBoolean(keyPath);
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer integerFromKeyPath(JSONObject jsonObject, String keyPath) {
        try {
            String[] keys = keyPath.split("[.]");
            for (int i = 0; i < keys.length - 1; i++) {
                jsonObject = jsonObject.getJSONObject(keys[i]);
            }
            return keys.length > 0 ? jsonObject.getInt(keys[keys.length - 1]) : jsonObject.getInt(keyPath);
        } catch (Exception e) {
            return 0;
        }
    }

    public static Double doubleFromKeyPath(JSONObject jsonObject, String keyPath) {
        try {
            String[] keys = keyPath.split("[.]");
            for (int i = 0; i < keys.length - 1; i++) {
                jsonObject = jsonObject.getJSONObject(keys[i]);
            }
            return keys.length > 0 ? jsonObject.getDouble(keys[keys.length - 1]) : jsonObject.getDouble(keyPath);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static Date dateFromKeyPath(JSONObject jsonObject, String keyPath) {

        try {
            String[] keys = keyPath.split("[.]");
            for (int i = 0; i < keys.length - 1; i++) {
                jsonObject = jsonObject.getJSONObject(keys[i]);
            }
            String value = keys.length > 0 ? jsonObject.getString(keys[keys.length - 1]) : jsonObject.getString(keyPath);
            if (value != null && !value.equals("null"))
                return KUSDate.dateFromString(value);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject jsonObjectFromKeyPath(JSONObject json, String keyPath) {
        try {
            String[] keys = keyPath.split("[.]");
            for (int i = 0; i < keys.length - 1; i++) {
                json = json.getJSONObject(keys[i]);
            }
            return keys.length > 0 ? json.getJSONObject(keys[keys.length - 1]) : json.getJSONObject(keyPath);
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject stringToJson(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<KUSModel> kusChatModelsFromJSON(Context context, JSONObject jsonObject) {
        if (jsonObject == null)
            return null;

        KUSChatMessage standardChatMessage = null;

        try {
            standardChatMessage = new KUSChatMessage(jsonObject);
        } catch (KUSInvalidJsonException e) {
            e.printStackTrace();
        }

        if (standardChatMessage == null)
            return new ArrayList<>();

        String body = KUSUtils.KUSUnescapeBackslashesFromString(standardChatMessage.getBody());
        standardChatMessage.setBody(body);

        //The markdown url pattern we want to detect
        String imagePattern = "!\\[.*\\]\\(.*\\)";
        List<KUSModel> chatMessages = new ArrayList<>();

        Pattern regex = Pattern.compile(imagePattern);

        int lastId = 0;
        int lastLocation = 0;

        if (!TextUtils.isEmpty(body)) {
            Matcher matcher = regex.matcher(body);

            while (matcher.find()) {
                Matcher urlMatcher = Pattern.compile(KUSConstants.Pattern.URL_PATTERN).matcher(matcher.group());

                if (urlMatcher.find()) {
                    String matchedText = KUSUtils.KUSUnescapeBackslashesFromString(urlMatcher.group());
                    try {
                        URL matchedURL = new URL(matchedText);
                        JSONObject previousJSON = new JSONObject(jsonObject.toString());
                        previousJSON.put("id", String.format(Locale.getDefault(),
                                "%s_%d", standardChatMessage.getId(), lastId));

                        String previousText = body.substring(lastLocation, matcher.start());
                        previousText = previousText.trim();
                        if (previousText.length() > 0) {
                            try {
                                KUSChatMessage previousChatMessage = new KUSChatMessage(previousJSON);
                                previousChatMessage.setBody(previousText);
                                chatMessages.add(previousChatMessage);
                                lastId++;
                            } catch (KUSInvalidJsonException e) {
                                e.printStackTrace();
                            }
                        }

                        JSONObject imageJSON = new JSONObject(jsonObject.toString());
                        imageJSON.put("id", String.format(Locale.getDefault(),
                                "%s_%d", standardChatMessage.getId(), lastId));
                        KUSChatMessage imageMessage = new KUSChatMessage(imageJSON,
                                KUSChatMessageType.KUS_CHAT_MESSAGE_TYPE_IMAGE, matchedURL);
                        imageMessage.setBody(matchedText);
                        chatMessages.add(imageMessage);
                        lastId++;
                        lastLocation = matcher.end();


                    } catch (MalformedURLException | JSONException | KUSInvalidJsonException e) {
                        e.printStackTrace();
                    }
                }

            }

            if (chatMessages.size() == 0)
                chatMessages.add(standardChatMessage);
            else {
                JSONObject previousJSON = null;
                try {

                    previousJSON = new JSONObject(jsonObject.toString());
                    previousJSON.put("id", String.format(Locale.getDefault(),
                            "%s_%d", standardChatMessage.getId(), lastId));

                    String previousText = body.substring(lastLocation);
                    previousText = previousText.trim();
                    if (previousText.length() > 0) {
                        try {
                            KUSChatMessage previousMessage = new KUSChatMessage(previousJSON);
                            previousMessage.setBody(previousText);
                            chatMessages.add(previousMessage);
                            lastId++;
                        } catch (KUSInvalidJsonException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if (standardChatMessage.getAttachmentIds() != null) {
            for (Object id : standardChatMessage.getAttachmentIds()) {
                String attachmentId = (String) id;
                try {
                    URL imageUrl = KUSChatMessage.attachmentUrlForMessageId(standardChatMessage.getId(), attachmentId);

                    JSONObject imageJson = new JSONObject(jsonObject.toString());
                    imageJson.put("id", String.format(Locale.getDefault(),
                            "%s_%d", standardChatMessage.getId(), lastId));
                    KUSChatMessage imageMessage = new KUSChatMessage(imageJson,
                            KUSChatMessageType.KUS_CHAT_MESSAGE_TYPE_IMAGE, imageUrl);

                    imageMessage.setBody(context.getString(R.string.com_kustomer_attachment));
                    chatMessages.add(imageMessage);
                    lastId++;
                } catch (MalformedURLException | JSONException | KUSInvalidJsonException e) {
                    e.printStackTrace();
                }

            }
        }

        return chatMessages;
    }
}
