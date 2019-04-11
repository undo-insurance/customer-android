package com.kustomer.kustomersdk;

import android.os.Build;

import com.kustomer.kustomersdk.API.KUSRequestManager;
import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Enums.KUSRequestType;
import com.kustomer.kustomersdk.Interfaces.KUSIdentifyListener;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;

import org.junit.Assert;
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
import static com.kustomer.kustomersdk.KustomerTestConstants.KUS_TEST_JWT_TOKEN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class IdentifyTestListenerTest {

    private KUSRequestManager requestManagerSpy;

    //region Private Methods
    private Answer getRequestAnswer(){
        return new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();

                HashMap<String, Object> params = ( HashMap<String, Object>)args[2];
                String token = (String) params.get("externalToken");

                if(token != null && token.equals(KUS_TEST_JWT_TOKEN))
                    ((KUSRequestCompletionListener)args[4])
                            .onCompletion(null,null);
                else
                    ((KUSRequestCompletionListener)args[4])
                            .onCompletion(new Error(),null);

                return null;
            }
        };
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

    //region Test

    //Assert Error should be thrown & API request shouldn't be made with null token
    @Test
    public void testIdentifyWithNullToken() {
        try {
            Kustomer.identify(null, null);
            Assert.fail("Should have thrown Assert Error");
        } catch (AssertionError ignore) {
        }

        Mockito.verify(requestManagerSpy, Mockito.times(0)).performRequestType(
                any(KUSRequestType.class),
                any(String.class),
                anyMapOf(String.class, Object.class),
                any(boolean.class),
                any(KUSRequestCompletionListener.class));
    }

    //API request shouldn't be made with empty token
    @Test
    public void testIdentifyWithEmptyToken() {
        Kustomer.identify("", new KUSIdentifyListener() {
            @Override
            public void onComplete(boolean success) {
                assertFalse(success);
            }
        });

        Mockito.verify(requestManagerSpy, Mockito.times(0)).performRequestType(
                any(KUSRequestType.class),
                any(String.class),
                anyMapOf(String.class, Object.class),
                any(boolean.class),
                any(KUSRequestCompletionListener.class));
    }

    //API request should be made with non-empty token
    @Test
    public void testIdentifyWithToken() {
        //Testing invalid token
        Kustomer.identify("invalid token", new KUSIdentifyListener() {
            @Override
            public void onComplete(boolean success) {
                assertFalse(success);
            }
        });

        Mockito.verify(requestManagerSpy, Mockito.times(1)).performRequestType(
                any(KUSRequestType.class),
                any(String.class),
                anyMapOf(String.class, Object.class),
                any(boolean.class),
                any(KUSRequestCompletionListener.class));

        //Testing valid token
        Kustomer.identify(KUS_TEST_JWT_TOKEN, new KUSIdentifyListener() {
            @Override
            public void onComplete(boolean success) {
                assertTrue(success);
            }
        });

        Mockito.verify(requestManagerSpy, Mockito.times(2)).performRequestType(
                any(KUSRequestType.class),
                any(String.class),
                anyMapOf(String.class, Object.class),
                any(boolean.class),
                any(KUSRequestCompletionListener.class));
    }
    //endregion
}
