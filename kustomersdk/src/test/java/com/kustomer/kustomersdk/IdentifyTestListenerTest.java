package com.kustomer.kustomersdk;

import android.os.Build;

import com.kustomer.kustomersdk.API.KUSRequestManager;
import com.kustomer.kustomersdk.API.KUSUserSession;
import com.kustomer.kustomersdk.Enums.KUSRequestType;
import com.kustomer.kustomersdk.Interfaces.KUSRequestCompletionListener;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static com.kustomer.kustomersdk.KustomerTestConstants.KUS_TEST_API_KEY;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class IdentifyTestListenerTest {

    private KUSRequestManager requestManagerSpy;

    @Before
    public void initKustomer() {
        //init Kustomer
        Kustomer.init(RuntimeEnvironment.application.getApplicationContext(), KUS_TEST_API_KEY);
    }

    //region Test

    @Test
    public void testIdentifyWithSecureIdNull() {
        try {
            Kustomer.identify(null, null);
            Assert.fail("Should have thrown Assert Error");
        } catch (AssertionError e) {

        }
    }

    @Test
    public void testIdentifyWithId() {
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

        Kustomer.identify("secure-id", null);

        Mockito.verify(requestManagerSpy, Mockito.times(1)).performRequestType(
                any(KUSRequestType.class),
                any(String.class),
                anyMapOf(String.class, Object.class),
                any(boolean.class),
                any(KUSRequestCompletionListener.class));
    }
}
