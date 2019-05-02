package com.kustomer.kustomersdk;

import android.os.Build;
import android.support.annotation.Nullable;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Models.KUSChatSettings;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.kustomer.kustomersdk.Enums.KUSBusinessHoursAvailability.KUS_BUSINESS_HOURS_AVAILABILITY_HIDE_CHAT;
import static com.kustomer.kustomersdk.Enums.KUSBusinessHoursAvailability.KUS_BUSINESS_HOURS_AVAILABILITY_OFFLINE;
import static com.kustomer.kustomersdk.Enums.KUSBusinessHoursAvailability.KUS_BUSINESS_HOURS_AVAILABILITY_ONLINE;
import static com.kustomer.kustomersdk.Enums.KUSVolumeControlMode.KUS_VOLUME_CONTROL_MODE_DELAYED;
import static com.kustomer.kustomersdk.Enums.KUSVolumeControlMode.KUS_VOLUME_CONTROL_MODE_UNKNOWN;
import static com.kustomer.kustomersdk.Enums.KUSVolumeControlMode.KUS_VOLUME_CONTROL_MODE_UPFRONT;
import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class KUSChatSettingsTest {

    //region public methods

    @Test
    public void testKUSBusinessHoursAvailability() {

        //Online
        KUSChatSettings chatSettingsOnline = getChatSettingForSingleAttrKeyAndValue("offhoursDisplay",
                "online");
        assertNotNull(chatSettingsOnline);
        assertEquals(chatSettingsOnline.getAvailability(), KUS_BUSINESS_HOURS_AVAILABILITY_ONLINE);

        //Offline
        KUSChatSettings chatSettingsOffline = getChatSettingForSingleAttrKeyAndValue("offhoursDisplay",
                "offline");
        assertNotNull(chatSettingsOffline);
        assertEquals(chatSettingsOffline.getAvailability(), KUS_BUSINESS_HOURS_AVAILABILITY_OFFLINE);

        //Hide Chat
        KUSChatSettings chatSettingsHideChat = getChatSettingForSingleAttrKeyAndValue("offhoursDisplay",
                "unknown");
        assertNotNull(chatSettingsHideChat);
        assertEquals(chatSettingsHideChat.getAvailability(), KUS_BUSINESS_HOURS_AVAILABILITY_HIDE_CHAT);
    }

    @Test
    public void testKUSVolumeControlMode() {
        JSONObject jsonUpfront = new JSONObject();

        try {
            jsonUpfront.put("mode", "upfront");
        } catch (JSONException ignore) {
        }

        KUSChatSettings chatSettingsUpfront = getChatSettingForSingleAttrKeyAndValue("volumeControl",
                jsonUpfront);
        assertNotNull(chatSettingsUpfront);
        assertEquals(chatSettingsUpfront.getVolumeControlMode(), KUS_VOLUME_CONTROL_MODE_UPFRONT);

        JSONObject jsonDelayed = new JSONObject();

        try {
            jsonDelayed.put("mode", "delayed");
        } catch (JSONException ignore) {
        }

        KUSChatSettings chatSettingsDelayed = getChatSettingForSingleAttrKeyAndValue("volumeControl",
                jsonDelayed);
        assertNotNull(chatSettingsDelayed);
        assertEquals(chatSettingsDelayed.getVolumeControlMode(), KUS_VOLUME_CONTROL_MODE_DELAYED);

        JSONObject jsonUnknown = new JSONObject();

        try {
            jsonUnknown.put("mode", "unknown");
        } catch (JSONException ignore) {
        }

        KUSChatSettings chatSettingsUnknown = getChatSettingForSingleAttrKeyAndValue("volumeControl",
                jsonUnknown);
        assertNotNull(chatSettingsUnknown);
        assertEquals(chatSettingsUnknown.getVolumeControlMode(), KUS_VOLUME_CONTROL_MODE_UNKNOWN);

    }

    @Test
    public void testTypingIndicatorSettingCustomerWeb() {

        //typingIndicatorCustomerWeb should be false
        KUSChatSettings typingIndicatorCustomerWebNull =
                getChatSettingForSingleAttrKeyAndValue("showTypingIndicatorCustomerWeb",
                        null);
        assertNotNull(typingIndicatorCustomerWebNull);
        assertFalse(typingIndicatorCustomerWebNull.getShouldShowTypingIndicatorCustomerWeb());

        //typingIndicatorCustomer should be false
        KUSChatSettings typingIndicatorCustomerWebFalse =
                getChatSettingForSingleAttrKeyAndValue("showTypingIndicatorCustomerWeb",
                        false);
        assertNotNull(typingIndicatorCustomerWebFalse);
        assertFalse(typingIndicatorCustomerWebFalse.getShouldShowTypingIndicatorCustomerWeb());

        //typingIndicatorCustomer should be true
        KUSChatSettings typingIndicatorCustomerWebTrue =
                getChatSettingForSingleAttrKeyAndValue("showTypingIndicatorCustomerWeb",
                        true);
        assertNotNull(typingIndicatorCustomerWebTrue);
        assertTrue(typingIndicatorCustomerWebTrue.getShouldShowTypingIndicatorCustomerWeb());
    }

    @Test
    public void testTypingIndicatorSettingWeb() {

        //typingIndicatorWeb should be false
        KUSChatSettings typingIndicatorWebNull =
                getChatSettingForSingleAttrKeyAndValue("showTypingIndicatorWeb",
                        null);
        assertNotNull(typingIndicatorWebNull);
        assertFalse(typingIndicatorWebNull.getShouldShowTypingIndicatorWeb());

        //typingIndicatorWeb should be false
        KUSChatSettings typingIndicatorWebFalse =
                getChatSettingForSingleAttrKeyAndValue("showTypingIndicatorWeb",
                        false);
        assertNotNull(typingIndicatorWebFalse);
        assertFalse(typingIndicatorWebFalse.getShouldShowTypingIndicatorWeb());

        //typingIndicatorWeb should be true
        KUSChatSettings typingIndicatorWebTrue =
                getChatSettingForSingleAttrKeyAndValue("showTypingIndicatorWeb",
                        true);
        assertNotNull(typingIndicatorWebTrue);
        assertTrue(typingIndicatorWebTrue.getShouldShowTypingIndicatorWeb());
    }

    //endregion

    //region private methods

    @Nullable
    private KUSChatSettings getChatSettingForSingleAttrKeyAndValue(String attr, Object value) {
        JSONObject attributesObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();

        try {
            attributesObject.put(attr, value);

            jsonObject.put("id", "__fake");
            jsonObject.put("type", "chat_settings");
            jsonObject.put("attributes", attributesObject);
        } catch (JSONException ignore) {
        }

        try {
            return new KUSChatSettings(jsonObject);
        } catch (KUSInvalidJsonException ignore) {
            return null;
        }
    }

    //endregion

}