package com.kustomer.kustomersdk;

import android.os.Build;

import com.kustomer.kustomersdk.Enums.KUSTypingStatus;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Models.KUSTypingIndicator;
import com.kustomer.kustomersdk.Utils.KUSJsonHelper;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class KUSTypingIndicatorTest {

    @Test
    public void testTypingIndicatorParsing() {

        //Constructor should work as expected and all json values are correctly assigned
        //typing status for "typing" is assigned correctly
        JSONObject data = KUSJsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("id", "temp-id");
            put("userId", "temp-user");
            put("status", "typing");
        }});

        try {
            KUSTypingIndicator testTypingIndicator = new KUSTypingIndicator(data);
            assertEquals(testTypingIndicator.getId(), "temp-id");
            assertEquals(testTypingIndicator.getUserId(), "temp-user");
            assertEquals(testTypingIndicator.getStatus(), KUSTypingStatus.KUS_TYPING);
        } catch (KUSInvalidJsonException e) {
            Assert.fail("No exception should occurs.");
        }

        //typing status for "typing-ended" is assigned correctly
        data = KUSJsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("id", "temp-id");
            put("userId", "temp-user");
            put("status", "typing-ended");
        }});

        try {
            KUSTypingIndicator testTypingIndicator = new KUSTypingIndicator(data);
            assertEquals(testTypingIndicator.getId(), "temp-id");
            assertEquals(testTypingIndicator.getUserId(), "temp-user");
            assertEquals(testTypingIndicator.getStatus(), KUSTypingStatus.KUS_TYPING_ENDED);
        } catch (KUSInvalidJsonException ignore) {
        }

        //typing status is KUS_TYPING_UNKNOWN when incorrectly value is passed
        data = KUSJsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("id", "temp-id");
            put("userId", "temp-user");
            put("status", "unknown-status");
        }});

        try {
            KUSTypingIndicator testTypingIndicator = new KUSTypingIndicator(data);
            assertEquals(testTypingIndicator.getId(), "temp-id");
            assertEquals(testTypingIndicator.getUserId(), "temp-user");
            assertEquals(testTypingIndicator.getStatus(), KUSTypingStatus.KUS_TYPING_UNKNOWN);
        } catch (KUSInvalidJsonException ignore) {
        }

        //Throws KUSInvalidJsonException if id is not available
        data = KUSJsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("userId", "temp-user");
            put("status", "unknown-status");
        }});

        try {
            KUSTypingIndicator testTypingIndicator = new KUSTypingIndicator(data);
            assertNull(testTypingIndicator);
            Assert.fail("Exception should occurs if id is not available in JSON object.");
        } catch (KUSInvalidJsonException ignore) {
        }
    }

    @Test
    public void testTypingIndicatorStatusSetter() {
        //status setter should work as expected
        JSONObject data = KUSJsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("id", "temp-id");
            put("userId", "temp-user");
            put("status", "typing");
        }});

        try {
            KUSTypingIndicator testTypingIndicator = new KUSTypingIndicator(data);
            assertEquals(testTypingIndicator.getStatus(), KUSTypingStatus.KUS_TYPING);

            testTypingIndicator.setStatus(KUSTypingStatus.KUS_TYPING_ENDED);
            assertEquals(testTypingIndicator.getStatus(), KUSTypingStatus.KUS_TYPING_ENDED);

            testTypingIndicator.setStatus(KUSTypingStatus.KUS_TYPING);
            assertEquals(testTypingIndicator.getStatus(), KUSTypingStatus.KUS_TYPING);
        } catch (KUSInvalidJsonException ignore) {
        }
    }

    @Test
    public void testTypingIndicatorGetters() {
        //Getter should work as expected
        JSONObject data = KUSJsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("id", "temp-id");
            put("userId", "temp-user");
            put("status", "typing");
        }});

        try {
            KUSTypingIndicator testTypingIndicator = new KUSTypingIndicator(data);
            assertEquals(testTypingIndicator.getId(), "temp-id");
            assertEquals(testTypingIndicator.getUserId(), "temp-user");
            assertEquals(testTypingIndicator.getStatus(), KUSTypingStatus.KUS_TYPING);
        } catch (KUSInvalidJsonException ignore) {
        }
    }

}