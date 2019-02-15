package com.kustomer.kustomersdk;

import android.os.Build;

import com.kustomer.kustomersdk.API.KUSRequestManager;
import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.DataSources.KUSChatSessionsDataSource;
import com.kustomer.kustomersdk.Enums.KUSRequestType;
import com.kustomer.kustomersdk.Helpers.KUSInvalidJsonException;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;
import com.kustomer.kustomersdk.Models.KUSChatSession;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.HashMap;

import static com.kustomer.kustomersdk.KustomerTestConstants.KUS_TEST_API_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class DescribeConversationTest {

    private KUSRequestManager requestManagerSpy;
    private KUSChatSessionsDataSource chatSessionsDataSourceSpy;

    private HashMap<String, Object> expectedFormData = new HashMap<>();

    private int requestCount = 0;

    //region Private Methods

    private JSONObject getCustomAttrs() {
        final JSONObject customAttributes = new JSONObject();
        try {
            customAttributes.put("ageNum", 22);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return customAttributes;
    }

    private JSONObject getNextCustomAttrs() {
        final JSONObject customAttributes = new JSONObject();
        try {
            customAttributes.put("NameStr", "TestName");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return customAttributes;
    }

    private KUSChatSession getChatSession() {
        JSONObject chatSessionJson = new JSONObject();
        try {
            chatSessionJson.put("id", "1");
            chatSessionJson.put("type", "chat_session");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            return new KUSChatSession(chatSessionJson);
        } catch (KUSInvalidJsonException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Answer getRequestAnswer() {
        return new Answer() {
            public Object answer(InvocationOnMock invocation) {
                //increment requestCount to track method invocation
                requestCount++;

                Object[] args = invocation.getArguments();
                HashMap<String, Object> formData = (HashMap<String, Object>) args[2];
                verifyFormData(formData);

                return null;
            }
        };
    }

    private void verifyFormData(HashMap<String, Object> formData) {
        assertTrue(KustomerTestUtils.mapsAreEqual(expectedFormData, formData));
    }

    //endregion

    @Before
    public void initKustomer() {
        //init Kustomer
        Kustomer.init(RuntimeEnvironment.application.getApplicationContext(), KUS_TEST_API_KEY);

        try {
            Field kusField = Kustomer.class.getDeclaredField("sharedInstance");
            kusField.setAccessible(true);
            Kustomer sharedInstance = (Kustomer) kusField.get(null);

            Field userSessionField = Kustomer.class.getDeclaredField("userSession");
            userSessionField.setAccessible(true);
            KUSUserSession session = (KUSUserSession) userSessionField.get(sharedInstance);

            requestManagerSpy = Mockito.spy(session.getRequestManager());
            Field requestManagerField = KUSUserSession.class.getDeclaredField("requestManager");
            requestManagerField.setAccessible(true);
            requestManagerField.set(session, requestManagerSpy);

            chatSessionsDataSourceSpy = Mockito.spy(session.getChatSessionsDataSource());
            Field chatSessionsDataSourceField = KUSUserSession.class.getDeclaredField("chatSessionsDataSource");
            chatSessionsDataSourceField.setAccessible(true);
            chatSessionsDataSourceField.set(session, chatSessionsDataSourceSpy);

        } catch (Exception e) {
            e.printStackTrace();
        }

        Mockito.doAnswer(getRequestAnswer())
                .when(requestManagerSpy).performRequestType(
                any(KUSRequestType.class),
                any(String.class),
                anyMapOf(String.class, Object.class),
                any(boolean.class),
                any(KUSRequestCompletionListener.class));
    }

    //region Tests

    @Test
    public void testDescribeConversationWhenChatSessionIsNull() {
        Mockito.when(chatSessionsDataSourceSpy.getMostRecentSession())
                .thenReturn(null);

        JSONObject customAttributes = getCustomAttrs();

        Kustomer.describeConversation(customAttributes);
        assertEquals(0, requestCount);
    }

    @Test
    public void testDescribeConversationWhenChatSessionIsNotNull() {
        Mockito.when(chatSessionsDataSourceSpy.getMostRecentSession())
                .thenReturn(getChatSession());

        JSONObject customAttributes = getCustomAttrs();
        expectedFormData.clear();
        expectedFormData.put("custom", customAttributes);

        Kustomer.describeConversation(customAttributes);
        assertEquals(1, requestCount);
    }

    @Test
    public void testDescribeNextConversation() {
        Mockito.when(chatSessionsDataSourceSpy.getMostRecentSession())
                .thenReturn(getChatSession());

        //describe conversation first time
        JSONObject customAttr = getCustomAttrs();
        expectedFormData.clear();
        expectedFormData.put("custom", customAttr);

        Kustomer.describeConversation(customAttr);
        assertEquals(1, requestCount);

        JSONObject nextCustomAttr = getNextCustomAttrs();
        expectedFormData.clear();
        expectedFormData.put("custom", KustomerTestUtils.combineJsonObjects(customAttr, nextCustomAttr));

        Kustomer.describeConversation(nextCustomAttr);
        assertEquals(2, requestCount);
    }

    //endregion

}
