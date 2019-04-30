package com.kustomer.kustomersdk;

import android.os.Build;

import com.kustomer.kustomersdk.Interfaces.KUSVolumeControlTimerListener;
import com.kustomer.kustomersdk.Managers.KUSVolumeControlTimerManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class VolumeControlTimerManagerTests {

    //region Test

    @Test
    public void testCreateVolumeControlTimerForSessionId() {
        try {
            Field timerManagerField = KUSVolumeControlTimerManager.class.getDeclaredField("volumeControlTimerManager");
            timerManagerField.setAccessible(true);
            KUSVolumeControlTimerManager vcTimerManager = (KUSVolumeControlTimerManager) timerManagerField.get(null);

            assertFalse(vcTimerManager.hasAnyTimer());

            KUSVolumeControlTimerListener listener = new KUSVolumeControlTimerListener() {
                @Override
                public void onDelayComplete() {

                }
            };

            vcTimerManager.createVolumeControlTimer("temp-session", 1000, listener);

            assertTrue(vcTimerManager.hasAnyTimer());
            assertTrue(vcTimerManager.sessionHasVcTimer("temp-session"));

        } catch (Exception e) {

        }
    }

    @Test
    public void testRemoveVolumeControlTimerForSessionId() {
        try {
            Field timerManagerField = KUSVolumeControlTimerManager.class.getDeclaredField("volumeControlTimerManager");
            timerManagerField.setAccessible(true);
            KUSVolumeControlTimerManager vcTimerManager = (KUSVolumeControlTimerManager) timerManagerField.get(null);

            assertFalse(vcTimerManager.hasAnyTimer());

            KUSVolumeControlTimerListener listener = new KUSVolumeControlTimerListener() {
                @Override
                public void onDelayComplete() {

                }
            };

            vcTimerManager.createVolumeControlTimer("temp-session", 1000, listener);
            assertTrue(vcTimerManager.sessionHasVcTimer("temp-session"));

            vcTimerManager.removeVcTimer("temp-session");
            assertFalse(vcTimerManager.sessionHasVcTimer("temp-session"));

        } catch (Exception e) {

        }
    }

    @Test
    public void testRemoveAllVolumeControlTimers() {
        try {
            Field timerManagerField = KUSVolumeControlTimerManager.class.getDeclaredField("volumeControlTimerManager");
            timerManagerField.setAccessible(true);
            KUSVolumeControlTimerManager vcTimerManager = (KUSVolumeControlTimerManager) timerManagerField.get(null);
            assertTrue(vcTimerManager.hasAnyTimer());

            KUSVolumeControlTimerListener listener = new KUSVolumeControlTimerListener() {
                @Override
                public void onDelayComplete() {

                }
            };

            vcTimerManager.createVolumeControlTimer("temp-session-1", 1000, listener);
            vcTimerManager.createVolumeControlTimer("temp-session-2", 1000, listener);
            vcTimerManager.createVolumeControlTimer("temp-session-3", 1000, listener);
            vcTimerManager.createVolumeControlTimer("temp-session-4", 1000, listener);

            assertTrue(vcTimerManager.hasAnyTimer());

            vcTimerManager.removeVcTimers();
            assertFalse(vcTimerManager.hasAnyTimer());
        } catch (Exception e) {

        }
    }

    //endregion
}
