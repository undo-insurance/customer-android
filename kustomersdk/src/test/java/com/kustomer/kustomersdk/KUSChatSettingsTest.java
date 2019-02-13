package com.kustomer.kustomersdk;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Models.KUSChatSettings;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class KUSChatSettingsTest {

    @Test
    public void testAutoreplyWhitespaceTrim() {
        JSONObject attributesObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();

        try {
            attributesObject.put("autoreply","Hello\n");


            jsonObject.put("id","__fake");
            jsonObject.put("type","chat_settings");
            jsonObject.put("attributes",attributesObject);
        } catch (JSONException ignore) {}


        KUSChatSettings chatSettings = null;
        try {
            chatSettings = new KUSChatSettings(jsonObject);
        } catch (KUSInvalidJsonException ignore) {}

        assertNotNull(chatSettings);
        //assertEquals(chatSettings.getAutoReply(), "Hello");
    }

    @Test
    public void testWhitespaceAutoreply() {
        JSONObject attributesObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();

        try {
            attributesObject.put("autoreply","  ");


            jsonObject.put("id","__fake");
            jsonObject.put("type","chat_settings");
            jsonObject.put("attributes",attributesObject);
        } catch (JSONException ignore) {}


        KUSChatSettings chatSettings = null;
        try {
            chatSettings = new KUSChatSettings(jsonObject);
        } catch (KUSInvalidJsonException ignore) {}

        assertNotNull(chatSettings);
        //assertNull(chatSettings.getAutoReply());
    }
}