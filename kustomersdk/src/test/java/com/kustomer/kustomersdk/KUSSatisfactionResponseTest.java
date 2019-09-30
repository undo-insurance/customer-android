package com.kustomer.kustomersdk;

import android.os.Build;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Models.KUSCSatisfactionResponse;
import com.kustomer.kustomersdk.Utils.KUSJsonHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import static com.kustomer.kustomersdk.Enums.KUSCSatisfactionFormResponseStatus.KUS_C_SATISFACTION_RESPONSE_STATUS_RATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class KUSSatisfactionResponseTest {

    @Test
    public void testSatisfactionFormInitializer() {

        final JSONArray answersArray = new JSONArray();
        answersArray.put(KUSJsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("id", "fake_answer_id");
            put("answer", "Nice experience");
        }}));

        final JSONObject attributes = KUSJsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("answers", answersArray);
            put("createdAt", "2019-04-26T13:03:39.511Z");
            put("updatedAt", "2019-04-26T13:03:39.511Z");
            put("lockedAt", "2019-04-26T13:03:39.511Z");
            put("submittedAt", "2019-04-26T13:03:39.511Z");
            put("rating", 2);
            put("status", "rated");
        }});

        JSONObject satisfactionResponseJson = KUSJsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("type", "satisfaction_response");
            put("id", "fake_satisfaction_response_id");
            put("attributes", attributes);
        }});

        KUSCSatisfactionResponse satisfactionResponse = null;
        try {
            satisfactionResponse = new KUSCSatisfactionResponse(satisfactionResponseJson);
        } catch (KUSInvalidJsonException e) {
            Assert.fail("No exception should occur");
        }

        assertNotNull(satisfactionResponse);
        assertNotNull(satisfactionResponse.getCreatedAt());
        assertNotNull(satisfactionResponse.getUpdatedAt());
        assertNotNull(satisfactionResponse.getLockedAt());
        assertNotNull(satisfactionResponse.getSubmittedAt());
        assertEquals(satisfactionResponse.getRating(), 2);
        assertEquals(satisfactionResponse.getStatus(), KUS_C_SATISFACTION_RESPONSE_STATUS_RATED);
        assertEquals(satisfactionResponse.getAnswers().size(), 1);
        assertEquals(satisfactionResponse.getAnswers().get("fake_answer_id"), "Nice experience");
    }
}
