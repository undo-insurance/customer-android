package com.kustomer.kustomersdk;

import android.os.Build;

import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Models.KUSCSatisfactionForm;
import com.kustomer.kustomersdk.Models.KUSFormQuestion;
import com.kustomer.kustomersdk.Utils.JsonHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import static com.kustomer.kustomersdk.Enums.KUSCSatisfactionScaleType.KUS_C_SATISFACTION_SCALE_TYPE_NUMBER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class KUSSatisfactionFormTest {

    @Test
    public void testSatisfactionFormInitializer() {

        final JSONArray questionArray = new JSONArray();
        questionArray.put(JsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("id", "fake_question_id");
            put("prompt", "Thank you, Any further details? Share your experience with us?");
            put("type", "response");
        }}));

        final JSONObject scaleJson = JsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("labelHigh", "Extremely Likely");
            put("labelLow", "Not Likely at all");
            put("options", 5);
            put("type", "number");
        }});

        final JSONObject attributes = JsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("questions", questionArray);
            put("ratingPrompt", "How satisfied were you with your interaction?");
            put("scale", scaleJson);
        }});

        JSONObject satisfactionFormJson = JsonHelper.jsonObjectFromHashMap(new HashMap<String, Object>() {{
            put("type", "satisfaction");
            put("id", "fake_satisfaction_id");
            put("attributes", attributes);
        }});

        KUSCSatisfactionForm satisfactionForm = null;
        try {
            satisfactionForm = new KUSCSatisfactionForm(satisfactionFormJson);
        } catch (KUSInvalidJsonException e) {
            Assert.fail("No exception should occur");
        }

        assertNotNull(satisfactionForm);
        assertEquals(satisfactionForm.getRatingPrompt(),
                "How satisfied were you with your interaction?");
        assertEquals(satisfactionForm.getScaleLabelLow(),
                "Not Likely at all");
        assertEquals(satisfactionForm.getScaleLabelHigh(),
                "Extremely Likely");
        assertEquals(satisfactionForm.getScaleType(),
                KUS_C_SATISFACTION_SCALE_TYPE_NUMBER);
        assertEquals(satisfactionForm.getScaleOptionsCount(),
                5);

        KUSFormQuestion formQuestion = satisfactionForm.getQuestions().get(0);
        assertEquals(formQuestion.getPrompt(),
                "Thank you, Any further details? Share your experience with us?");
    }
}
